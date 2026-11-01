package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

@Environment(EnvType.CLIENT)
public class MerchantScreen extends AbstractContainerScreen<MerchantMenu> {
   private static final Identifier OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/out_of_stock");
   private static final Identifier EXPERIENCE_BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_background");
   private static final Identifier EXPERIENCE_BAR_CURRENT_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_current");
   private static final Identifier EXPERIENCE_BAR_RESULT_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_result");
   private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller");
   private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller_disabled");
   private static final Identifier TRADE_ARROW_OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow_out_of_stock");
   private static final Identifier TRADE_ARROW_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow");
   private static final Identifier DISCOUNT_STRIKETHRUOGH_SPRITE = Identifier.withDefaultNamespace("container/villager/discount_strikethrough");
   private static final Identifier VILLAGER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/villager.png");
   private static final int TEXTURE_WIDTH = 512;
   private static final int TEXTURE_HEIGHT = 256;
   private static final int MERCHANT_MENU_PART_X = 99;
   private static final int PROGRESS_BAR_X = 136;
   private static final int PROGRESS_BAR_Y = 16;
   private static final int SELL_ITEM_1_X = 5;
   private static final int SELL_ITEM_2_X = 35;
   private static final int BUY_ITEM_X = 68;
   private static final int LABEL_Y = 6;
   private static final int NUMBER_OF_OFFER_BUTTONS = 7;
   private static final int TRADE_BUTTON_X = 5;
   private static final int TRADE_BUTTON_HEIGHT = 20;
   private static final int TRADE_BUTTON_WIDTH = 88;
   private static final int SCROLLER_HEIGHT = 27;
   private static final int SCROLLER_WIDTH = 6;
   private static final int SCROLL_BAR_HEIGHT = 139;
   private static final int SCROLL_BAR_TOP_POS_Y = 18;
   private static final int SCROLL_BAR_START_X = 94;
   private static final Component TRADES_LABEL = Component.translatable("merchant.trades");
   private static final Component DEPRECATED_TOOLTIP = Component.translatable("merchant.deprecated");
   private int shopItem;
   private final MerchantScreen.TradeOfferButton[] tradeOfferButtons = new MerchantScreen.TradeOfferButton[7];
   int scrollOff;
   private boolean isDragging;

   public MerchantScreen(MerchantMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 276;
      this.inventoryLabelX = 107;
   }

   private void postButtonClick() {
      this.menu.setSelectionHint(this.shopItem);
      this.menu.tryMoveItems(this.shopItem);
      this.minecraft.getConnection().send(new ServerboundSelectTradePacket(this.shopItem));
   }

   @Override
   protected void init() {
      super.init();
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      int k = j + 16 + 2;

      for (int l = 0; l < 7; l++) {
         this.tradeOfferButtons[l] = this.addRenderableWidget(new MerchantScreen.TradeOfferButton(i + 5, k, l, button -> {
            if (button instanceof MerchantScreen.TradeOfferButton) {
               this.shopItem = ((MerchantScreen.TradeOfferButton)button).getIndex() + this.scrollOff;
               this.postButtonClick();
            }
         }));
         k += 20;
      }
   }

   @Override
   protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      int i = this.menu.getTraderLevel();
      if (i > 0 && i <= 5 && this.menu.showProgressBar()) {
         Component component = Component.translatable("merchant.title", this.title, Component.translatable("merchant.level." + i));
         int j = this.font.width(component);
         int k = 49 + this.imageWidth / 2 - j / 2;
         guiGraphics.drawString(this.font, component, k, 6, -12566464, false);
      } else {
         guiGraphics.drawString(this.font, this.title, 49 + this.imageWidth / 2 - this.font.width(this.title) / 2, 6, -12566464, false);
      }

      guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
      int l = this.font.width(TRADES_LABEL);
      guiGraphics.drawString(this.font, TRADES_LABEL, 5 - l / 2 + 48, 6, -12566464, false);
   }

   @Override
   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, VILLAGER_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 512, 256);
      MerchantOffers merchantOffers = this.menu.getOffers();
      if (!merchantOffers.isEmpty()) {
         int k = this.shopItem;
         if (k < 0 || k >= merchantOffers.size()) {
            return;
         }

         MerchantOffer merchantOffer = merchantOffers.get(k);
         if (merchantOffer.isOutOfStock()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, OUT_OF_STOCK_SPRITE, this.leftPos + 83 + 99, this.topPos + 35, 28, 21);
         }
      }
   }

   private void renderProgressBar(GuiGraphics guiGraphics, int posX, int posY, MerchantOffer merchantOffer) {
      int i = this.menu.getTraderLevel();
      int j = this.menu.getTraderXp();
      if (i < 5) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_SPRITE, posX + 136, posY + 16, 102, 5);
         int k = VillagerData.getMinXpPerLevel(i);
         if (j >= k && VillagerData.canLevelUp(i)) {
            int l = 102;
            float f = 102.0F / (VillagerData.getMaxXpPerLevel(i) - k);
            int m = Math.min(Mth.floor(f * (j - k)), 102);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_CURRENT_SPRITE, 102, 5, 0, 0, posX + 136, posY + 16, m, 5);
            int n = this.menu.getFutureTraderXp();
            if (n > 0) {
               int o = Math.min(Mth.floor(n * f), 102 - m);
               guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_RESULT_SPRITE, 102, 5, m, 0, posX + 136 + m, posY + 16, o, 5);
            }
         }
      }
   }

   private void renderScroller(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, MerchantOffers offers) {
      int i = offers.size() + 1 - 7;
      if (i > 1) {
         int j = 139 - (27 + (i - 1) * 139 / i);
         int k = 1 + j / i + 139 / i;
         int l = 113;
         int m = Math.min(113, this.scrollOff * k);
         if (this.scrollOff == i - 1) {
            m = 113;
         }

         int n = x + 94;
         int o = y + 18 + m;
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, n, o, 6, 27);
         if (mouseX >= n && mouseX < x + 94 + 6 && mouseY >= o && mouseY <= o + 27) {
            guiGraphics.requestCursor(this.isDragging ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
         }
      } else {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED_SPRITE, x + 94, y + 18, 6, 27);
      }
   }

   @Override
   public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
      MerchantOffers merchantOffers = this.menu.getOffers();
      if (!merchantOffers.isEmpty()) {
         int i = (this.width - this.imageWidth) / 2;
         int j = (this.height - this.imageHeight) / 2;
         int k = j + 16 + 1;
         int l = i + 5 + 5;
         this.renderScroller(guiGraphics, i, j, mouseX, mouseY, merchantOffers);
         int m = 0;

         for (MerchantOffer merchantOffer : merchantOffers) {
            if (!this.canScroll(merchantOffers.size()) || m >= this.scrollOff && m < 7 + this.scrollOff) {
               ItemStack itemStack = merchantOffer.getBaseCostA();
               ItemStack itemStack2 = merchantOffer.getCostA();
               ItemStack itemStack3 = merchantOffer.getCostB();
               ItemStack itemStack4 = merchantOffer.getResult();
               int n = k + 2;
               this.renderAndDecorateCostA(guiGraphics, itemStack2, itemStack, l, n);
               if (!itemStack3.isEmpty()) {
                  guiGraphics.renderFakeItem(itemStack3, i + 5 + 35, n);
                  guiGraphics.renderItemDecorations(this.font, itemStack3, i + 5 + 35, n);
               }

               this.renderButtonArrows(guiGraphics, merchantOffer, i, n);
               guiGraphics.renderFakeItem(itemStack4, i + 5 + 68, n);
               guiGraphics.renderItemDecorations(this.font, itemStack4, i + 5 + 68, n);
               k += 20;
               m++;
            } else {
               m++;
            }
         }

         int o = this.shopItem;
         MerchantOffer merchantOfferx = merchantOffers.get(o);
         if (this.menu.showProgressBar()) {
            this.renderProgressBar(guiGraphics, i, j, merchantOfferx);
         }

         if (merchantOfferx.isOutOfStock() && this.isHovering(186, 35, 22, 21, mouseX, mouseY) && this.menu.canRestock()) {
            guiGraphics.setTooltipForNextFrame(this.font, DEPRECATED_TOOLTIP, mouseX, mouseY);
         }

         for (MerchantScreen.TradeOfferButton tradeOfferButton : this.tradeOfferButtons) {
            if (tradeOfferButton.isHoveredOrFocused()) {
               tradeOfferButton.renderToolTip(guiGraphics, mouseX, mouseY);
            }

            tradeOfferButton.visible = tradeOfferButton.index < this.menu.getOffers().size();
         }
      }

      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   private void renderButtonArrows(GuiGraphics guiGraphics, MerchantOffer merchantOffers, int posX, int posY) {
      if (merchantOffers.isOutOfStock()) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_OUT_OF_STOCK_SPRITE, posX + 5 + 35 + 20, posY + 3, 10, 9);
      } else {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_SPRITE, posX + 5 + 35 + 20, posY + 3, 10, 9);
      }
   }

   private void renderAndDecorateCostA(GuiGraphics guiGraphics, ItemStack realCost, ItemStack baseCost, int x, int y) {
      guiGraphics.renderFakeItem(realCost, x, y);
      if (baseCost.getCount() == realCost.getCount()) {
         guiGraphics.renderItemDecorations(this.font, realCost, x, y);
      } else {
         guiGraphics.renderItemDecorations(this.font, baseCost, x, y, baseCost.getCount() == 1 ? "1" : null);
         guiGraphics.renderItemDecorations(this.font, realCost, x + 14, y, realCost.getCount() == 1 ? "1" : null);
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISCOUNT_STRIKETHRUOGH_SPRITE, x + 7, y + 12, 9, 2);
      }
   }

   private boolean canScroll(int numOffers) {
      return numOffers > 7;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
         return true;
      } else {
         int i = this.menu.getOffers().size();
         if (this.canScroll(i)) {
            int j = i - 7;
            this.scrollOff = Mth.clamp((int)(this.scrollOff - scrollY), 0, j);
         }

         return true;
      }
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
      int i = this.menu.getOffers().size();
      if (this.isDragging) {
         int j = this.topPos + 18;
         int k = j + 139;
         int l = i - 7;
         float f = ((float)event.y() - j - 13.5F) / (k - j - 27.0F);
         f = f * l + 0.5F;
         this.scrollOff = Mth.clamp((int)f, 0, l);
         return true;
      } else {
         return super.mouseDragged(event, mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      if (this.canScroll(this.menu.getOffers().size()) && event.x() > i + 94 && event.x() < i + 94 + 6 && event.y() > j + 18 && event.y() <= j + 18 + 139 + 1) {
         this.isDragging = true;
      }

      return super.mouseClicked(event, isDoubleClick);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      this.isDragging = false;
      return super.mouseReleased(event);
   }

   @Environment(EnvType.CLIENT)
   class TradeOfferButton extends Button.Plain {
      final int index;

      public TradeOfferButton(final int x, final int y, final int index, final Button.OnPress onPress) {
         super(x, y, 88, 20, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
         this.index = index;
         this.visible = false;
      }

      public int getIndex() {
         return this.index;
      }

      public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
         if (this.isHovered && MerchantScreen.this.menu.getOffers().size() > this.index + MerchantScreen.this.scrollOff) {
            if (mouseX < this.getX() + 20) {
               ItemStack itemStack = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getCostA();
               guiGraphics.setTooltipForNextFrame(MerchantScreen.this.font, itemStack, mouseX, mouseY);
            } else if (mouseX < this.getX() + 50 && mouseX > this.getX() + 30) {
               ItemStack itemStack = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getCostB();
               if (!itemStack.isEmpty()) {
                  guiGraphics.setTooltipForNextFrame(MerchantScreen.this.font, itemStack, mouseX, mouseY);
               }
            } else if (mouseX > this.getX() + 65) {
               ItemStack itemStack = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getResult();
               guiGraphics.setTooltipForNextFrame(MerchantScreen.this.font, itemStack, mouseX, mouseY);
            }
         }
      }
   }
}
