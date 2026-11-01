package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface MultiLineLabel {
   MultiLineLabel EMPTY = new MultiLineLabel() {
      @Override
      public void renderCentered(GuiGraphics p_283287_, int p_94383_, int p_94384_) {
      }

      @Override
      public void renderCentered(GuiGraphics p_283208_, int p_210825_, int p_210826_, int p_210827_, int p_210828_) {
      }

      @Override
      public void renderLeftAligned(GuiGraphics p_283077_, int p_94379_, int p_94380_, int p_282157_, int p_282742_) {
      }

      @Override
      public int renderLeftAlignedNoShadow(GuiGraphics p_283645_, int p_94389_, int p_94390_, int p_94391_, int p_94392_) {
         return p_94390_;
      }

      @Override
      public int getLineCount() {
         return 0;
      }

      @Override
      public int getWidth() {
         return 0;
      }
   };

   static MultiLineLabel create(Font p_94351_, Component... p_94352_) {
      return create(p_94351_, Integer.MAX_VALUE, Integer.MAX_VALUE, p_94352_);
   }

   static MultiLineLabel create(Font p_94346_, int p_94348_, Component... p_352900_) {
      return create(p_94346_, p_94348_, Integer.MAX_VALUE, p_352900_);
   }

   static MultiLineLabel create(Font p_169037_, Component p_352901_, int p_352917_) {
      return create(p_169037_, p_352917_, Integer.MAX_VALUE, p_352901_);
   }

   static MultiLineLabel create(final Font p_94342_, final int p_94344_, final int p_352914_, final Component... p_352955_) {
      return p_352955_.length == 0 ? EMPTY : new MultiLineLabel() {
         @Nullable
         private List<MultiLineLabel.TextAndWidth> cachedTextAndWidth;
         @Nullable
         private Language splitWithLanguage;

         @Override
         public void renderCentered(GuiGraphics p_281603_, int p_281267_, int p_281819_) {
            this.renderCentered(p_281603_, p_281267_, p_281819_, 9, -1);
         }

         @Override
         public void renderCentered(GuiGraphics p_283492_, int p_283184_, int p_282078_, int p_352944_, int p_352919_) {
            int $$5 = p_282078_;

            for (MultiLineLabel.TextAndWidth $$6 : this.getSplitMessage()) {
               p_283492_.drawCenteredString(p_94342_, $$6.text, p_283184_, $$5, p_352919_);
               $$5 += p_352944_;
            }
         }

         @Override
         public void renderLeftAligned(GuiGraphics p_282318_, int p_283665_, int p_283416_, int p_281919_, int p_281686_) {
            int $$5 = p_283416_;

            for (MultiLineLabel.TextAndWidth $$6 : this.getSplitMessage()) {
               p_282318_.drawString(p_94342_, $$6.text, p_283665_, $$5, p_281686_);
               $$5 += p_281919_;
            }
         }

         @Override
         public int renderLeftAlignedNoShadow(GuiGraphics p_281782_, int p_282841_, int p_283554_, int p_282768_, int p_283499_) {
            int $$5 = p_283554_;

            for (MultiLineLabel.TextAndWidth $$6 : this.getSplitMessage()) {
               p_281782_.drawString(p_94342_, $$6.text, p_282841_, $$5, p_283499_, false);
               $$5 += p_282768_;
            }

            return $$5;
         }

         private List<MultiLineLabel.TextAndWidth> getSplitMessage() {
            Language $$0 = Language.getInstance();
            if (this.cachedTextAndWidth != null && $$0 == this.splitWithLanguage) {
               return this.cachedTextAndWidth;
            } else {
               this.splitWithLanguage = $$0;
               List<FormattedCharSequence> $$1 = new ArrayList<>();

               for (Component $$2 : p_352955_) {
                  $$1.addAll(p_94342_.split($$2, p_94344_));
               }

               this.cachedTextAndWidth = new ArrayList<>();

               for (FormattedCharSequence $$3 : $$1.subList(0, Math.min($$1.size(), p_352914_))) {
                  this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth($$3, p_94342_.width($$3)));
               }

               return this.cachedTextAndWidth;
            }
         }

         @Override
         public int getLineCount() {
            return this.getSplitMessage().size();
         }

         @Override
         public int getWidth() {
            return Math.min(p_94344_, this.getSplitMessage().stream().mapToInt(MultiLineLabel.TextAndWidth::width).max().orElse(0));
         }
      };
   }

   void renderCentered(GuiGraphics var1, int var2, int var3);

   void renderCentered(GuiGraphics var1, int var2, int var3, int var4, int var5);

   void renderLeftAligned(GuiGraphics var1, int var2, int var3, int var4, int var5);

   int renderLeftAlignedNoShadow(GuiGraphics var1, int var2, int var3, int var4, int var5);

   int getLineCount();

   int getWidth();

   @OnlyIn(Dist.CLIENT)
   public record TextAndWidth(FormattedCharSequence text, int width) {
   }
}
