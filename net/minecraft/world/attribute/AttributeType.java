package net.minecraft.world.attribute;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public record AttributeType<Value>(
   Codec<Value> valueCodec,
   Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary,
   Codec<AttributeModifier<Value, ?>> modifierCodec,
   LerpFunction<Value> keyframeLerp,
   LerpFunction<Value> stateChangeLerp,
   LerpFunction<Value> spatialLerp,
   LerpFunction<Value> partialTickLerp
) {
   public static <Value> AttributeType<Value> ofInterpolated(
      Codec<Value> p_457697_, Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> p_457874_, LerpFunction<Value> p_457635_
   ) {
      return ofInterpolated(p_457697_, p_457874_, p_457635_, p_457635_);
   }

   public static <Value> AttributeType<Value> ofInterpolated(
      Codec<Value> p_458094_,
      Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> p_457849_,
      LerpFunction<Value> p_458077_,
      LerpFunction<Value> p_458090_
   ) {
      return new AttributeType<>(p_458094_, p_457849_, createModifierCodec(p_457849_), p_458077_, p_458077_, p_458077_, p_458090_);
   }

   public static <Value> AttributeType<Value> ofNotInterpolated(
      Codec<Value> p_457940_, Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> p_457825_
   ) {
      return new AttributeType<>(
         p_457940_,
         p_457825_,
         createModifierCodec(p_457825_),
         LerpFunction.ofStep(1.0F),
         LerpFunction.ofStep(0.0F),
         LerpFunction.ofStep(0.5F),
         LerpFunction.ofStep(0.0F)
      );
   }

   public static <Value> AttributeType<Value> ofNotInterpolated(Codec<Value> p_457990_) {
      return ofNotInterpolated(p_457990_, Map.of());
   }

   private static <Value> Codec<AttributeModifier<Value, ?>> createModifierCodec(Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> p_457708_) {
      ImmutableBiMap<AttributeModifier.OperationId, AttributeModifier<Value, ?>> immutablebimap = ImmutableBiMap.builder()
         .put(AttributeModifier.OperationId.OVERRIDE, AttributeModifier.override())
         .putAll(p_457708_)
         .buildOrThrow();
      return ExtraCodecs.idResolverCodec(AttributeModifier.OperationId.CODEC, immutablebimap::get, immutablebimap.inverse()::get);
   }

   public void checkAllowedModifier(AttributeModifier<Value, ?> p_457533_) {
      if (p_457533_ != AttributeModifier.override() && !this.modifierLibrary.containsValue(p_457533_)) {
         throw new IllegalArgumentException("Modifier " + p_457533_ + " is not valid for " + this);
      }
   }

   @Override
   public String toString() {
      return Util.getRegisteredName(BuiltInRegistries.ATTRIBUTE_TYPE, this);
   }
}
