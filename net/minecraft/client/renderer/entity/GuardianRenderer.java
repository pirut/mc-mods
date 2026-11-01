package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuardianRenderer extends MobRenderer<Guardian, GuardianModel> {
   private static final ResourceLocation GUARDIAN_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/guardian.png");
   private static final ResourceLocation GUARDIAN_BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/guardian_beam.png");
   private static final RenderType BEAM_RENDER_TYPE = RenderType.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

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
            LivingEntity $$5 = p_114836_.getActiveAttackTarget();
            if ($$5 != null) {
               Vec3 $$6 = this.getPosition($$5, $$5.getBbHeight() * 0.5, 1.0F);
               Vec3 $$7 = this.getPosition(p_114836_, p_114836_.getEyeHeight(), 1.0F);
               return p_114837_.isVisible(new AABB($$7.x, $$7.y, $$7.z, $$6.x, $$6.y, $$6.z));
            }
         }

         return false;
      }
   }

   private Vec3 getPosition(LivingEntity p_114803_, double p_114804_, float p_114805_) {
      double $$3 = Mth.lerp((double)p_114805_, p_114803_.xOld, p_114803_.getX());
      double $$4 = Mth.lerp((double)p_114805_, p_114803_.yOld, p_114803_.getY()) + p_114804_;
      double $$5 = Mth.lerp((double)p_114805_, p_114803_.zOld, p_114803_.getZ());
      return new Vec3($$3, $$4, $$5);
   }

   public void render(Guardian p_114829_, float p_114830_, float p_114831_, PoseStack p_114832_, MultiBufferSource p_114833_, int p_114834_) {
      super.render(p_114829_, p_114830_, p_114831_, p_114832_, p_114833_, p_114834_);
      LivingEntity $$6 = p_114829_.getActiveAttackTarget();
      if ($$6 != null) {
         float $$7 = p_114829_.getAttackAnimationScale(p_114831_);
         float $$8 = p_114829_.getClientSideAttackTime() + p_114831_;
         float $$9 = $$8 * 0.5F % 1.0F;
         float $$10 = p_114829_.getEyeHeight();
         p_114832_.pushPose();
         p_114832_.translate(0.0F, $$10, 0.0F);
         Vec3 $$11 = this.getPosition($$6, $$6.getBbHeight() * 0.5, p_114831_);
         Vec3 $$12 = this.getPosition(p_114829_, $$10, p_114831_);
         Vec3 $$13 = $$11.subtract($$12);
         float $$14 = (float)($$13.length() + 1.0);
         $$13 = $$13.normalize();
         float $$15 = (float)Math.acos($$13.y);
         float $$16 = (float)Math.atan2($$13.z, $$13.x);
         p_114832_.mulPose(Axis.YP.rotationDegrees(((float) (Math.PI / 2) - $$16) * (180.0F / (float)Math.PI)));
         p_114832_.mulPose(Axis.XP.rotationDegrees($$15 * (180.0F / (float)Math.PI)));
         int $$17 = 1;
         float $$18 = $$8 * 0.05F * -1.5F;
         float $$19 = $$7 * $$7;
         int $$20 = 64 + (int)($$19 * 191.0F);
         int $$21 = 32 + (int)($$19 * 191.0F);
         int $$22 = 128 - (int)($$19 * 64.0F);
         float $$23 = 0.2F;
         float $$24 = 0.282F;
         float $$25 = Mth.cos($$18 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
         float $$26 = Mth.sin($$18 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
         float $$27 = Mth.cos($$18 + (float) (Math.PI / 4)) * 0.282F;
         float $$28 = Mth.sin($$18 + (float) (Math.PI / 4)) * 0.282F;
         float $$29 = Mth.cos($$18 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
         float $$30 = Mth.sin($$18 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
         float $$31 = Mth.cos($$18 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
         float $$32 = Mth.sin($$18 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
         float $$33 = Mth.cos($$18 + (float) Math.PI) * 0.2F;
         float $$34 = Mth.sin($$18 + (float) Math.PI) * 0.2F;
         float $$35 = Mth.cos($$18 + 0.0F) * 0.2F;
         float $$36 = Mth.sin($$18 + 0.0F) * 0.2F;
         float $$37 = Mth.cos($$18 + (float) (Math.PI / 2)) * 0.2F;
         float $$38 = Mth.sin($$18 + (float) (Math.PI / 2)) * 0.2F;
         float $$39 = Mth.cos($$18 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
         float $$40 = Mth.sin($$18 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
         float $$42 = 0.0F;
         float $$43 = 0.4999F;
         float $$44 = -1.0F + $$9;
         float $$45 = $$14 * 2.5F + $$44;
         VertexConsumer $$46 = p_114833_.getBuffer(BEAM_RENDER_TYPE);
         PoseStack.Pose $$47 = p_114832_.last();
         vertex($$46, $$47, $$33, $$14, $$34, $$20, $$21, $$22, 0.4999F, $$45);
         vertex($$46, $$47, $$33, 0.0F, $$34, $$20, $$21, $$22, 0.4999F, $$44);
         vertex($$46, $$47, $$35, 0.0F, $$36, $$20, $$21, $$22, 0.0F, $$44);
         vertex($$46, $$47, $$35, $$14, $$36, $$20, $$21, $$22, 0.0F, $$45);
         vertex($$46, $$47, $$37, $$14, $$38, $$20, $$21, $$22, 0.4999F, $$45);
         vertex($$46, $$47, $$37, 0.0F, $$38, $$20, $$21, $$22, 0.4999F, $$44);
         vertex($$46, $$47, $$39, 0.0F, $$40, $$20, $$21, $$22, 0.0F, $$44);
         vertex($$46, $$47, $$39, $$14, $$40, $$20, $$21, $$22, 0.0F, $$45);
         float $$48 = 0.0F;
         if (p_114829_.tickCount % 2 == 0) {
            $$48 = 0.5F;
         }

         vertex($$46, $$47, $$25, $$14, $$26, $$20, $$21, $$22, 0.5F, $$48 + 0.5F);
         vertex($$46, $$47, $$27, $$14, $$28, $$20, $$21, $$22, 1.0F, $$48 + 0.5F);
         vertex($$46, $$47, $$31, $$14, $$32, $$20, $$21, $$22, 1.0F, $$48);
         vertex($$46, $$47, $$29, $$14, $$30, $$20, $$21, $$22, 0.5F, $$48);
         p_114832_.popPose();
      }
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

   public ResourceLocation getTextureLocation(Guardian p_114827_) {
      return GUARDIAN_LOCATION;
   }
}
