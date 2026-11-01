package net.neoforged.neoforge.client.extensions.common;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.ClientHooks;
import org.jspecify.annotations.Nullable;

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

   default Model getHumanoidArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original) {
      return original;
   }

   default Model getGenericArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original) {
      Model replacement = this.getHumanoidArmorModel(itemStack, layerType, original);
      if (replacement != original) {
         if (original instanceof HumanoidModel<?> originalHumanoid && replacement instanceof HumanoidModel<?> replacementHumanoid) {
            ClientHooks.copyModelProperties(originalHumanoid, replacementHumanoid);
         }

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

   default void renderFirstPersonOverlay(ItemStack stack, EquipmentSlot equipmentSlot, Player player, GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
   }

   default boolean shouldBobAsEntity(ItemStack stack) {
      return true;
   }

   default boolean shouldSpreadAsEntity(ItemStack stack) {
      return true;
   }

   default int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int fallbackColor) {
      return EquipmentLayerRenderer.getColorForLayer(layer, fallbackColor);
   }

   default int getDefaultDyeColor(ItemStack stack) {
      return stack.is(ItemTags.DYEABLE) ? DyedItemColor.getOrDefault(stack, 0) : 0;
   }

   @Nullable
   default Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier _default) {
      return null;
   }

   default Identifier getScopeOverlayTexture(ItemStack stack) {
      return Gui.SPYGLASS_SCOPE_LOCATION;
   }

   public static enum FontContext {
      ITEM_COUNT,
      TOOLTIP,
      SELECTED_ITEM_NAME;
   }
}
