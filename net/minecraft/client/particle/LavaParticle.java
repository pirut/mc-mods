package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LavaParticle extends TextureSheetParticle {
   protected LavaParticle(ClientLevel p_107074_, double p_107075_, double p_107076_, double p_107077_) {
      super(p_107074_, p_107075_, p_107076_, p_107077_, 0.0, 0.0, 0.0);
      this.gravity = 0.75F;
      this.friction = 0.999F;
      this.xd *= 0.8F;
      this.yd *= 0.8F;
      this.zd *= 0.8F;
      this.yd = this.random.nextFloat() * 0.4F + 0.05F;
      this.quadSize = this.quadSize * (this.random.nextFloat() * 2.0F + 0.2F);
      this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public int getLightColor(float p_107086_) {
      int $$1 = super.getLightColor(p_107086_);
      int $$2 = 240;
      int $$3 = $$1 >> 16 & 0xFF;
      return 240 | $$3 << 16;
   }

   @Override
   public float getQuadSize(float p_107089_) {
      float $$1 = (this.age + p_107089_) / this.lifetime;
      return this.quadSize * (1.0F - $$1 * $$1);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         float $$0 = (float)this.age / this.lifetime;
         if (this.random.nextFloat() > $$0) {
            this.level.addParticle(ParticleTypes.SMOKE, this.x, this.y, this.z, this.xd, this.yd, this.zd);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_107092_) {
         this.sprite = p_107092_;
      }

      public Particle createParticle(
         SimpleParticleType p_107103_,
         ClientLevel p_107104_,
         double p_107105_,
         double p_107106_,
         double p_107107_,
         double p_107108_,
         double p_107109_,
         double p_107110_
      ) {
         LavaParticle $$8 = new LavaParticle(p_107104_, p_107105_, p_107106_, p_107107_);
         $$8.pickSprite(this.sprite);
         return $$8;
      }
   }
}
