package net.minecraft.util.parsing.packrat;

import org.jspecify.annotations.Nullable;

public interface Rule<S, T> {
   @Nullable
   T parse(ParseState<S> var1);

   static <S, T> Rule<S, T> fromTerm(Term<S> p_335422_, Rule.RuleAction<S, T> p_335862_) {
      return new Rule.WrappedTerm<>(p_335862_, p_335422_);
   }

   static <S, T> Rule<S, T> fromTerm(Term<S> p_335465_, Rule.SimpleRuleAction<S, T> p_336053_) {
      return new Rule.WrappedTerm<>(p_336053_, p_335465_);
   }

   @FunctionalInterface
   public interface RuleAction<S, T> {
      @Nullable
      T run(ParseState<S> var1);
   }

   @FunctionalInterface
   public interface SimpleRuleAction<S, T> extends Rule.RuleAction<S, T> {
      T run(Scope var1);

      @Override
      default T run(ParseState<S> p_410290_) {
         return this.run(p_410290_.scope());
      }
   }

   public record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
      @Nullable
      @Override
      public T parse(ParseState<S> p_336049_) {
         Scope scope = p_336049_.scope();
         scope.pushFrame();

         Object var4;
         try {
            if (this.child.parse(p_336049_, scope, Control.UNBOUND)) {
               return this.action.run(p_336049_);
            }

            var4 = null;
         } finally {
            scope.popFrame();
         }

         return (T)var4;
      }
   }
}
