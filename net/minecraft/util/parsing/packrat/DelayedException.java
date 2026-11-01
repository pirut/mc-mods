package net.minecraft.util.parsing.packrat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;

public interface DelayedException<T extends Exception> {
   T create(String var1, int var2);

   static DelayedException<CommandSyntaxException> create(SimpleCommandExceptionType p_410071_) {
      return (p_410118_, p_410691_) -> p_410071_.createWithContext(StringReaderTerms.createReader(p_410118_, p_410691_));
   }

   static DelayedException<CommandSyntaxException> create(DynamicCommandExceptionType p_410801_, String p_410622_) {
      return (p_410506_, p_410266_) -> p_410801_.createWithContext(StringReaderTerms.createReader(p_410506_, p_410266_), p_410622_);
   }
}
