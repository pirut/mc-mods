package net.minecraft.client.renderer.chunk;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
class RenderChunk {
   private final Map<BlockPos, BlockEntity> blockEntities;
   @Nullable
   private final List<PalettedContainer<BlockState>> sections;
   private final boolean debug;
   final LevelChunk wrapped;

   RenderChunk(LevelChunk p_200446_) {
      this.wrapped = p_200446_;
      this.debug = p_200446_.getLevel().isDebug();
      this.blockEntities = ImmutableMap.copyOf(p_200446_.getBlockEntities());
      if (p_200446_ instanceof EmptyLevelChunk) {
         this.sections = null;
      } else {
         LevelChunkSection[] $$1 = p_200446_.getSections();
         this.sections = new ArrayList<>($$1.length);

         for (LevelChunkSection $$2 : $$1) {
            this.sections.add($$2.hasOnlyAir() ? null : $$2.getStates().copy());
         }
      }
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos p_200452_) {
      return this.blockEntities.get(p_200452_);
   }

   public BlockState getBlockState(BlockPos p_200454_) {
      int $$1 = p_200454_.getX();
      int $$2 = p_200454_.getY();
      int $$3 = p_200454_.getZ();
      if (this.debug) {
         BlockState $$4 = null;
         if ($$2 == 60) {
            $$4 = Blocks.BARRIER.defaultBlockState();
         }

         if ($$2 == 70) {
            $$4 = DebugLevelSource.getBlockStateFor($$1, $$3);
         }

         return $$4 == null ? Blocks.AIR.defaultBlockState() : $$4;
      } else if (this.sections == null) {
         return Blocks.AIR.defaultBlockState();
      } else {
         try {
            int $$5 = this.wrapped.getSectionIndex($$2);
            if ($$5 >= 0 && $$5 < this.sections.size()) {
               PalettedContainer<BlockState> $$6 = this.sections.get($$5);
               if ($$6 != null) {
                  return $$6.get($$1 & 15, $$2 & 15, $$3 & 15);
               }
            }

            return Blocks.AIR.defaultBlockState();
         } catch (Throwable var8) {
            CrashReport $$8 = CrashReport.forThrowable(var8, "Getting block state");
            CrashReportCategory $$9 = $$8.addCategory("Block being got");
            $$9.setDetail("Location", () -> CrashReportCategory.formatLocation(this.wrapped, $$1, $$2, $$3));
            throw new ReportedException($$8);
         }
      }
   }
}
