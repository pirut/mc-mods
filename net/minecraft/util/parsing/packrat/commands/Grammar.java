package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.ErrorCollector;
import net.minecraft.util.parsing.packrat.ErrorEntry;
import net.minecraft.util.parsing.packrat.ParseState;

public record Grammar<T>(Dictionary<StringReader> rules, Atom<T> top) {
   public Optional<T> parse(ParseState<StringReader> p_335989_) {
      return p_335989_.parseTopRule(this.top);
   }

   public T parseForCommands(StringReader p_336148_) throws CommandSyntaxException {
      ErrorCollector.LongestOnly<StringReader> $$1 = new ErrorCollector.LongestOnly<>();
      StringReaderParserState $$2 = new StringReaderParserState(this.rules(), $$1, p_336148_);
      Optional<T> $$3 = this.parse($$2);
      if ($$3.isPresent()) {
         return $$3.get();
      } else {
         List<Exception> $$4 = $$1.entries().stream().<Exception>mapMulti((p_335740_, p_335854_) -> {
            if (p_335740_.reason() instanceof Exception $$3x) {
               p_335854_.accept($$3x);
            }
         }).toList();

         for (Exception $$5 : $$4) {
            if ($$5 instanceof CommandSyntaxException $$6) {
               throw $$6;
            }
         }

         if ($$4.size() == 1 && $$4.get(0) instanceof RuntimeException $$7) {
            throw $$7;
         } else {
            throw new IllegalStateException("Failed to parse: " + $$1.entries().stream().map(ErrorEntry::toString).collect(Collectors.joining(", ")));
         }
      }
   }

   public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder p_335783_) {
      StringReader $$1 = new StringReader(p_335783_.getInput());
      $$1.setCursor(p_335783_.getStart());
      ErrorCollector.LongestOnly<StringReader> $$2 = new ErrorCollector.LongestOnly<>();
      StringReaderParserState $$3 = new StringReaderParserState(this.rules(), $$2, $$1);
      this.parse($$3);
      List<ErrorEntry<StringReader>> $$4 = $$2.entries();
      if ($$4.isEmpty()) {
         return p_335783_.buildFuture();
      } else {
         SuggestionsBuilder $$5 = p_335783_.createOffset($$2.cursor());

         for (ErrorEntry<StringReader> $$6 : $$4) {
            if ($$6.suggestions() instanceof ResourceSuggestion $$7) {
               SharedSuggestionProvider.suggestResource($$7.possibleResources(), $$5);
            } else {
               SharedSuggestionProvider.suggest($$6.suggestions().possibleValues($$3), $$5);
            }
         }

         return $$5.buildFuture();
      }
   }
}
