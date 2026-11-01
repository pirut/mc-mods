package net.minecraft.server;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import org.slf4j.Logger;

public class ReloadableServerRegistries {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final RegistrationInfo DEFAULT_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());

   public static CompletableFuture<ReloadableServerRegistries.LoadResult> reload(
      LayeredRegistryAccess<RegistryLayer> p_335950_, List<Registry.PendingTags<?>> p_363295_, ResourceManager p_335786_, Executor p_335516_
   ) {
      List<HolderLookup.RegistryLookup<?>> list = TagLoader.buildUpdatedLookups(p_335950_.getAccessForLoading(RegistryLayer.RELOADABLE), p_363295_);
      HolderLookup.Provider holderlookup$provider = HolderLookup.Provider.create(list.stream());
      RegistryOps<JsonElement> registryops = holderlookup$provider.createSerializationContext(JsonOps.INSTANCE);
      List<CompletableFuture<WritableRegistry<?>>> list1 = LootDataType.values()
         .map(p_359506_ -> scheduleRegistryLoad((LootDataType<?>)p_359506_, registryops, p_335786_, p_335516_))
         .toList();
      CompletableFuture<List<WritableRegistry<?>>> completablefuture = Util.sequence(list1);
      return completablefuture.thenApplyAsync(
         p_359496_ -> createAndValidateFullContext(p_335950_, holderlookup$provider, (List<WritableRegistry<?>>)p_359496_), p_335516_
      );
   }

   private static <T> CompletableFuture<WritableRegistry<?>> scheduleRegistryLoad(
      LootDataType<T> p_335741_, RegistryOps<JsonElement> p_336173_, ResourceManager p_335893_, Executor p_336104_
   ) {
      return CompletableFuture.supplyAsync(
         () -> {
            WritableRegistry<T> writableregistry = new MappedRegistry<>(p_335741_.registryKey(), Lifecycle.experimental());
            Map<Identifier, T> map = new HashMap<>();
            HolderLookup.Provider provider = CommonHooks.extractLookupProvider(p_336173_);
            Map<Identifier, Optional<T>> optionalMap = new HashMap<>();
            SimpleJsonResourceReloadListener.scanDirectoryWithOptionalValues(
               p_335893_, p_335741_.registryKey(), p_336173_, p_335741_.conditionalCodec(), optionalMap
            );
            optionalMap.forEach((rl, optionalEntry) -> {
               optionalEntry.ifPresent(entry -> p_335741_.idSetter().accept((T)entry, rl));
               T value = optionalEntry.orElse(p_335741_.defaultValue());
               if (value instanceof LootTable lootTable) {
                  value = (T)EventHooks.loadLootTable(provider, rl, lootTable);
               }

               if (value != null) {
                  map.put(rl, value);
               }
            });
            map.forEach(
               (p_466168_, p_466169_) -> writableregistry.register(
                  ResourceKey.create(p_335741_.registryKey(), p_466168_), (T)p_466169_, DEFAULT_REGISTRATION_INFO
               )
            );
            TagLoader.loadTagsForRegistry(p_335893_, writableregistry);
            return writableregistry;
         },
         p_336104_
      );
   }

   private static ReloadableServerRegistries.LoadResult createAndValidateFullContext(
      LayeredRegistryAccess<RegistryLayer> p_362306_, HolderLookup.Provider p_361687_, List<WritableRegistry<?>> p_361223_
   ) {
      LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = createUpdatedRegistries(p_362306_, p_361223_);
      HolderLookup.Provider holderlookup$provider = concatenateLookups(p_361687_, layeredregistryaccess.getLayer(RegistryLayer.RELOADABLE));
      validateLootRegistries(holderlookup$provider);
      return new ReloadableServerRegistries.LoadResult(layeredregistryaccess, holderlookup$provider);
   }

   private static HolderLookup.Provider concatenateLookups(HolderLookup.Provider p_363853_, HolderLookup.Provider p_363956_) {
      return HolderLookup.Provider.create(Stream.concat(p_363853_.listRegistries(), p_363956_.listRegistries()));
   }

   private static void validateLootRegistries(HolderLookup.Provider p_363971_) {
      ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
      ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, p_363971_);
      LootDataType.values().forEach(p_359499_ -> validateRegistry(validationcontext, (LootDataType<?>)p_359499_, p_363971_));
      problemreporter$collector.forEach(
         (p_421316_, p_421317_) -> LOGGER.warn("Found loot table element validation problem in {}: {}", p_421316_, p_421317_.description())
      );
   }

   private static LayeredRegistryAccess<RegistryLayer> createUpdatedRegistries(
      LayeredRegistryAccess<RegistryLayer> p_335434_, List<WritableRegistry<?>> p_336097_
   ) {
      return p_335434_.replaceFrom(RegistryLayer.RELOADABLE, new RegistryAccess.ImmutableRegistryAccess(p_336097_).freeze());
   }

   private static <T> void validateRegistry(ValidationContext p_335565_, LootDataType<T> p_335997_, HolderLookup.Provider p_360819_) {
      HolderLookup<T> holderlookup = p_360819_.lookupOrThrow(p_335997_.registryKey());
      holderlookup.listElements().forEach(p_335842_ -> p_335997_.runValidation(p_335565_, p_335842_.key(), p_335842_.value()));
   }

   public static class Holder {
      private final HolderLookup.Provider registries;

      public Holder(HolderLookup.Provider p_361288_) {
         this.registries = p_361288_;
      }

      public HolderLookup.Provider lookup() {
         return this.registries;
      }

      public LootTable getLootTable(ResourceKey<LootTable> p_335504_) {
         return this.registries
            .lookup(Registries.LOOT_TABLE)
            .flatMap(p_335799_ -> p_335799_.get(p_335504_))
            .map(net.minecraft.core.Holder::value)
            .orElse(LootTable.EMPTY);
      }
   }

   public record LoadResult(LayeredRegistryAccess<RegistryLayer> layers, HolderLookup.Provider lookupWithUpdatedTags) {
   }
}
