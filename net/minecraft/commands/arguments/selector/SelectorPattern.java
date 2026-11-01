package net.minecraft.commands.arguments.selector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public record SelectorPattern(String pattern, EntitySelector resolved) {
   public static final Codec<SelectorPattern> CODEC = Codec.STRING.comapFlatMap(SelectorPattern::parse, SelectorPattern::pattern);

   public static DataResult<SelectorPattern> parse(String p_361421_) {
      try {
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(new StringReader(p_361421_), true);
         return DataResult.success(new SelectorPattern(p_361421_, entityselectorparser.parse()));
      } catch (CommandSyntaxException var2) {
         return DataResult.error(() -> "Invalid selector component: " + p_361421_ + ": " + var2.getMessage());
      }
   }

   @Override
   public boolean equals(Object p_360529_) {
      return p_360529_ instanceof SelectorPattern selectorpattern && this.pattern.equals(selectorpattern.pattern);
   }

   @Override
   public int hashCode() {
      return this.pattern.hashCode();
   }

   @Override
   public String toString() {
      return this.pattern;
   }
}
