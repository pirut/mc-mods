package net.minecraft.client.gui.components;

import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiLineEditBox extends AbstractScrollWidget {
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_INSERT_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   private static final int TEXT_COLOR = -2039584;
   private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
   private static final int CURSOR_BLINK_INTERVAL_MS = 300;
   private final Font font;
   private final Component placeholder;
   private final MultilineTextField textField;
   private long focusedTime = Util.getMillis();

   public MultiLineEditBox(Font p_239008_, int p_239009_, int p_239010_, int p_239011_, int p_239012_, Component p_239013_, Component p_239014_) {
      super(p_239009_, p_239010_, p_239011_, p_239012_, p_239014_);
      this.font = p_239008_;
      this.placeholder = p_239013_;
      this.textField = new MultilineTextField(p_239008_, p_239011_ - this.totalInnerPadding());
      this.textField.setCursorListener(this::scrollToCursor);
   }

   public void setCharacterLimit(int p_239314_) {
      this.textField.setCharacterLimit(p_239314_);
   }

   public void setValueListener(Consumer<String> p_239274_) {
      this.textField.setValueListener(p_239274_);
   }

   public void setValue(String p_240160_) {
      this.textField.setValue(p_240160_);
   }

   public String getValue() {
      return this.textField.value();
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_259393_) {
      p_259393_.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
   }

   @Override
   public boolean mouseClicked(double p_239101_, double p_239102_, int p_239103_) {
      if (this.withinContentAreaPoint(p_239101_, p_239102_) && p_239103_ == 0) {
         this.textField.setSelecting(Screen.hasShiftDown());
         this.seekCursorScreen(p_239101_, p_239102_);
         return true;
      } else {
         return super.mouseClicked(p_239101_, p_239102_, p_239103_);
      }
   }

   @Override
   public boolean mouseDragged(double p_238978_, double p_238979_, int p_238980_, double p_238981_, double p_238982_) {
      if (super.mouseDragged(p_238978_, p_238979_, p_238980_, p_238981_, p_238982_)) {
         return true;
      } else if (this.withinContentAreaPoint(p_238978_, p_238979_) && p_238980_ == 0) {
         this.textField.setSelecting(true);
         this.seekCursorScreen(p_238978_, p_238979_);
         this.textField.setSelecting(Screen.hasShiftDown());
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(int p_239433_, int p_239434_, int p_239435_) {
      return this.textField.keyPressed(p_239433_);
   }

   @Override
   public boolean charTyped(char p_239387_, int p_239388_) {
      if (this.visible && this.isFocused() && StringUtil.isAllowedChatCharacter(p_239387_)) {
         this.textField.insertText(Character.toString(p_239387_));
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void renderContents(GuiGraphics p_283676_, int p_281538_, int p_283033_, float p_281767_) {
      String $$4 = this.textField.value();
      if ($$4.isEmpty() && !this.isFocused()) {
         p_283676_.drawWordWrap(
            this.font,
            this.placeholder,
            this.getX() + this.innerPadding(),
            this.getY() + this.innerPadding(),
            this.width - this.totalInnerPadding(),
            -857677600
         );
      } else {
         int $$5 = this.textField.cursor();
         boolean $$6 = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
         boolean $$7 = $$5 < $$4.length();
         int $$8 = 0;
         int $$9 = 0;
         int $$10 = this.getY() + this.innerPadding();

         for (MultilineTextField.StringView $$11 : this.textField.iterateLines()) {
            boolean $$12 = this.withinContentAreaTopBottom($$10, $$10 + 9);
            if ($$6 && $$7 && $$5 >= $$11.beginIndex() && $$5 <= $$11.endIndex()) {
               if ($$12) {
                  $$8 = p_283676_.drawString(this.font, $$4.substring($$11.beginIndex(), $$5), this.getX() + this.innerPadding(), $$10, -2039584) - 1;
                  p_283676_.fill($$8, $$10 - 1, $$8 + 1, $$10 + 1 + 9, -3092272);
                  p_283676_.drawString(this.font, $$4.substring($$5, $$11.endIndex()), $$8, $$10, -2039584);
               }
            } else {
               if ($$12) {
                  $$8 = p_283676_.drawString(this.font, $$4.substring($$11.beginIndex(), $$11.endIndex()), this.getX() + this.innerPadding(), $$10, -2039584)
                     - 1;
               }

               $$9 = $$10;
            }

            $$10 += 9;
         }

         if ($$6 && !$$7 && this.withinContentAreaTopBottom($$9, $$9 + 9)) {
            p_283676_.drawString(this.font, "_", $$8, $$9, -3092272);
         }

         if (this.textField.hasSelection()) {
            MultilineTextField.StringView $$13 = this.textField.getSelected();
            int $$14 = this.getX() + this.innerPadding();
            $$10 = this.getY() + this.innerPadding();

            for (MultilineTextField.StringView $$15 : this.textField.iterateLines()) {
               if ($$13.beginIndex() > $$15.endIndex()) {
                  $$10 += 9;
               } else {
                  if ($$15.beginIndex() > $$13.endIndex()) {
                     break;
                  }

                  if (this.withinContentAreaTopBottom($$10, $$10 + 9)) {
                     int $$16 = this.font.width($$4.substring($$15.beginIndex(), Math.max($$13.beginIndex(), $$15.beginIndex())));
                     int $$17;
                     if ($$13.endIndex() > $$15.endIndex()) {
                        $$17 = this.width - this.innerPadding();
                     } else {
                        $$17 = this.font.width($$4.substring($$15.beginIndex(), $$13.endIndex()));
                     }

                     this.renderHighlight(p_283676_, $$14 + $$16, $$10, $$14 + $$17, $$10 + 9);
                  }

                  $$10 += 9;
               }
            }
         }
      }
   }

   @Override
   protected void renderDecorations(GuiGraphics p_282551_) {
      super.renderDecorations(p_282551_);
      if (this.textField.hasCharacterLimit()) {
         int $$1 = this.textField.characterLimit();
         Component $$2 = Component.translatable("gui.multiLineEditBox.character_limit", this.textField.value().length(), $$1);
         p_282551_.drawString(this.font, $$2, this.getX() + this.width - this.font.width($$2), this.getY() + this.height + 4, 10526880);
      }
   }

   @Override
   public int getInnerHeight() {
      return 9 * this.textField.getLineCount();
   }

   @Override
   protected boolean scrollbarVisible() {
      return this.textField.getLineCount() > this.getDisplayableLineCount();
   }

   @Override
   protected double scrollRate() {
      return 9.0 / 2.0;
   }

   private void renderHighlight(GuiGraphics p_282092_, int p_282814_, int p_282908_, int p_281451_, int p_281765_) {
      p_282092_.fill(RenderType.guiTextHighlight(), p_282814_, p_282908_, p_281451_, p_281765_, -16776961);
   }

   private void scrollToCursor() {
      double $$0 = this.scrollAmount();
      MultilineTextField.StringView $$1 = this.textField.getLineView((int)($$0 / 9.0));
      if (this.textField.cursor() <= $$1.beginIndex()) {
         $$0 = this.textField.getLineAtCursor() * 9;
      } else {
         MultilineTextField.StringView $$2 = this.textField.getLineView((int)(($$0 + this.height) / 9.0) - 1);
         if (this.textField.cursor() > $$2.endIndex()) {
            $$0 = this.textField.getLineAtCursor() * 9 - this.height + 9 + this.totalInnerPadding();
         }
      }

      this.setScrollAmount($$0);
   }

   private double getDisplayableLineCount() {
      return (this.height - this.totalInnerPadding()) / 9.0;
   }

   private void seekCursorScreen(double p_239276_, double p_239277_) {
      double $$2 = p_239276_ - this.getX() - this.innerPadding();
      double $$3 = p_239277_ - this.getY() - this.innerPadding() + this.scrollAmount();
      this.textField.seekCursorToPoint($$2, $$3);
   }

   @Override
   public void setFocused(boolean p_295271_) {
      super.setFocused(p_295271_);
      if (p_295271_) {
         this.focusedTime = Util.getMillis();
      }
   }
}
