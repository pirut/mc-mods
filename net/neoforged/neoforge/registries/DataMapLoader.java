package net.neoforged.neoforge.registries;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.io.Reader;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.registries.datamaps.AdvancedDataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapEntry;
import net.neoforged.neoforge.registries.datamaps.DataMapFile;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapValueMerger;
import net.neoforged.neoforge.registries.datamaps.DataMapValueRemover;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.slf4j.Logger;

public class DataMapLoader extends ContextAwareReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String PATH = "data_maps";
   private Map<ResourceKey<? extends Registry<?>>, DataMapLoader.LoadResult<?>> results;
   private final RegistryAccess registryAccess;

   @Deprecated(
      forRemoval = true,
      since = "1.21.1"
   )
   public DataMapLoader(ICondition.IContext conditionContext, RegistryAccess registryAccess) {
      this(registryAccess);
   }

   public DataMapLoader(RegistryAccess registryAccess) {
      this.registryAccess = registryAccess;
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier preparationBarrier,
      ResourceManager resourceManager,
      ProfilerFiller preparationsProfiler,
      ProfilerFiller reloadProfiler,
      Executor backgroundExecutor,
      Executor gameExecutor
   ) {
      return this.load(resourceManager, backgroundExecutor, preparationsProfiler)
         .thenCompose(preparationBarrier::wait)
         .thenAcceptAsync(values -> this.results = (Map<ResourceKey<? extends Registry<?>>, DataMapLoader.LoadResult<?>>)values, gameExecutor);
   }

   public void apply() {
      this.results
         .forEach((key, result) -> this.apply((BaseMappedRegistry)this.registryAccess.registryOrThrow((ResourceKey<? extends Registry<?>>)key), result));
      this.results = null;
   }

   private <T> void apply(BaseMappedRegistry<T> registry, DataMapLoader.LoadResult<T> result) {
      registry.dataMaps.clear();
      result.results()
         .forEach(
            (key, entries) -> registry.dataMaps
               .put((DataMapType<T, ?>)key, this.buildDataMap(registry, (DataMapType<T, ?>)key, (List<DataMapFile<?, T>>)entries))
         );
      NeoForge.EVENT_BUS.post(new DataMapsUpdatedEvent(this.registryAccess, registry, DataMapsUpdatedEvent.UpdateCause.SERVER_RELOAD));
   }

   private <T, R> Map<ResourceKey<R>, T> buildDataMap(Registry<R> registry, DataMapType<R, T> attachment, List<DataMapFile<T, R>> entries) {
      record WithSource<T, R>(T attachment, Either<TagKey<R>, ResourceKey<R>> source) {
      }

      Map<ResourceKey<R>, WithSource<T, R>> result = new IdentityHashMap<>();
      DataMapValueMerger<R, T> merger = attachment instanceof AdvancedDataMapType<R, T, ?> adv ? adv.merger() : DataMapValueMerger.defaultMerger();
      entries.forEach(entry -> {
         if (entry.replace()) {
            result.clear();
         }

         entry.values().forEach((tKey, value) -> {
            if (!value.isEmpty()) {
               this.resolve(registry, (Either<TagKey<R>, ResourceKey<R>>)tKey, true, holder -> {
                  DataMapEntry<T> newValue = (DataMapEntry<T>)((WithConditions)value.get()).carrier();
                  ResourceKey<R> key = holder.getKey();
                  WithSource<T, R> oldValue = result.get(key);
                  if (oldValue != null && !newValue.replace()) {
                     result.put(key, new WithSource<>(merger.merge(registry, oldValue.source(), oldValue.attachment(), tKey, newValue.value()), tKey));
                  } else {
                     result.put(key, new WithSource<>(newValue.value(), tKey));
                  }
               });
            }
         });

         for (DataMapEntry.Removal<T, R> removal : entry.removals()) {
            if (removal.remover().isPresent()) {
               DataMapValueRemover<R, T> remover = removal.remover().orElseThrow();
               this.resolve(registry, removal.key(), false, holder -> {
                  ResourceKey<R> key = holder.getKey();
                  WithSource<T, R> oldValue = result.get(key);
                  if (oldValue != null) {
                     Optional<T> newValue = remover.remove(oldValue.attachment(), registry, oldValue.source(), holder.value());
                     if (newValue.isEmpty()) {
                        result.remove(key);
                     } else {
                        result.put(key, new WithSource<>(newValue.get(), oldValue.source()));
                     }
                  }
               });
            } else {
               this.resolve(registry, removal.key(), false, holder -> result.remove(holder.getKey()));
            }
         }
      });
      Map<ResourceKey<R>, T> newMap = new IdentityHashMap<>();
      result.forEach((key, val) -> newMap.put((ResourceKey<R>)key, val.attachment()));
      return newMap;
   }

   private <R> void resolve(Registry<R> registry, Either<TagKey<R>, ResourceKey<R>> value, boolean required, Consumer<Holder<R>> consumer) {
      if (value.left().isPresent()) {
         registry.getTagOrEmpty((TagKey<R>)value.left().orElseThrow()).forEach(consumer);
      } else {
         Optional<Holder.Reference<R>> object = registry.getHolder((ResourceKey<R>)value.right().orElseThrow());
         if (object.isPresent()) {
            consumer.accept(object.get());
         } else if (required) {
            LOGGER.error(
               "Object with ID {} specified in data map for registry {} doesn't exist",
               ((ResourceKey)value.right().orElseThrow()).location(),
               registry.key().location()
            );
         }
      }
   }

   private CompletableFuture<Map<ResourceKey<? extends Registry<?>>, DataMapLoader.LoadResult<?>>> load(
      ResourceManager manager, Executor executor, ProfilerFiller profiler
   ) {
      return CompletableFuture.supplyAsync(() -> this.load(manager, profiler), executor);
   }

   private Map<ResourceKey<? extends Registry<?>>, DataMapLoader.LoadResult<?>> load(ResourceManager manager, ProfilerFiller profiler) {
      RegistryOps<JsonElement> ops = this.makeConditionalOps();
      Map<ResourceKey<? extends Registry<?>>, DataMapLoader.LoadResult<?>> values = new HashMap<>();
      this.getRegistryLookup()
         .listRegistries()
         .forEach(
            registryKey -> {
               profiler.push("registry_data_maps/" + registryKey.location() + "/locating");
               FileToIdConverter fileToId = FileToIdConverter.json("data_maps/" + getFolderLocation(registryKey.location()));

               for (Entry<ResourceLocation, List<Resource>> entry : fileToId.listMatchingResourceStacks(manager).entrySet()) {
                  ResourceLocation key = entry.getKey();
                  ResourceLocation attachmentId = fileToId.fileToId(key);
                  DataMapType attachment = RegistryManager.getDataMap((ResourceKey<? extends Registry<?>>)registryKey, attachmentId);
                  if (attachment == null) {
                     LOGGER.warn("Found data map file for non-existent data map type '{}' on registry '{}'.", attachmentId, registryKey.location());
                  } else {
                     profiler.popPush("registry_data_maps/" + registryKey.location() + "/" + attachmentId + "/loading");
                     values.computeIfAbsent((ResourceKey<? extends Registry<?>>)registryKey, k -> new DataMapLoader.LoadResult(new HashMap()))
                        .results
                        .put(attachment, readData(ops, attachment, (ResourceKey<Registry<?>>)registryKey, entry.getValue()));
                  }
               }

               profiler.pop();
            }
         );
      return values;
   }

   public static String getFolderLocation(ResourceLocation registryId) {
      return (registryId.getNamespace().equals("minecraft") ? "" : registryId.getNamespace() + "/") + registryId.getPath();
   }

   private static <A, T> List<DataMapFile<A, T>> readData(
      RegistryOps<JsonElement> ops, DataMapType<T, A> attachmentType, ResourceKey<Registry<T>> registryKey, List<Resource> resources
   ) {
      Codec<DataMapFile<A, T>> codec = DataMapFile.codec(registryKey, attachmentType);
      List<DataMapFile<A, T>> entries = new LinkedList<>();

      for (Resource resource : resources) {
         try (Reader reader = resource.openAsReader()) {
            JsonElement jsonelement = JsonParser.parseReader(reader);
            entries.add((DataMapFile<A, T>)((Pair)codec.decode(ops, jsonelement).getOrThrow()).getFirst());
         } catch (Exception var13) {
            LOGGER.error("Could not read data map of type {} for registry {}", new Object[]{attachmentType.id(), registryKey, var13});
         }
      }

      return entries;
   }

   private record LoadResult<T>(Map<DataMapType<T, ?>, List<DataMapFile<?, T>>> results) {
   }
}
