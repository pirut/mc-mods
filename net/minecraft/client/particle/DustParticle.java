package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class DustParticle extends DustParticleBase<DustParticleOptions> {
   protected DustParticle(
      ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, DustParticleOptions options, SpriteSet sprites
   ) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
      float f = this.random.nextFloat() * 0.4F + 0.6F;
      Vector3f vector3f = options.getColor();
      this.rCol = this.randomizeColor(vector3f.x(), f);
      this.gCol = this.randomizeColor(vector3f.y(), f);
      this.bCol = this.randomizeColor(vector3f.z(), f);
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<DustParticleOptions> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         DustParticleOptions dustParticleOptions,
         ClientLevel clientLevel,
         double d,
         double e,
         double f,
         double g,
         double h,
         double i,
         RandomSource randomSource
      ) {
         return new DustParticle(clientLevel, d, e, f, g, h, i, dustParticleOptions, this.sprites);
      }
   }
}
