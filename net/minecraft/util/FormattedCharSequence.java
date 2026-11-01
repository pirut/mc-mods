package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import net.minecraft.network.chat.Style;

@FunctionalInterface
public interface FormattedCharSequence {
   FormattedCharSequence EMPTY = formattedCharSink -> true;

   boolean accept(FormattedCharSink formattedCharSink);

   static FormattedCharSequence codepoint(int codePoint, Style style) {
      return formattedCharSink -> formattedCharSink.accept(0, style, codePoint);
   }

   static FormattedCharSequence forward(String text, Style style) {
      return text.isEmpty() ? EMPTY : formattedCharSink -> StringDecomposer.iterate(text, style, formattedCharSink);
   }

   static FormattedCharSequence forward(String text, Style style, Int2IntFunction codePointMapper) {
      return text.isEmpty() ? EMPTY : formattedCharSink -> StringDecomposer.iterate(text, style, decorateOutput(formattedCharSink, codePointMapper));
   }

   static FormattedCharSequence backward(String text, Style style) {
      return text.isEmpty() ? EMPTY : formattedCharSink -> StringDecomposer.iterateBackwards(text, style, formattedCharSink);
   }

   static FormattedCharSequence backward(String text, Style style, Int2IntFunction codePointMapper) {
      return text.isEmpty() ? EMPTY : formattedCharSink -> StringDecomposer.iterateBackwards(text, style, decorateOutput(formattedCharSink, codePointMapper));
   }

   static FormattedCharSink decorateOutput(FormattedCharSink sink, Int2IntFunction codePointMapper) {
      return (i, style, j) -> sink.accept(i, style, (Integer)codePointMapper.apply(j));
   }

   static FormattedCharSequence composite() {
      return EMPTY;
   }

   static FormattedCharSequence composite(FormattedCharSequence sequence) {
      return sequence;
   }

   static FormattedCharSequence composite(FormattedCharSequence first, FormattedCharSequence second) {
      return fromPair(first, second);
   }

   static FormattedCharSequence composite(FormattedCharSequence... parts) {
      return fromList(ImmutableList.copyOf(parts));
   }

   static FormattedCharSequence composite(List<FormattedCharSequence> parts) {
      int i = parts.size();
      switch (i) {
         case 0:
            return EMPTY;
         case 1:
            return parts.get(0);
         case 2:
            return fromPair(parts.get(0), parts.get(1));
         default:
            return fromList(ImmutableList.copyOf(parts));
      }
   }

   static FormattedCharSequence fromPair(FormattedCharSequence first, FormattedCharSequence second) {
      return formattedCharSink -> first.accept(formattedCharSink) && second.accept(formattedCharSink);
   }

   static FormattedCharSequence fromList(List<FormattedCharSequence> parts) {
      return formattedCharSink -> {
         for (FormattedCharSequence formattedCharSequence : parts) {
            if (!formattedCharSequence.accept(formattedCharSink)) {
               return false;
            }
         }

         return true;
      };
   }
}
