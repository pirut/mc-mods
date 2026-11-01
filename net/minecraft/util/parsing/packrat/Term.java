package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;

public interface Term<S> {
   boolean parse(ParseState<S> var1, Scope var2, Control var3);

   static <S, T> Term<S> marker(Atom<T> p_335833_, T p_335787_) {
      return new Term.Marker<>(p_335833_, p_335787_);
   }

   @SafeVarargs
   static <S> Term<S> sequence(Term<S>... p_336132_) {
      return new Term.Sequence<>(p_336132_);
   }

   @SafeVarargs
   static <S> Term<S> alternative(Term<S>... p_335455_) {
      return new Term.Alternative<>(p_335455_);
   }

   static <S> Term<S> optional(Term<S> p_335471_) {
      return new Term.Maybe<>(p_335471_);
   }

   static <S, T> Term<S> repeated(NamedRule<S, T> p_410325_, Atom<List<T>> p_410349_) {
      return repeated(p_410325_, p_410349_, 0);
   }

   static <S, T> Term<S> repeated(NamedRule<S, T> p_410540_, Atom<List<T>> p_409771_, int p_410812_) {
      return new Term.Repeated<>(p_410540_, p_409771_, p_410812_);
   }

   static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> p_410572_, Atom<List<T>> p_410697_, Term<S> p_410133_) {
      return repeatedWithTrailingSeparator(p_410572_, p_410697_, p_410133_, 0);
   }

   static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> p_410722_, Atom<List<T>> p_410003_, Term<S> p_410085_, int p_409982_) {
      return new Term.RepeatedWithSeparator<>(p_410722_, p_410003_, p_410085_, p_409982_, true);
   }

   static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> p_409741_, Atom<List<T>> p_410786_, Term<S> p_410595_) {
      return repeatedWithoutTrailingSeparator(p_409741_, p_410786_, p_410595_, 0);
   }

   static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> p_410243_, Atom<List<T>> p_410799_, Term<S> p_409788_, int p_410232_) {
      return new Term.RepeatedWithSeparator<>(p_410243_, p_410799_, p_409788_, p_410232_, false);
   }

   static <S> Term<S> positiveLookahead(Term<S> p_410502_) {
      return new Term.LookAhead<>(p_410502_, true);
   }

   static <S> Term<S> negativeLookahead(Term<S> p_410778_) {
      return new Term.LookAhead<>(p_410778_, false);
   }

   static <S> Term<S> cut() {
      return new Term<S>() {
         @Override
         public boolean parse(ParseState<S> p_335490_, Scope p_335377_, Control p_336074_) {
            p_336074_.cut();
            return true;
         }

         @Override
         public String toString() {
            return "↑";
         }
      };
   }

   static <S> Term<S> empty() {
      return new Term<S>() {
         @Override
         public boolean parse(ParseState<S> p_335978_, Scope p_335744_, Control p_335881_) {
            return true;
         }

         @Override
         public String toString() {
            return "ε";
         }
      };
   }

   static <S> Term<S> fail(final Object p_409759_) {
      return new Term<S>() {
         @Override
         public boolean parse(ParseState<S> p_410580_, Scope p_410090_, Control p_409973_) {
            p_410580_.errorCollector().store(p_410580_.mark(), p_409759_);
            return false;
         }

         @Override
         public String toString() {
            return "fail";
         }
      };
   }

   public record Alternative<S>(Term<S>[] elements) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_336147_, Scope p_335902_, Control p_335396_) {
         Control control = p_336147_.acquireControl();

         try {
            int i = p_336147_.mark();
            p_335902_.splitFrame();

            for (Term<S> term : this.elements) {
               if (term.parse(p_336147_, p_335902_, control)) {
                  p_335902_.mergeFrame();
                  return true;
               }

               p_335902_.clearFrameValues();
               p_336147_.restore(i);
               if (control.hasCut()) {
                  break;
               }
            }

            p_335902_.popFrame();
            return false;
         } finally {
            p_336147_.releaseControl();
         }
      }
   }

   public record LookAhead<S>(Term<S> term, boolean positive) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_409679_, Scope p_410389_, Control p_410731_) {
         int i = p_409679_.mark();
         boolean flag = this.term.parse(p_409679_.silent(), p_410389_, p_410731_);
         p_409679_.restore(i);
         return this.positive == flag;
      }
   }

   public record Marker<S, T>(Atom<T> name, T value) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_335600_, Scope p_335485_, Control p_335375_) {
         p_335485_.put(this.name, this.value);
         return true;
      }
   }

   public record Maybe<S>(Term<S> term) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_335415_, Scope p_335550_, Control p_336000_) {
         int i = p_335415_.mark();
         if (!this.term.parse(p_335415_, p_335550_, p_336000_)) {
            p_335415_.restore(i);
         }

         return true;
      }
   }

   public record Repeated<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_410665_, Scope p_410752_, Control p_410872_) {
         int i = p_410665_.mark();
         List<T> list = new ArrayList<>(this.minRepetitions);

         while (true) {
            int j = p_410665_.mark();
            T t = p_410665_.parse(this.element);
            if (t == null) {
               p_410665_.restore(j);
               if (list.size() < this.minRepetitions) {
                  p_410665_.restore(i);
                  return false;
               } else {
                  p_410752_.put(this.listName, list);
                  return true;
               }
            }

            list.add(t);
         }
      }
   }

   public record RepeatedWithSeparator<S, T>(
      NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions, boolean allowTrailingSeparator
   ) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_410533_, Scope p_410855_, Control p_410756_) {
         int i = p_410533_.mark();
         List<T> list = new ArrayList<>(this.minRepetitions);
         boolean flag = true;

         while (true) {
            int j = p_410533_.mark();
            if (!flag && !this.separator.parse(p_410533_, p_410855_, p_410756_)) {
               p_410533_.restore(j);
               break;
            }

            int k = p_410533_.mark();
            T t = p_410533_.parse(this.element);
            if (t == null) {
               if (flag) {
                  p_410533_.restore(k);
               } else {
                  if (!this.allowTrailingSeparator) {
                     p_410533_.restore(i);
                     return false;
                  }

                  p_410533_.restore(k);
               }
               break;
            }

            list.add(t);
            flag = false;
         }

         if (list.size() < this.minRepetitions) {
            p_410533_.restore(i);
            return false;
         } else {
            p_410855_.put(this.listName, list);
            return true;
         }
      }
   }

   public record Sequence<S>(Term<S>[] elements) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_336111_, Scope p_335911_, Control p_336188_) {
         int i = p_336111_.mark();

         for (Term<S> term : this.elements) {
            if (!term.parse(p_336111_, p_335911_, p_336188_)) {
               p_336111_.restore(i);
               return false;
            }
         }

         return true;
      }
   }
}
