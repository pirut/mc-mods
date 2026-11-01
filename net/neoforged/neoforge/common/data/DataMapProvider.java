package net.neoforged.neoforge.common.data;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.registries.DataMapLoader;
import net.neoforged.neoforge.registries.datamaps.AdvancedDataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapEntry;
import net.neoforged.neoforge.registries.datamaps.DataMapFile;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapValueRemover;

public abstract class DataMapProvider implements DataProvider {
   protected final CompletableFuture<HolderLookup.Provider> lookupProvider;
   protected final PackOutput.PathProvider pathProvider;
   private final Map<DataMapType<?, ?>, DataMapProvider.Builder<?, ?>> builders = new HashMap<>();

   protected DataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
      this.lookupProvider = lookupProvider;
      this.pathProvider = packOutput.createPathProvider(PackOutput.Target.DATA_PACK, "data_maps");
   }

   @Override
   public CompletableFuture<?> run(CachedOutput cache) {
      return this.lookupProvider.thenCompose(provider -> {
         this.gather(provider);
         DynamicOps<JsonElement> dynamicOps = provider.createSerializationContext(JsonOps.INSTANCE);
         return CompletableFuture.allOf(this.builders.entrySet().stream().map(entry -> {
            DataMapType<?, ?> type = entry.getKey();
            Path path = this.pathProvider.json(type.id().withPrefix(DataMapLoader.getFolderLocation(type.registryKey().location()) + "/"));
            return this.generate(path, cache, entry.getValue(), dynamicOps);
         }).toArray(CompletableFuture[]::new));
      });
   }

   private <T, R> CompletableFuture<?> generate(Path out, CachedOutput cache, DataMapProvider.Builder<T, R> builder, DynamicOps<JsonElement> ops) {
      return CompletableFuture.<JsonElement>supplyAsync(
            () -> {
               Codec<Optional<WithConditions<DataMapFile<T, R>>>> withConditionsCodec = ConditionalOps.createConditionalCodecWithConditions(
                  DataMapFile.codec(builder.registryKey, builder.type)
               );
               return (JsonElement)withConditionsCodec.encodeStart(ops, Optional.of(builder.build()))
                  .getOrThrow(msg -> new RuntimeException("Failed to encode %s: %s".formatted(out, msg)));
            }
         )
         .thenComposeAsync(encoded -> DataProvider.saveStable(cache, encoded, out));
   }

   @Deprecated(
      forRemoval = true
   )
   protected void gather() {
   }

   protected void gather(HolderLookup.Provider provider) {
      this.gather();
   }

   public <T, R> DataMapProvider.Builder<T, R> builder(DataMapType<R, T> type) {
      return (DataMapProvider.Builder<T, R>)(type instanceof AdvancedDataMapType<R, T, ?> advanced
         ? this.builder(advanced)
         : (DataMapProvider.Builder)this.builders.computeIfAbsent(type, k -> new DataMapProvider.Builder<>(type)));
   }

   public <T, R, VR extends DataMapValueRemover<R, T>> DataMapProvider.AdvancedBuilder<T, R, VR> builder(AdvancedDataMapType<R, T, VR> type) {
      return (DataMapProvider.AdvancedBuilder<T, R, VR>)this.builders.computeIfAbsent(type, k -> new DataMapProvider.AdvancedBuilder<>(type));
   }

   @Override
   public String getName() {
      return "Data Maps";
   }

   public static class AdvancedBuilder<T, R, VR extends DataMapValueRemover<R, T>> extends DataMapProvider.Builder<T, R> {
      public AdvancedBuilder(AdvancedDataMapType<R, T, VR> type) {
         super(type);
      }

      public DataMapProvider.AdvancedBuilder<T, R, VR> remove(TagKey<R> tag, VR remover) {
         this.removals.add(new DataMapEntry.Removal<>(Either.left(tag), Optional.of(remover)));
         return this;
      }

      public DataMapProvider.AdvancedBuilder<T, R, VR> remove(Holder<R> value, VR remover) {
         this.removals.add(new DataMapEntry.Removal<>(Either.right((ResourceKey)value.unwrap().orThrow()), Optional.of(remover)));
         return this;
      }

      public DataMapProvider.AdvancedBuilder<T, R, VR> remove(ResourceLocation id, VR remover) {
         this.removals.add(new DataMapEntry.Removal<>(Either.right(ResourceKey.create(this.registryKey, id)), Optional.of(remover)));
         return this;
      }
   }

   public static class Builder<T, R> {
      private final Map<Either<TagKey<R>, ResourceKey<R>>, Optional<WithConditions<DataMapEntry<T>>>> values = new LinkedHashMap<>();
      protected final List<DataMapEntry.Removal<T, R>> removals = new ArrayList<>();
      protected final ResourceKey<Registry<R>> registryKey;
      private final DataMapType<R, T> type;
      private final List<ICondition> conditions = new ArrayList<>();
      private boolean replace;

      public Builder(DataMapType<R, T> type) {
         this.type = type;
         this.registryKey = type.registryKey();
      }

      public DataMapProvider.Builder<T, R> add(ResourceKey<R> key, T value, boolean replace, ICondition... conditions) {
         this.values.put(Either.right(key), Optional.of(new WithConditions<>(new DataMapEntry<>(value, replace), conditions)));
         return this;
      }

      public DataMapProvider.Builder<T, R> add(ResourceLocation id, T value, boolean replace, ICondition... conditions) {
         return this.add(ResourceKey.create(this.registryKey, id), value, replace, conditions);
      }

      public DataMapProvider.Builder<T, R> add(Holder<R> object, T value, boolean replace, ICondition... conditions) {
         return this.add(object.unwrapKey().orElseThrow(), value, replace, conditions);
      }

      public DataMapProvider.Builder<T, R> add(TagKey<R> tag, T value, boolean replace, ICondition... conditions) {
         this.values.put(Either.left(tag), Optional.of(new WithConditions<>(new DataMapEntry<>(value, replace), conditions)));
         return this;
      }

      public DataMapProvider.Builder<T, R> remove(ResourceLocation id) {
         this.removals.add(new DataMapEntry.Removal<>(Either.right(ResourceKey.create(this.registryKey, id)), Optional.empty()));
         return this;
      }

      public DataMapProvider.Builder<T, R> remove(TagKey<R> tag) {
         this.removals.add(new DataMapEntry.Removal<>(Either.left(tag), Optional.empty()));
         return this;
      }

      public DataMapProvider.Builder<T, R> remove(Holder<R> value) {
         this.removals.add(new DataMapEntry.Removal<>(Either.right((ResourceKey)value.unwrap().orThrow()), Optional.empty()));
         return this;
      }

      public DataMapProvider.Builder<T, R> replace(boolean replace) {
         this.replace = replace;
         return this;
      }

      public DataMapProvider.Builder<T, R> conditions(ICondition... conditions) {
         Collections.addAll(this.conditions, conditions);
         return this;
      }

      public WithConditions<DataMapFile<T, R>> build() {
         return new WithConditions<>(this.conditions, new DataMapFile<>(this.replace, this.values, this.removals));
      }
   }
}
