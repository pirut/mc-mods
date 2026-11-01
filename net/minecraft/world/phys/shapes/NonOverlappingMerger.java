package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class NonOverlappingMerger extends AbstractDoubleList implements IndexMerger {
   private final DoubleList lower;
   private final DoubleList upper;
   private final boolean swap;

   protected NonOverlappingMerger(DoubleList lower, DoubleList upper, boolean swap) {
      this.lower = lower;
      this.upper = upper;
      this.swap = swap;
   }

   @Override
   public int size() {
      return this.lower.size() + this.upper.size();
   }

   @Override
   public boolean forMergedIndexes(IndexMerger.IndexConsumer consumer) {
      return this.swap ? this.forNonSwappedIndexes((i, j, k) -> consumer.merge(j, i, k)) : this.forNonSwappedIndexes(consumer);
   }

   private boolean forNonSwappedIndexes(IndexMerger.IndexConsumer consumer) {
      int i = this.lower.size();

      for (int j = 0; j < i; j++) {
         if (!consumer.merge(j, -1, j)) {
            return false;
         }
      }

      int jx = this.upper.size() - 1;

      for (int k = 0; k < jx; k++) {
         if (!consumer.merge(i - 1, k, i + k)) {
            return false;
         }
      }

      return true;
   }

   public double getDouble(int i) {
      return i < this.lower.size() ? this.lower.getDouble(i) : this.upper.getDouble(i - this.lower.size());
   }

   @Override
   public DoubleList getList() {
      return this;
   }
}
