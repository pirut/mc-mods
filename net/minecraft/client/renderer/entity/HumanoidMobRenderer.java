package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>> extends AgeableMobRenderer<T, S, M> {
   public HumanoidMobRenderer(EntityRendererProvider.Context p_174169_, M p_174170_, float p_174171_) {
      this(p_174169_, p_174170_, p_174170_, p_174171_);
   }

   public HumanoidMobRenderer(EntityRendererProvider.Context p_174173_, M p_174174_, M p_364374_, float p_174175_) {
      this(p_174173_, p_174174_, p_364374_, p_174175_, CustomHeadLayer.Transforms.DEFAULT);
   }

   public HumanoidMobRenderer(EntityRendererProvider.Context p_361718_, M p_361903_, M p_360842_, float p_364257_, CustomHeadLayer.Transforms p_364599_) {
      super(p_361718_, p_361903_, p_360842_, p_364257_);
      this.addLayer(new CustomHeadLayer<>(this, p_361718_.getModelSet(), p_361718_.getPlayerSkinRenderCache(), p_364599_));
      this.addLayer(new WingsLayer<>(this, p_361718_.getModelSet(), p_361718_.getEquipmentRenderer()));
      this.addLayer(new ItemInHandLayer<>(this));
   }

   protected HumanoidModel.ArmPose getArmPose(T p_387903_, HumanoidArm p_387623_) {
      ItemStack itemstack = p_387903_.getItemHeldByArm(p_387623_);
      SwingAnimation swinganimation = itemstack.get(DataComponents.SWING_ANIMATION);
      if (swinganimation != null && swinganimation.type() == SwingAnimationType.STAB && p_387903_.swinging) {
         return HumanoidModel.ArmPose.SPEAR;
      } else {
         return itemstack.is(ItemTags.SPEARS) ? HumanoidModel.ArmPose.SPEAR : HumanoidModel.ArmPose.EMPTY;
      }
   }

   public void extractRenderState(T p_365075_, S p_361774_, float p_363123_) {
      super.extractRenderState(p_365075_, p_361774_, p_363123_);
      extractHumanoidRenderState(p_365075_, p_361774_, p_363123_, this.itemModelResolver);
      p_361774_.leftArmPose = this.getArmPose(p_365075_, HumanoidArm.LEFT);
      p_361774_.rightArmPose = this.getArmPose(p_365075_, HumanoidArm.RIGHT);
   }

   public static void extractHumanoidRenderState(LivingEntity p_365104_, HumanoidRenderState p_362998_, float p_363706_, ItemModelResolver p_388651_) {
      ArmedEntityRenderState.extractArmedEntityRenderState(p_365104_, p_362998_, p_388651_, p_363706_);
      p_362998_.isCrouching = p_365104_.isCrouching();
      p_362998_.isFallFlying = p_365104_.isFallFlying();
      p_362998_.isVisuallySwimming = p_365104_.isVisuallySwimming();
      p_362998_.isPassenger = p_365104_.isPassenger() && p_365104_.getVehicle() != null && p_365104_.getVehicle().shouldRiderSit();
      p_362998_.speedValue = 1.0F;
      if (p_362998_.isFallFlying) {
         p_362998_.speedValue = (float)p_365104_.getDeltaMovement().lengthSqr();
         p_362998_.speedValue /= 0.2F;
         p_362998_.speedValue = p_362998_.speedValue * (p_362998_.speedValue * p_362998_.speedValue);
      }

      if (p_362998_.speedValue < 1.0F) {
         p_362998_.speedValue = 1.0F;
      }

      p_362998_.swimAmount = p_365104_.getSwimAmount(p_363706_);
      p_362998_.attackArm = getAttackArm(p_365104_);
      p_362998_.useItemHand = p_365104_.getUsedItemHand();
      p_362998_.maxCrossbowChargeDuration = CrossbowItem.getChargeDuration(p_365104_.getUseItem(), p_365104_);
      p_362998_.ticksUsingItem = p_365104_.getTicksUsingItem(p_363706_);
      p_362998_.isUsingItem = p_365104_.isUsingItem();
      p_362998_.elytraRotX = p_365104_.elytraAnimationState.getRotX(p_363706_);
      p_362998_.elytraRotY = p_365104_.elytraAnimationState.getRotY(p_363706_);
      p_362998_.elytraRotZ = p_365104_.elytraAnimationState.getRotZ(p_363706_);
      p_362998_.headEquipment = getEquipmentIfRenderable(p_365104_, EquipmentSlot.HEAD);
      p_362998_.chestEquipment = getEquipmentIfRenderable(p_365104_, EquipmentSlot.CHEST);
      p_362998_.legsEquipment = getEquipmentIfRenderable(p_365104_, EquipmentSlot.LEGS);
      p_362998_.feetEquipment = getEquipmentIfRenderable(p_365104_, EquipmentSlot.FEET);
   }

   private static ItemStack getEquipmentIfRenderable(LivingEntity p_386637_, EquipmentSlot p_386956_) {
      ItemStack itemstack = p_386637_.getItemBySlot(p_386956_);
      return HumanoidArmorLayer.shouldRender(itemstack, p_386956_) ? itemstack.copy() : ItemStack.EMPTY;
   }

   private static HumanoidArm getAttackArm(LivingEntity p_362737_) {
      HumanoidArm humanoidarm = p_362737_.getMainArm();
      return p_362737_.swingingArm == InteractionHand.MAIN_HAND ? humanoidarm : humanoidarm.getOpposite();
   }
}
