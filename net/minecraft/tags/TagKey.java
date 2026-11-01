package net.minecraft.tags;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public record TagKey<T>(ResourceKey<? extends Registry<T>> registry, Identifier location) {
   private static final Interner<TagKey<?>> VALUES = Interners.newWeakInterner();

   @Deprecated
   public TagKey(ResourceKey<? extends Registry<T>> registry, Identifier location) {
      this.registry = registry;
      this.location = location;
   }

   public static <T> Codec<TagKey<T>> codec(ResourceKey<? extends Registry<T>> p_203878_) {
      return Identifier.CODEC.xmap(p_466402_ -> create(p_203878_, p_466402_), TagKey::location);
   }

   public static <T> Codec<TagKey<T>> hashedCodec(ResourceKey<? extends Registry<T>> p_203887_) {
      return Codec.STRING
         .comapFlatMap(
            p_466400_ -> p_466400_.startsWith("#")
               ? Identifier.read(p_466400_.substring(1)).map(p_466404_ -> create(p_203887_, p_466404_))
               : DataResult.error(() -> "Not a tag id"),
            p_466405_ -> "#" + p_466405_.location
         );
   }

   public static <T> StreamCodec<ByteBuf, TagKey<T>> streamCodec(ResourceKey<? extends Registry<T>> p_372956_) {
      return Identifier.STREAM_CODEC.map(p_466407_ -> create(p_372956_, p_466407_), TagKey::location);
   }

   public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> p_203883_, Identifier p_469959_) {
      return (TagKey<T>)VALUES.intern(new TagKey<>(p_203883_, p_469959_));
   }

   public boolean isFor(ResourceKey<? extends Registry<?>> p_207646_) {
      return this.registry == p_207646_;
   }

   public <E> Optional<TagKey<E>> cast(ResourceKey<? extends Registry<E>> p_207648_) {
      return this.isFor(p_207648_) ? Optional.of((TagKey<E>)this) : Optional.empty();
   }

   @Override
   public String toString() {
      return "TagKey[" + this.registry.identifier() + " / " + this.location + "]";
   }
}
