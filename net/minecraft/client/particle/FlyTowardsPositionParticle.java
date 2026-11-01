package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class FlyTowardsPositionParticle extends SingleQuadParticle {
   private final double xStart;
   private final double yStart;
   private final double zStart;
   private final boolean isGlowing;
   private final Particle.LifetimeAlpha lifetimeAlpha;

   FlyTowardsPositionParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, TextureAtlasSprite textureAtlasSprite) {
      this(clientLevel, d, e, f, g, h, i, false, Particle.LifetimeAlpha.ALWAYS_OPAQUE, textureAtlasSprite);
   }

   FlyTowardsPositionParticle(
      ClientLevel level,
      double x,
      double y,
      double z,
      double xSpeed,
      double ySpeed,
      double zSpeed,
      boolean isGlowing,
      Particle.LifetimeAlpha lifetimeAlpha,
      TextureAtlasSprite sprite
   ) {
      super(level, x, y, z, sprite);
      this.isGlowing = isGlowing;
      this.lifetimeAlpha = lifetimeAlpha;
      this.setAlpha(lifetimeAlpha.startAlpha());
      this.xd = xSpeed;
      this.yd = ySpeed;
      this.zd = zSpeed;
      this.xStart = x;
      this.yStart = y;
      this.zStart = z;
      this.xo = x + xSpeed;
      this.yo = y + ySpeed;
      this.zo = z + zSpeed;
      this.x = this.xo;
      this.y = this.yo;
      this.z = this.zo;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
      float f = this.random.nextFloat() * 0.6F + 0.4F;
      this.rCol = 0.9F * f;
      this.gCol = 0.9F * f;
      this.bCol = f;
      this.hasPhysics = false;
      this.lifetime = (int)(this.random.nextFloat() * 10.0F) + 30;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return this.lifetimeAlpha.isOpaque() ? SingleQuadParticle.Layer.OPAQUE : SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void move(double x, double y, double z) {
      this.setBoundingBox(this.getBoundingBox().move(x, y, z));
      this.setLocationFromBoundingbox();
   }

   @Override
   public int getLightColor(float partialTick) {
      if (this.isGlowing) {
         return 240;
      } else {
         int i = super.getLightColor(partialTick);
         float f = (float)this.age / this.lifetime;
         f *= f;
         f *= f;
         int j = i & 0xFF;
         int k = i >> 16 & 0xFF;
         k += (int)(f * 15.0F * 16.0F);
         if (k > 240) {
            k = 240;
         }

         return j | k << 16;
      }
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float f = (float)this.age / this.lifetime;
         f = 1.0F - f;
         float g = 1.0F - f;
         g *= g;
         g *= g;
         this.x = this.xStart + this.xd * f;
         this.y = this.yStart + this.yd * f - g * 1.2F;
         this.z = this.zStart + this.zd * f;
      }
   }

   @Override
   public void extract(QuadParticleRenderState reusedState, Camera camera, float partialTick) {
      this.setAlpha(this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, partialTick));
      super.extract(reusedState, camera, partialTick);
   }

   @Environment(EnvType.CLIENT)
   public static class EnchantProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public EnchantProvider(SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         return new FlyTowardsPositionParticle(clientLevel, d, e, f, g, h, i, this.sprite.get(randomSource));
      }
   }

   @Environment(EnvType.CLIENT)
   public static class NautilusProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public NautilusProvider(SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         return new FlyTowardsPositionParticle(clientLevel, d, e, f, g, h, i, this.sprite.get(randomSource));
      }
   }

   @Environment(EnvType.CLIENT)
   public static class VaultConnectionProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public VaultConnectionProvider(SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         FlyTowardsPositionParticle flyTowardsPositionParticle = new FlyTowardsPositionParticle(
            clientLevel, d, e, f, g, h, i, true, new Particle.LifetimeAlpha(0.0F, 0.6F, 0.25F, 1.0F), this.sprite.get(randomSource)
         );
         flyTowardsPositionParticle.scale(1.5F);
         return flyTowardsPositionParticle;
      }
   }
}
