package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SculkChargeParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected SculkChargeParticle(
      ClientLevel p_233892_, double p_233893_, double p_233894_, double p_233895_, double p_233896_, double p_233897_, double p_233898_, SpriteSet p_233899_
   ) {
      super(p_233892_, p_233893_, p_233894_, p_233895_, p_233896_, p_233897_, p_233898_);
      this.friction = 0.96F;
      this.sprites = p_233899_;
      this.scale(1.5F);
      this.hasPhysics = false;
      this.setSpriteFromAge(p_233899_);
   }

   @Override
   public int getLightColor(float p_233902_) {
      return 240;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   @OnlyIn(Dist.CLIENT)
   public record Provider(SpriteSet sprite) implements ParticleProvider<SculkChargeParticleOptions> {
      public Particle createParticle(
         SculkChargeParticleOptions p_233918_,
         ClientLevel p_233919_,
         double p_233920_,
         double p_233921_,
         double p_233922_,
         double p_233923_,
         double p_233924_,
         double p_233925_
      ) {
         SculkChargeParticle $$8 = new SculkChargeParticle(p_233919_, p_233920_, p_233921_, p_233922_, p_233923_, p_233924_, p_233925_, this.sprite);
         $$8.setAlpha(1.0F);
         $$8.setParticleSpeed(p_233923_, p_233924_, p_233925_);
         $$8.oRoll = p_233918_.roll();
         $$8.roll = p_233918_.roll();
         $$8.setLifetime(p_233919_.random.nextInt(12) + 8);
         return $$8;
      }
   }
}
