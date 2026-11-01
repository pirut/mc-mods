package net.minecraft.world.attribute;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public final class EnvironmentAttributeMap {
   public static final EnvironmentAttributeMap EMPTY = new EnvironmentAttributeMap(Map.of());
   public static final Codec<EnvironmentAttributeMap> CODEC = Codec.lazyInitialized(
      () -> Codec.dispatchedMap(EnvironmentAttributes.CODEC, Util.memoize(EnvironmentAttributeMap.Entry::createCodec))
         .xmap(EnvironmentAttributeMap::new, p_457769_ -> p_457769_.entries)
   );
   public static final Codec<EnvironmentAttributeMap> NETWORK_CODEC = CODEC.xmap(
      EnvironmentAttributeMap::filterSyncable, EnvironmentAttributeMap::filterSyncable
   );
   public static final Codec<EnvironmentAttributeMap> CODEC_ONLY_POSITIONAL = CODEC.validate(p_457772_ -> {
      List<EnvironmentAttribute<?>> list = p_457772_.keySet().stream().filter(p_457601_ -> !p_457601_.isPositional()).toList();
      return !list.isEmpty() ? DataResult.error(() -> "The following attributes cannot be positional: " + list) : DataResult.success(p_457772_);
   });
   final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries;

   private static EnvironmentAttributeMap filterSyncable(EnvironmentAttributeMap p_457829_) {
      return new EnvironmentAttributeMap(Map.copyOf(Maps.filterKeys(p_457829_.entries, EnvironmentAttribute::isSyncable)));
   }

   EnvironmentAttributeMap(Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> p_457517_) {
      this.entries = p_457517_;
   }

   public static EnvironmentAttributeMap.Builder builder() {
      return new EnvironmentAttributeMap.Builder();
   }

   @Nullable
   public <Value> EnvironmentAttributeMap.Entry<Value, ?> get(EnvironmentAttribute<Value> p_457760_) {
      return (EnvironmentAttributeMap.Entry<Value, ?>)this.entries.get(p_457760_);
   }

   public <Value> Value applyModifier(EnvironmentAttribute<Value> p_458288_, Value p_458136_) {
      EnvironmentAttributeMap.Entry<Value, ?> entry = this.get(p_458288_);
      return entry != null ? entry.applyModifier(p_458136_) : p_458136_;
   }

   public boolean contains(EnvironmentAttribute<?> p_457767_) {
      return this.entries.containsKey(p_457767_);
   }

   public Set<EnvironmentAttribute<?>> keySet() {
      return this.entries.keySet();
   }

   @Override
   public boolean equals(Object p_458019_) {
      return p_458019_ == this
         ? true
         : p_458019_ instanceof EnvironmentAttributeMap environmentattributemap && this.entries.equals(environmentattributemap.entries);
   }

   @Override
   public int hashCode() {
      return this.entries.hashCode();
   }

   @Override
   public String toString() {
      return this.entries.toString();
   }

   public static class Builder {
      private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries = new HashMap<>();

      Builder() {
      }

      public EnvironmentAttributeMap.Builder putAll(EnvironmentAttributeMap p_458159_) {
         this.entries.putAll(p_458159_.entries);
         return this;
      }

      public <Value, Parameter> EnvironmentAttributeMap.Builder modify(
         EnvironmentAttribute<Value> p_458048_, AttributeModifier<Value, Parameter> p_457718_, Parameter p_458208_
      ) {
         p_458048_.type().checkAllowedModifier(p_457718_);
         this.entries.put(p_458048_, new EnvironmentAttributeMap.Entry<>(p_458208_, p_457718_));
         return this;
      }

      public <Value> EnvironmentAttributeMap.Builder set(EnvironmentAttribute<Value> p_458169_, Value p_458070_) {
         return this.modify(p_458169_, AttributeModifier.override(), p_458070_);
      }

      public EnvironmentAttributeMap build() {
         return this.entries.isEmpty() ? EnvironmentAttributeMap.EMPTY : new EnvironmentAttributeMap(Map.copyOf(this.entries));
      }
   }

   public record Entry<Value, Argument>(Argument argument, AttributeModifier<Value, Argument> modifier) {
      private static <Value> Codec<EnvironmentAttributeMap.Entry<Value, ?>> createCodec(EnvironmentAttribute<Value> p_458152_) {
         Codec<EnvironmentAttributeMap.Entry<Value, ?>> codec = p_458152_.type()
            .modifierCodec()
            .dispatch("modifier", EnvironmentAttributeMap.Entry::modifier, Util.memoize(p_457747_ -> createFullCodec(p_458152_, p_457747_)));
         return Codec.either(p_458152_.valueCodec(), codec)
            .xmap(
               p_457896_ -> (EnvironmentAttributeMap.Entry)p_457896_.map(
                  p_457523_ -> new EnvironmentAttributeMap.Entry<>(p_457523_, AttributeModifier.override()), p_457679_ -> p_457679_
               ),
               p_457999_ -> p_457999_.modifier == AttributeModifier.override() ? Either.left(p_457999_.argument()) : Either.right(p_457999_)
            );
      }

      private static <Value, Argument> MapCodec<EnvironmentAttributeMap.Entry<Value, Argument>> createFullCodec(
         EnvironmentAttribute<Value> p_458001_, AttributeModifier<Value, Argument> p_458021_
      ) {
         return RecordCodecBuilder.mapCodec(
            p_458215_ -> p_458215_.group(p_458021_.argumentCodec(p_458001_).fieldOf("argument").forGetter(EnvironmentAttributeMap.Entry::argument))
               .apply(p_458215_, p_458234_ -> new EnvironmentAttributeMap.Entry<>(p_458234_, p_458021_))
         );
      }

      public Value applyModifier(Value p_458218_) {
         return this.modifier.apply(p_458218_, this.argument);
      }
   }
}
