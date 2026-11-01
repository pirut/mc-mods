package net.minecraft.world.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.LongSupplier;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public record AttributeTrack<Value, Argument>(AttributeModifier<Value, Argument> modifier, KeyframeTrack<Argument> argumentTrack) {
   public static <Value> Codec<AttributeTrack<Value, ?>> createCodec(EnvironmentAttribute<Value> p_469585_) {
      MapCodec<AttributeModifier<Value, ?>> mapcodec = p_469585_.type().modifierCodec().optionalFieldOf("modifier", AttributeModifier.override());
      return mapcodec.dispatch(AttributeTrack::modifier, Util.memoize(p_468589_ -> createCodecWithModifier(p_469585_, p_468589_)));
   }

   private static <Value, Argument> MapCodec<AttributeTrack<Value, Argument>> createCodecWithModifier(
      EnvironmentAttribute<Value> p_467117_, AttributeModifier<Value, Argument> p_469250_
   ) {
      return KeyframeTrack.mapCodec(p_469250_.argumentCodec(p_467117_))
         .xmap(p_467722_ -> new AttributeTrack<>(p_469250_, p_467722_), AttributeTrack::argumentTrack);
   }

   public AttributeTrackSampler<Value, Argument> bakeSampler(EnvironmentAttribute<Value> p_467815_, Optional<Integer> p_469196_, LongSupplier p_468166_) {
      return new AttributeTrackSampler<>(p_469196_, this.modifier, this.argumentTrack, this.modifier.argumentKeyframeLerp(p_467815_), p_468166_);
   }

   public static DataResult<AttributeTrack<?, ?>> validatePeriod(AttributeTrack<?, ?> p_467189_, int p_468331_) {
      return KeyframeTrack.validatePeriod(p_467189_.argumentTrack(), p_468331_).map(p_469865_ -> p_467189_);
   }
}
