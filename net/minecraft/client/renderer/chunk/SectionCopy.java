package net.minecraft.client.renderer.chunk;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
class SectionCopy {
   private final Map<BlockPos, BlockEntity> blockEntities;
   @Nullable
   private final PalettedContainer<BlockState> section;
   private final boolean debug;
   private final LevelHeightAccessor levelHeightAccessor;

   SectionCopy(LevelChunk chunk, int sectionIndex) {
      this.levelHeightAccessor = chunk;
      this.debug = chunk.getLevel().isDebug();
      this.blockEntities = ImmutableMap.copyOf(chunk.getBlockEntities());
      if (chunk instanceof EmptyLevelChunk) {
         this.section = null;
      } else {
         LevelChunkSection[] levelChunkSections = chunk.getSections();
         if (sectionIndex >= 0 && sectionIndex < levelChunkSections.length) {
            LevelChunkSection levelChunkSection = levelChunkSections[sectionIndex];
            this.section = levelChunkSection.hasOnlyAir() ? null : levelChunkSection.getStates().copy();
         } else {
            this.section = null;
         }
      }
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.blockEntities.get(pos);
   }

   public BlockState getBlockState(BlockPos pos) {
      int i = pos.getX();
      int j = pos.getY();
      int k = pos.getZ();
      if (this.debug) {
         BlockState blockState = null;
         if (j == 60) {
            blockState = Blocks.BARRIER.defaultBlockState();
         }

         if (j == 70) {
            blockState = DebugLevelSource.getBlockStateFor(i, k);
         }

         return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
      } else if (this.section == null) {
         return Blocks.AIR.defaultBlockState();
      } else {
         try {
            return this.section.get(i & 15, j & 15, k & 15);
         } catch (Throwable var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Getting block state");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
            crashReportCategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this.levelHeightAccessor, i, j, k));
            throw new ReportedException(crashReport);
         }
      }
   }
}
