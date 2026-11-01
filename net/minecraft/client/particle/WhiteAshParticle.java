package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WhiteAshParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected WhiteAshParticle(
      ClientLevel p_108512_,
      double p_108513_,
      double p_108514_,
      double p_108515_,
      double p_108516_,
      double p_108517_,
      double p_108518_,
      float p_108519_,
      SpriteSet p_108520_
   ) {
      super(p_108512_, p_108513_, p_108514_, p_108515_, 0.1F, -0.1F, 0.1F, p_108516_, p_108517_, p_108518_, p_108519_, p_108520_, 0.0F, 20, 0.0125F, false);
      this.rCol = FastColor.ARGB32.red(12235202) / 255.0F;
      this.gCol = FastColor.ARGB32.green(12235202) / 255.0F;
      this.bCol = FastColor.ARGB32.blue(12235202) / 255.0F;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet p_108523_) {
         this.sprites = p_108523_;
      }

      public Particle createParticle(
         SimpleParticleType p_108534_,
         ClientLevel p_108535_,
         double p_108536_,
         double p_108537_,
         double p_108538_,
         double p_108539_,
         double p_108540_,
         double p_108541_
      ) {
         RandomSource $$8 = p_108535_.random;
         double $$9 = $$8.nextFloat() * -1.9 * $$8.nextFloat() * 0.1;
         double $$10 = $$8.nextFloat() * -0.5 * $$8.nextFloat() * 0.1 * 5.0;
         double $$11 = $$8.nextFloat() * -1.9 * $$8.nextFloat() * 0.1;
         return new WhiteAshParticle(p_108535_, p_108536_, p_108537_, p_108538_, $$9, $$10, $$11, 1.0F, this.sprites);
      }
   }
}
