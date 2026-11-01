package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class SquidInkParticle extends SimpleAnimatedParticle {
   SquidInkParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int packedColor, SpriteSet sprites) {
      super(level, x, y, z, sprites, 0.0F);
      this.friction = 0.92F;
      this.quadSize = 0.5F;
      this.setAlpha(1.0F);
      this.setColor(ARGB.redFloat(packedColor), ARGB.greenFloat(packedColor), ARGB.blueFloat(packedColor));
      this.lifetime = (int)(this.quadSize * 12.0F / (this.random.nextFloat() * 0.8F + 0.2F));
      this.setSpriteFromAge(sprites);
      this.hasPhysics = false;
      this.xd = xSpeed;
      this.yd = ySpeed;
      this.zd = zSpeed;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
         if (this.age > this.lifetime / 2) {
            this.setAlpha(1.0F - ((float)this.age - this.lifetime / 2) / this.lifetime);
         }

         if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
            this.yd -= 0.0074F;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static class GlowInkProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public GlowInkProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         return new SquidInkParticle(clientLevel, d, e, f, g, h, i, ARGB.colorFromFloat(1.0F, 0.2F, 0.8F, 0.6F), this.sprites);
      }
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
         return new SquidInkParticle(clientLevel, d, e, f, g, h, i, -16777216, this.sprites);
      }
   }
}
