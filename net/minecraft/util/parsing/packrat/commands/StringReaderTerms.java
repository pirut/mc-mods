package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.stream.Stream;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;

public interface StringReaderTerms {
   static Term<StringReader> word(String p_335707_) {
      return new StringReaderTerms.TerminalWord(p_335707_);
   }

   static Term<StringReader> character(char p_336175_) {
      return new StringReaderTerms.TerminalCharacter(p_336175_);
   }

   public record TerminalCharacter(char value) implements Term<StringReader> {
      @Override
      public boolean parse(ParseState<StringReader> p_335747_, Scope p_336031_, Control p_336041_) {
         p_335747_.input().skipWhitespace();
         int $$3 = p_335747_.mark();
         if (p_335747_.input().canRead() && p_335747_.input().read() == this.value) {
            return true;
         } else {
            p_335747_.errorCollector()
               .store($$3, p_335790_ -> Stream.of(String.valueOf(this.value)), CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().create(this.value));
            return false;
         }
      }
   }

   public record TerminalWord(String value) implements Term<StringReader> {
      @Override
      public boolean parse(ParseState<StringReader> p_335419_, Scope p_335724_, Control p_335868_) {
         p_335419_.input().skipWhitespace();
         int $$3 = p_335419_.mark();
         String $$4 = p_335419_.input().readUnquotedString();
         if (!$$4.equals(this.value)) {
            p_335419_.errorCollector()
               .store($$3, p_336037_ -> Stream.of(this.value), CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().create(this.value));
            return false;
         } else {
            return true;
         }
      }
   }
}
