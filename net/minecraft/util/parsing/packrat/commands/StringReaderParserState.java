package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import net.minecraft.util.parsing.packrat.CachedParseState;
import net.minecraft.util.parsing.packrat.ErrorCollector;

public class StringReaderParserState extends CachedParseState<StringReader> {
   private final StringReader input;

   public StringReaderParserState(ErrorCollector<StringReader> p_335915_, StringReader p_335601_) {
      super(p_335915_);
      this.input = p_335601_;
   }

   public StringReader input() {
      return this.input;
   }

   @Override
   public int mark() {
      return this.input.getCursor();
   }

   @Override
   public void restore(int p_335710_) {
      this.input.setCursor(p_335710_);
   }
}
