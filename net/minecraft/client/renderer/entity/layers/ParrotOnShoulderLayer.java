package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotOnShoulderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
   private final ParrotModel model;

   public ParrotOnShoulderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> p_174511_, EntityModelSet p_174512_) {
      super(p_174511_);
      this.model = new ParrotModel(p_174512_.bakeLayer(ModelLayers.PARROT));
   }

   public void submit(PoseStack p_434235_, SubmitNodeCollector p_434714_, int p_434156_, AvatarRenderState p_446731_, float p_433591_, float p_433688_) {
      Parrot.Variant parrot$variant = p_446731_.parrotOnLeftShoulder;
      if (parrot$variant != null) {
         this.submitOnShoulder(p_434235_, p_434714_, p_434156_, p_446731_, parrot$variant, p_433591_, p_433688_, true);
      }

      Parrot.Variant parrot$variant1 = p_446731_.parrotOnRightShoulder;
      if (parrot$variant1 != null) {
         this.submitOnShoulder(p_434235_, p_434714_, p_434156_, p_446731_, parrot$variant1, p_433591_, p_433688_, false);
      }
   }

   private void submitOnShoulder(
      PoseStack p_434521_,
      SubmitNodeCollector p_432918_,
      int p_434486_,
      AvatarRenderState p_445379_,
      Parrot.Variant p_478886_,
      float p_435558_,
      float p_433589_,
      boolean p_434700_
   ) {
      p_434521_.pushPose();
      p_434521_.translate(p_434700_ ? 0.4F : -0.4F, p_445379_.isCrouching ? -1.3F : -1.5F, 0.0F);
      ParrotRenderState parrotrenderstate = new ParrotRenderState();
      parrotrenderstate.pose = ParrotModel.Pose.ON_SHOULDER;
      parrotrenderstate.ageInTicks = p_445379_.ageInTicks;
      parrotrenderstate.walkAnimationPos = p_445379_.walkAnimationPos;
      parrotrenderstate.walkAnimationSpeed = p_445379_.walkAnimationSpeed;
      parrotrenderstate.yRot = p_435558_;
      parrotrenderstate.xRot = p_433589_;
      p_432918_.submitModel(
         this.model,
         parrotrenderstate,
         p_434521_,
         this.model.renderType(ParrotRenderer.getVariantTexture(p_478886_)),
         p_434486_,
         OverlayTexture.NO_OVERLAY,
         p_445379_.outlineColor,
         null
      );
      p_434521_.popPose();
   }
}
