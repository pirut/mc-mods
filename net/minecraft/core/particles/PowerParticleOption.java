package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class PowerParticleOption implements ParticleOptions {
   private final ParticleType<PowerParticleOption> type;
   private final float power;

   public static MapCodec<PowerParticleOption> codec(ParticleType<PowerParticleOption> p_446876_) {
      return Codec.FLOAT
         .xmap(p_445919_ -> new PowerParticleOption(p_446876_, p_445919_), p_446827_ -> p_446827_.power)
         .optionalFieldOf("power", create(p_446876_, 1.0F));
   }

   public static StreamCodec<? super ByteBuf, PowerParticleOption> streamCodec(ParticleType<PowerParticleOption> p_446484_) {
      return ByteBufCodecs.FLOAT.map(p_445551_ -> new PowerParticleOption(p_446484_, p_445551_), p_447254_ -> p_447254_.power);
   }

   private PowerParticleOption(ParticleType<PowerParticleOption> p_447129_, float p_446678_) {
      this.type = p_447129_;
      this.power = p_446678_;
   }

   @Override
   public ParticleType<PowerParticleOption> getType() {
      return this.type;
   }

   public float getPower() {
      return this.power;
   }

   public static PowerParticleOption create(ParticleType<PowerParticleOption> p_445604_, float p_446838_) {
      return new PowerParticleOption(p_445604_, p_446838_);
   }
}
