package net.neoforged.neoforge.registries.datamaps;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

@FunctionalInterface
public interface DataMapValueRemover<R, T> {
   Optional<T> remove(T var1, Registry<R> var2, Either<TagKey<R>, ResourceKey<R>> var3, R var4);

   public static class Default<T, R> implements DataMapValueRemover<R, T> {
      public static final DataMapValueRemover.Default<?, ?> INSTANCE = new DataMapValueRemover.Default();

      public static <T, R> DataMapValueRemover.Default<T, R> defaultRemover() {
         return (DataMapValueRemover.Default<T, R>)INSTANCE;
      }

      public static <T, R> Codec<DataMapValueRemover.Default<T, R>> codec() {
         return MapCodec.unitCodec(defaultRemover());
      }

      private Default() {
      }

      @Override
      public Optional<T> remove(T value, Registry<R> registry, Either<TagKey<R>, ResourceKey<R>> source, R object) {
         return Optional.empty();
      }
   }
}
