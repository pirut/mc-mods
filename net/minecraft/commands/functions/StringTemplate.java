package net.minecraft.commands.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;

public record StringTemplate(List<String> segments, List<String> variables) {
   public static StringTemplate fromString(String input) {
      Builder<String> builder = ImmutableList.builder();
      Builder<String> builder2 = ImmutableList.builder();
      int i = input.length();
      int j = 0;
      int k = input.indexOf(36);

      while (k != -1) {
         if (k != i - 1 && input.charAt(k + 1) == '(') {
            builder.add(input.substring(j, k));
            int l = input.indexOf(41, k + 1);
            if (l == -1) {
               throw new IllegalArgumentException("Unterminated macro variable");
            }

            String string = input.substring(k + 2, l);
            if (!isValidVariableName(string)) {
               throw new IllegalArgumentException("Invalid macro variable name '" + string + "'");
            }

            builder2.add(string);
            j = l + 1;
            k = input.indexOf(36, j);
         } else {
            k = input.indexOf(36, k + 1);
         }
      }

      if (j == 0) {
         throw new IllegalArgumentException("No variables in macro");
      } else {
         if (j != i) {
            builder.add(input.substring(j));
         }

         return new StringTemplate(builder.build(), builder2.build());
      }
   }

   public static boolean isValidVariableName(String variableName) {
      for (int i = 0; i < variableName.length(); i++) {
         char c = variableName.charAt(i);
         if (!Character.isLetterOrDigit(c) && c != '_') {
            return false;
         }
      }

      return true;
   }

   public String substitute(List<String> arguments) {
      StringBuilder stringBuilder = new StringBuilder();

      for (int i = 0; i < this.variables.size(); i++) {
         stringBuilder.append(this.segments.get(i)).append(arguments.get(i));
         CommandFunction.checkCommandLineLength(stringBuilder);
      }

      if (this.segments.size() > this.variables.size()) {
         stringBuilder.append(this.segments.getLast());
      }

      CommandFunction.checkCommandLineLength(stringBuilder);
      return stringBuilder.toString();
   }
}
