package net.minecraft.client.particle;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class SingleQuadParticle extends Particle {
   protected float quadSize;
   protected float rCol = 1.0F;
   protected float gCol = 1.0F;
   protected float bCol = 1.0F;
   protected float alpha = 1.0F;
   protected float roll;
   protected float oRoll;
   protected TextureAtlasSprite sprite;

   public SingleQuadParticle(ClientLevel p_107665_, double p_107666_, double p_107667_, double p_107668_, TextureAtlasSprite p_445944_) {
      super(p_107665_, p_107666_, p_107667_, p_107668_);
      this.sprite = p_445944_;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
   }

   public SingleQuadParticle(
      ClientLevel p_107670_,
      double p_107671_,
      double p_107672_,
      double p_107673_,
      double p_107674_,
      double p_107675_,
      double p_107676_,
      TextureAtlasSprite p_446145_
   ) {
      super(p_107670_, p_107671_, p_107672_, p_107673_, p_107674_, p_107675_, p_107676_);
      this.sprite = p_446145_;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
   }

   public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
      return SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ;
   }

   public void extract(QuadParticleRenderState p_451421_, Camera p_446118_, float p_445889_) {
      Quaternionf quaternionf = new Quaternionf();
      this.getFacingCameraMode().setRotation(quaternionf, p_446118_, p_445889_);
      if (this.roll != 0.0F) {
         quaternionf.rotateZ(Mth.lerp(p_445889_, this.oRoll, this.roll));
      }

      this.extractRotatedQuad(p_451421_, p_446118_, quaternionf, p_445889_);
   }

   protected void extractRotatedQuad(QuadParticleRenderState p_451463_, Camera p_447286_, Quaternionf p_445471_, float p_447114_) {
      Vec3 vec3 = p_447286_.position();
      float f = (float)(Mth.lerp((double)p_447114_, this.xo, this.x) - vec3.x());
      float f1 = (float)(Mth.lerp((double)p_447114_, this.yo, this.y) - vec3.y());
      float f2 = (float)(Mth.lerp((double)p_447114_, this.zo, this.z) - vec3.z());
      this.extractRotatedQuad(p_451463_, p_445471_, f, f1, f2, p_447114_);
   }

   protected void extractRotatedQuad(
      QuadParticleRenderState p_451200_, Quaternionf p_445800_, float p_446574_, float p_447224_, float p_447277_, float p_445989_
   ) {
      p_451200_.add(
         this.getLayer(),
         p_446574_,
         p_447224_,
         p_447277_,
         p_445800_.x,
         p_445800_.y,
         p_445800_.z,
         p_445800_.w,
         this.getQuadSize(p_445989_),
         this.getU0(),
         this.getU1(),
         this.getV0(),
         this.getV1(),
         ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol),
         this.getLightColor(p_445989_)
      );
   }

   public float getQuadSize(float p_107681_) {
      return this.quadSize;
   }

   @Override
   public Particle scale(float p_107683_) {
      this.quadSize *= p_107683_;
      return super.scale(p_107683_);
   }

   @Override
   public ParticleRenderType getGroup() {
      return ParticleRenderType.SINGLE_QUADS;
   }

   public void setSpriteFromAge(SpriteSet p_446010_) {
      if (!this.removed) {
         this.setSprite(p_446010_.get(this.age, this.lifetime));
      }
   }

   protected void setSprite(TextureAtlasSprite p_447288_) {
      this.sprite = p_447288_;
   }

   protected float getU0() {
      return this.sprite.getU0();
   }

   protected float getU1() {
      return this.sprite.getU1();
   }

   protected float getV0() {
      return this.sprite.getV0();
   }

   protected float getV1() {
      return this.sprite.getV1();
   }

   protected abstract SingleQuadParticle.Layer getLayer();

   public void setColor(float p_447289_, float p_447183_, float p_445535_) {
      this.rCol = p_447289_;
      this.gCol = p_447183_;
      this.bCol = p_445535_;
   }

   protected void setAlpha(float p_446481_) {
      this.alpha = p_446481_;
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName()
         + ", Pos ("
         + this.x
         + ","
         + this.y
         + ","
         + this.z
         + "), RGBA ("
         + this.rCol
         + ","
         + this.gCol
         + ","
         + this.bCol
         + ","
         + this.alpha
         + "), Age "
         + this.age;
   }

   @OnlyIn(Dist.CLIENT)
   public interface FacingCameraMode {
      SingleQuadParticle.FacingCameraMode LOOKAT_XYZ = (p_312316_, p_311843_, p_312119_) -> p_312316_.set(p_311843_.rotation());
      SingleQuadParticle.FacingCameraMode LOOKAT_Y = (p_312695_, p_312346_, p_312064_) -> p_312695_.set(
         0.0F, p_312346_.rotation().y, 0.0F, p_312346_.rotation().w
      );

      void setRotation(Quaternionf var1, Camera var2, float var3);
   }

   @OnlyIn(Dist.CLIENT)
   public record Layer(boolean translucent, Identifier textureAtlasLocation, RenderPipeline pipeline) {
      public static final SingleQuadParticle.Layer TERRAIN = new SingleQuadParticle.Layer(
         true, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.TRANSLUCENT_PARTICLE
      );
      public static final SingleQuadParticle.Layer ITEMS = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_ITEMS, RenderPipelines.TRANSLUCENT_PARTICLE);
      public static final SingleQuadParticle.Layer OPAQUE = new SingleQuadParticle.Layer(
         false, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.OPAQUE_PARTICLE
      );
      public static final SingleQuadParticle.Layer TRANSLUCENT = new SingleQuadParticle.Layer(
         true, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_PARTICLE
      );
   }
}
