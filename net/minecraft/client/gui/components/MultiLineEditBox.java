package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class MultiLineEditBox extends AbstractTextAreaWidget {
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   private static final int PLACEHOLDER_TEXT_COLOR = ARGB.color(204, -2039584);
   private static final int CURSOR_BLINK_INTERVAL_MS = 300;
   private final Font font;
   private final Component placeholder;
   private final MultilineTextField textField;
   private final int textColor;
   private final boolean textShadow;
   private final int cursorColor;
   private long focusedTime = Util.getMillis();

   MultiLineEditBox(
      Font font,
      int x,
      int y,
      int width,
      int height,
      Component placeholder,
      Component message,
      int textColor,
      boolean textShadow,
      int cursorColor,
      boolean showBackground,
      boolean showDecorations
   ) {
      super(x, y, width, height, message, showBackground, showDecorations);
      this.font = font;
      this.textShadow = textShadow;
      this.textColor = textColor;
      this.cursorColor = cursorColor;
      this.placeholder = placeholder;
      this.textField = new MultilineTextField(font, width - this.totalInnerPadding());
      this.textField.setCursorListener(this::scrollToCursor);
   }

   public void setCharacterLimit(int characterLimit) {
      this.textField.setCharacterLimit(characterLimit);
   }

   public void setLineLimit(int lineLimit) {
      this.textField.setLineLimit(lineLimit);
   }

   public void setValueListener(Consumer<String> valueListener) {
      this.textField.setValueListener(valueListener);
   }

   public void setValue(String value) {
      this.setValue(value, false);
   }

   public void setValue(String value, boolean force) {
      this.textField.setValue(value, force);
   }

   public String getValue() {
      return this.textField.value();
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
   }

   @Override
   public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
      if (isDoubleClick) {
         this.textField.selectWordAtCursor();
      } else {
         this.textField.setSelecting(event.hasShiftDown());
         this.seekCursorScreen(event.x(), event.y());
      }
   }

   @Override
   protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
      this.textField.setSelecting(true);
      this.seekCursorScreen(event.x(), event.y());
      this.textField.setSelecting(event.hasShiftDown());
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      return this.textField.keyPressed(event);
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (this.visible && this.isFocused() && event.isAllowedChatCharacter()) {
         this.textField.insertText(event.codepointAsString());
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      String string = this.textField.value();
      if (string.isEmpty() && !this.isFocused()) {
         guiGraphics.drawWordWrap(
            this.font, this.placeholder, this.getInnerLeft(), this.getInnerTop(), this.width - this.totalInnerPadding(), PLACEHOLDER_TEXT_COLOR
         );
      } else {
         int i = this.textField.cursor();
         boolean bl = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
         boolean bl2 = i < string.length();
         int j = 0;
         int k = 0;
         int l = this.getInnerTop();
         boolean bl3 = false;

         for (MultilineTextField.StringView stringView : this.textField.iterateLines()) {
            boolean bl4 = this.withinContentAreaTopBottom(l, l + 9);
            int m = this.getInnerLeft();
            if (bl && bl2 && i >= stringView.beginIndex() && i <= stringView.endIndex()) {
               if (bl4) {
                  String string2 = string.substring(stringView.beginIndex(), i);
                  guiGraphics.drawString(this.font, string2, m, l, this.textColor, this.textShadow);
                  j = m + this.font.width(string2);
                  if (!bl3) {
                     guiGraphics.fill(j, l - 1, j + 1, l + 1 + 9, this.cursorColor);
                     bl3 = true;
                  }

                  guiGraphics.drawString(this.font, string.substring(i, stringView.endIndex()), j, l, this.textColor, this.textShadow);
               }
            } else {
               if (bl4) {
                  String string2 = string.substring(stringView.beginIndex(), stringView.endIndex());
                  guiGraphics.drawString(this.font, string2, m, l, this.textColor, this.textShadow);
                  j = m + this.font.width(string2) - 1;
               }

               k = l;
            }

            l += 9;
         }

         if (bl && !bl2 && this.withinContentAreaTopBottom(k, k + 9)) {
            guiGraphics.drawString(this.font, "_", j + 1, k, this.cursorColor, this.textShadow);
         }

         if (this.textField.hasSelection()) {
            MultilineTextField.StringView stringView2 = this.textField.getSelected();
            int n = this.getInnerLeft();
            l = this.getInnerTop();

            for (MultilineTextField.StringView stringView3 : this.textField.iterateLines()) {
               if (stringView2.beginIndex() > stringView3.endIndex()) {
                  l += 9;
               } else {
                  if (stringView3.beginIndex() > stringView2.endIndex()) {
                     break;
                  }

                  if (this.withinContentAreaTopBottom(l, l + 9)) {
                     int o = this.font.width(string.substring(stringView3.beginIndex(), Math.max(stringView2.beginIndex(), stringView3.beginIndex())));
                     int p;
                     if (stringView2.endIndex() > stringView3.endIndex()) {
                        p = this.width - this.innerPadding();
                     } else {
                        p = this.font.width(string.substring(stringView3.beginIndex(), stringView2.endIndex()));
                     }

                     guiGraphics.textHighlight(n + o, l, n + p, l + 9, true);
                  }

                  l += 9;
               }
            }
         }

         if (this.isHovered()) {
            guiGraphics.requestCursor(CursorTypes.IBEAM);
         }
      }
   }

   @Override
   protected void renderDecorations(GuiGraphics guiGraphics) {
      super.renderDecorations(guiGraphics);
      if (this.textField.hasCharacterLimit()) {
         int i = this.textField.characterLimit();
         Component component = Component.translatable("gui.multiLineEditBox.character_limit", this.textField.value().length(), i);
         guiGraphics.drawString(this.font, component, this.getX() + this.width - this.font.width(component), this.getY() + this.height + 4, -6250336);
      }
   }

   @Override
   public int getInnerHeight() {
      return 9 * this.textField.getLineCount();
   }

   @Override
   protected double scrollRate() {
      return 9.0 / 2.0;
   }

   private void scrollToCursor() {
      double d = this.scrollAmount();
      MultilineTextField.StringView stringView = this.textField.getLineView((int)(d / 9.0));
      if (this.textField.cursor() <= stringView.beginIndex()) {
         d = this.textField.getLineAtCursor() * 9;
      } else {
         MultilineTextField.StringView stringView2 = this.textField.getLineView((int)((d + this.height) / 9.0) - 1);
         if (this.textField.cursor() > stringView2.endIndex()) {
            d = this.textField.getLineAtCursor() * 9 - this.height + 9 + this.totalInnerPadding();
         }
      }

      this.setScrollAmount(d);
   }

   private void seekCursorScreen(double mouseX, double mouseY) {
      double d = mouseX - this.getX() - this.innerPadding();
      double e = mouseY - this.getY() - this.innerPadding() + this.scrollAmount();
      this.textField.seekCursorToPoint(d, e);
   }

   @Override
   public void setFocused(boolean focused) {
      super.setFocused(focused);
      if (focused) {
         this.focusedTime = Util.getMillis();
      }
   }

   public static MultiLineEditBox.Builder builder() {
      return new MultiLineEditBox.Builder();
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private int x;
      private int y;
      private Component placeholder = CommonComponents.EMPTY;
      private int textColor = -2039584;
      private boolean textShadow = true;
      private int cursorColor = -3092272;
      private boolean showBackground = true;
      private boolean showDecorations = true;

      public MultiLineEditBox.Builder setX(int x) {
         this.x = x;
         return this;
      }

      public MultiLineEditBox.Builder setY(int y) {
         this.y = y;
         return this;
      }

      public MultiLineEditBox.Builder setPlaceholder(Component placeholder) {
         this.placeholder = placeholder;
         return this;
      }

      public MultiLineEditBox.Builder setTextColor(int textColor) {
         this.textColor = textColor;
         return this;
      }

      public MultiLineEditBox.Builder setTextShadow(boolean textShadow) {
         this.textShadow = textShadow;
         return this;
      }

      public MultiLineEditBox.Builder setCursorColor(int cursorColor) {
         this.cursorColor = cursorColor;
         return this;
      }

      public MultiLineEditBox.Builder setShowBackground(boolean showBackground) {
         this.showBackground = showBackground;
         return this;
      }

      public MultiLineEditBox.Builder setShowDecorations(boolean showDecorations) {
         this.showDecorations = showDecorations;
         return this;
      }

      public MultiLineEditBox build(Font font, int width, int height, Component message) {
         return new MultiLineEditBox(
            font,
            this.x,
            this.y,
            width,
            height,
            this.placeholder,
            message,
            this.textColor,
            this.textShadow,
            this.cursorColor,
            this.showBackground,
            this.showDecorations
         );
      }
   }
}
