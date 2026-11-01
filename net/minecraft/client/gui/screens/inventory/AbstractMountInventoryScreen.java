package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractMountInventoryScreen<T extends AbstractMountInventoryMenu> extends AbstractContainerScreen<T> {
   protected final int inventoryColumns;
   protected float xMouse;
   protected float yMouse;
   protected LivingEntity mount;

   public AbstractMountInventoryScreen(T menu, Inventory playerInventory, Component title, int inventoryColumns, LivingEntity mount) {
      super(menu, playerInventory, title);
      this.inventoryColumns = inventoryColumns;
      this.mount = mount;
   }

   @Override
   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.getBackgroundTextureLocation(), i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      if (this.inventoryColumns > 0 && this.getChestSlotsSpriteLocation() != null) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getChestSlotsSpriteLocation(), 90, 54, 0, 0, i + 79, j + 17, this.inventoryColumns * 18, 54);
      }

      if (this.shouldRenderSaddleSlot()) {
         this.drawSlot(guiGraphics, i + 7, j + 35 - 18);
      }

      if (this.shouldRenderArmorSlot()) {
         this.drawSlot(guiGraphics, i + 7, j + 35);
      }

      InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, i + 26, j + 18, i + 78, j + 70, 17, 0.25F, this.xMouse, this.yMouse, this.mount);
   }

   protected void drawSlot(GuiGraphics guiGraphics, int x, int y) {
      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSlotSpriteLocation(), x, y, 18, 18);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.xMouse = mouseX;
      this.yMouse = mouseY;
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   protected abstract Identifier getBackgroundTextureLocation();

   protected abstract Identifier getSlotSpriteLocation();

   @Nullable
   protected abstract Identifier getChestSlotsSpriteLocation();

   protected abstract boolean shouldRenderSaddleSlot();

   protected abstract boolean shouldRenderArmorSlot();
}
