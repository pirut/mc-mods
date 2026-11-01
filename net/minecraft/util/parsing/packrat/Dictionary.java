package net.minecraft.util.parsing.packrat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class Dictionary<S> {
   private final Map<Atom<?>, Dictionary.Entry<S, ?>> terms = new IdentityHashMap<>();

   public <T> NamedRule<S, T> put(Atom<T> p_335994_, Rule<S, T> p_336190_) {
      Dictionary.Entry<S, T> entry = (Dictionary.Entry<S, T>)this.terms.computeIfAbsent(p_335994_, Dictionary.Entry::new);
      if (entry.value != null) {
         throw new IllegalArgumentException("Trying to override rule: " + p_335994_);
      } else {
         entry.value = p_336190_;
         return entry;
      }
   }

   public <T> NamedRule<S, T> putComplex(Atom<T> p_410268_, Term<S> p_410760_, Rule.RuleAction<S, T> p_410488_) {
      return this.put(p_410268_, Rule.fromTerm(p_410760_, p_410488_));
   }

   public <T> NamedRule<S, T> put(Atom<T> p_335571_, Term<S> p_336089_, Rule.SimpleRuleAction<S, T> p_410867_) {
      return this.put(p_335571_, Rule.fromTerm(p_336089_, p_410867_));
   }

   public void checkAllBound() {
      List<? extends Atom<?>> list = this.terms.entrySet().stream().filter(p_460385_ -> p_460385_.getValue().value == null).map(Map.Entry::getKey).toList();
      if (!list.isEmpty()) {
         throw new IllegalStateException("Unbound names: " + list);
      }
   }

   public <T> NamedRule<S, T> getOrThrow(Atom<T> p_410765_) {
      return (NamedRule<S, T>)Objects.requireNonNull(this.terms.get(p_410765_), () -> "No rule called " + p_410765_);
   }

   public <T> NamedRule<S, T> forward(Atom<T> p_409601_) {
      return this.getOrCreateEntry(p_409601_);
   }

   private <T> Dictionary.Entry<S, T> getOrCreateEntry(Atom<T> p_410050_) {
      return (Dictionary.Entry<S, T>)this.terms.computeIfAbsent(p_410050_, Dictionary.Entry::new);
   }

   public <T> Term<S> named(Atom<T> p_410244_) {
      return new Dictionary.Reference<>(this.getOrCreateEntry(p_410244_), p_410244_);
   }

   public <T> Term<S> namedWithAlias(Atom<T> p_409625_, Atom<T> p_410453_) {
      return new Dictionary.Reference<>(this.getOrCreateEntry(p_409625_), p_410453_);
   }

   static class Entry<S, T> implements NamedRule<S, T>, Supplier<String> {
      private final Atom<T> name;
      @Nullable
      Rule<S, T> value;

      private Entry(Atom<T> p_410058_) {
         this.name = p_410058_;
      }

      @Override
      public Atom<T> name() {
         return this.name;
      }

      @Override
      public Rule<S, T> value() {
         return Objects.requireNonNull(this.value, this);
      }

      public String get() {
         return "Unbound rule " + this.name;
      }
   }

   record Reference<S, T>(Dictionary.Entry<S, T> ruleToParse, Atom<T> nameToStore) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_409926_, Scope p_409751_, Control p_409959_) {
         T t = p_409926_.parse(this.ruleToParse);
         if (t == null) {
            return false;
         } else {
            p_409751_.put(this.nameToStore, t);
            return true;
         }
      }
   }
}
