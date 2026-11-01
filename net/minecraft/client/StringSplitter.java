package net.minecraft.client;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

@OnlyIn(Dist.CLIENT)
public class StringSplitter {
   final StringSplitter.WidthProvider widthProvider;

   public StringSplitter(StringSplitter.WidthProvider p_92335_) {
      this.widthProvider = p_92335_;
   }

   public float stringWidth(@Nullable String p_92354_) {
      if (p_92354_ == null) {
         return 0.0F;
      } else {
         MutableFloat $$1 = new MutableFloat();
         StringDecomposer.iterateFormatted(p_92354_, Style.EMPTY, (p_92429_, p_92430_, p_92431_) -> {
            $$1.add(this.widthProvider.getWidth(p_92431_, p_92430_));
            return true;
         });
         return $$1.floatValue();
      }
   }

   public float stringWidth(FormattedText p_92385_) {
      MutableFloat $$1 = new MutableFloat();
      StringDecomposer.iterateFormatted(p_92385_, Style.EMPTY, (p_92420_, p_92421_, p_92422_) -> {
         $$1.add(this.widthProvider.getWidth(p_92422_, p_92421_));
         return true;
      });
      return $$1.floatValue();
   }

   public float stringWidth(FormattedCharSequence p_92337_) {
      MutableFloat $$1 = new MutableFloat();
      p_92337_.accept((p_92400_, p_92401_, p_92402_) -> {
         $$1.add(this.widthProvider.getWidth(p_92402_, p_92401_));
         return true;
      });
      return $$1.floatValue();
   }

   public int plainIndexAtWidth(String p_92361_, int p_92362_, Style p_92363_) {
      StringSplitter.WidthLimitedCharSink $$3 = new StringSplitter.WidthLimitedCharSink(p_92362_);
      StringDecomposer.iterate(p_92361_, p_92363_, $$3);
      return $$3.getPosition();
   }

   public String plainHeadByWidth(String p_92411_, int p_92412_, Style p_92413_) {
      return p_92411_.substring(0, this.plainIndexAtWidth(p_92411_, p_92412_, p_92413_));
   }

   public String plainTailByWidth(String p_92424_, int p_92425_, Style p_92426_) {
      MutableFloat $$3 = new MutableFloat();
      MutableInt $$4 = new MutableInt(p_92424_.length());
      StringDecomposer.iterateBackwards(p_92424_, p_92426_, (p_92407_, p_92408_, p_92409_) -> {
         float $$6 = $$3.addAndGet(this.widthProvider.getWidth(p_92409_, p_92408_));
         if ($$6 > p_92425_) {
            return false;
         } else {
            $$4.setValue(p_92407_);
            return true;
         }
      });
      return p_92424_.substring($$4.intValue());
   }

   public int formattedIndexByWidth(String p_168627_, int p_168628_, Style p_168629_) {
      StringSplitter.WidthLimitedCharSink $$3 = new StringSplitter.WidthLimitedCharSink(p_168628_);
      StringDecomposer.iterateFormatted(p_168627_, p_168629_, $$3);
      return $$3.getPosition();
   }

   @Nullable
   public Style componentStyleAtWidth(FormattedText p_92387_, int p_92388_) {
      StringSplitter.WidthLimitedCharSink $$2 = new StringSplitter.WidthLimitedCharSink(p_92388_);
      return p_92387_.<Style>visit(
            (p_92343_, p_92344_) -> StringDecomposer.iterateFormatted(p_92344_, p_92343_, $$2) ? Optional.empty() : Optional.of(p_92343_), Style.EMPTY
         )
         .orElse(null);
   }

   @Nullable
   public Style componentStyleAtWidth(FormattedCharSequence p_92339_, int p_92340_) {
      StringSplitter.WidthLimitedCharSink $$2 = new StringSplitter.WidthLimitedCharSink(p_92340_);
      MutableObject<Style> $$3 = new MutableObject();
      p_92339_.accept((p_92348_, p_92349_, p_92350_) -> {
         if (!$$2.accept(p_92348_, p_92349_, p_92350_)) {
            $$3.setValue(p_92349_);
            return false;
         } else {
            return true;
         }
      });
      return (Style)$$3.getValue();
   }

