package net.neoforged.neoforge.client.extensions.common;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.Nullable;

public interface IClientItemExtensions {
   IClientItemExtensions DEFAULT = new IClientItemExtensions() {};

   static IClientItemExtensions of(ItemStack stack) {
      return of(stack.getItem());
   }

   static IClientItemExtensions of(Item item) {
      return ClientExtensionsManager.ITEM_EXTENSIONS.getOrDefault(item, DEFAULT);
   }

   @Nullable
   default Font getFont(ItemStack stack, IClientItemExtensions.FontContext context) {
      return null;
   }

   @Nullable
   default HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
      return null;
   }

   default boolean applyForgeHandTransform(
      PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess
   ) {
      return false;
   }

   default HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
      return original;
   }

   default Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
      HumanoidModel<?> replacement = this.getHumanoidArmorModel(livingEntity, itemStack, equipmentSlot, original);
      if (replacement != original) {
         ClientHooks.copyModelProperties(original, replacement);
         return replacement;
      } else {
         return original;
      }
   }

   default void setupModelAnimations(
      LivingEntity livingEntity,
      ItemStack itemStack,
      EquipmentSlot equipmentSlot,
      Model model,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
   }

   @Deprecated(
      forRemoval = true
   )
   default void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTick) {
   }

   default void renderHelmetOverlay(ItemStack stack, Player player, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      this.renderHelmetOverlay(stack, player, guiGraphics.guiWidth(), guiGraphics.guiHeight(), deltaTracker.getGameTimeDeltaPartialTick(true));
   }

   default BlockEntityWithoutLevelRenderer getCustomRenderer() {
      return Minecraft.getInstance().getItemRenderer().getBlockEntityRenderer();
   }

   default boolean shouldBobAsEntity(ItemStack stack) {
      return true;
   }

   default boolean shouldSpreadAsEntity(ItemStack stack) {
      return true;
   }

   default int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx, int fallbackColor) {
      return layer.dyeable() ? fallbackColor : -1;
   }

   default int getDefaultDyeColor(ItemStack stack) {
      return stack.is(ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, -6265536)) : -1;
   }

   default ResourceLocation getScopeOverlayTexture(ItemStack stack) {
      return Gui.SPYGLASS_SCOPE_LOCATION;
   }

   public static enum FontContext {
      ITEM_COUNT,
      TOOLTIP,
      SELECTED_ITEM_NAME;
   }
}
