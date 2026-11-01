package net.minecraft.world.level.chunk.storage;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.world.level.ChunkPos;

public interface ChunkIOErrorReporter {
   void reportChunkLoadFailure(Throwable var1, RegionStorageInfo var2, ChunkPos var3);

   void reportChunkSaveFailure(Throwable var1, RegionStorageInfo var2, ChunkPos var3);

   static ReportedException createMisplacedChunkReport(ChunkPos p_352158_, ChunkPos p_352311_) {
      CrashReport $$2 = CrashReport.forThrowable(
         new IllegalStateException("Retrieved chunk position " + p_352158_ + " does not match requested " + p_352311_), "Chunk found in invalid location"
      );
      CrashReportCategory $$3 = $$2.addCategory("Misplaced Chunk");
      $$3.setDetail("Stored Position", p_352158_::toString);
      return new ReportedException($$2);
   }

   default void reportMisplacedChunk(ChunkPos p_352223_, ChunkPos p_352433_, RegionStorageInfo p_352461_) {
      this.reportChunkLoadFailure(createMisplacedChunkReport(p_352223_, p_352433_), p_352461_, p_352433_);
   }
}
