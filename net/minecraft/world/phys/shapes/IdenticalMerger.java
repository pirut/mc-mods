package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public class IdenticalMerger implements IndexMerger {
   private final DoubleList coords;

   public IdenticalMerger(DoubleList p_82903_) {
      this.coords = p_82903_;
   }

   @Override
   public boolean forMergedIndexes(IndexMerger.IndexConsumer p_82906_) {
      int $$1 = this.coords.size() - 1;

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         if (!p_82906_.merge($$2, $$2, $$2)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public int size() {
      return this.coords.size();
   }

   @Override
   public DoubleList getList() {
      return this.coords;
   }
}
