package net.neoforged.neoforge.registries.datamaps;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

public final class AdvancedDataMapType<R, T, VR extends DataMapValueRemover<R, T>> extends DataMapType<R, T> {
   private final Codec<VR> remover;
   private final DataMapValueMerger<R, T> merger;

   private AdvancedDataMapType(
      ResourceKey<Registry<R>> registryKey,
      Identifier id,
      Codec<T> codec,
      @Nullable Codec<T> networkCodec,
      boolean mandatorySync,
      Codec<VR> remover,
      DataMapValueMerger<R, T> merger
   ) {
      super(registryKey, id, codec, networkCodec, mandatorySync);
      this.remover = Objects.requireNonNull(remover, "remover must not be null");
      this.merger = Objects.requireNonNull(merger, "merger must not be null");
   }

   public Codec<VR> remover() {
      return this.remover;
   }

   public DataMapValueMerger<R, T> merger() {
      return this.merger;
   }

   public static <T, R> AdvancedDataMapType.Builder<T, R, DataMapValueRemover.Default<T, R>> builder(
      Identifier id, ResourceKey<Registry<R>> registry, Codec<T> codec
   ) {
      return new AdvancedDataMapType.Builder<T, R, VR>(registry, id, codec).remover(DataMapValueRemover.Default.codec());
   }

   public static final class Builder<T, R, VR extends DataMapValueRemover<R, T>> extends DataMapType.Builder<T, R> {
      private Codec<VR> remover;
      private DataMapValueMerger<R, T> merger = DataMapValueMerger.defaultMerger();

      Builder(ResourceKey<Registry<R>> registryKey, Identifier id, Codec<T> codec) {
         super(registryKey, id, codec);
      }

      public <VR1 extends DataMapValueRemover<R, T>> AdvancedDataMapType.Builder<T, R, VR1> remover(Codec<VR1> remover) {
         this.remover = remover;
         return this;
      }

      public AdvancedDataMapType.Builder<T, R, VR> merger(DataMapValueMerger<R, T> merger) {
         this.merger = merger;
         return this;
      }

      public AdvancedDataMapType.Builder<T, R, VR> synced(Codec<T> networkCodec, boolean mandatory) {
         super.synced(networkCodec, mandatory);
         return this;
      }

      public AdvancedDataMapType<R, T, VR> build() {
         return new AdvancedDataMapType<>(this.registryKey, this.id, this.codec, this.networkCodec, this.mandatorySync, this.remover, this.merger);
      }
   }
}
