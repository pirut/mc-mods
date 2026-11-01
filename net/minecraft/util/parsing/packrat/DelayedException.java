package net.minecraft.util.parsing.packrat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;

public interface DelayedException<T extends Exception> {
   T create(String string, int i);

   static DelayedException<CommandSyntaxException> create(SimpleCommandExceptionType exception) {
      return (string, i) -> exception.createWithContext(StringReaderTerms.createReader(string, i));
   }

   static DelayedException<CommandSyntaxException> create(DynamicCommandExceptionType exception, String argument) {
      return (string2, i) -> exception.createWithContext(StringReaderTerms.createReader(string2, i), argument);
   }
}
