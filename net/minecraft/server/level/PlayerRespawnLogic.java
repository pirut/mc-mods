package net.minecraft.server.level;

import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class PlayerRespawnLogic {
   @Nullable
   protected static BlockPos getOverworldRespawnPos(ServerLevel p_183929_, int p_183930_, int p_183931_) {
      boolean $$3 = p_183929_.dimensionType().hasCeiling();
      LevelChunk $$4 = p_183929_.getChunk(SectionPos.blockToSectionCoord(p_183930_), SectionPos.blockToSectionCoord(p_183931_));
      int $$5 = $$3
         ? p_183929_.getChunkSource().getGenerator().getSpawnHeight(p_183929_)
         : $$4.getHeight(Heightmap.Types.MOTION_BLOCKING, p_183930_ & 15, p_183931_ & 15);
      if ($$5 < p_183929_.getMinBuildHeight()) {
         return null;
      } else {
         int $$6 = $$4.getHeight(Heightmap.Types.WORLD_SURFACE, p_183930_ & 15, p_183931_ & 15);
         if ($$6 <= $$5 && $$6 > $$4.getHeight(Heightmap.Types.OCEAN_FLOOR, p_183930_ & 15, p_183931_ & 15)) {
            return null;
         } else {
            BlockPos.MutableBlockPos $$7 = new BlockPos.MutableBlockPos();

            for (int $$8 = $$5 + 1; $$8 >= p_183929_.getMinBuildHeight(); $$8--) {
               $$7.set(p_183930_, $$8, p_183931_);
               BlockState $$9 = p_183929_.getBlockState($$7);
               if (!$$9.getFluidState().isEmpty()) {
                  break;
               }

               if (Block.isFaceFull($$9.getCollisionShape(p_183929_, $$7), Direction.UP)) {
                  return $$7.above().immutable();
               }
            }

            return null;
         }
      }
   }

   @Nullable
   public static BlockPos getSpawnPosInChunk(ServerLevel p_183933_, ChunkPos p_183934_) {
      if (SharedConstants.debugVoidTerrain(p_183934_)) {
         return null;
      } else {
         for (int $$2 = p_183934_.getMinBlockX(); $$2 <= p_183934_.getMaxBlockX(); $$2++) {
            for (int $$3 = p_183934_.getMinBlockZ(); $$3 <= p_183934_.getMaxBlockZ(); $$3++) {
               BlockPos $$4 = getOverworldRespawnPos(p_183933_, $$2, $$3);
               if ($$4 != null) {
                  return $$4;
               }
            }
         }

         return null;
      }
   }
}
