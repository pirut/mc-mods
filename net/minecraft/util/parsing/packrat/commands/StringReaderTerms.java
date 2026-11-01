package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.chars.CharList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import net.minecraft.util.parsing.packrat.Term;

public interface StringReaderTerms {
   static Term<StringReader> word(String p_335707_) {
      return new StringReaderTerms.TerminalWord(p_335707_);
   }

   static Term<StringReader> character(final char p_336175_) {
      return new StringReaderTerms.TerminalCharacters(CharList.of(p_336175_)) {
         @Override
         protected boolean isAccepted(char p_410825_) {
            return p_336175_ == p_410825_;
         }
      };
   }

   static Term<StringReader> characters(final char p_410334_, final char p_409760_) {
      return new StringReaderTerms.TerminalCharacters(CharList.of(p_410334_, p_409760_)) {
         @Override
         protected boolean isAccepted(char p_410435_) {
            return p_410435_ == p_410334_ || p_410435_ == p_409760_;
         }
      };
   }

   static StringReader createReader(String p_409968_, int p_410429_) {
      StringReader stringreader = new StringReader(p_409968_);
      stringreader.setCursor(p_410429_);
      return stringreader;
   }

   public abstract static class TerminalCharacters implements Term<StringReader> {
      private final DelayedException<CommandSyntaxException> error;
      private final SuggestionSupplier<StringReader> suggestions;

      public TerminalCharacters(CharList p_410625_) {
         String s = p_410625_.intStream().mapToObj(Character::toString).collect(Collectors.joining("|"));
         this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), s);
         this.suggestions = p_410379_ -> p_410625_.intStream().mapToObj(Character::toString);
      }

      @Override
      public boolean parse(ParseState<StringReader> p_409975_, Scope p_409815_, Control p_410378_) {
         p_409975_.input().skipWhitespace();
         int i = p_409975_.mark();
         if (p_409975_.input().canRead() && this.isAccepted(p_409975_.input().read())) {
            return true;
         } else {
            p_409975_.errorCollector().store(i, this.suggestions, this.error);
            return false;
         }
      }

      protected abstract boolean isAccepted(char var1);
   }

   public static final class TerminalWord implements Term<StringReader> {
      private final String value;
      private final DelayedException<CommandSyntaxException> error;
      private final SuggestionSupplier<StringReader> suggestions;

      public TerminalWord(String p_335903_) {
         this.value = p_335903_;
         this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), p_335903_);
         this.suggestions = p_409184_ -> Stream.of(p_335903_);
      }

      @Override
      public boolean parse(ParseState<StringReader> p_335419_, Scope p_335724_, Control p_335868_) {
         p_335419_.input().skipWhitespace();
         int i = p_335419_.mark();
         String s = p_335419_.input().readUnquotedString();
         if (!s.equals(this.value)) {
            p_335419_.errorCollector().store(i, this.suggestions, this.error);
            return false;
         } else {
            return true;
         }
      }

      @Override
      public String toString() {
         return "terminal[" + this.value + "]";
      }
   }
}
