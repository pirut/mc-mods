package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;

@Environment(EnvType.CLIENT)
public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel> extends RenderLayer<S, M> {
   public ItemInHandLayer(RenderLayerParent<S, M> renderLayerParent) {
      super(renderLayerParent);
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S armedEntityRenderState, float f, float g) {
      this.submitArmWithItem(
         armedEntityRenderState,
         armedEntityRenderState.rightHandItemState,
         armedEntityRenderState.rightHandItemStack,
         HumanoidArm.RIGHT,
         poseStack,
         submitNodeCollector,
         i
      );
      this.submitArmWithItem(
         armedEntityRenderState,
         armedEntityRenderState.leftHandItemState,
         armedEntityRenderState.leftHandItemStack,
         HumanoidArm.LEFT,
         poseStack,
         submitNodeCollector,
         i
      );
   }

   protected void submitArmWithItem(
      S renderState,
      ItemStackRenderState itemStackRenderState,
      ItemStack item,
      HumanoidArm arm,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight
   ) {
      if (!itemStackRenderState.isEmpty()) {
         poseStack.pushPose();
         this.getParentModel().translateToHand(renderState, arm, poseStack);
         poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         boolean bl = arm == HumanoidArm.LEFT;
         poseStack.translate((bl ? -1 : 1) / 16.0F, 0.125F, -0.625F);
         if (renderState.attackTime > 0.0F && renderState.mainArm == arm && renderState.swingAnimationType == SwingAnimationType.STAB) {
            SpearAnimations.thirdPersonAttackItem(renderState, poseStack);
         }

         float f = renderState.ticksUsingItem(arm);
         if (f != 0.0F) {
            (arm == HumanoidArm.RIGHT ? renderState.rightArmPose : renderState.leftArmPose).animateUseItem(renderState, poseStack, f, arm, item);
         }

         itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
         poseStack.popPose();
      }
   }
}
