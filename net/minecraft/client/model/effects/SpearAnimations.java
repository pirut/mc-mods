package net.minecraft.client.model.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpearAnimations {
   static float progress(float p_480622_, float p_479675_, float p_482024_) {
      return Mth.clamp(Mth.inverseLerp(p_480622_, p_479675_, p_482024_), 0.0F, 1.0F);
   }

   public static <T extends HumanoidRenderState> void thirdPersonHandUse(
      ModelPart p_482016_, ModelPart p_479636_, boolean p_479704_, ItemStack p_480071_, T p_479300_
   ) {
      int i = p_479704_ ? 1 : -1;
      p_482016_.yRot = -0.1F * i + p_479636_.yRot;
      p_482016_.xRot = (float) (-Math.PI / 2) + p_479636_.xRot + 0.8F;
      if (p_479300_.isFallFlying || p_479300_.swimAmount > 0.0F) {
         p_482016_.xRot -= 0.9599311F;
      }

      p_482016_.yRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * p_482016_.yRot, -60.0F, 60.0F);
      p_482016_.xRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * p_482016_.xRot, -120.0F, 30.0F);
      if (!(p_479300_.ticksUsingItem <= 0.0F)
         && (!p_479300_.isUsingItem || p_479300_.useItemHand == (p_479704_ ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND))) {
         KineticWeapon kineticweapon = p_480071_.get(DataComponents.KINETIC_WEAPON);
         if (kineticweapon != null) {
            SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, p_479300_.ticksUsingItem);
            p_482016_.yRot = p_482016_.yRot
               + -i * spearanimations$useparams.swayScaleFast() * (float) (Math.PI / 180.0) * spearanimations$useparams.swayIntensity() * 1.0F;
            p_482016_.zRot = p_482016_.zRot
               + -i * spearanimations$useparams.swayScaleSlow() * (float) (Math.PI / 180.0) * spearanimations$useparams.swayIntensity() * 0.5F;
            p_482016_.xRot = p_482016_.xRot
               + (float) (Math.PI / 180.0)
                  * (
                     -40.0F * spearanimations$useparams.raiseProgressStart()
                        + 30.0F * spearanimations$useparams.raiseProgressMiddle()
                        + -20.0F * spearanimations$useparams.raiseProgressEnd()
                        + 20.0F * spearanimations$useparams.lowerProgress()
                        + 10.0F * spearanimations$useparams.raiseBackProgress()
                        + 0.6F * spearanimations$useparams.swayScaleSlow() * spearanimations$useparams.swayIntensity()
                  );
         }
      }
   }

   public static <S extends ArmedEntityRenderState> void thirdPersonUseItem(
      S p_479589_, PoseStack p_479502_, float p_478047_, HumanoidArm p_480268_, ItemStack p_478333_
   ) {
      KineticWeapon kineticweapon = p_478333_.get(DataComponents.KINETIC_WEAPON);
      if (kineticweapon != null && p_478047_ != 0.0F) {
         float f = Ease.inQuad(progress(p_479589_.attackTime, 0.05F, 0.2F));
         float f1 = Ease.inOutExpo(progress(p_479589_.attackTime, 0.4F, 1.0F));
         SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, p_478047_);
         int i = p_480268_ == HumanoidArm.RIGHT ? 1 : -1;
         float f2 = 1.0F - Ease.outBack(1.0F - spearanimations$useparams.raiseProgress());
         float f3 = 0.125F;
         float f4 = hitFeedbackAmount(p_479589_.ticksSinceKineticHitFeedback);
         p_479502_.translate(0.0, -f4 * 0.4, (double)(-kineticweapon.forwardMovement() * (f2 - spearanimations$useparams.raiseBackProgress()) + f4));
         p_479502_.rotateAround(
            Axis.XN.rotationDegrees(70.0F * (spearanimations$useparams.raiseProgress() - spearanimations$useparams.raiseBackProgress()) - 40.0F * (f - f1)),
            0.0F,
            -0.03125F,
            0.125F
         );
         p_479502_.rotateAround(
            Axis.YP.rotationDegrees(i * 90 * (spearanimations$useparams.raiseProgress() - spearanimations$useparams.swayProgress() + 3.0F * f1 + f)),
            0.0F,
            0.0F,
            0.125F
         );
      }
   }

   public static <T extends HumanoidRenderState> void thirdPersonAttackHand(HumanoidModel<T> p_480300_, T p_481512_) {
      float f = p_481512_.attackTime;
      HumanoidArm humanoidarm = p_481512_.attackArm;
      p_480300_.rightArm.yRot = p_480300_.rightArm.yRot - p_480300_.body.yRot;
      p_480300_.leftArm.yRot = p_480300_.leftArm.yRot - p_480300_.body.yRot;
      p_480300_.leftArm.xRot = p_480300_.leftArm.xRot - p_480300_.body.yRot;
      float f1 = Ease.inOutSine(progress(f, 0.0F, 0.05F));
      float f2 = Ease.inQuad(progress(f, 0.05F, 0.2F));
      float f3 = Ease.inOutExpo(progress(f, 0.4F, 1.0F));
      p_480300_.getArm(humanoidarm).xRot += (90.0F * f1 - 120.0F * f2 + 30.0F * f3) * (float) (Math.PI / 180.0);
   }

   public static <S extends ArmedEntityRenderState> void thirdPersonAttackItem(S p_479786_, PoseStack p_481422_) {
      if (!(p_479786_.attackTime <= 0.0F)) {
         KineticWeapon kineticweapon = p_479786_.getMainHandItemStack().get(DataComponents.KINETIC_WEAPON);
         float f = kineticweapon != null ? kineticweapon.forwardMovement() : 0.0F;
         float f1 = 0.125F;
         float f2 = p_479786_.attackTime;
         float f3 = Ease.inQuad(progress(f2, 0.05F, 0.2F));
         float f4 = Ease.inOutExpo(progress(f2, 0.4F, 1.0F));
         p_481422_.rotateAround(Axis.XN.rotationDegrees(70.0F * (f3 - f4)), 0.0F, -0.125F, 0.125F);
         p_481422_.translate(0.0F, f * (f3 - f4), 0.0F);
      }
   }

   private static float hitFeedbackAmount(float p_480331_) {
      return 0.4F * (Ease.outQuart(progress(p_480331_, 1.0F, 3.0F)) - Ease.inOutSine(progress(p_480331_, 3.0F, 10.0F)));
   }

   public static void firstPersonUse(float p_480073_, PoseStack p_479047_, float p_478783_, HumanoidArm p_481636_, ItemStack p_479584_) {
      KineticWeapon kineticweapon = p_479584_.get(DataComponents.KINETIC_WEAPON);
      if (kineticweapon != null) {
         SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, p_478783_);
         int i = p_481636_ == HumanoidArm.RIGHT ? 1 : -1;
         p_479047_.translate(
            (double)(
               i
                  * (
                     spearanimations$useparams.raiseProgress() * 0.15F
                        + spearanimations$useparams.raiseProgressEnd() * -0.05F
                        + spearanimations$useparams.swayProgress() * -0.1F
                        + spearanimations$useparams.swayScaleSlow() * 0.005F
                  )
            ),
            (double)(
               spearanimations$useparams.raiseProgress() * -0.075F
                  + spearanimations$useparams.raiseProgressMiddle() * 0.075F
                  + spearanimations$useparams.swayScaleFast() * 0.01F
            ),
            spearanimations$useparams.raiseProgressStart() * 0.05
               + spearanimations$useparams.raiseProgressEnd() * -0.05
               + spearanimations$useparams.swayScaleSlow() * 0.005F
         );
         p_479047_.rotateAround(
            Axis.XP
               .rotationDegrees(
                  -65.0F * Ease.inOutBack(spearanimations$useparams.raiseProgress())
                     - 35.0F * spearanimations$useparams.lowerProgress()
                     + 100.0F * spearanimations$useparams.raiseBackProgress()
                     + -0.5F * spearanimations$useparams.swayScaleFast()
               ),
            0.0F,
            0.1F,
            0.0F
         );
         p_479047_.rotateAround(
            Axis.YN
               .rotationDegrees(
                  i
                     * (
                        -90.0F * progress(spearanimations$useparams.raiseProgress(), 0.5F, 0.55F)
                           + 90.0F * spearanimations$useparams.swayProgress()
                           + 2.0F * spearanimations$useparams.swayScaleSlow()
                     )
               ),
            i * 0.15F,
            0.0F,
            0.0F
         );
         p_479047_.translate(0.0F, -hitFeedbackAmount(p_480073_), 0.0F);
      }
   }

   public static void firstPersonAttack(float p_480658_, PoseStack p_479029_, int p_480781_, HumanoidArm p_480219_) {
      float f = Ease.inOutSine(progress(p_480658_, 0.0F, 0.05F));
      float f1 = Ease.outBack(progress(p_480658_, 0.05F, 0.2F));
      float f2 = Ease.inOutExpo(progress(p_480658_, 0.4F, 1.0F));
      p_479029_.translate(p_480781_ * 0.1F * (f - f1), -0.075F * (f - f2), 0.65F * (f - f1));
      p_479029_.mulPose(Axis.XP.rotationDegrees(-70.0F * (f - f2)));
      p_479029_.translate(0.0, 0.0, -0.25 * (f2 - f1));
   }

   @OnlyIn(Dist.CLIENT)
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
      public static SpearAnimations.UseParams fromKineticWeapon(KineticWeapon p_478069_, float p_482100_) {
         int i = p_478069_.delayTicks();
         int j = p_478069_.dismountConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         int k = j - 20;
         int l = p_478069_.knockbackConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         int i1 = l - 40;
         int j1 = p_478069_.damageConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         float f = SpearAnimations.progress(p_482100_, 0.0F, i);
         float f1 = SpearAnimations.progress(f, 0.0F, 0.5F);
         float f2 = SpearAnimations.progress(f, 0.5F, 0.8F);
         float f3 = SpearAnimations.progress(f, 0.8F, 1.0F);
         float f4 = SpearAnimations.progress(p_482100_, k, i1);
         float f5 = Ease.outCubic(Ease.inOutElastic(SpearAnimations.progress(p_482100_ - 20.0F, i1, l)));
         float f6 = SpearAnimations.progress(p_482100_, j1 - 5, j1);
         float f7 = 2.0F * Ease.outCirc(f4) - 2.0F * Ease.inCirc(f6);
         float f8 = Mth.sin(p_482100_ * 19.0F * (float) (Math.PI / 180.0)) * f7;
         float f9 = Mth.sin(p_482100_ * 30.0F * (float) (Math.PI / 180.0)) * f7;
         return new SpearAnimations.UseParams(f, f1, f2, f3, f4, f5, f6, f7, f8, f9);
      }
   }
}
