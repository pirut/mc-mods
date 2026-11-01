package net.minecraft.client.renderer.chunk;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
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
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
class SectionCopy {
   private final Map<BlockPos, BlockEntity> blockEntities;
   @Nullable
   private final PalettedContainer<BlockState> section;
   private final boolean debug;
   private final LevelHeightAccessor levelHeightAccessor;
   final LevelChunk wrapped;

   SectionCopy(LevelChunk p_427469_, int p_427413_) {
      this.wrapped = p_427469_;
      this.levelHeightAccessor = p_427469_;
      this.debug = p_427469_.getLevel().isDebug();
      this.blockEntities = ImmutableMap.copyOf(p_427469_.getBlockEntities());
      if (p_427469_ instanceof EmptyLevelChunk) {
         this.section = null;
      } else {
         LevelChunkSection[] alevelchunksection = p_427469_.getSections();
         if (p_427413_ >= 0 && p_427413_ < alevelchunksection.length) {
            LevelChunkSection levelchunksection = alevelchunksection[p_427413_];
            this.section = levelchunksection.hasOnlyAir() ? null : levelchunksection.getStates().copy();
         } else {
            this.section = null;
         }
      }
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos p_427251_) {
      return this.blockEntities.get(p_427251_);
   }

   public BlockState getBlockState(BlockPos p_427342_) {
      int i = p_427342_.getX();
      int j = p_427342_.getY();
      int k = p_427342_.getZ();
      if (this.debug) {
         BlockState blockstate = null;
         if (j == 60) {
            blockstate = Blocks.BARRIER.defaultBlockState();
         }

         if (j == 70) {
            blockstate = DebugLevelSource.getBlockStateFor(i, k);
         }

         return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
      } else if (this.section == null) {
         return Blocks.AIR.defaultBlockState();
      } else {
         try {
            return this.section.get(i & 15, j & 15, k & 15);
         } catch (Throwable var8) {
            CrashReport crashreport = CrashReport.forThrowable(var8, "Getting block state");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this.levelHeightAccessor, i, j, k));
            throw new ReportedException(crashreport);
         }
      }
   }
}
