package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ClientBundleTooltip implements ClientTooltipComponent {
   private static final Identifier PROGRESSBAR_BORDER_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_border");
   private static final Identifier PROGRESSBAR_FILL_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_fill");
   private static final Identifier PROGRESSBAR_FULL_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_full");
   private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_back");
   private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");
   private static final Identifier SLOT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_background");
   private static final int SLOT_MARGIN = 4;
   private static final int SLOT_SIZE = 24;
   private static final int GRID_WIDTH = 96;
   private static final int PROGRESSBAR_HEIGHT = 13;
   private static final int PROGRESSBAR_WIDTH = 96;
   private static final int PROGRESSBAR_BORDER = 1;
   private static final int PROGRESSBAR_FILL_MAX = 94;
   private static final int PROGRESSBAR_MARGIN_Y = 4;
   private static final Component BUNDLE_FULL_TEXT = Component.translatable("item.minecraft.bundle.full");
   private static final Component BUNDLE_EMPTY_TEXT = Component.translatable("item.minecraft.bundle.empty");
   private static final Component BUNDLE_EMPTY_DESCRIPTION = Component.translatable("item.minecraft.bundle.empty.description");
   private final BundleContents contents;

   public ClientBundleTooltip(BundleContents contents) {
      this.contents = contents;
   }

   @Override
   public int getHeight(Font font) {
      return this.contents.isEmpty() ? getEmptyBundleBackgroundHeight(font) : this.backgroundHeight();
   }

   @Override
   public int getWidth(Font font) {
      return 96;
   }

   @Override
   public boolean showTooltipWithItemInHand() {
      return true;
   }

   private static int getEmptyBundleBackgroundHeight(Font font) {
      return getEmptyBundleDescriptionTextHeight(font) + 13 + 8;
   }

   private int backgroundHeight() {
      return this.itemGridHeight() + 13 + 8;
   }

   private int itemGridHeight() {
      return this.gridSizeY() * 24;
   }

   private int getContentXOffset(int width) {
      return (width - 96) / 2;
   }

   private int gridSizeY() {
      return Mth.positiveCeilDiv(this.slotCount(), 4);
   }

   private int slotCount() {
      return Math.min(12, this.contents.size());
   }

   @Override
   public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
      if (this.contents.isEmpty()) {
         this.renderEmptyBundleTooltip(font, x, y, width, height, guiGraphics);
      } else {
         this.renderBundleWithItemsTooltip(font, x, y, width, height, guiGraphics);
      }
   }

   private void renderEmptyBundleTooltip(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
      drawEmptyBundleDescriptionText(x + this.getContentXOffset(width), y, font, guiGraphics);
      this.drawProgressbar(x + this.getContentXOffset(width), y + getEmptyBundleDescriptionTextHeight(font) + 4, font, guiGraphics);
   }

   private void renderBundleWithItemsTooltip(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
      boolean bl = this.contents.size() > 12;
      List<ItemStack> list = this.getShownItems(this.contents.getNumberOfItemsToShow());
      int i = x + this.getContentXOffset(width) + 96;
      int j = y + this.gridSizeY() * 24;
      int k = 1;

      for (int l = 1; l <= this.gridSizeY(); l++) {
         for (int m = 1; m <= 4; m++) {
            int n = i - m * 24;
            int o = j - l * 24;
            if (shouldRenderSurplusText(bl, m, l)) {
               renderCount(n, o, this.getAmountOfHiddenItems(list), font, guiGraphics);
            } else if (shouldRenderItemSlot(list, k)) {
               this.renderSlot(k, n, o, list, k, font, guiGraphics);
               k++;
            }
         }
      }

      this.drawSelectedItemTooltip(font, guiGraphics, x, y, width);
      this.drawProgressbar(x + this.getContentXOffset(width), y + this.itemGridHeight() + 4, font, guiGraphics);
   }

   private List<ItemStack> getShownItems(int itemsToShow) {
      int i = Math.min(this.contents.size(), itemsToShow);
      return this.contents.itemCopyStream().toList().subList(0, i);
   }

   private static boolean shouldRenderSurplusText(boolean hasEnoughItems, int cellX, int cellY) {
      return hasEnoughItems && cellX * cellY == 1;
   }

   private static boolean shouldRenderItemSlot(List<ItemStack> shownItems, int slotIndex) {
      return shownItems.size() >= slotIndex;
   }

   private int getAmountOfHiddenItems(List<ItemStack> shownItems) {
      return this.contents.itemCopyStream().skip(shownItems.size()).mapToInt(ItemStack::getCount).sum();
   }

   private void renderSlot(int slotIndex, int x, int y, List<ItemStack> shownItems, int seed, Font font, GuiGraphics guiGraphics) {
      int i = shownItems.size() - slotIndex;
      boolean bl = i == this.contents.getSelectedItem();
      ItemStack itemStack = shownItems.get(i);
      if (bl) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, x, y, 24, 24);
      } else {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, x, y, 24, 24);
      }

      guiGraphics.renderItem(itemStack, x + 4, y + 4, seed);
      guiGraphics.renderItemDecorations(font, itemStack, x + 4, y + 4);
      if (bl) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, x, y, 24, 24);
      }
   }

   private static void renderCount(int slotX, int slotY, int count, Font font, GuiGraphics guiGraphics) {
      guiGraphics.drawCenteredString(font, "+" + count, slotX + 12, slotY + 10, -1);
   }

   private void drawSelectedItemTooltip(Font font, GuiGraphics guiGraphics, int x, int y, int width) {
      if (this.contents.hasSelectedItem()) {
         ItemStack itemStack = this.contents.getItemUnsafe(this.contents.getSelectedItem());
         Component component = itemStack.getStyledHoverName();
         int i = font.width(component.getVisualOrderText());
         int j = x + width / 2 - 12;
         ClientTooltipComponent clientTooltipComponent = ClientTooltipComponent.create(component.getVisualOrderText());
         guiGraphics.renderTooltip(
            font, List.of(clientTooltipComponent), j - i / 2, y - 15, DefaultTooltipPositioner.INSTANCE, itemStack.get(DataComponents.TOOLTIP_STYLE)
         );
      }
   }

   private void drawProgressbar(int x, int y, Font font, GuiGraphics guiGraphics) {
      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getProgressBarTexture(), x + 1, y, this.getProgressBarFill(), 13);
      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER_SPRITE, x, y, 96, 13);
      Component component = this.getProgressBarFillText();
      if (component != null) {
         guiGraphics.drawCenteredString(font, component, x + 48, y + 3, -1);
      }
   }

   private static void drawEmptyBundleDescriptionText(int x, int y, Font font, GuiGraphics guiGraphics) {
      guiGraphics.drawWordWrap(font, BUNDLE_EMPTY_DESCRIPTION, x, y, 96, -5592406);
   }

   private static int getEmptyBundleDescriptionTextHeight(Font font) {
      return font.split(BUNDLE_EMPTY_DESCRIPTION, 96).size() * 9;
   }

   private int getProgressBarFill() {
      return Mth.clamp(Mth.mulAndTruncate(this.contents.weight(), 94), 0, 94);
   }

   private Identifier getProgressBarTexture() {
      return this.contents.weight().compareTo(Fraction.ONE) >= 0 ? PROGRESSBAR_FULL_SPRITE : PROGRESSBAR_FILL_SPRITE;
   }

   @Nullable
   private Component getProgressBarFillText() {
      if (this.contents.isEmpty()) {
         return BUNDLE_EMPTY_TEXT;
      } else {
         return this.contents.weight().compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL_TEXT : null;
      }
   }
}
