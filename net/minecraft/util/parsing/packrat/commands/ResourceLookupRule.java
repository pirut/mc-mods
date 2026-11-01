package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public abstract class ResourceLookupRule<C, V> implements Rule<StringReader, V>, ResourceSuggestion {
   private final Atom<ResourceLocation> idParser;
   protected final C context;

   protected ResourceLookupRule(Atom<ResourceLocation> p_336007_, C p_335882_) {
      this.idParser = p_336007_;
      this.context = p_335882_;
   }

   @Override
   public Optional<V> parse(ParseState<StringReader> p_335941_) {
      p_335941_.input().skipWhitespace();
      int $$1 = p_335941_.mark();
      Optional<ResourceLocation> $$2 = p_335941_.parse(this.idParser);
      if ($$2.isPresent()) {
         try {
            return Optional.of(this.validateElement((ImmutableStringReader)p_335941_.input(), $$2.get()));
         } catch (Exception var5) {
            p_335941_.errorCollector().store($$1, this, var5);
            return Optional.empty();
         }
      } else {
         p_335941_.errorCollector().store($$1, this, ResourceLocation.ERROR_INVALID.createWithContext((ImmutableStringReader)p_335941_.input()));
         return Optional.empty();
      }
   }

   protected abstract V validateElement(ImmutableStringReader var1, ResourceLocation var2) throws Exception;
}
