package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Brightness(int block, int sky) {
   public static final Codec<Integer> LIGHT_VALUE_CODEC = ExtraCodecs.intRange(0, 15);
   public static final Codec<Brightness> CODEC = RecordCodecBuilder.create(
      p_270774_ -> p_270774_.group(LIGHT_VALUE_CODEC.fieldOf("block").forGetter(Brightness::block), LIGHT_VALUE_CODEC.fieldOf("sky").forGetter(Brightness::sky))
         .apply(p_270774_, Brightness::new)
   );
   public static final Brightness FULL_BRIGHT = new Brightness(15, 15);

   public static int pack(int p_412973_, int p_412975_) {
      return p_412973_ << 4 | p_412975_ << 20;
   }

   public int pack() {
      return pack(this.block, this.sky);
   }

   public static int block(int p_412970_) {
      return p_412970_ >> 4 & 65535;
   }

   public static int sky(int p_412969_) {
      return p_412969_ >> 20 & 65535;
   }

   public static Brightness unpack(int p_270207_) {
      return new Brightness(block(p_270207_), sky(p_270207_));
   }
}
