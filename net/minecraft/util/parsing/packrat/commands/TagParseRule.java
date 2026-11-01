package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import java.util.Optional;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public class TagParseRule implements Rule<StringReader, Tag> {
   public static final Rule<StringReader, Tag> INSTANCE = new TagParseRule();

   private TagParseRule() {
   }

   @Override
   public Optional<Tag> parse(ParseState<StringReader> p_336059_) {
      p_336059_.input().skipWhitespace();
      int $$1 = p_336059_.mark();

      try {
         return Optional.of(new TagParser(p_336059_.input()).readValue());
      } catch (Exception var4) {
         p_336059_.errorCollector().store($$1, var4);
         return Optional.empty();
      }
   }
}
