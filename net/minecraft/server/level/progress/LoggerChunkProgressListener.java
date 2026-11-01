package net.minecraft.server.level.progress;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;

public class LoggerChunkProgressListener implements ChunkProgressListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final int maxCount;
   private int count;
   private long startTime;
   private long nextTickTime = Long.MAX_VALUE;

   private LoggerChunkProgressListener(int p_9629_) {
      this.maxCount = p_9629_;
   }

   public static LoggerChunkProgressListener createFromGameruleRadius(int p_319913_) {
      return p_319913_ > 0 ? create(p_319913_ + 1) : createCompleted();
   }

   public static LoggerChunkProgressListener create(int p_320293_) {
      int $$1 = ChunkProgressListener.calculateDiameter(p_320293_);
      return new LoggerChunkProgressListener($$1 * $$1);
   }

   public static LoggerChunkProgressListener createCompleted() {
      return new LoggerChunkProgressListener(0);
   }

   @Override
   public void updateSpawnPos(ChunkPos p_9631_) {
      this.nextTickTime = Util.getMillis();
      this.startTime = this.nextTickTime;
   }

   @Override
   public void onStatusChange(ChunkPos p_9633_, @Nullable ChunkStatus p_332174_) {
      if (p_332174_ == ChunkStatus.FULL) {
         this.count++;
      }

      int $$2 = this.getProgress();
      if (Util.getMillis() > this.nextTickTime) {
         this.nextTickTime += 500L;
         LOGGER.info(Component.translatable("menu.preparingSpawn", Mth.clamp($$2, 0, 100)).getString());
      }
   }

   @Override
   public void start() {
   }

   @Override
   public void stop() {
      LOGGER.info("Time elapsed: {} ms", Util.getMillis() - this.startTime);
      this.nextTickTime = Long.MAX_VALUE;
   }

   public int getProgress() {
      return this.maxCount == 0 ? 100 : Mth.floor(this.count * 100.0F / this.maxCount);
   }
}
