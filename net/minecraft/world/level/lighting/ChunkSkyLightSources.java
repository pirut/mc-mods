package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChunkSkyLightSources {
   private static final int SIZE = 16;
   public static final int NEGATIVE_INFINITY = Integer.MIN_VALUE;
   private final int minY;
   private final BitStorage heightmap;
   private final BlockPos.MutableBlockPos mutablePos1 = new BlockPos.MutableBlockPos();
   private final BlockPos.MutableBlockPos mutablePos2 = new BlockPos.MutableBlockPos();

   public ChunkSkyLightSources(LevelHeightAccessor p_285502_) {
      this.minY = p_285502_.getMinBuildHeight() - 1;
      int $$1 = p_285502_.getMaxBuildHeight();
      int $$2 = Mth.ceillog2($$1 - this.minY + 1);
      this.heightmap = new SimpleBitStorage($$2, 256);
   }

   public void fillFrom(ChunkAccess p_285152_) {
      int $$1 = p_285152_.getHighestFilledSectionIndex();
      if ($$1 == -1) {
         this.fill(this.minY);
      } else {
         for (int $$2 = 0; $$2 < 16; $$2++) {
            for (int $$3 = 0; $$3 < 16; $$3++) {
               int $$4 = Math.max(this.findLowestSourceY(p_285152_, $$1, $$3, $$2), this.minY);
               this.set(index($$3, $$2), $$4);
            }
         }
      }
   }

   private int findLowestSourceY(ChunkAccess p_285214_, int p_285171_, int p_285021_, int p_285226_) {
      int $$4 = SectionPos.sectionToBlockCoord(p_285214_.getSectionYFromSectionIndex(p_285171_) + 1);
      BlockPos.MutableBlockPos $$5 = this.mutablePos1.set(p_285021_, $$4, p_285226_);
      BlockPos.MutableBlockPos $$6 = this.mutablePos2.setWithOffset($$5, Direction.DOWN);
      BlockState $$7 = Blocks.AIR.defaultBlockState();

      for (int $$8 = p_285171_; $$8 >= 0; $$8--) {
         LevelChunkSection $$9 = p_285214_.getSection($$8);
         if ($$9.hasOnlyAir()) {
            $$7 = Blocks.AIR.defaultBlockState();
            int $$10 = p_285214_.getSectionYFromSectionIndex($$8);
            $$5.setY(SectionPos.sectionToBlockCoord($$10));
            $$6.setY($$5.getY() - 1);
         } else {
            for (int $$11 = 15; $$11 >= 0; $$11--) {
               BlockState $$12 = $$9.getBlockState(p_285021_, $$11, p_285226_);
               if (isEdgeOccluded(p_285214_, $$5, $$7, $$6, $$12)) {
                  return $$5.getY();
               }

               $$7 = $$12;
               $$5.set($$6);
               $$6.move(Direction.DOWN);
            }
         }
      }

      return this.minY;
   }

   public boolean update(BlockGetter p_285514_, int p_284999_, int p_285358_, int p_284944_) {
      int $$4 = p_285358_ + 1;
      int $$5 = index(p_284999_, p_284944_);
      int $$6 = this.get($$5);
      if ($$4 < $$6) {
         return false;
      } else {
         BlockPos $$7 = this.mutablePos1.set(p_284999_, p_285358_ + 1, p_284944_);
         BlockState $$8 = p_285514_.getBlockState($$7);
         BlockPos $$9 = this.mutablePos2.set(p_284999_, p_285358_, p_284944_);
         BlockState $$10 = p_285514_.getBlockState($$9);
         if (this.updateEdge(p_285514_, $$5, $$6, $$7, $$8, $$9, $$10)) {
            return true;
         } else {
            BlockPos $$11 = this.mutablePos1.set(p_284999_, p_285358_ - 1, p_284944_);
            BlockState $$12 = p_285514_.getBlockState($$11);
            return this.updateEdge(p_285514_, $$5, $$6, $$9, $$10, $$11, $$12);
         }
      }
   }

   private boolean updateEdge(
      BlockGetter p_285066_, int p_285184_, int p_285101_, BlockPos p_285446_, BlockState p_285185_, BlockPos p_285103_, BlockState p_285009_
   ) {
      int $$7 = p_285446_.getY();
      if (isEdgeOccluded(p_285066_, p_285446_, p_285185_, p_285103_, p_285009_)) {
         if ($$7 > p_285101_) {
            this.set(p_285184_, $$7);
            return true;
         }
      } else if ($$7 == p_285101_) {
         this.set(p_285184_, this.findLowestSourceBelow(p_285066_, p_285103_, p_285009_));
         return true;
      }

      return false;
   }

   private int findLowestSourceBelow(BlockGetter p_285279_, BlockPos p_285119_, BlockState p_285096_) {
      BlockPos.MutableBlockPos $$3 = this.mutablePos1.set(p_285119_);
      BlockPos.MutableBlockPos $$4 = this.mutablePos2.setWithOffset(p_285119_, Direction.DOWN);
      BlockState $$5 = p_285096_;

      while ($$4.getY() >= this.minY) {
         BlockState $$6 = p_285279_.getBlockState($$4);
         if (isEdgeOccluded(p_285279_, $$3, $$5, $$4, $$6)) {
            return $$3.getY();
         }

         $$5 = $$6;
         $$3.set($$4);
         $$4.move(Direction.DOWN);
      }

      return this.minY;
   }

   private static boolean isEdgeOccluded(BlockGetter p_285329_, BlockPos p_285258_, BlockState p_285219_, BlockPos p_285288_, BlockState p_285512_) {
      if (p_285512_.getLightBlock(p_285329_, p_285288_) != 0) {
         return true;
      } else {
         VoxelShape $$5 = LightEngine.getOcclusionShape(p_285329_, p_285258_, p_285219_, Direction.DOWN);
         VoxelShape $$6 = LightEngine.getOcclusionShape(p_285329_, p_285288_, p_285512_, Direction.UP);
         return Shapes.faceShapeOccludes($$5, $$6);
      }
   }

   public int getLowestSourceY(int p_285247_, int p_285082_) {
      int $$2 = this.get(index(p_285247_, p_285082_));
      return this.extendSourcesBelowWorld($$2);
   }

   public int getHighestLowestSourceY() {
      int $$0 = Integer.MIN_VALUE;

      for (int $$1 = 0; $$1 < this.heightmap.getSize(); $$1++) {
         int $$2 = this.heightmap.get($$1);
         if ($$2 > $$0) {
            $$0 = $$2;
         }
      }

      return this.extendSourcesBelowWorld($$0 + this.minY);
   }

   private void fill(int p_285311_) {
      int $$1 = p_285311_ - this.minY;

      for (int $$2 = 0; $$2 < this.heightmap.getSize(); $$2++) {
         this.heightmap.set($$2, $$1);
      }
   }

   private void set(int p_285323_, int p_285220_) {
      this.heightmap.set(p_285323_, p_285220_ - this.minY);
   }

   private int get(int p_284951_) {
      return this.heightmap.get(p_284951_) + this.minY;
   }

   private int extendSourcesBelowWorld(int p_284953_) {
      return p_284953_ == this.minY ? Integer.MIN_VALUE : p_284953_;
   }

   private static int index(int p_284980_, int p_285277_) {
      return p_284980_ + p_285277_ * 16;
   }
}
