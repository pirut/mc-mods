package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public abstract class HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>> extends AgeableMobRenderer<T, S, M> {
   public HumanoidMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
      this(context, model, model, shadowRadius);
   }

   public HumanoidMobRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel, float shadowRadius) {
      this(context, adultModel, babyModel, shadowRadius, CustomHeadLayer.Transforms.DEFAULT);
   }

   public HumanoidMobRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel, float shadowRadius, CustomHeadLayer.Transforms transforms) {
      super(context, adultModel, babyModel, shadowRadius);
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache(), transforms));
      this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
      this.addLayer(new ItemInHandLayer<>(this));
   }

   protected HumanoidModel.ArmPose getArmPose(T mob, HumanoidArm arm) {
      ItemStack itemStack = mob.getItemHeldByArm(arm);
      SwingAnimation swingAnimation = itemStack.get(DataComponents.SWING_ANIMATION);
      if (swingAnimation != null && swingAnimation.type() == SwingAnimationType.STAB && mob.swinging) {
         return HumanoidModel.ArmPose.SPEAR;
      } else {
         return itemStack.is(ItemTags.SPEARS) ? HumanoidModel.ArmPose.SPEAR : HumanoidModel.ArmPose.EMPTY;
      }
   }

   public void extractRenderState(T mob, S humanoidRenderState, float f) {
      super.extractRenderState(mob, humanoidRenderState, f);
      extractHumanoidRenderState(mob, humanoidRenderState, f, this.itemModelResolver);
      humanoidRenderState.leftArmPose = this.getArmPose(mob, HumanoidArm.LEFT);
      humanoidRenderState.rightArmPose = this.getArmPose(mob, HumanoidArm.RIGHT);
   }

   public static void extractHumanoidRenderState(LivingEntity entity, HumanoidRenderState reusedState, float partialTick, ItemModelResolver itemModelResolver) {
      ArmedEntityRenderState.extractArmedEntityRenderState(entity, reusedState, itemModelResolver, partialTick);
      reusedState.isCrouching = entity.isCrouching();
      reusedState.isFallFlying = entity.isFallFlying();
      reusedState.isVisuallySwimming = entity.isVisuallySwimming();
      reusedState.isPassenger = entity.isPassenger();
      reusedState.speedValue = 1.0F;
      if (reusedState.isFallFlying) {
         reusedState.speedValue = (float)entity.getDeltaMovement().lengthSqr();
         reusedState.speedValue /= 0.2F;
         reusedState.speedValue = reusedState.speedValue * (reusedState.speedValue * reusedState.speedValue);
      }

      if (reusedState.speedValue < 1.0F) {
         reusedState.speedValue = 1.0F;
      }

      reusedState.swimAmount = entity.getSwimAmount(partialTick);
      reusedState.attackArm = getAttackArm(entity);
      reusedState.useItemHand = entity.getUsedItemHand();
      reusedState.maxCrossbowChargeDuration = CrossbowItem.getChargeDuration(entity.getUseItem(), entity);
      reusedState.ticksUsingItem = entity.getTicksUsingItem(partialTick);
      reusedState.isUsingItem = entity.isUsingItem();
      reusedState.elytraRotX = entity.elytraAnimationState.getRotX(partialTick);
      reusedState.elytraRotY = entity.elytraAnimationState.getRotY(partialTick);
      reusedState.elytraRotZ = entity.elytraAnimationState.getRotZ(partialTick);
      reusedState.headEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.HEAD);
      reusedState.chestEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.CHEST);
      reusedState.legsEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.LEGS);
      reusedState.feetEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.FEET);
   }

   private static ItemStack getEquipmentIfRenderable(LivingEntity entity, EquipmentSlot slot) {
      ItemStack itemStack = entity.getItemBySlot(slot);
      return HumanoidArmorLayer.shouldRender(itemStack, slot) ? itemStack.copy() : ItemStack.EMPTY;
   }

   private static HumanoidArm getAttackArm(LivingEntity entity) {
      HumanoidArm humanoidArm = entity.getMainArm();
      return entity.swingingArm == InteractionHand.MAIN_HAND ? humanoidArm : humanoidArm.getOpposite();
   }
}
