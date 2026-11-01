package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class ExplodeParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   protected ExplodeParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
      super(level, x, y, z, sprites.first());
      this.gravity = -0.1F;
      this.friction = 0.9F;
      this.sprites = sprites;
      this.xd = xSpeed + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
      this.yd = ySpeed + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
      this.zd = zSpeed + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
      float f = this.random.nextFloat() * 0.3F + 0.7F;
      this.rCol = f;
      this.gCol = f;
      this.bCol = f;
      this.quadSize = 0.1F * (this.random.nextFloat() * this.random.nextFloat() * 6.0F + 1.0F);
      this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2)) + 2;
      this.setSpriteFromAge(sprites);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
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
         return new ExplodeParticle(clientLevel, d, e, f, g, h, i, this.sprites);
      }
   }
}
