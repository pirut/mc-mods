package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

public class SubStringSource {
   private final String plainText;
   private final List<Style> charStyles;
   private final Int2IntFunction reverseCharModifier;

   private SubStringSource(String p_131232_, List<Style> p_131233_, Int2IntFunction p_131234_) {
      this.plainText = p_131232_;
      this.charStyles = ImmutableList.copyOf(p_131233_);
      this.reverseCharModifier = p_131234_;
   }

   public String getPlainText() {
      return this.plainText;
   }

   public List<FormattedCharSequence> substring(int p_131237_, int p_131238_, boolean p_131239_) {
      if (p_131238_ == 0) {
         return ImmutableList.of();
      } else {
         List<FormattedCharSequence> $$3 = Lists.newArrayList();
         Style $$4 = this.charStyles.get(p_131237_);
         int $$5 = p_131237_;

         for (int $$6 = 1; $$6 < p_131238_; $$6++) {
            int $$7 = p_131237_ + $$6;
            Style $$8 = this.charStyles.get($$7);
            if (!$$8.equals($$4)) {
               String $$9 = this.plainText.substring($$5, $$7);
               $$3.add(p_131239_ ? FormattedCharSequence.backward($$9, $$4, this.reverseCharModifier) : FormattedCharSequence.forward($$9, $$4));
               $$4 = $$8;
               $$5 = $$7;
            }
         }

         if ($$5 < p_131237_ + p_131238_) {
            String $$10 = this.plainText.substring($$5, p_131237_ + p_131238_);
            $$3.add(p_131239_ ? FormattedCharSequence.backward($$10, $$4, this.reverseCharModifier) : FormattedCharSequence.forward($$10, $$4));
         }

         return p_131239_ ? Lists.reverse($$3) : $$3;
      }
   }

   public static SubStringSource create(FormattedText p_178537_) {
      return create(p_178537_, p_178527_ -> p_178527_, p_178529_ -> p_178529_);
   }

   public static SubStringSource create(FormattedText p_131252_, Int2IntFunction p_131253_, UnaryOperator<String> p_131254_) {
      StringBuilder $$3 = new StringBuilder();
      List<Style> $$4 = Lists.newArrayList();
      p_131252_.visit((p_131249_, p_131250_) -> {
         StringDecomposer.iterateFormatted(p_131250_, p_131249_, (p_178533_, p_178534_, p_178535_) -> {
            $$3.appendCodePoint(p_178535_);
            int $$5 = Character.charCount(p_178535_);

            for (int $$6 = 0; $$6 < $$5; $$6++) {
               $$4.add(p_178534_);
            }

            return true;
         });
         return Optional.empty();
      }, Style.EMPTY);
      return new SubStringSource(p_131254_.apply($$3.toString()), $$4, p_131253_);
   }
}
