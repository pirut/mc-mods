package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;

public class RegionBitmap {
   private final BitSet used = new BitSet();

   public void force(int sectorOffset, int sectorCount) {
      this.used.set(sectorOffset, sectorOffset + sectorCount);
   }

   public void free(int sectorOffset, int sectorCount) {
      this.used.clear(sectorOffset, sectorOffset + sectorCount);
   }

   public int allocate(int sectorCount) {
      int i = 0;

      while (true) {
         int j = this.used.nextClearBit(i);
         int k = this.used.nextSetBit(j);
         if (k == -1 || k - j >= sectorCount) {
            this.force(j, sectorCount);
            return j;
         }

         i = k;
      }
   }

   @VisibleForTesting
   public IntSet getUsed() {
      return this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
   }
}
