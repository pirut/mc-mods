package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ParticleProvider<T extends ParticleOptions> {
   @Nullable
   Particle createParticle(T particleType, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random);

   @Environment(EnvType.CLIENT)
   public interface Sprite<T extends ParticleOptions> {
      @Nullable
      SingleQuadParticle createParticle(
         T particleOptions, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random
      );
   }
}
