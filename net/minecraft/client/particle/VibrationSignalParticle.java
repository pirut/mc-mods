package net.minecraft.client.particle;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class VibrationSignalParticle extends SingleQuadParticle {
   private final PositionSource target;
   private float rot;
   private float rotO;
   private float pitch;
   private float pitchO;

   VibrationSignalParticle(ClientLevel level, double x, double y, double z, PositionSource target, int lifetime, TextureAtlasSprite sprite) {
      super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
      this.quadSize = 0.3F;
      this.target = target;
      this.lifetime = lifetime;
      Optional<Vec3> optional = target.getPosition(level);
      if (optional.isPresent()) {
         Vec3 vec3 = optional.get();
         double d = x - vec3.x();
         double e = y - vec3.y();
         double f = z - vec3.z();
         this.rotO = this.rot = (float)Mth.atan2(d, f);
         this.pitchO = this.pitch = (float)Mth.atan2(e, Math.sqrt(d * d + f * f));
      }
   }

   @Override
   public void extract(QuadParticleRenderState reusedState, Camera camera, float partialTick) {
      float f = Mth.sin((this.age + partialTick - (float) (Math.PI * 2)) * 0.05F) * 2.0F;
      float g = Mth.lerp(partialTick, this.rotO, this.rot);
      float h = Mth.lerp(partialTick, this.pitchO, this.pitch) + (float) (Math.PI / 2);
      Quaternionf quaternionf = new Quaternionf();
      quaternionf.rotationY(g).rotateX(-h).rotateY(f);
      this.extractRotatedQuad(reusedState, camera, quaternionf, partialTick);
      quaternionf.rotationY((float) -Math.PI + g).rotateX(h).rotateY(f);
      this.extractRotatedQuad(reusedState, camera, quaternionf, partialTick);
   }

   @Override
   public int getLightColor(float partialTick) {
      return 240;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         Optional<Vec3> optional = this.target.getPosition(this.level);
         if (optional.isEmpty()) {
            this.remove();
         } else {
            int i = this.lifetime - this.age;
            double d = 1.0 / i;
            Vec3 vec3 = optional.get();
            this.x = Mth.lerp(d, this.x, vec3.x());
            this.y = Mth.lerp(d, this.y, vec3.y());
            this.z = Mth.lerp(d, this.z, vec3.z());
            double e = this.x - vec3.x();
            double f = this.y - vec3.y();
            double g = this.z - vec3.z();
            this.rotO = this.rot;
            this.rot = (float)Mth.atan2(e, g);
            this.pitchO = this.pitch;
            this.pitch = (float)Mth.atan2(f, Math.sqrt(e * e + g * g));
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<VibrationParticleOption> {
      private final SpriteSet sprite;

      public Provider(SpriteSet sprites) {
         this.sprite = sprites;
      }

      public Particle createParticle(
         VibrationParticleOption vibrationParticleOption,
         ClientLevel clientLevel,
         double d,
         double e,
         double f,
         double g,
         double h,
         double i,
         RandomSource randomSource
      ) {
         VibrationSignalParticle vibrationSignalParticle = new VibrationSignalParticle(
            clientLevel, d, e, f, vibrationParticleOption.getDestination(), vibrationParticleOption.getArrivalInTicks(), this.sprite.get(randomSource)
         );
         vibrationSignalParticle.setAlpha(1.0F);
         return vibrationSignalParticle;
      }
   }
}
