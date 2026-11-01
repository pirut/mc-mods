package net.minecraft.client.resources.language;

import com.google.common.collect.Lists;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;
import java.util.List;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.SubStringSource;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FormattedBidiReorder {
   public static FormattedCharSequence reorder(FormattedText p_118932_, boolean p_118933_) {
      SubStringSource $$2 = SubStringSource.create(p_118932_, UCharacter::getMirror, FormattedBidiReorder::shape);
      Bidi $$3 = new Bidi($$2.getPlainText(), p_118933_ ? 127 : 126);
      $$3.setReorderingMode(0);
      List<FormattedCharSequence> $$4 = Lists.newArrayList();
      int $$5 = $$3.countRuns();

      for (int $$6 = 0; $$6 < $$5; $$6++) {
         BidiRun $$7 = $$3.getVisualRun($$6);
         $$4.addAll($$2.substring($$7.getStart(), $$7.getLength(), $$7.isOddRun()));
      }

      return FormattedCharSequence.composite($$4);
   }

   private static String shape(String p_118930_) {
      try {
         return new ArabicShaping(8).shape(p_118930_);
      } catch (Exception var2) {
         return p_118930_;
      }
   }
}
