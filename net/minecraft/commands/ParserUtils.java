package net.minecraft.commands;

import com.mojang.brigadier.StringReader;
import net.minecraft.CharPredicate;

public class ParserUtils {
   public static String readWhile(StringReader p_332772_, CharPredicate p_332664_) {
      int i = p_332772_.getCursor();

      while (p_332772_.canRead() && p_332664_.test(p_332772_.peek())) {
         p_332772_.skip();
      }

      return p_332772_.getString().substring(i, p_332772_.getCursor());
   }
}
