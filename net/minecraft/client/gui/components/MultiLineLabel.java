package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface MultiLineLabel {
   MultiLineLabel EMPTY = new MultiLineLabel() {
      @Override
      public int visitLines(TextAlignment p_458262_, int p_94389_, int p_94390_, int p_94391_, ActiveTextCollector p_457837_) {
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
      return p_352955_.length == 0
         ? EMPTY
         : new MultiLineLabel() {
            @Nullable
            private List<MultiLineLabel.TextAndWidth> cachedTextAndWidth;
            @Nullable
            private Language splitWithLanguage;

            @Override
            public int visitLines(TextAlignment p_457779_, int p_458259_, int p_458018_, int p_457763_, ActiveTextCollector p_458016_) {
               int i = p_458018_;

               for (MultiLineLabel.TextAndWidth multilinelabel$textandwidth : this.getSplitMessage()) {
                  int j = p_457779_.calculateLeft(p_458259_, multilinelabel$textandwidth.width);
                  p_458016_.accept(j, i, multilinelabel$textandwidth.text);
                  i += p_457763_;
               }

               return i;
            }

            private List<MultiLineLabel.TextAndWidth> getSplitMessage() {
               Language language = Language.getInstance();
               if (this.cachedTextAndWidth != null && language == this.splitWithLanguage) {
                  return this.cachedTextAndWidth;
               } else {
                  this.splitWithLanguage = language;
                  List<FormattedText> list = new ArrayList<>();

                  for (Component component : p_352955_) {
                     list.addAll(p_94342_.splitIgnoringLanguage(component, p_94344_));
                  }

                  this.cachedTextAndWidth = new ArrayList<>();
                  int i = Math.min(list.size(), p_352914_);
                  List<FormattedText> list1 = list.subList(0, i);

                  for (int j = 0; j < list1.size(); j++) {
                     FormattedText formattedtext2 = list1.get(j);
                     FormattedCharSequence formattedcharsequence = Language.getInstance().getVisualOrder(formattedtext2);
                     if (j == list1.size() - 1 && i == p_352914_ && i != list.size()) {
                        FormattedText formattedtext = p_94342_.substrByWidth(
                           formattedtext2, p_94342_.width(formattedtext2) - p_94342_.width(CommonComponents.ELLIPSIS)
                        );
                        FormattedText formattedtext1 = FormattedText.composite(
                           formattedtext, CommonComponents.ELLIPSIS.copy().withStyle(p_352955_[p_352955_.length - 1].getStyle())
                        );
                        this.cachedTextAndWidth
                           .add(new MultiLineLabel.TextAndWidth(Language.getInstance().getVisualOrder(formattedtext1), p_94342_.width(formattedtext1)));
                     } else {
                        this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth(formattedcharsequence, p_94342_.width(formattedcharsequence)));
                     }
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

   int visitLines(TextAlignment var1, int var2, int var3, int var4, ActiveTextCollector var5);

   int getLineCount();

   int getWidth();

   @OnlyIn(Dist.CLIENT)
   public record TextAndWidth(FormattedCharSequence text, int width) {
   }
}
