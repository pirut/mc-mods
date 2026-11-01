package net.minecraft.util.parsing.packrat;

import java.util.Optional;

public interface Rule<S, T> {
   Optional<T> parse(ParseState<S> var1);

   static <S, T> Rule<S, T> fromTerm(Term<S> p_335422_, Rule.RuleAction<S, T> p_335862_) {
      return new Rule.WrappedTerm<>(p_335862_, p_335422_);
   }

   static <S, T> Rule<S, T> fromTerm(Term<S> p_335465_, Rule.SimpleRuleAction<T> p_336053_) {
      return new Rule.WrappedTerm<>((p_336011_, p_336192_) -> Optional.of(p_336053_.run(p_336192_)), p_335465_);
   }

   @FunctionalInterface
   public interface RuleAction<S, T> {
      Optional<T> run(ParseState<S> var1, Scope var2);
   }

   @FunctionalInterface
   public interface SimpleRuleAction<T> {
      T run(Scope var1);
   }

   public record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
      @Override
      public Optional<T> parse(ParseState<S> p_336049_) {
         Scope $$1 = new Scope();
         return this.child.parse(p_336049_, $$1, Control.UNBOUND) ? this.action.run(p_336049_, $$1) : Optional.empty();
      }
   }
}
