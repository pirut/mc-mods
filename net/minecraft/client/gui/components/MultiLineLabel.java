package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface MultiLineLabel {
   MultiLineLabel EMPTY = new MultiLineLabel() {
      @Override
      public int visitLines(TextAlignment textAlignment, int x, int y, int lineHeight, ActiveTextCollector activeTextCollector) {
         return y;
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

   static MultiLineLabel create(Font font, Component... components) {
      return create(font, Integer.MAX_VALUE, Integer.MAX_VALUE, components);
   }

   static MultiLineLabel create(Font font, int maxWidth, Component... components) {
      return create(font, maxWidth, Integer.MAX_VALUE, components);
   }

   static MultiLineLabel create(Font font, Component component, int maxWidth) {
      return create(font, maxWidth, Integer.MAX_VALUE, component);
   }

   static MultiLineLabel create(Font font, int maxWidth, int maxRows, Component... components) {
      return components.length == 0
         ? EMPTY
         : new MultiLineLabel() {
            @Nullable
            private List<MultiLineLabel.TextAndWidth> cachedTextAndWidth;
            @Nullable
            private Language splitWithLanguage;

            @Override
            public int visitLines(TextAlignment textAlignment, int x, int y, int lineHeight, ActiveTextCollector activeTextCollector) {
               int i = y;

               for (MultiLineLabel.TextAndWidth textAndWidth : this.getSplitMessage()) {
                  int j = textAlignment.calculateLeft(x, textAndWidth.width);
                  activeTextCollector.accept(j, i, textAndWidth.text);
                  i += lineHeight;
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

                  for (Component component : components) {
                     list.addAll(font.splitIgnoringLanguage(component, maxWidth));
                  }

                  this.cachedTextAndWidth = new ArrayList<>();
                  int i = Math.min(list.size(), maxRows);
                  List<FormattedText> list2 = list.subList(0, i);

                  for (int j = 0; j < list2.size(); j++) {
                     FormattedText formattedText = list2.get(j);
                     FormattedCharSequence formattedCharSequence = Language.getInstance().getVisualOrder(formattedText);
                     if (j == list2.size() - 1 && i == maxRows && i != list.size()) {
                        FormattedText formattedText2 = font.substrByWidth(formattedText, font.width(formattedText) - font.width(CommonComponents.ELLIPSIS));
                        FormattedText formattedText3 = FormattedText.composite(
                           formattedText2, CommonComponents.ELLIPSIS.copy().withStyle(components[components.length - 1].getStyle())
                        );
                        this.cachedTextAndWidth
                           .add(new MultiLineLabel.TextAndWidth(Language.getInstance().getVisualOrder(formattedText3), font.width(formattedText3)));
                     } else {
                        this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth(formattedCharSequence, font.width(formattedCharSequence)));
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
               return Math.min(maxWidth, this.getSplitMessage().stream().mapToInt(MultiLineLabel.TextAndWidth::width).max().orElse(0));
            }
         };
   }

   int visitLines(TextAlignment textAlignment, int x, int y, int lineHeight, ActiveTextCollector activeTextCollector);

   int getLineCount();

   int getWidth();

   @Environment(EnvType.CLIENT)
   public record TextAndWidth(FormattedCharSequence text, int width) {
   }
}
