package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public abstract class ScatteredFeaturePiece extends StructurePiece {
   protected final int width;
   protected final int height;
   protected final int depth;
   protected int heightPosition = -1;

   protected ScatteredFeaturePiece(
      StructurePieceType p_209920_, int p_209921_, int p_209922_, int p_209923_, int p_209924_, int p_209925_, int p_209926_, Direction p_209927_
   ) {
      super(p_209920_, 0, StructurePiece.makeBoundingBox(p_209921_, p_209922_, p_209923_, p_209927_, p_209924_, p_209925_, p_209926_));
      this.width = p_209924_;
      this.height = p_209925_;
      this.depth = p_209926_;
      this.setOrientation(p_209927_);
   }

   protected ScatteredFeaturePiece(StructurePieceType p_209929_, CompoundTag p_209930_) {
      super(p_209929_, p_209930_);
      this.width = p_209930_.getInt("Width");
      this.height = p_209930_.getInt("Height");
      this.depth = p_209930_.getInt("Depth");
      this.heightPosition = p_209930_.getInt("HPos");
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext p_192471_, CompoundTag p_192472_) {
      p_192472_.putInt("Width", this.width);
      p_192472_.putInt("Height", this.height);
      p_192472_.putInt("Depth", this.depth);
      p_192472_.putInt("HPos", this.heightPosition);
   }

   protected boolean updateAverageGroundHeight(LevelAccessor p_72804_, BoundingBox p_72805_, int p_72806_) {
      if (this.heightPosition >= 0) {
         return true;
      } else {
         int $$3 = 0;
         int $$4 = 0;
         BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

         for (int $$6 = this.boundingBox.minZ(); $$6 <= this.boundingBox.maxZ(); $$6++) {
            for (int $$7 = this.boundingBox.minX(); $$7 <= this.boundingBox.maxX(); $$7++) {
               $$5.set($$7, 64, $$6);
               if (p_72805_.isInside($$5)) {
                  $$3 += p_72804_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, $$5).getY();
                  $$4++;
               }
            }
         }

         if ($$4 == 0) {
            return false;
         } else {
            this.heightPosition = $$3 / $$4;
            this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + p_72806_, 0);
            return true;
         }
      }
   }

   protected boolean updateHeightPositionToLowestGroundHeight(LevelAccessor p_192468_, int p_192469_) {
      if (this.heightPosition >= 0) {
         return true;
      } else {
         int $$2 = p_192468_.getMaxBuildHeight();
         boolean $$3 = false;
         BlockPos.MutableBlockPos $$4 = new BlockPos.MutableBlockPos();

         for (int $$5 = this.boundingBox.minZ(); $$5 <= this.boundingBox.maxZ(); $$5++) {
            for (int $$6 = this.boundingBox.minX(); $$6 <= this.boundingBox.maxX(); $$6++) {
               $$4.set($$6, 0, $$5);
               $$2 = Math.min($$2, p_192468_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, $$4).getY());
               $$3 = true;
            }
         }

         if (!$$3) {
            return false;
         } else {
            this.heightPosition = $$2;
            this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + p_192469_, 0);
            return true;
         }
      }
   }
}
