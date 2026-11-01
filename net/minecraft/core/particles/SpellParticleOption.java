package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;

public class SpellParticleOption implements ParticleOptions {
   private final ParticleType<SpellParticleOption> type;
   private final int color;
   private final float power;

   public static MapCodec<SpellParticleOption> codec(ParticleType<SpellParticleOption> p_446074_) {
      return RecordCodecBuilder.mapCodec(
         p_445952_ -> p_445952_.group(
               ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color", -1).forGetter(p_447303_ -> p_447303_.color),
               Codec.FLOAT.optionalFieldOf("power", 1.0F).forGetter(p_446927_ -> p_446927_.power)
            )
            .apply(p_445952_, (p_447356_, p_446433_) -> new SpellParticleOption(p_446074_, p_447356_, p_446433_))
      );
   }

   public static StreamCodec<? super ByteBuf, SpellParticleOption> streamCodec(ParticleType<SpellParticleOption> p_447255_) {
      return StreamCodec.composite(
         ByteBufCodecs.INT,
         p_447209_ -> p_447209_.color,
         ByteBufCodecs.FLOAT,
         p_445780_ -> p_445780_.power,
         (p_446915_, p_446565_) -> new SpellParticleOption(p_447255_, p_446915_, p_446565_)
      );
   }

   private SpellParticleOption(ParticleType<SpellParticleOption> p_447023_, int p_445477_, float p_446865_) {
      this.type = p_447023_;
      this.color = p_445477_;
      this.power = p_446865_;
   }

   @Override
   public ParticleType<SpellParticleOption> getType() {
      return this.type;
   }

   public float getRed() {
      return ARGB.red(this.color) / 255.0F;
   }

   public float getGreen() {
      return ARGB.green(this.color) / 255.0F;
   }

   public float getBlue() {
      return ARGB.blue(this.color) / 255.0F;
   }

   public float getPower() {
      return this.power;
   }

   public static SpellParticleOption create(ParticleType<SpellParticleOption> p_446700_, int p_446185_, float p_445508_) {
      return new SpellParticleOption(p_446700_, p_446185_, p_445508_);
   }

   public static SpellParticleOption create(ParticleType<SpellParticleOption> p_445664_, float p_445899_, float p_445452_, float p_446083_, float p_445494_) {
      return create(p_445664_, ARGB.colorFromFloat(1.0F, p_445899_, p_445452_, p_446083_), p_445494_);
   }
}