   public String formattedHeadByWidth(String p_168631_, int p_168632_, Style p_168633_) {
      return p_168631_.substring(0, this.formattedIndexByWidth(p_168631_, p_168632_, p_168633_));
   }

   public FormattedText headByWidth(FormattedText p_92390_, int p_92391_, Style p_92392_) {
      final StringSplitter.WidthLimitedCharSink $$3 = new StringSplitter.WidthLimitedCharSink(p_92391_);
      return p_92390_.visit(new FormattedText.StyledContentConsumer<FormattedText>() {
         private final ComponentCollector collector = new ComponentCollector();

         @Override
         public Optional<FormattedText> accept(Style p_92443_, String p_92444_) {
            $$3.resetPosition();
            if (!StringDecomposer.iterateFormatted(p_92444_, p_92443_, $$3)) {
               String $$2 = p_92444_.substring(0, $$3.getPosition());
               if (!$$2.isEmpty()) {
                  this.collector.append(FormattedText.of($$2, p_92443_));
               }

               return Optional.of(this.collector.getResultOrEmpty());
            } else {
               if (!p_92444_.isEmpty()) {
                  this.collector.append(FormattedText.of(p_92444_, p_92443_));
               }

               return Optional.empty();
            }
         }
      }, p_92392_).orElse(p_92390_);
   }

   public int findLineBreak(String p_168635_, int p_168636_, Style p_168637_) {
      StringSplitter.LineBreakFinder $$3 = new StringSplitter.LineBreakFinder(p_168636_);
      StringDecomposer.iterateFormatted(p_168635_, p_168637_, $$3);
      return $$3.getSplitPosition();
   }

   public static int getWordPosition(String p_92356_, int p_92357_, int p_92358_, boolean p_92359_) {
      int $$4 = p_92358_;
      boolean $$5 = p_92357_ < 0;
      int $$6 = Math.abs(p_92357_);

      for (int $$7 = 0; $$7 < $$6; $$7++) {
         if ($$5) {
            while (p_92359_ && $$4 > 0 && (p_92356_.charAt($$4 - 1) == ' ' || p_92356_.charAt($$4 - 1) == '\n')) {
               $$4--;
            }

            while ($$4 > 0 && p_92356_.charAt($$4 - 1) != ' ' && p_92356_.charAt($$4 - 1) != '\n') {
               $$4--;
            }
         } else {
            int $$8 = p_92356_.length();
            int $$9 = p_92356_.indexOf(32, $$4);
            int $$10 = p_92356_.indexOf(10, $$4);
            if ($$9 == -1 && $$10 == -1) {
               $$4 = -1;
            } else if ($$9 != -1 && $$10 != -1) {
               $$4 = Math.min($$9, $$10);
            } else if ($$9 != -1) {
               $$4 = $$9;
            } else {
               $$4 = $$10;
            }

            if ($$4 == -1) {
               $$4 = $$8;
            } else {
               while (p_92359_ && $$4 < $$8 && (p_92356_.charAt($$4) == ' ' || p_92356_.charAt($$4) == '\n')) {
                  $$4++;
               }
            }
         }
      }

      return $$4;
   }

   public void splitLines(String p_92365_, int p_92366_, Style p_92367_, boolean p_92368_, StringSplitter.LinePosConsumer p_92369_) {
      int $$5 = 0;
      int $$6 = p_92365_.length();
      Style $$7 = p_92367_;

      while ($$5 < $$6) {
         StringSplitter.LineBreakFinder $$8 = new StringSplitter.LineBreakFinder(p_92366_);
         boolean $$9 = StringDecomposer.iterateFormatted(p_92365_, $$5, $$7, p_92367_, $$8);
         if ($$9) {
            p_92369_.accept($$7, $$5, $$6);
            break;
         }

         int $$10 = $$8.getSplitPosition();
         char $$11 = p_92365_.charAt($$10);
         int $$12 = $$11 != '\n' && $$11 != ' ' ? $$10 : $$10 + 1;
         p_92369_.accept($$7, $$5, p_92368_ ? $$12 : $$10);
         $$5 = $$12;
         $$7 = $$8.getSplitStyle();
      }
   }

