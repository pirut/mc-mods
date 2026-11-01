package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

@Environment(EnvType.CLIENT)
public abstract class AbstractRecipeBookScreen<T extends RecipeBookMenu> extends AbstractContainerScreen<T> implements RecipeUpdateListener {
   private final RecipeBookComponent<?> recipeBookComponent;
   private boolean widthTooNarrow;

   public AbstractRecipeBookScreen(T menu, RecipeBookComponent<?> recipeBookComponent, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.recipeBookComponent = recipeBookComponent;
   }

   @Override
   protected void init() {
      super.init();
      this.widthTooNarrow = this.width < 379;
      this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow);
      this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
      this.initButton();
   }

   protected abstract ScreenPosition getRecipeBookButtonPosition();

   private void initButton() {
      ScreenPosition screenPosition = this.getRecipeBookButtonPosition();
      this.addRenderableWidget(new ImageButton(screenPosition.x(), screenPosition.y(), 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
         this.recipeBookComponent.toggleVisibility();
         this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
         ScreenPosition screenPositionx = this.getRecipeBookButtonPosition();
         button.setPosition(screenPositionx.x(), screenPositionx.y());
         this.onRecipeBookButtonClick();
      }));
      this.addWidget(this.recipeBookComponent);
   }

   protected void onRecipeBookButtonClick() {
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
         this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      } else {
         super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
      }

      guiGraphics.nextStratum();
      this.recipeBookComponent.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.nextStratum();
      this.renderCarriedItem(guiGraphics, mouseX, mouseY);
      this.renderSnapbackItem(guiGraphics);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
      this.recipeBookComponent.renderTooltip(guiGraphics, mouseX, mouseY, this.hoveredSlot);
   }

   @Override
   protected void renderSlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      super.renderSlots(guiGraphics, mouseX, mouseY);
      this.recipeBookComponent.renderGhostRecipe(guiGraphics, this.isBiggerResultSlot());
   }

   protected boolean isBiggerResultSlot() {
      return true;
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      return this.recipeBookComponent.charTyped(event) ? true : super.charTyped(event);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      return this.recipeBookComponent.keyPressed(event) ? true : super.keyPressed(event);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      if (this.recipeBookComponent.mouseClicked(event, isDoubleClick)) {
         this.setFocused(this.recipeBookComponent);
         return true;
      } else {
         return this.widthTooNarrow && this.recipeBookComponent.isVisible() ? true : super.mouseClicked(event, isDoubleClick);
      }
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
      return this.recipeBookComponent.mouseDragged(event, mouseX, mouseY) ? true : super.mouseDragged(event, mouseX, mouseY);
   }

   @Override
   protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
      return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible()) && super.isHovering(x, y, width, height, mouseX, mouseY);
   }

   @Override
   protected boolean hasClickedOutside(double x, double y, int left, int top) {
      boolean bl = x < left || y < top || x >= left + this.imageWidth || y >= top + this.imageHeight;
      return this.recipeBookComponent.hasClickedOutside(x, y, this.leftPos, this.topPos, this.imageWidth, this.imageHeight) && bl;
   }

   @Override
   protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
      super.slotClicked(slot, slotId, mouseButton, type);
      this.recipeBookComponent.slotClicked(slot);
   }

   @Override
   public void containerTick() {
      super.containerTick();
      this.recipeBookComponent.tick();
   }

   @Override
   public void recipesUpdated() {
      this.recipeBookComponent.recipesUpdated();
   }

   @Override
   public void fillGhostRecipe(RecipeDisplay recipeDisplay) {
      this.recipeBookComponent.fillGhostRecipe(recipeDisplay);
   }
}
