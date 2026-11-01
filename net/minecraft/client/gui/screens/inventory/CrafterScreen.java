package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {
   private static final Identifier DISABLED_SLOT_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/disabled_slot");
   private static final Identifier POWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/powered_redstone");
   private static final Identifier UNPOWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/unpowered_redstone");
   private static final Identifier CONTAINER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafter.png");
   private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");
   private final Player player;

   public CrafterScreen(CrafterMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.player = playerInventory.player;
   }

   @Override
   protected void init() {
      super.init();
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
      if (slot instanceof CrafterSlot && !slot.hasItem() && !this.player.isSpectator()) {
         switch (type) {
            case PICKUP:
               if (this.menu.isSlotDisabled(slotId)) {
                  this.enableSlot(slotId);
               } else if (this.menu.getCarried().isEmpty()) {
                  this.disableSlot(slotId);
               }
               break;
            case SWAP:
               ItemStack itemStack = this.player.getInventory().getItem(mouseButton);
               if (this.menu.isSlotDisabled(slotId) && !itemStack.isEmpty()) {
                  this.enableSlot(slotId);
               }
         }
      }

      super.slotClicked(slot, slotId, mouseButton, type);
   }

   private void enableSlot(int slot) {
      this.updateSlotState(slot, true);
   }

   private void disableSlot(int slot) {
      this.updateSlotState(slot, false);
   }

   private void updateSlotState(int slot, boolean state) {
      this.menu.setSlotState(slot, state);
      super.handleSlotStateChanged(slot, this.menu.containerId, state);
      float f = state ? 1.0F : 0.75F;
      this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, f);
   }

   @Override
   public void renderSlot(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY) {
      if (slot instanceof CrafterSlot crafterSlot) {
         if (this.menu.isSlotDisabled(slot.index)) {
            this.renderDisabledSlot(guiGraphics, crafterSlot);
         } else {
            super.renderSlot(guiGraphics, slot, mouseX, mouseY);
         }

         int i = this.leftPos + crafterSlot.x - 2;
         int j = this.topPos + crafterSlot.y - 2;
         if (mouseX > i && mouseY > j && mouseX < i + 19 && mouseY < j + 19) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
         }
      } else {
         super.renderSlot(guiGraphics, slot, mouseX, mouseY);
      }
   }

   private void renderDisabledSlot(GuiGraphics guiGraphics, CrafterSlot slot) {
      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISABLED_SLOT_LOCATION_SPRITE, slot.x - 1, slot.y - 1, 18, 18);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.renderRedstone(guiGraphics);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
      if (this.hoveredSlot instanceof CrafterSlot
         && !this.menu.isSlotDisabled(this.hoveredSlot.index)
         && this.menu.getCarried().isEmpty()
         && !this.hoveredSlot.hasItem()
         && !this.player.isSpectator()) {
         guiGraphics.setTooltipForNextFrame(this.font, DISABLED_SLOT_TOOLTIP, mouseX, mouseY);
      }
   }

   private void renderRedstone(GuiGraphics guiGraphics) {
      int i = this.width / 2 + 9;
      int j = this.height / 2 - 48;
      Identifier identifier;
      if (this.menu.isPowered()) {
         identifier = POWERED_REDSTONE_LOCATION_SPRITE;
      } else {
         identifier = UNPOWERED_REDSTONE_LOCATION_SPRITE;
      }

      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, i, j, 16, 16);
   }

   @Override
   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
   }
}
