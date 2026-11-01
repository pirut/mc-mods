package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class GustParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   protected GustParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
      super(level, x, y, z, sprites.first());
      this.sprites = sprites;
      this.setSpriteFromAge(sprites);
      this.lifetime = 12 + this.random.nextInt(4);
      this.quadSize = 1.0F;
      this.setSize(1.0F, 1.0F);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public int getLightColor(float partialTick) {
      return 15728880;
   }

   @Override
   public void tick() {
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
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
         return new GustParticle(clientLevel, d, e, f, this.sprites);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class SmallProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SmallProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         Particle particle = new GustParticle(clientLevel, d, e, f, this.sprites);
         particle.scale(0.15F);
         return particle;
      }
   }
}
