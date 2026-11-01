package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class DustPlumeParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected DustPlumeParticle(
      ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, float quadSizeMultiplier, SpriteSet sprites
   ) {
      super(level, x, y, z, 0.7F, 0.6F, 0.7F, xSpeed, ySpeed + 0.15F, zSpeed, quadSizeMultiplier, sprites, 0.5F, 7, 0.5F, false);
      float f = this.random.nextFloat() * 0.2F;
      this.rCol = ARGB.red(12235202) / 255.0F - f;
      this.gCol = ARGB.green(12235202) / 255.0F - f;
      this.bCol = ARGB.blue(12235202) / 255.0F - f;
   }

   @Override
   public void tick() {
      this.gravity = 0.88F * this.gravity;
      this.friction = 0.92F * this.friction;
      super.tick();
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         return new DustPlumeParticle(clientLevel, d, e, f, g, h, i, 1.0F, this.sprites);
      }
   }
}
