package net.minecraft.server.level;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class SectionTracker extends DynamicGraphMinFixedPoint {
   protected SectionTracker(int p_8274_, int p_8275_, int p_8276_) {
      super(p_8274_, p_8275_, p_8276_);
   }

   @Override
   protected void checkNeighborsAfterUpdate(long p_8280_, int p_8281_, boolean p_8282_) {
      if (!p_8282_ || p_8281_ < this.levelCount - 2) {
         for (int $$3 = -1; $$3 <= 1; $$3++) {
            for (int $$4 = -1; $$4 <= 1; $$4++) {
               for (int $$5 = -1; $$5 <= 1; $$5++) {
                  long $$6 = SectionPos.offset(p_8280_, $$3, $$4, $$5);
                  if ($$6 != p_8280_) {
                     this.checkNeighbor(p_8280_, $$6, p_8281_, p_8282_);
                  }
               }
            }
         }
      }
   }

   @Override
   protected int getComputedLevel(long p_8284_, long p_8285_, int p_8286_) {
      int $$3 = p_8286_;

      for (int $$4 = -1; $$4 <= 1; $$4++) {
         for (int $$5 = -1; $$5 <= 1; $$5++) {
            for (int $$6 = -1; $$6 <= 1; $$6++) {
               long $$7 = SectionPos.offset(p_8284_, $$4, $$5, $$6);
               if ($$7 == p_8284_) {
                  $$7 = Long.MAX_VALUE;
               }

               if ($$7 != p_8285_) {
                  int $$8 = this.computeLevelFromNeighbor($$7, p_8284_, this.getLevel($$7));
                  if ($$3 > $$8) {
                     $$3 = $$8;
                  }

                  if ($$3 == 0) {
                     return $$3;
                  }
               }
            }
         }
      }

      return $$3;
   }

   @Override
   protected int computeLevelFromNeighbor(long p_8293_, long p_8294_, int p_8295_) {
      return this.isSource(p_8293_) ? this.getLevelFromSource(p_8294_) : p_8295_ + 1;
   }

   protected abstract int getLevelFromSource(long var1);

   public void update(long p_8289_, int p_8290_, boolean p_8291_) {
      this.checkEdge(Long.MAX_VALUE, p_8289_, p_8290_, p_8291_);
   }
}
