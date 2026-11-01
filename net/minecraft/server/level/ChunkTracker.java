package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class ChunkTracker extends DynamicGraphMinFixedPoint {
   protected ChunkTracker(int p_140701_, int p_140702_, int p_140703_) {
      super(p_140701_, p_140702_, p_140703_);
   }

   @Override
   protected boolean isSource(long p_140705_) {
      return p_140705_ == ChunkPos.INVALID_CHUNK_POS;
   }

   @Override
   protected void checkNeighborsAfterUpdate(long p_140707_, int p_140708_, boolean p_140709_) {
      if (!p_140709_ || p_140708_ < this.levelCount - 2) {
         ChunkPos $$3 = new ChunkPos(p_140707_);
         int $$4 = $$3.x;
         int $$5 = $$3.z;

         for (int $$6 = -1; $$6 <= 1; $$6++) {
            for (int $$7 = -1; $$7 <= 1; $$7++) {
               long $$8 = ChunkPos.asLong($$4 + $$6, $$5 + $$7);
               if ($$8 != p_140707_) {
                  this.checkNeighbor(p_140707_, $$8, p_140708_, p_140709_);
               }
            }
         }
      }
   }

   @Override
   protected int getComputedLevel(long p_140711_, long p_140712_, int p_140713_) {
      int $$3 = p_140713_;
      ChunkPos $$4 = new ChunkPos(p_140711_);
      int $$5 = $$4.x;
      int $$6 = $$4.z;

      for (int $$7 = -1; $$7 <= 1; $$7++) {
         for (int $$8 = -1; $$8 <= 1; $$8++) {
            long $$9 = ChunkPos.asLong($$5 + $$7, $$6 + $$8);
            if ($$9 == p_140711_) {
               $$9 = ChunkPos.INVALID_CHUNK_POS;
            }

            if ($$9 != p_140712_) {
               int $$10 = this.computeLevelFromNeighbor($$9, p_140711_, this.getLevel($$9));
               if ($$3 > $$10) {
                  $$3 = $$10;
               }

               if ($$3 == 0) {
                  return $$3;
               }
            }
         }
      }

      return $$3;
   }

   @Override
   protected int computeLevelFromNeighbor(long p_140720_, long p_140721_, int p_140722_) {
      return p_140720_ == ChunkPos.INVALID_CHUNK_POS ? this.getLevelFromSource(p_140721_) : p_140722_ + 1;
   }

   protected abstract int getLevelFromSource(long var1);

   public void update(long p_140716_, int p_140717_, boolean p_140718_) {
      this.checkEdge(ChunkPos.INVALID_CHUNK_POS, p_140716_, p_140717_, p_140718_);
   }
}