   public List<FormattedText> splitLines(String p_92433_, int p_92434_, Style p_92435_) {
      List<FormattedText> $$3 = Lists.newArrayList();
      this.splitLines(
         p_92433_, p_92434_, p_92435_, false, (p_92373_, p_92374_, p_92375_) -> $$3.add(FormattedText.of(p_92433_.substring(p_92374_, p_92375_), p_92373_))
      );
      return $$3;
   }

   public List<FormattedText> splitLines(FormattedText p_92415_, int p_92416_, Style p_92417_) {
      List<FormattedText> $$3 = Lists.newArrayList();
      this.splitLines(p_92415_, p_92416_, p_92417_, (p_92378_, p_92379_) -> $$3.add(p_92378_));
      return $$3;
   }

   public List<FormattedText> splitLines(FormattedText p_168622_, int p_168623_, Style p_168624_, FormattedText p_168625_) {
      List<FormattedText> $$4 = Lists.newArrayList();
      this.splitLines(p_168622_, p_168623_, p_168624_, (p_168619_, p_168620_) -> $$4.add(p_168620_ ? FormattedText.composite(p_168625_, p_168619_) : p_168619_));
      return $$4;
   }

   public void splitLines(FormattedText p_92394_, int p_92395_, Style p_92396_, BiConsumer<FormattedText, Boolean> p_92397_) {
      List<StringSplitter.LineComponent> $$4 = Lists.newArrayList();
      p_92394_.visit((p_92382_, p_92383_) -> {
         if (!p_92383_.isEmpty()) {
            $$4.add(new StringSplitter.LineComponent(p_92383_, p_92382_));
         }

         return Optional.empty();
      }, p_92396_);
      StringSplitter.FlatComponents $$5 = new StringSplitter.FlatComponents($$4);
      boolean $$6 = true;
      boolean $$7 = false;
      boolean $$8 = false;

      while ($$6) {
         $$6 = false;
         StringSplitter.LineBreakFinder $$9 = new StringSplitter.LineBreakFinder(p_92395_);

         for (StringSplitter.LineComponent $$10 : $$5.parts) {
            boolean $$11 = StringDecomposer.iterateFormatted($$10.contents, 0, $$10.style, p_92396_, $$9);
            if (!$$11) {
               int $$12 = $$9.getSplitPosition();
               Style $$13 = $$9.getSplitStyle();
               char $$14 = $$5.charAt($$12);
               boolean $$15 = $$14 == '\n';
               boolean $$16 = $$15 || $$14 == ' ';
               $$7 = $$15;
               FormattedText $$17 = $$5.splitAt($$12, $$16 ? 1 : 0, $$13);
               p_92397_.accept($$17, $$8);
               $$8 = !$$15;
               $$6 = true;
               break;
            }

            $$9.addToOffset($$10.contents.length());
         }
      }

      FormattedText $$18 = $$5.getRemainder();
      if ($$18 != null) {
         p_92397_.accept($$18, $$8);
      } else if ($$7) {
         p_92397_.accept(FormattedText.EMPTY, false);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FlatComponents {
      final List<StringSplitter.LineComponent> parts;
      private String flatParts;

      public FlatComponents(List<StringSplitter.LineComponent> p_92448_) {
         this.parts = p_92448_;
         this.flatParts = p_92448_.stream().map(p_92459_ -> p_92459_.contents).collect(Collectors.joining());
      }

      public char charAt(int p_92451_) {
         return this.flatParts.charAt(p_92451_);
      }

      public FormattedText splitAt(int p_92453_, int p_92454_, Style p_92455_) {
         ComponentCollector $$3 = new ComponentCollector();
         ListIterator<StringSplitter.LineComponent> $$4 = this.parts.listIterator();
         int $$5 = p_92453_;
         boolean $$6 = false;

         while ($$4.hasNext()) {
            StringSplitter.LineComponent $$7 = $$4.next();
            String $$8 = $$7.contents;
            int $$9 = $$8.length();
            if (!$$6) {
               if ($$5 > $$9) {
                  $$3.append($$7);
                  $$4.remove();
                  $$5 -= $$9;
               } else {
                  String $$10 = $$8.substring(0, $$5);
                  if (!$$10.isEmpty()) {
                     $$3.append(FormattedText.of($$10, $$7.style));
                  }

                  $$5 += p_92454_;
                  $$6 = true;
               }
            }

            if ($$6) {
               if ($$5 <= $$9) {
                  String $$11 = $$8.substring($$5);
                  if ($$11.isEmpty()) {
                     $$4.remove();
                  } else {
                     $$4.set(new StringSplitter.LineComponent($$11, p_92455_));
                  }
                  break;
               }

               $$4.remove();
               $$5 -= $$9;
            }
         }

         this.flatParts = this.flatParts.substring(p_92453_ + p_92454_);
         return $$3.getResultOrEmpty();
      }

      @Nullable
      public FormattedText getRemainder() {
         ComponentCollector $$0 = new ComponentCollector();
         this.parts.forEach($$0::append);
         this.parts.clear();
         return $$0.getResult();
      }
   }

   @OnlyIn(Dist.CLIENT)
   class LineBreakFinder implements FormattedCharSink {
      private final float maxWidth;
      private int lineBreak = -1;
      private Style lineBreakStyle = Style.EMPTY;
      private boolean hadNonZeroWidthChar;
      private float width;
      private int lastSpace = -1;
      private Style lastSpaceStyle = Style.EMPTY;
      private int nextChar;
      private int offset;

      public LineBreakFinder(float p_92472_) {
         this.maxWidth = Math.max(p_92472_, 1.0F);
      }

      @Override
      public boolean accept(int p_92480_, Style p_92481_, int p_92482_) {
         int $$3 = p_92480_ + this.offset;
         switch (p_92482_) {
            case 10:
               return this.finishIteration($$3, p_92481_);
            case 32:
               this.lastSpace = $$3;
               this.lastSpaceStyle = p_92481_;
            default:
               float $$4 = StringSplitter.this.widthProvider.getWidth(p_92482_, p_92481_);
               this.width += $$4;
               if (!this.hadNonZeroWidthChar || !(this.width > this.maxWidth)) {
                  this.hadNonZeroWidthChar |= $$4 != 0.0F;
                  this.nextChar = $$3 + Character.charCount(p_92482_);
                  return true;
               } else {
                  return this.lastSpace != -1 ? this.finishIteration(this.lastSpace, this.lastSpaceStyle) : this.finishIteration($$3, p_92481_);
               }
         }
      }

      private boolean finishIteration(int p_92477_, Style p_92478_) {
         this.lineBreak = p_92477_;
         this.lineBreakStyle = p_92478_;
         return false;
      }

      private boolean lineBreakFound() {
         return this.lineBreak != -1;
      }

      public int getSplitPosition() {
         return this.lineBreakFound() ? this.lineBreak : this.nextChar;
      }

      public Style getSplitStyle() {
         return this.lineBreakStyle;
      }

      public void addToOffset(int p_92475_) {
         this.offset += p_92475_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class LineComponent implements FormattedText {
      final String contents;
      final Style style;

      public LineComponent(String p_92488_, Style p_92489_) {
         this.contents = p_92488_;
         this.style = p_92489_;
      }

      @Override
      public <T> Optional<T> visit(FormattedText.ContentConsumer<T> p_92493_) {
         return p_92493_.accept(this.contents);
      }

      @Override
      public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> p_92495_, Style p_92496_) {
         return p_92495_.accept(this.style.applyTo(p_92496_), this.contents);
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface LinePosConsumer {
      void accept(Style var1, int var2, int var3);
   }

   @OnlyIn(Dist.CLIENT)
   class WidthLimitedCharSink implements FormattedCharSink {
      private float maxWidth;
      private int position;

      public WidthLimitedCharSink(float p_92508_) {
         this.maxWidth = p_92508_;
      }

      @Override
      public boolean accept(int p_92511_, Style p_92512_, int p_92513_) {
         this.maxWidth = this.maxWidth - StringSplitter.this.widthProvider.getWidth(p_92513_, p_92512_);
         if (this.maxWidth >= 0.0F) {
            this.position = p_92511_ + Character.charCount(p_92513_);
            return true;
         } else {
            return false;
         }
      }

      public int getPosition() {
         return this.position;
      }

      public void resetPosition() {
         this.position = 0;
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface WidthProvider {
      float getWidth(int var1, Style var2);
   }
}
