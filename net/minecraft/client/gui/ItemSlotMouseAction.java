package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public interface ItemSlotMouseAction {
   boolean matches(Slot slot);

   boolean onMouseScrolled(double xOffset, double yOffset, int hoveredSlotIndex, ItemStack hoveredSlotItem);

   void onStopHovering(Slot slot);

   void onSlotClicked(Slot slot, ClickType clickType);
}
