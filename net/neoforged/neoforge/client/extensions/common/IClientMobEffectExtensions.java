package net.neoforged.neoforge.client.extensions.common;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public interface IClientMobEffectExtensions {
   IClientMobEffectExtensions DEFAULT = new IClientMobEffectExtensions() {};

   static IClientMobEffectExtensions of(MobEffectInstance instance) {
      return of(instance.getEffect().value());
   }

   static IClientMobEffectExtensions of(MobEffect effect) {
      return ClientExtensionsManager.MOB_EFFECT_EXTENSIONS.getOrDefault(effect, DEFAULT);
   }

   default boolean isVisibleInInventory(MobEffectInstance instance) {
      return true;
   }

   default boolean isVisibleInGui(MobEffectInstance instance) {
      return true;
   }

   default boolean renderInventoryIcon(MobEffectInstance instance, AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset) {
      return false;
   }

   default boolean renderInventoryText(MobEffectInstance instance, AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset) {
      return false;
   }

   default boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics, int x, int y, float z, float alpha) {
      return false;
   }
}
