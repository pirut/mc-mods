package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public class PlayerCloudParticle extends SingleQuadParticle {
   private final SpriteSet sprites;

   PlayerCloudParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
      super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
      this.friction = 0.96F;
      this.sprites = sprites;
      float f = 2.5F;
      this.xd *= 0.1F;
      this.yd *= 0.1F;
      this.zd *= 0.1F;
      this.xd += xSpeed;
      this.yd += ySpeed;
      this.zd += zSpeed;
      float g = 1.0F - this.random.nextFloat() * 0.3F;
      this.rCol = g;
      this.gCol = g;
      this.bCol = g;
      this.quadSize *= 1.875F;
      int i = (int)(8.0 / (this.random.nextFloat() * 0.8 + 0.3));
      this.lifetime = (int)Math.max(i * 2.5F, 1.0F);
      this.hasPhysics = false;
      this.setSpriteFromAge(sprites);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public float getQuadSize(float scaleFactor) {
      return this.quadSize * Mth.clamp((this.age + scaleFactor) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
         Player player = this.level.getNearestPlayer(this.x, this.y, this.z, 2.0, false);
         if (player != null) {
            double d = player.getY();
            if (this.y > d) {
               this.y = this.y + (d - this.y) * 0.2;
               this.yd = this.yd + (player.getDeltaMovement().y - this.yd) * 0.2;
               this.setPos(this.x, this.y, this.z);
            }
         }
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
         return new PlayerCloudParticle(clientLevel, d, e, f, g, h, i, this.sprites);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class SneezeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SneezeProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         PlayerCloudParticle playerCloudParticle = new PlayerCloudParticle(clientLevel, d, e, f, g, h, i, this.sprites);
         playerCloudParticle.setColor(0.22F, 1.0F, 0.53F);
         playerCloudParticle.setAlpha(0.4F);
         return playerCloudParticle;
      }
   }
}
