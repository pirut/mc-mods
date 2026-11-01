package net.minecraft.world.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record EitherHolder<T>(Either<Holder<T>, ResourceKey<T>> contents) {
   public EitherHolder(Holder<T> p_350710_) {
      this(Either.left(p_350710_));
   }

   public EitherHolder(ResourceKey<T> p_350883_) {
      this(Either.right(p_350883_));
   }

   public static <T> Codec<EitherHolder<T>> codec(ResourceKey<Registry<T>> p_350627_, Codec<Holder<T>> p_350442_) {
      return Codec.either(
            p_350442_,
            ResourceKey.codec(p_350627_).comapFlatMap(p_350331_ -> DataResult.error(() -> "Cannot parse as key without registry"), Function.identity())
         )
         .xmap(EitherHolder::new, EitherHolder::contents);
   }

   public static <T> StreamCodec<RegistryFriendlyByteBuf, EitherHolder<T>> streamCodec(
      ResourceKey<Registry<T>> p_350628_, StreamCodec<RegistryFriendlyByteBuf, Holder<T>> p_350420_
   ) {
      return StreamCodec.composite(ByteBufCodecs.either(p_350420_, ResourceKey.streamCodec(p_350628_)), EitherHolder::contents, EitherHolder::new);
   }

   public Optional<T> unwrap(Registry<T> p_350328_) {
      return (Optional<T>)this.contents.map(p_400939_ -> Optional.of(p_400939_.value()), p_350328_::getOptional);
   }

   public Optional<Holder<T>> unwrap(HolderLookup.Provider p_350642_) {
      return (Optional<Holder<T>>)this.contents.map(Optional::of, p_400937_ -> p_350642_.get(p_400937_).map(p_400938_ -> (Holder)p_400938_));
   }

   public Optional<ResourceKey<T>> key() {
      return (Optional<ResourceKey<T>>)this.contents.map(Holder::unwrapKey, Optional::of);
   }
}
