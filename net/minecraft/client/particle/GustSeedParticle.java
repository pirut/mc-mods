package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class GustSeedParticle extends NoRenderParticle {
   private final double scale;
   private final int tickDelayInBetween;

   GustSeedParticle(ClientLevel level, double x, double y, double z, double scale, int lifetime, int tickDelayInBetween) {
      super(level, x, y, z, 0.0, 0.0, 0.0);
      this.scale = scale;
      this.lifetime = lifetime;
      this.tickDelayInBetween = tickDelayInBetween;
   }

   @Override
   public void tick() {
      if (this.age % (this.tickDelayInBetween + 1) == 0) {
         for (int i = 0; i < 3; i++) {
            double d = this.x + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            double e = this.y + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            double f = this.z + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            this.level.addParticle(ParticleTypes.GUST, d, e, f, (float)this.age / this.lifetime, 0.0, 0.0);
         }
      }

      if (this.age++ == this.lifetime) {
         this.remove();
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final double scale;
      private final int lifetime;
      private final int tickDelayInBetween;

      public Provider(double scale, int lifetime, int tickDelayInBetween) {
         this.scale = scale;
         this.lifetime = lifetime;
         this.tickDelayInBetween = tickDelayInBetween;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         return new GustSeedParticle(clientLevel, d, e, f, this.scale, this.lifetime, this.tickDelayInBetween);
      }
   }
}
