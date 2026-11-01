package net.neoforged.neoforge.common.util;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import net.neoforged.neoforge.common.util.strategy.BasicStrategy;

public class InsertableLinkedOpenCustomHashSet<T> extends ObjectLinkedOpenCustomHashSet<T> {
   private static final long LINK_BIT_SPACE = 32L;
   private static final long NEXT_LINK = 4294967295L;
   private static final long PREV_LINK = -4294967296L;

   public InsertableLinkedOpenCustomHashSet() {
      super(BasicStrategy.BASIC);
   }

   public InsertableLinkedOpenCustomHashSet(Strategy<? super T> strategy) {
      super(strategy);
   }

   public boolean addAfter(T insertAfter, T element) {
      int afterPos;
      if (this.contains(insertAfter) && this.last != (afterPos = this.getPos(insertAfter))) {
         int pos = HashCommon.mix(this.strategy.hashCode(element)) & this.mask;
         T curr = (T)this.key[pos];
         if (curr != null) {
            do {
               if (this.strategy.equals(curr, element)) {
                  return false;
               }
            } while ((curr = (T)this.key[pos = pos + 1 & this.mask]) != null);
         }

         this.key[pos] = element;
         long nextPos = this.link[afterPos] & 4294967295L;
         this.link[(int)nextPos] = this.link[(int)nextPos] ^ (this.link[(int)nextPos] ^ (pos & 4294967295L) << 32) & -4294967296L;
         this.link[afterPos] = this.link[afterPos] ^ (this.link[afterPos] ^ pos & 4294967295L) & 4294967295L;
         this.link[pos] = (afterPos & 4294967295L) << 32 | nextPos;
         if (this.size++ >= this.maxFill) {
            this.rehash(HashCommon.arraySize(this.size + 1, this.f));
         }

         return true;
      } else {
         return this.add(element);
      }
   }

   public boolean addBefore(T insertBefore, T element) {
      if (this.contains(insertBefore)) {
         int beforePos = this.getPos(insertBefore);
         if (beforePos == this.first) {
            return this.contains(element) ? false : this.addAndMoveToFirst(element);
         } else {
            int pos = HashCommon.mix(this.strategy.hashCode(element)) & this.mask;
            T curr = (T)this.key[pos];
            if (curr != null) {
               do {
                  if (this.strategy.equals(curr, element)) {
                     return false;
                  }
               } while ((curr = (T)this.key[pos = pos + 1 & this.mask]) != null);
            }

            this.key[pos] = element;
            long prevPos = (this.link[beforePos] & -4294967296L) >> 32;
            this.link[(int)prevPos] = this.link[(int)prevPos] ^ (this.link[(int)prevPos] ^ pos & 4294967295L) & 4294967295L;
            this.link[beforePos] = this.link[beforePos] ^ (this.link[beforePos] ^ (pos & 4294967295L) << 32) & -4294967296L;
            this.link[pos] = prevPos << 32 | beforePos & 4294967295L;
            if (this.size++ >= this.maxFill) {
               this.rehash(HashCommon.arraySize(this.size + 1, this.f));
            }

            return true;
         }
      } else {
         return this.add(element);
      }
   }

   public void addFirst(T element) {
      this.addAndMoveToFirst(element);
   }

   public void addLast(T element) {
      this.addAndMoveToLast(element);
   }

   private int getPos(T existingElement) {
      int pos = HashCommon.mix(this.strategy.hashCode(existingElement)) & this.mask;
      T curr = (T)this.key[pos];

      while (!this.strategy.equals(curr, existingElement) && (curr = (T)this.key[pos = pos + 1 & this.mask]) != null) {
      }

      return pos;
   }
}
