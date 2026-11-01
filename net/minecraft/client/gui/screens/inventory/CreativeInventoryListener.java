package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class CreativeInventoryListener implements ContainerListener {
   private final Minecraft minecraft;

   public CreativeInventoryListener(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
      this.minecraft.gameMode.handleCreativeModeItemAdd(stack, dataSlotIndex);
   }

   @Override
   public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {
   }
}
