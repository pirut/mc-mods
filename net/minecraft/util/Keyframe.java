package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Keyframe<T>(int ticks, T value) {
   public static <T> Codec<Keyframe<T>> codec(Codec<T> p_468963_) {
      return RecordCodecBuilder.create(
         p_467463_ -> p_467463_.group(
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks").forGetter(Keyframe::ticks), p_468963_.fieldOf("value").forGetter(Keyframe::value)
            )
            .apply(p_467463_, Keyframe::new)
      );
   }
}
