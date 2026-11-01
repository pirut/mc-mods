package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrailParticle extends SingleQuadParticle {
   private final Vec3 target;

   public TrailParticle(
      ClientLevel p_380008_,
      double p_380201_,
      double p_380198_,
      double p_380357_,
      double p_380375_,
      double p_380026_,
      double p_379483_,
      Vec3 p_379353_,
      int p_379830_,
      TextureAtlasSprite p_446045_
   ) {
      super(p_380008_, p_380201_, p_380198_, p_380357_, p_380375_, p_380026_, p_379483_, p_446045_);
      p_379830_ = ARGB.scaleRGB(
         p_379830_, 0.875F + this.random.nextFloat() * 0.25F, 0.875F + this.random.nextFloat() * 0.25F, 0.875F + this.random.nextFloat() * 0.25F
      );
      this.rCol = ARGB.red(p_379830_) / 255.0F;
      this.gCol = ARGB.green(p_379830_) / 255.0F;
      this.bCol = ARGB.blue(p_379830_) / 255.0F;
      this.quadSize = 0.26F;
      this.target = p_379353_;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         int i = this.lifetime - this.age;
         double d0 = 1.0 / i;
         this.x = Mth.lerp(d0, this.x, this.target.x());
         this.y = Mth.lerp(d0, this.y, this.target.y());
         this.z = Mth.lerp(d0, this.z, this.target.z());
      }
   }

   @Override
   public int getLightColor(float p_379977_) {
      return 15728880;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<TrailParticleOption> {
      private final SpriteSet sprite;

      public Provider(SpriteSet p_380057_) {
         this.sprite = p_380057_;
      }

      public Particle createParticle(
         TrailParticleOption p_383016_,
         ClientLevel p_379929_,
         double p_380369_,
         double p_380404_,
         double p_379536_,
         double p_380010_,
         double p_379607_,
         double p_379901_,
         RandomSource p_446411_
      ) {
         TrailParticle trailparticle = new TrailParticle(
            p_379929_, p_380369_, p_380404_, p_379536_, p_380010_, p_379607_, p_379901_, p_383016_.target(), p_383016_.color(), this.sprite.get(p_446411_)
         );
         trailparticle.setLifetime(p_383016_.duration());
         return trailparticle;
      }
   }
}
