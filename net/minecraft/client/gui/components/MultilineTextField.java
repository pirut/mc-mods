package net.minecraft.client.gui.components;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class MultilineTextField {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int NO_LIMIT = Integer.MAX_VALUE;
   private static final int LINE_SEEK_PIXEL_BIAS = 2;
   private final Font font;
   private final List<MultilineTextField.StringView> displayLines = Lists.newArrayList();
   private String value;
   private int cursor;
   private int selectCursor;
   private boolean selecting;
   private int characterLimit = Integer.MAX_VALUE;
   private int lineLimit = Integer.MAX_VALUE;
   private final int width;
   private Consumer<String> valueListener = string -> {};
   private Runnable cursorListener = () -> {};

   public MultilineTextField(Font font, int width) {
      this.font = font;
      this.width = width;
      this.setValue("");
   }

   public int characterLimit() {
      return this.characterLimit;
   }

   public void setCharacterLimit(int characterLimit) {
      if (characterLimit < 0) {
         throw new IllegalArgumentException("Character limit cannot be negative");
      } else {
         this.characterLimit = characterLimit;
      }
   }

   public void setLineLimit(int lineLimit) {
      if (lineLimit < 0) {
         throw new IllegalArgumentException("Character limit cannot be negative");
      } else {
         this.lineLimit = lineLimit;
      }
   }

   public boolean hasCharacterLimit() {
      return this.characterLimit != Integer.MAX_VALUE;
   }

   public boolean hasLineLimit() {
      return this.lineLimit != Integer.MAX_VALUE;
   }

   public void setValueListener(Consumer<String> valueListener) {
      this.valueListener = valueListener;
   }

   public void setCursorListener(Runnable cursorListener) {
      this.cursorListener = cursorListener;
   }

   public void setValue(String value) {
      this.setValue(value, false);
   }

   public void setValue(String value, boolean force) {
      String string = this.truncateFullText(value);
      if (force || !this.overflowsLineLimit(string)) {
         this.value = string;
         this.cursor = this.value.length();
         this.selectCursor = this.cursor;
         this.onValueChange();
      }
   }

   public String value() {
      return this.value;
   }

   public void insertText(String text) {
      if (!text.isEmpty() || this.hasSelection()) {
         String string = this.truncateInsertionText(StringUtil.filterText(text, true));
         MultilineTextField.StringView stringView = this.getSelected();
         String string2 = new StringBuilder(this.value).replace(stringView.beginIndex, stringView.endIndex, string).toString();
         if (!this.overflowsLineLimit(string2)) {
            this.value = string2;
            this.cursor = stringView.beginIndex + string.length();
            this.selectCursor = this.cursor;
            this.onValueChange();
         }
      }
   }

   public void deleteText(int length) {
      if (!this.hasSelection()) {
         this.selectCursor = Mth.clamp(this.cursor + length, 0, this.value.length());
      }

      this.insertText("");
   }

   public int cursor() {
      return this.cursor;
   }

   public void setSelecting(boolean selecting) {
      this.selecting = selecting;
   }

   public MultilineTextField.StringView getSelected() {
      return new MultilineTextField.StringView(Math.min(this.selectCursor, this.cursor), Math.max(this.selectCursor, this.cursor));
   }

   public int getLineCount() {
      return this.displayLines.size();
   }

   public int getLineAtCursor() {
      for (int i = 0; i < this.displayLines.size(); i++) {
         MultilineTextField.StringView stringView = this.displayLines.get(i);
         if (this.cursor >= stringView.beginIndex && this.cursor <= stringView.endIndex) {
            return i;
         }
      }

      return -1;
   }

   public MultilineTextField.StringView getLineView(int lineNumber) {
      return this.displayLines.get(Mth.clamp(lineNumber, 0, this.displayLines.size() - 1));
   }

   public void seekCursor(Whence whence, int position) {
      switch (whence) {
         case ABSOLUTE:
            this.cursor = position;
            break;
         case RELATIVE:
            this.cursor += position;
            break;
         case END:
            this.cursor = this.value.length() + position;
      }

      this.cursor = Mth.clamp(this.cursor, 0, this.value.length());
      this.cursorListener.run();
      if (!this.selecting) {
         this.selectCursor = this.cursor;
      }
   }

   public void seekCursorLine(int offset) {
      if (offset != 0) {
         int i = this.font.width(this.value.substring(this.getCursorLineView().beginIndex, this.cursor)) + 2;
         MultilineTextField.StringView stringView = this.getCursorLineView(offset);
         int j = this.font.plainSubstrByWidth(this.value.substring(stringView.beginIndex, stringView.endIndex), i).length();
         this.seekCursor(Whence.ABSOLUTE, stringView.beginIndex + j);
      }
   }

   public void seekCursorToPoint(double x, double y) {
      int i = Mth.floor(x);
      int j = Mth.floor(y / 9.0);
      MultilineTextField.StringView stringView = this.displayLines.get(Mth.clamp(j, 0, this.displayLines.size() - 1));
      int k = this.font.plainSubstrByWidth(this.value.substring(stringView.beginIndex, stringView.endIndex), i).length();
      this.seekCursor(Whence.ABSOLUTE, stringView.beginIndex + k);
   }

   public void selectWordAtCursor() {
      MultilineTextField.StringView stringView = this.getPreviousWord();
      this.seekCursor(Whence.ABSOLUTE, stringView.beginIndex);
      this.setSelecting(true);
      this.seekCursor(Whence.ABSOLUTE, stringView.endIndex);
   }

   public boolean keyPressed(KeyEvent event) {
      this.selecting = event.hasShiftDown();
      if (event.isSelectAll()) {
         this.cursor = this.value.length();
         this.selectCursor = 0;
         return true;
      } else if (event.isCopy()) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
         return true;
      } else if (event.isPaste()) {
         this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
         return true;
      } else if (event.isCut()) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
         this.insertText("");
         return true;
      } else {
         switch (event.key()) {
            case 257:
            case 335:
               this.insertText("\n");
               return true;
            case 259:
               if (event.hasControlDownWithQuirk()) {
                  MultilineTextField.StringView stringView = this.getPreviousWord();
                  this.deleteText(stringView.beginIndex - this.cursor);
               } else {
                  this.deleteText(-1);
               }

               return true;
            case 261:
               if (event.hasControlDownWithQuirk()) {
                  MultilineTextField.StringView stringView = this.getNextWord();
                  this.deleteText(stringView.beginIndex - this.cursor);
               } else {
                  this.deleteText(1);
               }

               return true;
            case 262:
               if (event.hasControlDownWithQuirk()) {
                  MultilineTextField.StringView stringView = this.getNextWord();
                  this.seekCursor(Whence.ABSOLUTE, stringView.beginIndex);
               } else {
                  this.seekCursor(Whence.RELATIVE, 1);
               }

               return true;
            case 263:
               if (event.hasControlDownWithQuirk()) {
                  MultilineTextField.StringView stringView = this.getPreviousWord();
                  this.seekCursor(Whence.ABSOLUTE, stringView.beginIndex);
               } else {
                  this.seekCursor(Whence.RELATIVE, -1);
               }

               return true;
            case 264:
               if (!event.hasControlDownWithQuirk()) {
                  this.seekCursorLine(1);
               }

               return true;
            case 265:
               if (!event.hasControlDownWithQuirk()) {
                  this.seekCursorLine(-1);
               }

               return true;
            case 266:
               this.seekCursor(Whence.ABSOLUTE, 0);
               return true;
            case 267:
               this.seekCursor(Whence.END, 0);
               return true;
            case 268:
               if (event.hasControlDownWithQuirk()) {
                  this.seekCursor(Whence.ABSOLUTE, 0);
               } else {
                  this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
               }

               return true;
            case 269:
               if (event.hasControlDownWithQuirk()) {
                  this.seekCursor(Whence.END, 0);
               } else {
                  this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().endIndex);
               }

               return true;
            default:
               return false;
         }
      }
   }

   public Iterable<MultilineTextField.StringView> iterateLines() {
      return this.displayLines;
   }

   public boolean hasSelection() {
      return this.selectCursor != this.cursor;
   }

   @VisibleForTesting
   public String getSelectedText() {
      MultilineTextField.StringView stringView = this.getSelected();
      return this.value.substring(stringView.beginIndex, stringView.endIndex);
   }

   private MultilineTextField.StringView getCursorLineView() {
      return this.getCursorLineView(0);
   }

   private MultilineTextField.StringView getCursorLineView(int offset) {
      int i = this.getLineAtCursor();
      if (i < 0) {
         LOGGER.error("Cursor is not within text (cursor = {}, length = {})", this.cursor, this.value.length());
         return this.displayLines.getLast();
      } else {
         return this.displayLines.get(Mth.clamp(i + offset, 0, this.displayLines.size() - 1));
      }
   }

   @VisibleForTesting
   public MultilineTextField.StringView getPreviousWord() {
      if (this.value.isEmpty()) {
         return MultilineTextField.StringView.EMPTY;
      } else {
         int i = Mth.clamp(this.cursor, 0, this.value.length() - 1);

         while (i > 0 && Character.isWhitespace(this.value.charAt(i - 1))) {
            i--;
         }

         while (i > 0 && !Character.isWhitespace(this.value.charAt(i - 1))) {
            i--;
         }

         return new MultilineTextField.StringView(i, this.getWordEndPosition(i));
      }
   }

   @VisibleForTesting
   public MultilineTextField.StringView getNextWord() {
      if (this.value.isEmpty()) {
         return MultilineTextField.StringView.EMPTY;
      } else {
         int i = Mth.clamp(this.cursor, 0, this.value.length() - 1);

         while (i < this.value.length() && !Character.isWhitespace(this.value.charAt(i))) {
            i++;
         }

         while (i < this.value.length() && Character.isWhitespace(this.value.charAt(i))) {
            i++;
         }

         return new MultilineTextField.StringView(i, this.getWordEndPosition(i));
      }
   }

   private int getWordEndPosition(int cursor) {
      int i = cursor;

      while (i < this.value.length() && !Character.isWhitespace(this.value.charAt(i))) {
         i++;
      }

      return i;
   }

   private void onValueChange() {
      this.reflowDisplayLines();
      this.valueListener.accept(this.value);
      this.cursorListener.run();
   }

   private void reflowDisplayLines() {
      this.displayLines.clear();
      if (this.value.isEmpty()) {
         this.displayLines.add(MultilineTextField.StringView.EMPTY);
      } else {
         this.font
            .getSplitter()
            .splitLines(this.value, this.width, Style.EMPTY, false, (style, i, j) -> this.displayLines.add(new MultilineTextField.StringView(i, j)));
         if (this.value.charAt(this.value.length() - 1) == '\n') {
            this.displayLines.add(new MultilineTextField.StringView(this.value.length(), this.value.length()));
         }
      }
   }

   private String truncateFullText(String fullText) {
      return this.hasCharacterLimit() ? StringUtil.truncateStringIfNecessary(fullText, this.characterLimit, false) : fullText;
   }

   private String truncateInsertionText(String text) {
      String string = text;
      if (this.hasCharacterLimit()) {
         int i = this.characterLimit - this.value.length();
         string = StringUtil.truncateStringIfNecessary(text, i, false);
      }

      return string;
   }

   private boolean overflowsLineLimit(String text) {
      return this.hasLineLimit()
         && this.font.getSplitter().splitLines(text, this.width, Style.EMPTY).size() + (StringUtil.endsWithNewLine(text) ? 1 : 0) > this.lineLimit;
   }

   @Environment(EnvType.CLIENT)
   protected record StringView(int beginIndex, int endIndex) {
      static final MultilineTextField.StringView EMPTY = new MultilineTextField.StringView(0, 0);
   }
}
