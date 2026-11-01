package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;

public class RegionBitmap {
   private final BitSet used = new BitSet();

   public void force(int p_63613_, int p_63614_) {
      this.used.set(p_63613_, p_63613_ + p_63614_);
   }

   public void free(int p_63616_, int p_63617_) {
      this.used.clear(p_63616_, p_63616_ + p_63617_);
   }

   public int allocate(int p_63611_) {
      int $$1 = 0;

      while (true) {
         int $$2 = this.used.nextClearBit($$1);
         int $$3 = this.used.nextSetBit($$2);
         if ($$3 == -1 || $$3 - $$2 >= p_63611_) {
            this.force($$2, p_63611_);
            return $$2;
         }

         $$1 = $$3;
      }
   }

   @VisibleForTesting
   public IntSet getUsed() {
      return this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
   }
}
