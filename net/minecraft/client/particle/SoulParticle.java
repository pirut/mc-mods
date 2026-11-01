package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class SoulParticle extends RisingParticle {
   private final SpriteSet sprites;
   protected boolean isGlowing;

   SoulParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites.first());
      this.sprites = sprites;
      this.scale(1.5F);
      this.setSpriteFromAge(sprites);
   }

   @Override
   public int getLightColor(float partialTick) {
      return this.isGlowing ? 240 : super.getLightColor(partialTick);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   @Environment(EnvType.CLIENT)
   public static class EmissiveProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public EmissiveProvider(SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         SoulParticle soulParticle = new SoulParticle(clientLevel, d, e, f, g, h, i, this.sprite);
         soulParticle.setAlpha(1.0F);
         soulParticle.isGlowing = true;
         return soulParticle;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet sprites) {
         this.sprite = sprites;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, RandomSource randomSource
      ) {
         SoulParticle soulParticle = new SoulParticle(clientLevel, d, e, f, g, h, i, this.sprite);
         soulParticle.setAlpha(1.0F);
         return soulParticle;
      }
   }
}
