package net.minecraft.client.gui.screens.inventory;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BookViewScreen extends Screen {
   public static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
   public static final int PAGE_TEXT_X_OFFSET = 36;
   public static final int PAGE_TEXT_Y_OFFSET = 30;
   public static final BookViewScreen.BookAccess EMPTY_ACCESS = new BookViewScreen.BookAccess(List.of());
   public static final ResourceLocation BOOK_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
   protected static final int TEXT_WIDTH = 114;
   protected static final int TEXT_HEIGHT = 128;
   protected static final int IMAGE_WIDTH = 192;
   protected static final int IMAGE_HEIGHT = 192;
   private BookViewScreen.BookAccess bookAccess;
   private int currentPage;
   private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
   private int cachedPage = -1;
   private Component pageMsg = CommonComponents.EMPTY;
   private PageButton forwardButton;
   private PageButton backButton;
   private final boolean playTurnSound;

   public BookViewScreen(BookViewScreen.BookAccess p_98264_) {
      this(p_98264_, true);
   }

   public BookViewScreen() {
      this(EMPTY_ACCESS, false);
   }

   private BookViewScreen(BookViewScreen.BookAccess p_98266_, boolean p_98267_) {
      super(GameNarrator.NO_TITLE);
      this.bookAccess = p_98266_;
      this.playTurnSound = p_98267_;
   }

   public void setBookAccess(BookViewScreen.BookAccess p_98289_) {
      this.bookAccess = p_98289_;
      this.currentPage = Mth.clamp(this.currentPage, 0, p_98289_.getPageCount());
      this.updateButtonVisibility();
      this.cachedPage = -1;
   }

   public boolean setPage(int p_98276_) {
      int $$1 = Mth.clamp(p_98276_, 0, this.bookAccess.getPageCount() - 1);
      if ($$1 != this.currentPage) {
         this.currentPage = $$1;
         this.updateButtonVisibility();
         this.cachedPage = -1;
         return true;
      } else {
         return false;
      }
   }

   protected boolean forcePage(int p_98295_) {
      return this.setPage(p_98295_);
   }

   @Override
   protected void init() {
      this.createMenuControls();
      this.createPageControlButtons();
   }

   protected void createMenuControls() {
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_315823_ -> this.onClose()).bounds(this.width / 2 - 100, 196, 200, 20).build());
   }

   protected void createPageControlButtons() {
      int $$0 = (this.width - 192) / 2;
      int $$1 = 2;
      this.forwardButton = this.addRenderableWidget(new PageButton($$0 + 116, 159, true, p_98297_ -> this.pageForward(), this.playTurnSound));
      this.backButton = this.addRenderableWidget(new PageButton($$0 + 43, 159, false, p_98287_ -> this.pageBack(), this.playTurnSound));
      this.updateButtonVisibility();
   }

   private int getNumPages() {
      return this.bookAccess.getPageCount();
   }

   protected void pageBack() {
      if (this.currentPage > 0) {
         this.currentPage--;
      }

      this.updateButtonVisibility();
   }

   protected void pageForward() {
      if (this.currentPage < this.getNumPages() - 1) {
         this.currentPage++;
      }

      this.updateButtonVisibility();
   }

   private void updateButtonVisibility() {
      this.forwardButton.visible = this.currentPage < this.getNumPages() - 1;
      this.backButton.visible = this.currentPage > 0;
   }

   @Override
   public boolean keyPressed(int p_98278_, int p_98279_, int p_98280_) {
      if (super.keyPressed(p_98278_, p_98279_, p_98280_)) {
         return true;
      } else {
         switch (p_98278_) {
            case 266:
               this.backButton.onPress();
               return true;
            case 267:
               this.forwardButton.onPress();
               return true;
            default:
               return false;
         }
      }
   }

   @Override
   public void render(GuiGraphics p_281997_, int p_281262_, int p_283321_, float p_282251_) {
      super.render(p_281997_, p_281262_, p_283321_, p_282251_);
      int $$4 = (this.width - 192) / 2;
      int $$5 = 2;
      if (this.cachedPage != this.currentPage) {
         FormattedText $$6 = this.bookAccess.getPage(this.currentPage);
         this.cachedPageComponents = this.font.split($$6, 114);
         this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
      }

      this.cachedPage = this.currentPage;
      int $$7 = this.font.width(this.pageMsg);
      p_281997_.drawString(this.font, this.pageMsg, $$4 - $$7 + 192 - 44, 18, 0, false);
      int $$8 = Math.min(128 / 9, this.cachedPageComponents.size());

      for (int $$9 = 0; $$9 < $$8; $$9++) {
         FormattedCharSequence $$10 = this.cachedPageComponents.get($$9);
         p_281997_.drawString(this.font, $$10, $$4 + 36, 32 + $$9 * 9, 0, false);
      }

      Style $$11 = this.getClickedComponentStyleAt(p_281262_, p_283321_);
      if ($$11 != null) {
         p_281997_.renderComponentHoverEffect(this.font, $$11, p_281262_, p_283321_);
      }
   }

   @Override
   public void renderBackground(GuiGraphics p_295678_, int p_296491_, int p_294260_, float p_294869_) {
      this.renderTransparentBackground(p_295678_);
      p_295678_.blit(BOOK_LOCATION, (this.width - 192) / 2, 2, 0, 0, 192, 192);
   }

   @Override
   public boolean mouseClicked(double p_98272_, double p_98273_, int p_98274_) {
      if (p_98274_ == 0) {
         Style $$3 = this.getClickedComponentStyleAt(p_98272_, p_98273_);
         if ($$3 != null && this.handleComponentClicked($$3)) {
            return true;
         }
      }

      return super.mouseClicked(p_98272_, p_98273_, p_98274_);
   }

   @Override
   public boolean handleComponentClicked(Style p_98293_) {
      ClickEvent $$1 = p_98293_.getClickEvent();
      if ($$1 == null) {
         return false;
      } else if ($$1.getAction() == ClickEvent.Action.CHANGE_PAGE) {
         String $$2 = $$1.getValue();

         try {
            int $$3 = Integer.parseInt($$2) - 1;
            return this.forcePage($$3);
         } catch (Exception var5) {
            return false;
         }
      } else {
         boolean $$4 = super.handleComponentClicked(p_98293_);
         if ($$4 && $$1.getAction() == ClickEvent.Action.RUN_COMMAND) {
            this.closeScreen();
         }

         return $$4;
      }
   }

   protected void closeScreen() {
      this.minecraft.setScreen(null);
   }

   @Nullable
   public Style getClickedComponentStyleAt(double p_98269_, double p_98270_) {
      if (this.cachedPageComponents.isEmpty()) {
         return null;
      } else {
         int $$2 = Mth.floor(p_98269_ - (this.width - 192) / 2 - 36.0);
         int $$3 = Mth.floor(p_98270_ - 2.0 - 30.0);
         if ($$2 >= 0 && $$3 >= 0) {
            int $$4 = Math.min(128 / 9, this.cachedPageComponents.size());
            if ($$2 <= 114 && $$3 < 9 * $$4 + $$4) {
               int $$5 = $$3 / 9;
               if ($$5 >= 0 && $$5 < this.cachedPageComponents.size()) {
                  FormattedCharSequence $$6 = this.cachedPageComponents.get($$5);
                  return this.minecraft.font.getSplitter().componentStyleAtWidth($$6, $$2);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record BookAccess(List<Component> pages) {
      public int getPageCount() {
         return this.pages.size();
      }

      public FormattedText getPage(int p_98311_) {
         return p_98311_ >= 0 && p_98311_ < this.getPageCount() ? this.pages.get(p_98311_) : FormattedText.EMPTY;
      }

      @Nullable
      public static BookViewScreen.BookAccess fromItem(ItemStack p_98309_) {
         boolean $$1 = Minecraft.getInstance().isTextFilteringEnabled();
         WrittenBookContent $$2 = p_98309_.get(DataComponents.WRITTEN_BOOK_CONTENT);
         if ($$2 != null) {
            return new BookViewScreen.BookAccess($$2.getPages($$1));
         } else {
            WritableBookContent $$3 = p_98309_.get(DataComponents.WRITABLE_BOOK_CONTENT);
            return $$3 != null ? new BookViewScreen.BookAccess($$3.getPages($$1).map(Component::literal).toList()) : null;
         }
      }
   }
}
