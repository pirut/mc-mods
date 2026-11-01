package net.minecraft.util.parsing.packrat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

public abstract class ParseState<S> {
   private final Map<ParseState.CacheKey<?>, ParseState.CacheEntry<?>> ruleCache = new HashMap<>();
   private final Dictionary<S> dictionary;
   private final ErrorCollector<S> errorCollector;

   protected ParseState(Dictionary<S> p_336166_, ErrorCollector<S> p_336116_) {
      this.dictionary = p_336166_;
      this.errorCollector = p_336116_;
   }

   public ErrorCollector<S> errorCollector() {
      return this.errorCollector;
   }

   public <T> Optional<T> parseTopRule(Atom<T> p_335920_) {
      Optional<T> $$1 = this.parse(p_335920_);
      if ($$1.isPresent()) {
         this.errorCollector.finish(this.mark());
      }

      return $$1;
   }

   public <T> Optional<T> parse(Atom<T> p_336112_) {
      ParseState.CacheKey<T> $$1 = new ParseState.CacheKey<>(p_336112_, this.mark());
      ParseState.CacheEntry<T> $$2 = this.lookupInCache($$1);
      if ($$2 != null) {
         this.restore($$2.mark());
         return $$2.value;
      } else {
         Rule<S, T> $$3 = this.dictionary.get(p_336112_);
         if ($$3 == null) {
            throw new IllegalStateException("No symbol " + p_336112_);
         } else {
            Optional<T> $$4 = $$3.parse(this);
            this.storeInCache($$1, $$4);
            return $$4;
         }
      }
   }

   @Nullable
   private <T> ParseState.CacheEntry<T> lookupInCache(ParseState.CacheKey<T> p_335816_) {
      return (ParseState.CacheEntry<T>)this.ruleCache.get(p_335816_);
   }

   private <T> void storeInCache(ParseState.CacheKey<T> p_336008_, Optional<T> p_335995_) {
      this.ruleCache.put(p_336008_, new ParseState.CacheEntry<>(p_335995_, this.mark()));
   }

   public abstract S input();

   public abstract int mark();

   public abstract void restore(int var1);

   record CacheEntry<T>(Optional<T> value, int mark) {
   }

   record CacheKey<T>(Atom<T> name, int mark) {
   }
}
