package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public abstract class ItemCombinerScreen<T extends ItemCombinerMenu> extends AbstractContainerScreen<T> implements ContainerListener {
   private final Identifier menuResource;

   public ItemCombinerScreen(T menu, Inventory playerInventory, Component title, Identifier menuResource) {
      super(menu, playerInventory, title);
      this.menuResource = menuResource;
   }

   protected void subInit() {
   }

   @Override
   protected void init() {
      super.init();
      this.subInit();
      this.menu.addSlotListener(this);
   }

   @Override
   public void removed() {
      super.removed();
      this.menu.removeSlotListener(this);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   @Override
   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.menuResource, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      this.renderErrorIcon(guiGraphics, this.leftPos, this.topPos);
   }

   protected abstract void renderErrorIcon(GuiGraphics guiGraphics, int x, int y);

   @Override
   public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {
   }

   @Override
   public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
   }
}
