package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FallingLeavesParticle extends SingleQuadParticle {
   private static final float ACCELERATION_SCALE = 0.0025F;
   private static final int INITIAL_LIFETIME = 300;
   private static final int CURVE_ENDPOINT_TIME = 300;
   private float rotSpeed = (float)Math.toRadians(this.random.nextBoolean() ? -30.0 : 30.0);
   private final float spinAcceleration = (float)Math.toRadians(this.random.nextBoolean() ? -5.0 : 5.0);
   private final float windBig;
   private final boolean swirl;
   private final boolean flowAway;
   private final double xaFlowScale;
   private final double zaFlowScale;
   private final double swirlPeriod;

   public FallingLeavesParticle(
      ClientLevel p_383228_,
      double p_383201_,
      double p_382929_,
      double p_382848_,
      TextureAtlasSprite p_446062_,
      float p_383015_,
      float p_383012_,
      boolean p_383052_,
      boolean p_382926_,
      float p_383165_,
      float p_382942_
   ) {
      super(p_383228_, p_383201_, p_382929_, p_382848_, p_446062_);
      this.windBig = p_383012_;
      this.swirl = p_383052_;
      this.flowAway = p_382926_;
      this.lifetime = 300;
      this.gravity = p_383015_ * 1.2F * 0.0025F;
      float f = p_383165_ * (this.random.nextBoolean() ? 0.05F : 0.075F);
      this.quadSize = f;
      this.setSize(f, f);
      this.friction = 1.0F;
      this.yd = -p_382942_;
      float f1 = this.random.nextFloat();
      this.xaFlowScale = Math.cos(Math.toRadians(f1 * 60.0F)) * this.windBig;
      this.zaFlowScale = Math.sin(Math.toRadians(f1 * 60.0F)) * this.windBig;
      this.swirlPeriod = Math.toRadians(1000.0F + f1 * 3000.0F);
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
      if (this.lifetime-- <= 0) {
         this.remove();
      }

      if (!this.removed) {
         float f = 300 - this.lifetime;
         float f1 = Math.min(f / 300.0F, 1.0F);
         double d0 = 0.0;
         double d1 = 0.0;
         if (this.flowAway) {
            d0 += this.xaFlowScale * Math.pow(f1, 1.25);
            d1 += this.zaFlowScale * Math.pow(f1, 1.25);
         }

         if (this.swirl) {
            d0 += f1 * Math.cos(f1 * this.swirlPeriod) * this.windBig;
            d1 += f1 * Math.sin(f1 * this.swirlPeriod) * this.windBig;
         }

         this.xd += d0 * 0.0025F;
         this.zd += d1 * 0.0025F;
         this.yd = this.yd - this.gravity;
         this.rotSpeed = this.rotSpeed + this.spinAcceleration / 20.0F;
         this.oRoll = this.roll;
         this.roll = this.roll + this.rotSpeed / 20.0F;
         this.move(this.xd, this.yd, this.zd);
         if (this.onGround || this.lifetime < 299 && (this.xd == 0.0 || this.zd == 0.0)) {
            this.remove();
         }

         if (!this.removed) {
            this.xd = this.xd * this.friction;
            this.yd = this.yd * this.friction;
            this.zd = this.zd * this.friction;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class CherryProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public CherryProvider(SpriteSet p_383119_) {
         this.sprites = p_383119_;
      }

      public Particle createParticle(
         SimpleParticleType p_447048_,
         ClientLevel p_382862_,
         double p_383067_,
         double p_382867_,
         double p_382813_,
         double p_383026_,
         double p_382872_,
         double p_383020_,
         RandomSource p_446352_
      ) {
         return new FallingLeavesParticle(p_382862_, p_383067_, p_382867_, p_382813_, this.sprites.get(p_446352_), 0.25F, 2.0F, false, true, 1.0F, 0.0F);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class PaleOakProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public PaleOakProvider(SpriteSet p_383092_) {
         this.sprites = p_383092_;
      }

      public Particle createParticle(
         SimpleParticleType p_383203_,
         ClientLevel p_383110_,
         double p_383063_,
         double p_382907_,
         double p_383062_,
         double p_382964_,
         double p_382864_,
         double p_382906_,
         RandomSource p_447188_
      ) {
         return new FallingLeavesParticle(p_383110_, p_383063_, p_382907_, p_383062_, this.sprites.get(p_447188_), 0.07F, 10.0F, true, false, 2.0F, 0.021F);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TintedLeavesProvider implements ParticleProvider<ColorParticleOption> {
      private final SpriteSet sprites;

      public TintedLeavesProvider(SpriteSet p_394294_) {
         this.sprites = p_394294_;
      }

      public Particle createParticle(
         ColorParticleOption p_400051_,
         ClientLevel p_393524_,
         double p_393550_,
         double p_394425_,
         double p_394620_,
         double p_393739_,
         double p_394589_,
         double p_394199_,
         RandomSource p_445513_
      ) {
         FallingLeavesParticle fallingleavesparticle = new FallingLeavesParticle(
            p_393524_, p_393550_, p_394425_, p_394620_, this.sprites.get(p_445513_), 0.07F, 10.0F, true, false, 2.0F, 0.021F
         );
         fallingleavesparticle.setColor(p_400051_.getRed(), p_400051_.getGreen(), p_400051_.getBlue());
         return fallingleavesparticle;
      }
   }
}
