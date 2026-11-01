package net.minecraft.server.level;

import net.minecraft.world.level.TicketStorage;

class LoadingChunkTracker extends ChunkTracker {
   private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
   private final DistanceManager distanceManager;
   private final TicketStorage ticketStorage;

   public LoadingChunkTracker(DistanceManager distanceManager, TicketStorage ticketStorage) {
      super(MAX_LEVEL + 1, 16, 256);
      this.distanceManager = distanceManager;
      this.ticketStorage = ticketStorage;
      ticketStorage.setLoadingChunkUpdatedListener(this::update);
   }

   @Override
   protected int getLevelFromSource(long pos) {
      return this.ticketStorage.getTicketLevelAt(pos, false);
   }

   @Override
   protected int getLevel(long chunkPos) {
      if (!this.distanceManager.isChunkToRemove(chunkPos)) {
         ChunkHolder chunkHolder = this.distanceManager.getChunk(chunkPos);
         if (chunkHolder != null) {
            return chunkHolder.getTicketLevel();
         }
      }

      return MAX_LEVEL;
   }

   @Override
   protected void setLevel(long chunkPos, int level) {
      ChunkHolder chunkHolder = this.distanceManager.getChunk(chunkPos);
      int i = chunkHolder == null ? MAX_LEVEL : chunkHolder.getTicketLevel();
      if (i != level) {
         chunkHolder = this.distanceManager.updateChunkScheduling(chunkPos, level, chunkHolder, i);
         if (chunkHolder != null) {
            this.distanceManager.chunksToUpdateFutures.add(chunkHolder);
         }
      }
   }

   public int runDistanceUpdates(int toUpdateCount) {
      return this.runUpdates(toUpdateCount);
   }
}
