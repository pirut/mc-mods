package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.guardian.GuardianModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GuardianRenderer extends MobRenderer<Guardian, GuardianRenderState, GuardianModel> {
   private static final Identifier GUARDIAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian.png");
   private static final Identifier GUARDIAN_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian_beam.png");
   private static final RenderType BEAM_RENDER_TYPE = RenderTypes.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

   public GuardianRenderer(EntityRendererProvider.Context p_174159_) {
      this(p_174159_, 0.5F, ModelLayers.GUARDIAN);
   }

   protected GuardianRenderer(EntityRendererProvider.Context p_174161_, float p_174162_, ModelLayerLocation p_174163_) {
      super(p_174161_, new GuardianModel(p_174161_.bakeLayer(p_174163_)), p_174162_);
   }

   public boolean shouldRender(Guardian p_114836_, Frustum p_114837_, double p_114838_, double p_114839_, double p_114840_) {
      if (super.shouldRender(p_114836_, p_114837_, p_114838_, p_114839_, p_114840_)) {
         return true;
      } else {
         if (p_114836_.hasActiveAttackTarget()) {
            LivingEntity livingentity = p_114836_.getActiveAttackTarget();
            if (livingentity != null) {
               Vec3 vec3 = this.getPosition(livingentity, livingentity.getBbHeight() * 0.5, 1.0F);
               Vec3 vec31 = this.getPosition(p_114836_, p_114836_.getEyeHeight(), 1.0F);
               return p_114837_.isVisible(new AABB(vec31.x, vec31.y, vec31.z, vec3.x, vec3.y, vec3.z));
            }
         }

         return false;
      }
   }

   private Vec3 getPosition(LivingEntity p_114803_, double p_114804_, float p_114805_) {
      double d0 = Mth.lerp((double)p_114805_, p_114803_.xOld, p_114803_.getX());
      double d1 = Mth.lerp((double)p_114805_, p_114803_.yOld, p_114803_.getY()) + p_114804_;
      double d2 = Mth.lerp((double)p_114805_, p_114803_.zOld, p_114803_.getZ());
      return new Vec3(d0, d1, d2);
   }

   public void submit(GuardianRenderState p_451369_, PoseStack p_433393_, SubmitNodeCollector p_433420_, CameraRenderState p_451043_) {
      super.submit(p_451369_, p_433393_, p_433420_, p_451043_);
      Vec3 vec3 = p_451369_.attackTargetPosition;
      if (vec3 != null) {
         float f = p_451369_.attackTime * 0.5F % 1.0F;
         p_433393_.pushPose();
         p_433393_.translate(0.0F, p_451369_.eyeHeight, 0.0F);
         renderBeam(p_433393_, p_433420_, vec3.subtract(p_451369_.eyePosition), p_451369_.attackTime, p_451369_.attackScale, f);
         p_433393_.popPose();
      }
   }

   private static void renderBeam(PoseStack p_363906_, SubmitNodeCollector p_435106_, Vec3 p_360399_, float p_363920_, float p_363755_, float p_361709_) {
      float f = (float)(p_360399_.length() + 1.0);
      p_360399_ = p_360399_.normalize();
      float f1 = (float)Math.acos(p_360399_.y);
      float f2 = (float) (Math.PI / 2) - (float)Math.atan2(p_360399_.z, p_360399_.x);
      p_363906_.mulPose(Axis.YP.rotationDegrees(f2 * (180.0F / (float)Math.PI)));
      p_363906_.mulPose(Axis.XP.rotationDegrees(f1 * (180.0F / (float)Math.PI)));
      float f3 = p_363920_ * 0.05F * -1.5F;
      float f4 = p_363755_ * p_363755_;
      int i = 64 + (int)(f4 * 191.0F);
      int j = 32 + (int)(f4 * 191.0F);
      int k = 128 - (int)(f4 * 64.0F);
      float f5 = 0.2F;
      float f6 = 0.282F;
      float f7 = Mth.cos(f3 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
      float f8 = Mth.sin(f3 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
      float f9 = Mth.cos(f3 + (float) (Math.PI / 4)) * 0.282F;
      float f10 = Mth.sin(f3 + (float) (Math.PI / 4)) * 0.282F;
      float f11 = Mth.cos(f3 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
      float f12 = Mth.sin(f3 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
      float f13 = Mth.cos(f3 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
      float f14 = Mth.sin(f3 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
      float f15 = Mth.cos(f3 + (float) Math.PI) * 0.2F;
      float f16 = Mth.sin(f3 + (float) Math.PI) * 0.2F;
      float f17 = Mth.cos(f3 + 0.0F) * 0.2F;
      float f18 = Mth.sin(f3 + 0.0F) * 0.2F;
      float f19 = Mth.cos(f3 + (float) (Math.PI / 2)) * 0.2F;
      float f20 = Mth.sin(f3 + (float) (Math.PI / 2)) * 0.2F;
      float f21 = Mth.cos(f3 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
      float f22 = Mth.sin(f3 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
      float f23 = 0.0F;
      float f24 = 0.4999F;
      float f25 = -1.0F + p_361709_;
      float f26 = f25 + f * 2.5F;
      p_435106_.submitCustomGeometry(p_363906_, BEAM_RENDER_TYPE, (p_432958_, p_433082_) -> {
         vertex(p_433082_, p_432958_, f15, f, f16, i, j, k, 0.4999F, f26);
         vertex(p_433082_, p_432958_, f15, 0.0F, f16, i, j, k, 0.4999F, f25);
         vertex(p_433082_, p_432958_, f17, 0.0F, f18, i, j, k, 0.0F, f25);
         vertex(p_433082_, p_432958_, f17, f, f18, i, j, k, 0.0F, f26);
         vertex(p_433082_, p_432958_, f19, f, f20, i, j, k, 0.4999F, f26);
         vertex(p_433082_, p_432958_, f19, 0.0F, f20, i, j, k, 0.4999F, f25);
         vertex(p_433082_, p_432958_, f21, 0.0F, f22, i, j, k, 0.0F, f25);
         vertex(p_433082_, p_432958_, f21, f, f22, i, j, k, 0.0F, f26);
         float f27 = Mth.floor(p_363920_) % 2 == 0 ? 0.5F : 0.0F;
         vertex(p_433082_, p_432958_, f7, f, f8, i, j, k, 0.5F, f27 + 0.5F);
         vertex(p_433082_, p_432958_, f9, f, f10, i, j, k, 1.0F, f27 + 0.5F);
         vertex(p_433082_, p_432958_, f13, f, f14, i, j, k, 1.0F, f27);
         vertex(p_433082_, p_432958_, f11, f, f12, i, j, k, 0.5F, f27);
      });
   }

   private static void vertex(
      VertexConsumer p_253637_,
      PoseStack.Pose p_323627_,
      float p_253994_,
      float p_254492_,
      float p_254474_,
      int p_254080_,
      int p_253655_,
      int p_254133_,
      float p_254233_,
      float p_253939_
   ) {
      p_253637_.addVertex(p_323627_, p_253994_, p_254492_, p_254474_)
         .setColor(p_254080_, p_253655_, p_254133_, 255)
         .setUv(p_254233_, p_253939_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(p_323627_, 0.0F, 1.0F, 0.0F);
   }

   public Identifier getTextureLocation(GuardianRenderState p_363236_) {
      return GUARDIAN_LOCATION;
   }

   public GuardianRenderState createRenderState() {
      return new GuardianRenderState();
   }

   public void extractRenderState(Guardian p_360824_, GuardianRenderState p_360947_, float p_361770_) {
      super.extractRenderState(p_360824_, p_360947_, p_361770_);
      p_360947_.spikesAnimation = p_360824_.getSpikesAnimation(p_361770_);
      p_360947_.tailAnimation = p_360824_.getTailAnimation(p_361770_);
      p_360947_.eyePosition = p_360824_.getEyePosition(p_361770_);
      Entity entity = getEntityToLookAt(p_360824_);
      if (entity != null) {
         p_360947_.lookDirection = p_360824_.getViewVector(p_361770_);
         p_360947_.lookAtPosition = entity.getEyePosition(p_361770_);
      } else {
         p_360947_.lookDirection = null;
         p_360947_.lookAtPosition = null;
      }

      LivingEntity livingentity = p_360824_.getActiveAttackTarget();
      if (livingentity != null) {
         p_360947_.attackScale = p_360824_.getAttackAnimationScale(p_361770_);
         p_360947_.attackTime = p_360824_.getClientSideAttackTime() + p_361770_;
         p_360947_.attackTargetPosition = this.getPosition(livingentity, livingentity.getBbHeight() * 0.5, p_361770_);
      } else {
         p_360947_.attackTargetPosition = null;
      }
   }

   @Nullable
   private static Entity getEntityToLookAt(Guardian p_364936_) {
      Entity entity = Minecraft.getInstance().getCameraEntity();
      return (Entity)(p_364936_.hasActiveAttackTarget() ? p_364936_.getActiveAttackTarget() : entity);
   }
}
