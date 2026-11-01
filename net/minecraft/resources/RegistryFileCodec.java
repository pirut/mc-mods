package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;

public final class RegistryFileCodec<E> implements Codec<Holder<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<E> elementCodec;
   private final boolean allowInline;

   public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> p_135590_, Codec<E> p_135591_) {
      return create(p_135590_, p_135591_, true);
   }

   public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> p_135593_, Codec<E> p_135594_, boolean p_135595_) {
      return new RegistryFileCodec<>(p_135593_, p_135594_, p_135595_);
   }

   private RegistryFileCodec(ResourceKey<? extends Registry<E>> p_135574_, Codec<E> p_135575_, boolean p_135576_) {
      this.registryKey = p_135574_;
      this.elementCodec = p_135575_;
      this.allowInline = p_135576_;
   }

   public <T> DataResult<T> encode(Holder<E> p_206716_, DynamicOps<T> p_206717_, T p_206718_) {
      if (p_206717_ instanceof RegistryOps<?> $$3) {
         Optional<HolderOwner<E>> $$4 = $$3.owner(this.registryKey);
         if ($$4.isPresent()) {
            if (!p_206716_.canSerializeIn($$4.get())) {
               return DataResult.error(() -> "Element " + p_206716_ + " is not valid in current registry set");
            }

            return (DataResult<T>)p_206716_.unwrap()
               .map(
                  p_206714_ -> ResourceLocation.CODEC.encode(p_206714_.location(), p_206717_, p_206718_),
                  p_206710_ -> this.elementCodec.encode(p_206710_, p_206717_, p_206718_)
               );
         }
      }

      return this.elementCodec.encode(p_206716_.value(), p_206717_, p_206718_);
   }

   public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> p_135608_, T p_135609_) {
      if (p_135608_ instanceof RegistryOps<?> $$2) {
         Optional<HolderGetter<E>> $$3 = $$2.getter(this.registryKey);
         if ($$3.isEmpty()) {
            return DataResult.error(() -> "Registry does not exist: " + this.registryKey);
         } else {
            HolderGetter<E> $$4 = $$3.get();
            DataResult<Pair<ResourceLocation, T>> $$5 = ResourceLocation.CODEC.decode(p_135608_, p_135609_);
            if ($$5.result().isEmpty()) {
               return !this.allowInline
                  ? DataResult.error(() -> "Inline definitions not allowed here")
                  : this.elementCodec.decode(p_135608_, p_135609_).map(p_206720_ -> p_206720_.mapFirst(Holder::direct));
            } else {
               Pair<ResourceLocation, T> $$6 = (Pair<ResourceLocation, T>)$$5.result().get();
               ResourceKey<E> $$7 = ResourceKey.create(this.registryKey, (ResourceLocation)$$6.getFirst());
               return $$4.get($$7)
                  .<DataResult>map(DataResult::success)
                  .orElseGet(() -> DataResult.error(() -> "Failed to get element " + $$7))
                  .map(p_255658_ -> Pair.of(p_255658_, $$6.getSecond()))
                  .setLifecycle(Lifecycle.stable());
            }
         }
      } else {
         return this.elementCodec.decode(p_135608_, p_135609_).map(p_214212_ -> p_214212_.mapFirst(Holder::direct));
      }
   }

   @Override
   public String toString() {
      return "RegistryFileCodec[" + this.registryKey + " " + this.elementCodec + "]";
   }
}
