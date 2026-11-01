package net.minecraft.client.model.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;

@Environment(EnvType.CLIENT)
public class SpearAnimations {
   static float progress(float porgress, float min, float max) {
      return Mth.clamp(Mth.inverseLerp(porgress, min, max), 0.0F, 1.0F);
   }

   public static <T extends HumanoidRenderState> void thirdPersonHandUse(ModelPart arm, ModelPart head, boolean isLeftArm, ItemStack stack, T renderState) {
      int i = isLeftArm ? 1 : -1;
      arm.yRot = -0.1F * i + head.yRot;
      arm.xRot = (float) (-Math.PI / 2) + head.xRot + 0.8F;
      if (renderState.isFallFlying || renderState.swimAmount > 0.0F) {
         arm.xRot -= 0.9599311F;
      }

      arm.yRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * arm.yRot, -60.0F, 60.0F);
      arm.xRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * arm.xRot, -120.0F, 30.0F);
      if (!(renderState.ticksUsingItem <= 0.0F)
         && (!renderState.isUsingItem || renderState.useItemHand == (isLeftArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND))) {
         KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
         if (kineticWeapon != null) {
            SpearAnimations.UseParams useParams = SpearAnimations.UseParams.fromKineticWeapon(kineticWeapon, renderState.ticksUsingItem);
            arm.yRot = arm.yRot + -i * useParams.swayScaleFast() * (float) (Math.PI / 180.0) * useParams.swayIntensity() * 1.0F;
            arm.zRot = arm.zRot + -i * useParams.swayScaleSlow() * (float) (Math.PI / 180.0) * useParams.swayIntensity() * 0.5F;
            arm.xRot = arm.xRot
               + (float) (Math.PI / 180.0)
                  * (
                     -40.0F * useParams.raiseProgressStart()
                        + 30.0F * useParams.raiseProgressMiddle()
                        + -20.0F * useParams.raiseProgressEnd()
                        + 20.0F * useParams.lowerProgress()
                        + 10.0F * useParams.raiseBackProgress()
                        + 0.6F * useParams.swayScaleSlow() * useParams.swayIntensity()
                  );
         }
      }
   }

   public static <S extends ArmedEntityRenderState> void thirdPersonUseItem(S renderState, PoseStack poseStack, float useTime, HumanoidArm arm, ItemStack item) {
      KineticWeapon kineticWeapon = item.get(DataComponents.KINETIC_WEAPON);
      if (kineticWeapon != null && useTime != 0.0F) {
         float f = Ease.inQuad(progress(renderState.attackTime, 0.05F, 0.2F));
         float g = Ease.inOutExpo(progress(renderState.attackTime, 0.4F, 1.0F));
         SpearAnimations.UseParams useParams = SpearAnimations.UseParams.fromKineticWeapon(kineticWeapon, useTime);
         int i = arm == HumanoidArm.RIGHT ? 1 : -1;
         float h = 1.0F - Ease.outBack(1.0F - useParams.raiseProgress());
         float j = 0.125F;
         float k = hitFeedbackAmount(renderState.ticksSinceKineticHitFeedback);
         poseStack.translate(0.0, -k * 0.4, (double)(-kineticWeapon.forwardMovement() * (h - useParams.raiseBackProgress()) + k));
         poseStack.rotateAround(
            Axis.XN.rotationDegrees(70.0F * (useParams.raiseProgress() - useParams.raiseBackProgress()) - 40.0F * (f - g)), 0.0F, -0.03125F, 0.125F
         );
         poseStack.rotateAround(Axis.YP.rotationDegrees(i * 90 * (useParams.raiseProgress() - useParams.swayProgress() + 3.0F * g + f)), 0.0F, 0.0F, 0.125F);
      }
   }

   public static <T extends HumanoidRenderState> void thirdPersonAttackHand(HumanoidModel<T> model, T renderState) {
      float f = renderState.attackTime;
      HumanoidArm humanoidArm = renderState.attackArm;
      model.rightArm.yRot = model.rightArm.yRot - model.body.yRot;
      model.leftArm.yRot = model.leftArm.yRot - model.body.yRot;
      model.leftArm.xRot = model.leftArm.xRot - model.body.yRot;
      float g = Ease.inOutSine(progress(f, 0.0F, 0.05F));
      float h = Ease.inQuad(progress(f, 0.05F, 0.2F));
      float i = Ease.inOutExpo(progress(f, 0.4F, 1.0F));
      model.getArm(humanoidArm).xRot += (90.0F * g - 120.0F * h + 30.0F * i) * (float) (Math.PI / 180.0);
   }

   public static <S extends ArmedEntityRenderState> void thirdPersonAttackItem(S renderState, PoseStack poseStack) {
      if (!(renderState.attackTime <= 0.0F)) {
         KineticWeapon kineticWeapon = renderState.getMainHandItemStack().get(DataComponents.KINETIC_WEAPON);
         float f = kineticWeapon != null ? kineticWeapon.forwardMovement() : 0.0F;
         float g = 0.125F;
         float h = renderState.attackTime;
         float i = Ease.inQuad(progress(h, 0.05F, 0.2F));
         float j = Ease.inOutExpo(progress(h, 0.4F, 1.0F));
         poseStack.rotateAround(Axis.XN.rotationDegrees(70.0F * (i - j)), 0.0F, -0.125F, 0.125F);
         poseStack.translate(0.0F, f * (i - j), 0.0F);
      }
   }

   private static float hitFeedbackAmount(float ticksSinceEnemyHit) {
      return 0.4F * (Ease.outQuart(progress(ticksSinceEnemyHit, 1.0F, 3.0F)) - Ease.inOutSine(progress(ticksSinceEnemyHit, 3.0F, 10.0F)));
   }

   public static void firstPersonUse(float ticksSinceEnemyHit, PoseStack poseStack, float useTime, HumanoidArm arm, ItemStack item) {
      KineticWeapon kineticWeapon = item.get(DataComponents.KINETIC_WEAPON);
      if (kineticWeapon != null) {
         SpearAnimations.UseParams useParams = SpearAnimations.UseParams.fromKineticWeapon(kineticWeapon, useTime);
         int i = arm == HumanoidArm.RIGHT ? 1 : -1;
         poseStack.translate(
            (double)(
               i
                  * (
                     useParams.raiseProgress() * 0.15F
                        + useParams.raiseProgressEnd() * -0.05F
                        + useParams.swayProgress() * -0.1F
                        + useParams.swayScaleSlow() * 0.005F
                  )
            ),
            (double)(useParams.raiseProgress() * -0.075F + useParams.raiseProgressMiddle() * 0.075F + useParams.swayScaleFast() * 0.01F),
            useParams.raiseProgressStart() * 0.05 + useParams.raiseProgressEnd() * -0.05 + useParams.swayScaleSlow() * 0.005F
         );
         poseStack.rotateAround(
            Axis.XP
               .rotationDegrees(
                  -65.0F * Ease.inOutBack(useParams.raiseProgress())
                     - 35.0F * useParams.lowerProgress()
                     + 100.0F * useParams.raiseBackProgress()
                     + -0.5F * useParams.swayScaleFast()
               ),
            0.0F,
            0.1F,
            0.0F
         );
         poseStack.rotateAround(
            Axis.YN
               .rotationDegrees(
                  i * (-90.0F * progress(useParams.raiseProgress(), 0.5F, 0.55F) + 90.0F * useParams.swayProgress() + 2.0F * useParams.swayScaleSlow())
               ),
            i * 0.15F,
            0.0F,
            0.0F
         );
         poseStack.translate(0.0F, -hitFeedbackAmount(ticksSinceEnemyHit), 0.0F);
      }
   }

   public static void firstPersonAttack(float swingProgress, PoseStack poseStack, int offset, HumanoidArm arm) {
      float f = Ease.inOutSine(progress(swingProgress, 0.0F, 0.05F));
      float g = Ease.outBack(progress(swingProgress, 0.05F, 0.2F));
      float h = Ease.inOutExpo(progress(swingProgress, 0.4F, 1.0F));
      poseStack.translate(offset * 0.1F * (f - g), -0.075F * (f - h), 0.65F * (f - g));
      poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F * (f - h)));
      poseStack.translate(0.0, 0.0, -0.25 * (h - g));
   }

   @Environment(EnvType.CLIENT)
   record UseParams(
      float raiseProgress,
      float raiseProgressStart,
      float raiseProgressMiddle,
      float raiseProgressEnd,
      float swayProgress,
      float lowerProgress,
      float raiseBackProgress,
      float swayIntensity,
      float swayScaleSlow,
      float swayScaleFast
   ) {
      public static SpearAnimations.UseParams fromKineticWeapon(KineticWeapon kineticWeapon, float useTime) {
         int i = kineticWeapon.delayTicks();
         int j = kineticWeapon.dismountConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         int k = j - 20;
         int l = kineticWeapon.knockbackConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         int m = l - 40;
         int n = kineticWeapon.damageConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         float f = SpearAnimations.progress(useTime, 0.0F, i);
         float g = SpearAnimations.progress(f, 0.0F, 0.5F);
         float h = SpearAnimations.progress(f, 0.5F, 0.8F);
         float o = SpearAnimations.progress(f, 0.8F, 1.0F);
         float p = SpearAnimations.progress(useTime, k, m);
         float q = Ease.outCubic(Ease.inOutElastic(SpearAnimations.progress(useTime - 20.0F, m, l)));
         float r = SpearAnimations.progress(useTime, n - 5, n);
         float s = 2.0F * Ease.outCirc(p) - 2.0F * Ease.inCirc(r);
         float t = Mth.sin(useTime * 19.0F * (float) (Math.PI / 180.0)) * s;
         float u = Mth.sin(useTime * 30.0F * (float) (Math.PI / 180.0)) * s;
         return new SpearAnimations.UseParams(f, g, h, o, p, q, r, s, t, u);
      }
   }
}
