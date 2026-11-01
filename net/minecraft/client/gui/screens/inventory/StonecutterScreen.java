package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

@Environment(EnvType.CLIENT)
public class StonecutterScreen extends AbstractContainerScreen<StonecutterMenu> {
   private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
   private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
   private static final Identifier RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
   private static final Identifier RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
   private static final Identifier RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
   private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   private static final int RECIPES_COLUMNS = 4;
   private static final int RECIPES_ROWS = 3;
   private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
   private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
   private static final int SCROLLER_FULL_HEIGHT = 54;
   private static final int RECIPES_X = 52;
   private static final int RECIPES_Y = 14;
   private float scrollOffs;
   private boolean scrolling;
   private int startIndex;
   private boolean displayRecipes;

   public StonecutterScreen(StonecutterMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      menu.registerUpdateListener(this::containerChanged);
      this.titleLabelY--;
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   @Override
   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      int i = this.leftPos;
      int j = this.topPos;
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      int k = (int)(41.0F * this.scrollOffs);
      Identifier identifier = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
      int l = i + 119;
      int m = j + 15 + k;
      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, l, m, 12, 15);
      if (mouseX >= l && mouseX < l + 12 && mouseY >= m && mouseY < m + 15) {
         guiGraphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
      }

      int n = this.leftPos + 52;
      int o = this.topPos + 14;
      int p = this.startIndex + 12;
      this.renderButtons(guiGraphics, mouseX, mouseY, n, o, p);
      this.renderRecipes(guiGraphics, n, o, p);
   }

   @Override
   protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
      super.renderTooltip(guiGraphics, x, y);
      if (this.displayRecipes) {
         int i = this.leftPos + 52;
         int j = this.topPos + 14;
         int k = this.startIndex + 12;
         SelectableRecipe.SingleInputSet<StonecutterRecipe> singleInputSet = this.menu.getVisibleRecipes();

         for (int l = this.startIndex; l < k && l < singleInputSet.size(); l++) {
            int m = l - this.startIndex;
            int n = i + m % 4 * 16;
            int o = j + m / 4 * 18 + 2;
            if (x >= n && x < n + 16 && y >= o && y < o + 18) {
               ContextMap contextMap = SlotDisplayContext.fromLevel(this.minecraft.level);
               SlotDisplay slotDisplay = singleInputSet.entries().get(l).recipe().optionDisplay();
               guiGraphics.setTooltipForNextFrame(this.font, slotDisplay.resolveForFirstStack(contextMap), x, y);
            }
         }
      }
   }

   private void renderButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int lastVisibleElementIndex) {
      for (int i = this.startIndex; i < lastVisibleElementIndex && i < this.menu.getNumberOfVisibleRecipes(); i++) {
         int j = i - this.startIndex;
         int k = x + j % 4 * 16;
         int l = j / 4;
         int m = y + l * 18 + 2;
         Identifier identifier;
         if (i == this.menu.getSelectedRecipeIndex()) {
            identifier = RECIPE_SELECTED_SPRITE;
         } else if (mouseX >= k && mouseY >= m && mouseX < k + 16 && mouseY < m + 18) {
            identifier = RECIPE_HIGHLIGHTED_SPRITE;
         } else {
            identifier = RECIPE_SPRITE;
         }

         int n = m - 1;
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, k, n, 16, 18);
         if (mouseX >= k && mouseY >= n && mouseX < k + 16 && mouseY < n + 18) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
         }
      }
   }

   private void renderRecipes(GuiGraphics guiGraphics, int x, int y, int startIndex) {
      SelectableRecipe.SingleInputSet<StonecutterRecipe> singleInputSet = this.menu.getVisibleRecipes();
      ContextMap contextMap = SlotDisplayContext.fromLevel(this.minecraft.level);

      for (int i = this.startIndex; i < startIndex && i < singleInputSet.size(); i++) {
         int j = i - this.startIndex;
         int k = x + j % 4 * 16;
         int l = j / 4;
         int m = y + l * 18 + 2;
         SlotDisplay slotDisplay = singleInputSet.entries().get(i).recipe().optionDisplay();
         guiGraphics.renderItem(slotDisplay.resolveForFirstStack(contextMap), k, m);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      if (this.displayRecipes) {
         int i = this.leftPos + 52;
         int j = this.topPos + 14;
         int k = this.startIndex + 12;

         for (int l = this.startIndex; l < k; l++) {
            int m = l - this.startIndex;
            double d = event.x() - (i + m % 4 * 16);
            double e = event.y() - (j + m / 4 * 18);
            if (d >= 0.0 && e >= 0.0 && d < 16.0 && e < 18.0 && this.menu.clickMenuButton(this.minecraft.player, l)) {
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
               this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, l);
               return true;
            }
         }

         i = this.leftPos + 119;
         j = this.topPos + 9;
         if (event.x() >= i && event.x() < i + 12 && event.y() >= j && event.y() < j + 54) {
            this.scrolling = true;
         }
      }

      return super.mouseClicked(event, isDoubleClick);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
      if (this.scrolling && this.isScrollBarActive()) {
         int i = this.topPos + 14;
         int j = i + 54;
         this.scrollOffs = ((float)event.y() - i - 7.5F) / (j - i - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.startIndex = (int)(this.scrollOffs * this.getOffscreenRows() + 0.5) * 4;
         return true;
      } else {
         return super.mouseDragged(event, mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      this.scrolling = false;
      return super.mouseReleased(event);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
         return true;
      } else {
         if (this.isScrollBarActive()) {
            int i = this.getOffscreenRows();
            float f = (float)scrollY / i;
            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
            this.startIndex = (int)(this.scrollOffs * i + 0.5) * 4;
         }

         return true;
      }
   }

   private boolean isScrollBarActive() {
      return this.displayRecipes && this.menu.getNumberOfVisibleRecipes() > 12;
   }

   protected int getOffscreenRows() {
      return (this.menu.getNumberOfVisibleRecipes() + 4 - 1) / 4 - 3;
   }

   private void containerChanged() {
      this.displayRecipes = this.menu.hasInputItem();
      if (!this.displayRecipes) {
         this.scrollOffs = 0.0F;
         this.startIndex = 0;
      }
   }
}
