package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

@OnlyIn(Dist.CLIENT)
public class BookEditScreen extends Screen {
   private static final int TEXT_WIDTH = 114;
   private static final int TEXT_HEIGHT = 128;
   private static final int IMAGE_WIDTH = 192;
   private static final int IMAGE_HEIGHT = 192;
   private static final Component EDIT_TITLE_LABEL = Component.translatable("book.editTitle");
   private static final Component FINALIZE_WARNING_LABEL = Component.translatable("book.finalizeWarning");
   private static final FormattedCharSequence BLACK_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.BLACK));
   private static final FormattedCharSequence GRAY_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.GRAY));
   private final Player owner;
   private final ItemStack book;
   private boolean isModified;
   private boolean isSigning;
   private int frameTick;
   private int currentPage;
   private final List<String> pages = Lists.newArrayList();
   private String title = "";
   private final TextFieldHelper pageEdit = new TextFieldHelper(
      this::getCurrentPageText,
      this::setCurrentPageText,
      this::getClipboard,
      this::setClipboard,
      p_280853_ -> p_280853_.length() < 1024 && this.font.wordWrapHeight(p_280853_, 114) <= 128
   );
   private final TextFieldHelper titleEdit = new TextFieldHelper(
      () -> this.title, p_98175_ -> this.title = p_98175_, this::getClipboard, this::setClipboard, p_98170_ -> p_98170_.length() < 16
   );
   private long lastClickTime;
   private int lastIndex = -1;
   private PageButton forwardButton;
   private PageButton backButton;
   private Button doneButton;
   private Button signButton;
   private Button finalizeButton;
   private Button cancelButton;
   private final InteractionHand hand;
   @Nullable
   private BookEditScreen.DisplayCache displayCache = BookEditScreen.DisplayCache.EMPTY;
   private Component pageMsg = CommonComponents.EMPTY;
   private final Component ownerText;

   public BookEditScreen(Player p_98076_, ItemStack p_98077_, InteractionHand p_98078_) {
      super(GameNarrator.NO_TITLE);
      this.owner = p_98076_;
      this.book = p_98077_;
      this.hand = p_98078_;
      WritableBookContent $$3 = p_98077_.get(DataComponents.WRITABLE_BOOK_CONTENT);
      if ($$3 != null) {
         $$3.getPages(Minecraft.getInstance().isTextFilteringEnabled()).forEach(this.pages::add);
      }

      if (this.pages.isEmpty()) {
         this.pages.add("");
      }

      this.ownerText = Component.translatable("book.byAuthor", p_98076_.getName()).withStyle(ChatFormatting.DARK_GRAY);
   }

   private void setClipboard(String p_98148_) {
      if (this.minecraft != null) {
         TextFieldHelper.setClipboardContents(this.minecraft, p_98148_);
      }
   }

   private String getClipboard() {
      return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
   }

   private int getNumPages() {
      return this.pages.size();
   }

   @Override
   public void tick() {
      super.tick();
      this.frameTick++;
   }

   @Override
   protected void init() {
      this.clearDisplayCache();
      this.signButton = this.addRenderableWidget(Button.builder(Component.translatable("book.signButton"), p_98177_ -> {
         this.isSigning = true;
         this.updateButtonVisibility();
      }).bounds(this.width / 2 - 100, 196, 98, 20).build());
      this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_280851_ -> {
         this.minecraft.setScreen(null);
         this.saveChanges(false);
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
      this.finalizeButton = this.addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), p_280852_ -> {
         if (this.isSigning) {
            this.saveChanges(true);
            this.minecraft.setScreen(null);
         }
      }).bounds(this.width / 2 - 100, 196, 98, 20).build());
      this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_98157_ -> {
         if (this.isSigning) {
            this.isSigning = false;
         }

         this.updateButtonVisibility();
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
      int $$0 = (this.width - 192) / 2;
      int $$1 = 2;
      this.forwardButton = this.addRenderableWidget(new PageButton($$0 + 116, 159, true, p_98144_ -> this.pageForward(), true));
      this.backButton = this.addRenderableWidget(new PageButton($$0 + 43, 159, false, p_98113_ -> this.pageBack(), true));
      this.updateButtonVisibility();
   }

   private void pageBack() {
      if (this.currentPage > 0) {
         this.currentPage--;
      }

      this.updateButtonVisibility();
      this.clearDisplayCacheAfterPageChange();
   }

   private void pageForward() {
      if (this.currentPage < this.getNumPages() - 1) {
         this.currentPage++;
      } else {
         this.appendPageToBook();
         if (this.currentPage < this.getNumPages() - 1) {
            this.currentPage++;
         }
      }

      this.updateButtonVisibility();
      this.clearDisplayCacheAfterPageChange();
   }

   private void updateButtonVisibility() {
      this.backButton.visible = !this.isSigning && this.currentPage > 0;
      this.forwardButton.visible = !this.isSigning;
      this.doneButton.visible = !this.isSigning;
      this.signButton.visible = !this.isSigning;
      this.cancelButton.visible = this.isSigning;
      this.finalizeButton.visible = this.isSigning;
      this.finalizeButton.active = !StringUtil.isBlank(this.title);
   }

   private void eraseEmptyTrailingPages() {
      ListIterator<String> $$0 = this.pages.listIterator(this.pages.size());

      while ($$0.hasPrevious() && $$0.previous().isEmpty()) {
         $$0.remove();
      }
   }

   private void saveChanges(boolean p_98161_) {
      if (this.isModified) {
         this.eraseEmptyTrailingPages();
         this.updateLocalCopy();
         int $$1 = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().selected : 40;
         this.minecraft.getConnection().send(new ServerboundEditBookPacket($$1, this.pages, p_98161_ ? Optional.of(this.title.trim()) : Optional.empty()));
      }
   }

   private void updateLocalCopy() {
      this.book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(this.pages.stream().map(Filterable::passThrough).toList()));
   }

   private void appendPageToBook() {
      if (this.getNumPages() < 100) {
         this.pages.add("");
         this.isModified = true;
      }
   }

   @Override
   public boolean keyPressed(int p_98100_, int p_98101_, int p_98102_) {
      if (super.keyPressed(p_98100_, p_98101_, p_98102_)) {
         return true;
      } else if (this.isSigning) {
         return this.titleKeyPressed(p_98100_, p_98101_, p_98102_);
      } else {
         boolean $$3 = this.bookKeyPressed(p_98100_, p_98101_, p_98102_);
         if ($$3) {
            this.clearDisplayCache();
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean charTyped(char p_98085_, int p_98086_) {
      if (super.charTyped(p_98085_, p_98086_)) {
         return true;
      } else if (this.isSigning) {
         boolean $$2 = this.titleEdit.charTyped(p_98085_);
         if ($$2) {
            this.updateButtonVisibility();
            this.isModified = true;
            return true;
         } else {
            return false;
         }
      } else if (StringUtil.isAllowedChatCharacter(p_98085_)) {
         this.pageEdit.insertText(Character.toString(p_98085_));
         this.clearDisplayCache();
         return true;
      } else {
         return false;
      }
   }

   private boolean bookKeyPressed(int p_98153_, int p_98154_, int p_98155_) {
      if (Screen.isSelectAll(p_98153_)) {
         this.pageEdit.selectAll();
         return true;
      } else if (Screen.isCopy(p_98153_)) {
         this.pageEdit.copy();
         return true;
      } else if (Screen.isPaste(p_98153_)) {
         this.pageEdit.paste();
         return true;
      } else if (Screen.isCut(p_98153_)) {
         this.pageEdit.cut();
         return true;
      } else {
         TextFieldHelper.CursorStep $$3 = Screen.hasControlDown() ? TextFieldHelper.CursorStep.WORD : TextFieldHelper.CursorStep.CHARACTER;
         switch (p_98153_) {
            case 257:
            case 335:
               this.pageEdit.insertText("\n");
               return true;
            case 259:
               this.pageEdit.removeFromCursor(-1, $$3);
               return true;
            case 261:
               this.pageEdit.removeFromCursor(1, $$3);
               return true;
            case 262:
               this.pageEdit.moveBy(1, Screen.hasShiftDown(), $$3);
               return true;
            case 263:
               this.pageEdit.moveBy(-1, Screen.hasShiftDown(), $$3);
               return true;
            case 264:
               this.keyDown();
               return true;
            case 265:
               this.keyUp();
               return true;
            case 266:
               this.backButton.onPress();
               return true;
            case 267:
               this.forwardButton.onPress();
               return true;
            case 268:
               this.keyHome();
               return true;
            case 269:
               this.keyEnd();
               return true;
            default:
               return false;
         }
      }
   }

   private void keyUp() {
      this.changeLine(-1);
   }

   private void keyDown() {
      this.changeLine(1);
   }

   private void changeLine(int p_98098_) {
      int $$1 = this.pageEdit.getCursorPos();
      int $$2 = this.getDisplayCache().changeLine($$1, p_98098_);
      this.pageEdit.setCursorPos($$2, Screen.hasShiftDown());
   }

   private void keyHome() {
      if (Screen.hasControlDown()) {
         this.pageEdit.setCursorToStart(Screen.hasShiftDown());
      } else {
         int $$0 = this.pageEdit.getCursorPos();
         int $$1 = this.getDisplayCache().findLineStart($$0);
         this.pageEdit.setCursorPos($$1, Screen.hasShiftDown());
      }
   }

   private void keyEnd() {
      if (Screen.hasControlDown()) {
         this.pageEdit.setCursorToEnd(Screen.hasShiftDown());
      } else {
         BookEditScreen.DisplayCache $$0 = this.getDisplayCache();
         int $$1 = this.pageEdit.getCursorPos();
         int $$2 = $$0.findLineEnd($$1);
         this.pageEdit.setCursorPos($$2, Screen.hasShiftDown());
      }
   }

   private boolean titleKeyPressed(int p_98164_, int p_98165_, int p_98166_) {
      switch (p_98164_) {
         case 257:
         case 335:
            if (!this.title.isEmpty()) {
               this.saveChanges(true);
               this.minecraft.setScreen(null);
            }

            return true;
         case 259:
            this.titleEdit.removeCharsFromCursor(-1);
            this.updateButtonVisibility();
            this.isModified = true;
            return true;
         default:
            return false;
      }
   }

   private String getCurrentPageText() {
      return this.currentPage >= 0 && this.currentPage < this.pages.size() ? this.pages.get(this.currentPage) : "";
   }

   private void setCurrentPageText(String p_98159_) {
      if (this.currentPage >= 0 && this.currentPage < this.pages.size()) {
         this.pages.set(this.currentPage, p_98159_);
         this.isModified = true;
         this.clearDisplayCache();
      }
   }

   @Override
   public void render(GuiGraphics p_281724_, int p_282965_, int p_283294_, float p_281293_) {
      super.render(p_281724_, p_282965_, p_283294_, p_281293_);
      this.setFocused(null);
      int $$4 = (this.width - 192) / 2;
      int $$5 = 2;
      if (this.isSigning) {
         boolean $$6 = this.frameTick / 6 % 2 == 0;
         FormattedCharSequence $$7 = FormattedCharSequence.composite(FormattedCharSequence.forward(this.title, Style.EMPTY), $$6 ? BLACK_CURSOR : GRAY_CURSOR);
         int $$8 = this.font.width(EDIT_TITLE_LABEL);
         p_281724_.drawString(this.font, EDIT_TITLE_LABEL, $$4 + 36 + (114 - $$8) / 2, 34, 0, false);
         int $$9 = this.font.width($$7);
         p_281724_.drawString(this.font, $$7, $$4 + 36 + (114 - $$9) / 2, 50, 0, false);
         int $$10 = this.font.width(this.ownerText);
         p_281724_.drawString(this.font, this.ownerText, $$4 + 36 + (114 - $$10) / 2, 60, 0, false);
         p_281724_.drawWordWrap(this.font, FINALIZE_WARNING_LABEL, $$4 + 36, 82, 114, 0);
      } else {
         int $$11 = this.font.width(this.pageMsg);
         p_281724_.drawString(this.font, this.pageMsg, $$4 - $$11 + 192 - 44, 18, 0, false);
         BookEditScreen.DisplayCache $$12 = this.getDisplayCache();

         for (BookEditScreen.LineInfo $$13 : $$12.lines) {
            p_281724_.drawString(this.font, $$13.asComponent, $$13.x, $$13.y, -16777216, false);
         }

         this.renderHighlight(p_281724_, $$12.selection);
         this.renderCursor(p_281724_, $$12.cursor, $$12.cursorAtEnd);
      }
   }

   @Override
   public void renderBackground(GuiGraphics p_294860_, int p_295019_, int p_294307_, float p_295562_) {
      this.renderTransparentBackground(p_294860_);
      p_294860_.blit(BookViewScreen.BOOK_LOCATION, (this.width - 192) / 2, 2, 0, 0, 192, 192);
   }

   private void renderCursor(GuiGraphics p_281833_, BookEditScreen.Pos2i p_282190_, boolean p_282412_) {
      if (this.frameTick / 6 % 2 == 0) {
         p_282190_ = this.convertLocalToScreen(p_282190_);
         if (!p_282412_) {
            p_281833_.fill(p_282190_.x, p_282190_.y - 1, p_282190_.x + 1, p_282190_.y + 9, -16777216);
         } else {
            p_281833_.drawString(this.font, "_", p_282190_.x, p_282190_.y, 0, false);
         }
      }
   }

   private void renderHighlight(GuiGraphics p_282188_, Rect2i[] p_265482_) {
      for (Rect2i $$2 : p_265482_) {
         int $$3 = $$2.getX();
         int $$4 = $$2.getY();
         int $$5 = $$3 + $$2.getWidth();
         int $$6 = $$4 + $$2.getHeight();
         p_282188_.fill(RenderType.guiTextHighlight(), $$3, $$4, $$5, $$6, -16776961);
      }
   }

   private BookEditScreen.Pos2i convertScreenToLocal(BookEditScreen.Pos2i p_98115_) {
      return new BookEditScreen.Pos2i(p_98115_.x - (this.width - 192) / 2 - 36, p_98115_.y - 32);
   }

   private BookEditScreen.Pos2i convertLocalToScreen(BookEditScreen.Pos2i p_98146_) {
      return new BookEditScreen.Pos2i(p_98146_.x + (this.width - 192) / 2 + 36, p_98146_.y + 32);
   }

   @Override
   public boolean mouseClicked(double p_98088_, double p_98089_, int p_98090_) {
      if (super.mouseClicked(p_98088_, p_98089_, p_98090_)) {
         return true;
      } else {
         if (p_98090_ == 0) {
            long $$3 = Util.getMillis();
            BookEditScreen.DisplayCache $$4 = this.getDisplayCache();
            int $$5 = $$4.getIndexAtPosition(this.font, this.convertScreenToLocal(new BookEditScreen.Pos2i((int)p_98088_, (int)p_98089_)));
            if ($$5 >= 0) {
               if ($$5 != this.lastIndex || $$3 - this.lastClickTime >= 250L) {
                  this.pageEdit.setCursorPos($$5, Screen.hasShiftDown());
               } else if (!this.pageEdit.isSelecting()) {
                  this.selectWord($$5);
               } else {
                  this.pageEdit.selectAll();
               }

               this.clearDisplayCache();
            }

            this.lastIndex = $$5;
            this.lastClickTime = $$3;
         }

         return true;
      }
   }

   private void selectWord(int p_98142_) {
      String $$1 = this.getCurrentPageText();
      this.pageEdit.setSelectionRange(StringSplitter.getWordPosition($$1, -1, p_98142_, false), StringSplitter.getWordPosition($$1, 1, p_98142_, false));
   }

   @Override
   public boolean mouseDragged(double p_98092_, double p_98093_, int p_98094_, double p_98095_, double p_98096_) {
      if (super.mouseDragged(p_98092_, p_98093_, p_98094_, p_98095_, p_98096_)) {
         return true;
      } else {
         if (p_98094_ == 0) {
            BookEditScreen.DisplayCache $$5 = this.getDisplayCache();
            int $$6 = $$5.getIndexAtPosition(this.font, this.convertScreenToLocal(new BookEditScreen.Pos2i((int)p_98092_, (int)p_98093_)));
            this.pageEdit.setCursorPos($$6, true);
            this.clearDisplayCache();
         }

         return true;
      }
   }

   private BookEditScreen.DisplayCache getDisplayCache() {
      if (this.displayCache == null) {
         this.displayCache = this.rebuildDisplayCache();
         this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, this.getNumPages());
      }

      return this.displayCache;
   }

   private void clearDisplayCache() {
      this.displayCache = null;
   }

   private void clearDisplayCacheAfterPageChange() {
      this.pageEdit.setCursorToEnd();
      this.clearDisplayCache();
   }

   private BookEditScreen.DisplayCache rebuildDisplayCache() {
      String $$0 = this.getCurrentPageText();
      if ($$0.isEmpty()) {
         return BookEditScreen.DisplayCache.EMPTY;
      } else {
         int $$1 = this.pageEdit.getCursorPos();
         int $$2 = this.pageEdit.getSelectionPos();
         IntList $$3 = new IntArrayList();
         List<BookEditScreen.LineInfo> $$4 = Lists.newArrayList();
         MutableInt $$5 = new MutableInt();
         MutableBoolean $$6 = new MutableBoolean();
         StringSplitter $$7 = this.font.getSplitter();
         $$7.splitLines($$0, 114, Style.EMPTY, true, (p_98132_, p_98133_, p_98134_) -> {
            int $$8x = $$5.getAndIncrement();
            String $$9x = $$0.substring(p_98133_, p_98134_);
            $$6.setValue($$9x.endsWith("\n"));
            String $$10 = StringUtils.stripEnd($$9x, " \n");
            int $$11 = $$8x * 9;
            BookEditScreen.Pos2i $$12 = this.convertLocalToScreen(new BookEditScreen.Pos2i(0, $$11));
            $$3.add(p_98133_);
            $$4.add(new BookEditScreen.LineInfo(p_98132_, $$10, $$12.x, $$12.y));
         });
         int[] $$8 = $$3.toIntArray();
         boolean $$9 = $$1 == $$0.length();
         BookEditScreen.Pos2i $$10;
         if ($$9 && $$6.isTrue()) {
            $$10 = new BookEditScreen.Pos2i(0, $$4.size() * 9);
         } else {
            int $$11 = findLineFromPos($$8, $$1);
            int $$12 = this.font.width($$0.substring($$8[$$11], $$1));
            $$10 = new BookEditScreen.Pos2i($$12, $$11 * 9);
         }

         List<Rect2i> $$14 = Lists.newArrayList();
         if ($$1 != $$2) {
            int $$15 = Math.min($$1, $$2);
            int $$16 = Math.max($$1, $$2);
            int $$17 = findLineFromPos($$8, $$15);
            int $$18 = findLineFromPos($$8, $$16);
            if ($$17 == $$18) {
               int $$19 = $$17 * 9;
               int $$20 = $$8[$$17];
               $$14.add(this.createPartialLineSelection($$0, $$7, $$15, $$16, $$19, $$20));
            } else {
               int $$21 = $$17 + 1 > $$8.length ? $$0.length() : $$8[$$17 + 1];
               $$14.add(this.createPartialLineSelection($$0, $$7, $$15, $$21, $$17 * 9, $$8[$$17]));

               for (int $$22 = $$17 + 1; $$22 < $$18; $$22++) {
                  int $$23 = $$22 * 9;
                  String $$24 = $$0.substring($$8[$$22], $$8[$$22 + 1]);
                  int $$25 = (int)$$7.stringWidth($$24);
                  $$14.add(this.createSelection(new BookEditScreen.Pos2i(0, $$23), new BookEditScreen.Pos2i($$25, $$23 + 9)));
               }

               $$14.add(this.createPartialLineSelection($$0, $$7, $$8[$$18], $$16, $$18 * 9, $$8[$$18]));
            }
         }

         return new BookEditScreen.DisplayCache($$0, $$10, $$9, $$8, $$4.toArray(new BookEditScreen.LineInfo[0]), $$14.toArray(new Rect2i[0]));
      }
   }

   static int findLineFromPos(int[] p_98150_, int p_98151_) {
      int $$2 = Arrays.binarySearch(p_98150_, p_98151_);
      return $$2 < 0 ? -($$2 + 2) : $$2;
   }

   private Rect2i createPartialLineSelection(String p_98120_, StringSplitter p_98121_, int p_98122_, int p_98123_, int p_98124_, int p_98125_) {
      String $$6 = p_98120_.substring(p_98125_, p_98122_);
      String $$7 = p_98120_.substring(p_98125_, p_98123_);
      BookEditScreen.Pos2i $$8 = new BookEditScreen.Pos2i((int)p_98121_.stringWidth($$6), p_98124_);
      BookEditScreen.Pos2i $$9 = new BookEditScreen.Pos2i((int)p_98121_.stringWidth($$7), p_98124_ + 9);
      return this.createSelection($$8, $$9);
   }

   private Rect2i createSelection(BookEditScreen.Pos2i p_98117_, BookEditScreen.Pos2i p_98118_) {
      BookEditScreen.Pos2i $$2 = this.convertLocalToScreen(p_98117_);
      BookEditScreen.Pos2i $$3 = this.convertLocalToScreen(p_98118_);
      int $$4 = Math.min($$2.x, $$3.x);
      int $$5 = Math.max($$2.x, $$3.x);
      int $$6 = Math.min($$2.y, $$3.y);
      int $$7 = Math.max($$2.y, $$3.y);
      return new Rect2i($$4, $$6, $$5 - $$4, $$7 - $$6);
   }

   @OnlyIn(Dist.CLIENT)
   static class DisplayCache {
      static final BookEditScreen.DisplayCache EMPTY = new BookEditScreen.DisplayCache(
         "",
         new BookEditScreen.Pos2i(0, 0),
         true,
         new int[]{0},
         new BookEditScreen.LineInfo[]{new BookEditScreen.LineInfo(Style.EMPTY, "", 0, 0)},
         new Rect2i[0]
      );
      private final String fullText;
      final BookEditScreen.Pos2i cursor;
      final boolean cursorAtEnd;
      private final int[] lineStarts;
      final BookEditScreen.LineInfo[] lines;
      final Rect2i[] selection;

      public DisplayCache(
         String p_98201_, BookEditScreen.Pos2i p_98202_, boolean p_98203_, int[] p_98204_, BookEditScreen.LineInfo[] p_98205_, Rect2i[] p_98206_
      ) {
         this.fullText = p_98201_;
         this.cursor = p_98202_;
         this.cursorAtEnd = p_98203_;
         this.lineStarts = p_98204_;
         this.lines = p_98205_;
         this.selection = p_98206_;
      }

      public int getIndexAtPosition(Font p_98214_, BookEditScreen.Pos2i p_98215_) {
         int $$2 = p_98215_.y / 9;
         if ($$2 < 0) {
            return 0;
         } else if ($$2 >= this.lines.length) {
            return this.fullText.length();
         } else {
            BookEditScreen.LineInfo $$3 = this.lines[$$2];
            return this.lineStarts[$$2] + p_98214_.getSplitter().plainIndexAtWidth($$3.contents, p_98215_.x, $$3.style);
         }
      }

      public int changeLine(int p_98211_, int p_98212_) {
         int $$2 = BookEditScreen.findLineFromPos(this.lineStarts, p_98211_);
         int $$3 = $$2 + p_98212_;
         int $$6;
         if (0 <= $$3 && $$3 < this.lineStarts.length) {
            int $$4 = p_98211_ - this.lineStarts[$$2];
            int $$5 = this.lines[$$3].contents.length();
            $$6 = this.lineStarts[$$3] + Math.min($$4, $$5);
         } else {
            $$6 = p_98211_;
         }

         return $$6;
      }

      public int findLineStart(int p_98209_) {
         int $$1 = BookEditScreen.findLineFromPos(this.lineStarts, p_98209_);
         return this.lineStarts[$$1];
      }

      public int findLineEnd(int p_98219_) {
         int $$1 = BookEditScreen.findLineFromPos(this.lineStarts, p_98219_);
         return this.lineStarts[$$1] + this.lines[$$1].contents.length();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class LineInfo {
      final Style style;
      final String contents;
      final Component asComponent;
      final int x;
      final int y;

      public LineInfo(Style p_98232_, String p_98233_, int p_98234_, int p_98235_) {
         this.style = p_98232_;
         this.contents = p_98233_;
         this.x = p_98234_;
         this.y = p_98235_;
         this.asComponent = Component.literal(p_98233_).setStyle(p_98232_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Pos2i {
      public final int x;
      public final int y;

      Pos2i(int p_98249_, int p_98250_) {
         this.x = p_98249_;
         this.y = p_98250_;
      }
   }
}
