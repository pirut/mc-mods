package net.minecraft.commands.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;

public record StringTemplate(List<String> segments, List<String> variables) {
   public static StringTemplate fromString(String p_306229_, int p_305937_) {
      Builder<String> $$2 = ImmutableList.builder();
      Builder<String> $$3 = ImmutableList.builder();
      int $$4 = p_306229_.length();
      int $$5 = 0;
      int $$6 = p_306229_.indexOf(36);

      while ($$6 != -1) {
         if ($$6 != $$4 - 1 && p_306229_.charAt($$6 + 1) == '(') {
            $$2.add(p_306229_.substring($$5, $$6));
            int $$7 = p_306229_.indexOf(41, $$6 + 1);
            if ($$7 == -1) {
               throw new IllegalArgumentException("Unterminated macro variable in macro '" + p_306229_ + "' on line " + p_305937_);
            }

            String $$8 = p_306229_.substring($$6 + 2, $$7);
            if (!isValidVariableName($$8)) {
               throw new IllegalArgumentException("Invalid macro variable name '" + $$8 + "' on line " + p_305937_);
            }

            $$3.add($$8);
            $$5 = $$7 + 1;
            $$6 = p_306229_.indexOf(36, $$5);
         } else {
            $$6 = p_306229_.indexOf(36, $$6 + 1);
         }
      }

      if ($$5 == 0) {
         throw new IllegalArgumentException("Macro without variables on line " + p_305937_);
      } else {
         if ($$5 != $$4) {
            $$2.add(p_306229_.substring($$5));
         }

         return new StringTemplate($$2.build(), $$3.build());
      }
   }

   private static boolean isValidVariableName(String p_306141_) {
      for (int $$1 = 0; $$1 < p_306141_.length(); $$1++) {
         char $$2 = p_306141_.charAt($$1);
         if (!Character.isLetterOrDigit($$2) && $$2 != '_') {
            return false;
         }
      }

      return true;
   }

   public String substitute(List<String> p_306329_) {
      StringBuilder $$1 = new StringBuilder();

      for (int $$2 = 0; $$2 < this.variables.size(); $$2++) {
         $$1.append(this.segments.get($$2)).append(p_306329_.get($$2));
         CommandFunction.checkCommandLineLength($$1);
      }

      if (this.segments.size() > this.variables.size()) {
         $$1.append(this.segments.get(this.segments.size() - 1));
      }

      CommandFunction.checkCommandLineLength($$1);
      return $$1.toString();
   }
}
