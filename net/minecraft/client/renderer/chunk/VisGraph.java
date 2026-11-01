package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VisGraph {
   private static final int SIZE_IN_BITS = 4;
   private static final int LEN = 16;
   private static final int MASK = 15;
   private static final int SIZE = 4096;
   private static final int X_SHIFT = 0;
   private static final int Z_SHIFT = 4;
   private static final int Y_SHIFT = 8;
   private static final int DX = (int)Math.pow(16.0, 0.0);
   private static final int DZ = (int)Math.pow(16.0, 1.0);
   private static final int DY = (int)Math.pow(16.0, 2.0);
   private static final int INVALID_INDEX = -1;
   private static final Direction[] DIRECTIONS = Direction.values();
   private final BitSet bitSet = new BitSet(4096);
   private static final int[] INDEX_OF_EDGES = Util.make(new int[1352], p_112974_ -> {
      int $$1 = 0;
      int $$2 = 15;
      int $$3 = 0;

      for (int $$4 = 0; $$4 < 16; $$4++) {
         for (int $$5 = 0; $$5 < 16; $$5++) {
            for (int $$6 = 0; $$6 < 16; $$6++) {
               if ($$4 == 0 || $$4 == 15 || $$5 == 0 || $$5 == 15 || $$6 == 0 || $$6 == 15) {
                  p_112974_[$$3++] = getIndex($$4, $$5, $$6);
               }
            }
         }
      }
   });
   private int empty = 4096;

   public void setOpaque(BlockPos p_112972_) {
      this.bitSet.set(getIndex(p_112972_), true);
      this.empty--;
   }

   private static int getIndex(BlockPos p_112976_) {
      return getIndex(p_112976_.getX() & 15, p_112976_.getY() & 15, p_112976_.getZ() & 15);
   }

   private static int getIndex(int p_112962_, int p_112963_, int p_112964_) {
      return p_112962_ << 0 | p_112963_ << 8 | p_112964_ << 4;
   }

   public VisibilitySet resolve() {
      VisibilitySet $$0 = new VisibilitySet();
      if (4096 - this.empty < 256) {
         $$0.setAll(true);
      } else if (this.empty == 0) {
         $$0.setAll(false);
      } else {
         for (int $$1 : INDEX_OF_EDGES) {
            if (!this.bitSet.get($$1)) {
               $$0.add(this.floodFill($$1));
            }
         }
      }

      return $$0;
   }

   private Set<Direction> floodFill(int p_112960_) {
      Set<Direction> $$1 = EnumSet.noneOf(Direction.class);
      IntPriorityQueue $$2 = new IntArrayFIFOQueue();
      $$2.enqueue(p_112960_);
      this.bitSet.set(p_112960_, true);

      while (!$$2.isEmpty()) {
         int $$3 = $$2.dequeueInt();
         this.addEdges($$3, $$1);

         for (Direction $$4 : DIRECTIONS) {
            int $$5 = this.getNeighborIndexAtFace($$3, $$4);
            if ($$5 >= 0 && !this.bitSet.get($$5)) {
               this.bitSet.set($$5, true);
               $$2.enqueue($$5);
            }
         }
      }

      return $$1;
   }

   private void addEdges(int p_112969_, Set<Direction> p_112970_) {
      int $$2 = p_112969_ >> 0 & 15;
      if ($$2 == 0) {
         p_112970_.add(Direction.WEST);
      } else if ($$2 == 15) {
         p_112970_.add(Direction.EAST);
      }

      int $$3 = p_112969_ >> 8 & 15;
      if ($$3 == 0) {
         p_112970_.add(Direction.DOWN);
      } else if ($$3 == 15) {
         p_112970_.add(Direction.UP);
      }

      int $$4 = p_112969_ >> 4 & 15;
      if ($$4 == 0) {
         p_112970_.add(Direction.NORTH);
      } else if ($$4 == 15) {
         p_112970_.add(Direction.SOUTH);
      }
   }

   private int getNeighborIndexAtFace(int p_112966_, Direction p_112967_) {
      switch (p_112967_) {
         case DOWN:
            if ((p_112966_ >> 8 & 15) == 0) {
               return -1;
            }

            return p_112966_ - DY;
         case UP:
            if ((p_112966_ >> 8 & 15) == 15) {
               return -1;
            }

            return p_112966_ + DY;
         case NORTH:
            if ((p_112966_ >> 4 & 15) == 0) {
               return -1;
            }

            return p_112966_ - DZ;
         case SOUTH:
            if ((p_112966_ >> 4 & 15) == 15) {
               return -1;
            }

            return p_112966_ + DZ;
         case WEST:
            if ((p_112966_ >> 0 & 15) == 0) {
               return -1;
            }

            return p_112966_ - DX;
         case EAST:
            if ((p_112966_ >> 0 & 15) == 15) {
               return -1;
            }

            return p_112966_ + DX;
         default:
            return -1;
      }
   }
}
