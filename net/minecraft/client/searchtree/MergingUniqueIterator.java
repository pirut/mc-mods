package net.minecraft.client.searchtree;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.Comparator;
import java.util.Iterator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MergingUniqueIterator<T> extends AbstractIterator<T> {
   private final PeekingIterator<T> firstIterator;
   private final PeekingIterator<T> secondIterator;
   private final Comparator<T> comparator;

   public MergingUniqueIterator(Iterator<T> p_235186_, Iterator<T> p_235187_, Comparator<T> p_235188_) {
      this.firstIterator = Iterators.peekingIterator(p_235186_);
      this.secondIterator = Iterators.peekingIterator(p_235187_);
      this.comparator = p_235188_;
   }

   protected T computeNext() {
      boolean $$0 = !this.firstIterator.hasNext();
      boolean $$1 = !this.secondIterator.hasNext();
      if ($$0 && $$1) {
         return (T)this.endOfData();
      } else if ($$0) {
         return (T)this.secondIterator.next();
      } else if ($$1) {
         return (T)this.firstIterator.next();
      } else {
         int $$2 = this.comparator.compare((T)this.firstIterator.peek(), (T)this.secondIterator.peek());
         if ($$2 == 0) {
            this.secondIterator.next();
         }

         return (T)($$2 <= 0 ? this.firstIterator.next() : this.secondIterator.next());
      }
   }
}
