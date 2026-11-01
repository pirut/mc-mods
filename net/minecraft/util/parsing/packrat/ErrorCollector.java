package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Util;

public interface ErrorCollector<S> {
   void store(int var1, SuggestionSupplier<S> var2, Object var3);

   default void store(int p_335680_, Object p_335827_) {
      this.store(p_335680_, SuggestionSupplier.empty(), p_335827_);
   }

   void finish(int var1);

   public static class LongestOnly<S> implements ErrorCollector<S> {
      private ErrorCollector.LongestOnly.MutableErrorEntry<S>[] entries = new ErrorCollector.LongestOnly.MutableErrorEntry[16];
      private int nextErrorEntry;
      private int lastCursor = -1;

      private void discardErrorsFromShorterParse(int p_335634_) {
         if (p_335634_ > this.lastCursor) {
            this.lastCursor = p_335634_;
            this.nextErrorEntry = 0;
         }
      }

      @Override
      public void finish(int p_335534_) {
         this.discardErrorsFromShorterParse(p_335534_);
      }

      @Override
      public void store(int p_335763_, SuggestionSupplier<S> p_336144_, Object p_335736_) {
         this.discardErrorsFromShorterParse(p_335763_);
         if (p_335763_ == this.lastCursor) {
            this.addErrorEntry(p_336144_, p_335736_);
         }
      }

      private void addErrorEntry(SuggestionSupplier<S> p_409930_, Object p_409668_) {
         int i = this.entries.length;
         if (this.nextErrorEntry >= i) {
            int j = Util.growByHalf(i, this.nextErrorEntry + 1);
            ErrorCollector.LongestOnly.MutableErrorEntry<S>[] mutableerrorentry = new ErrorCollector.LongestOnly.MutableErrorEntry[j];
            System.arraycopy(this.entries, 0, mutableerrorentry, 0, i);
            this.entries = mutableerrorentry;
         }

         int k = this.nextErrorEntry++;
         ErrorCollector.LongestOnly.MutableErrorEntry<S> mutableerrorentry1 = this.entries[k];
         if (mutableerrorentry1 == null) {
            mutableerrorentry1 = new ErrorCollector.LongestOnly.MutableErrorEntry<>();
            this.entries[k] = mutableerrorentry1;
         }

         mutableerrorentry1.suggestions = p_409930_;
         mutableerrorentry1.reason = p_409668_;
      }

      public List<ErrorEntry<S>> entries() {
         int i = this.nextErrorEntry;
         if (i == 0) {
            return List.of();
         } else {
            List<ErrorEntry<S>> list = new ArrayList<>(i);

            for (int j = 0; j < i; j++) {
               ErrorCollector.LongestOnly.MutableErrorEntry<S> mutableerrorentry = this.entries[j];
               list.add(new ErrorEntry<>(this.lastCursor, mutableerrorentry.suggestions, mutableerrorentry.reason));
            }

            return list;
         }
      }

      public int cursor() {
         return this.lastCursor;
      }

      static class MutableErrorEntry<S> {
         SuggestionSupplier<S> suggestions = SuggestionSupplier.empty();
         Object reason = "empty";
      }
   }

   public static class Nop<S> implements ErrorCollector<S> {
      @Override
      public void store(int p_409770_, SuggestionSupplier<S> p_409587_, Object p_410314_) {
      }

      @Override
      public void finish(int p_409839_) {
      }
   }
}
