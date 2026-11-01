package net.minecraft.server.level;

import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ChunkHolder extends GenerationChunkHolder {
   public static final ChunkResult<LevelChunk> UNLOADED_LEVEL_CHUNK = ChunkResult.error("Unloaded level chunk");
   private static final CompletableFuture<ChunkResult<LevelChunk>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_LEVEL_CHUNK);
   private final LevelHeightAccessor levelHeightAccessor;
   private volatile CompletableFuture<ChunkResult<LevelChunk>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private volatile CompletableFuture<ChunkResult<LevelChunk>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private volatile CompletableFuture<ChunkResult<LevelChunk>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private int oldTicketLevel;
   private int ticketLevel;
   private int queueLevel;
   private boolean hasChangedSections;
   private final ShortSet[] changedBlocksPerSection;
   private final BitSet blockChangedLightSectionFilter = new BitSet();
   private final BitSet skyChangedLightSectionFilter = new BitSet();
   private final LevelLightEngine lightEngine;
   private final ChunkHolder.LevelChangeListener onLevelChange;
   private final ChunkHolder.PlayerProvider playerProvider;
   private boolean wasAccessibleSinceLastSave;
   private CompletableFuture<?> pendingFullStateConfirmation = CompletableFuture.completedFuture(null);
   private CompletableFuture<?> sendSync = CompletableFuture.completedFuture(null);
   private CompletableFuture<?> saveSync = CompletableFuture.completedFuture(null);

   public ChunkHolder(
      ChunkPos p_142986_,
      int p_142987_,
      LevelHeightAccessor p_142988_,
      LevelLightEngine p_142989_,
      ChunkHolder.LevelChangeListener p_142990_,
      ChunkHolder.PlayerProvider p_142991_
   ) {
      super(p_142986_);
      this.levelHeightAccessor = p_142988_;
      this.lightEngine = p_142989_;
      this.onLevelChange = p_142990_;
      this.playerProvider = p_142991_;
      this.oldTicketLevel = ChunkLevel.MAX_LEVEL + 1;
      this.ticketLevel = this.oldTicketLevel;
      this.queueLevel = this.oldTicketLevel;
      this.setTicketLevel(p_142987_);
      this.changedBlocksPerSection = new ShortSet[p_142988_.getSectionsCount()];
   }

   public CompletableFuture<ChunkResult<LevelChunk>> getTickingChunkFuture() {
      return this.tickingChunkFuture;
   }

   public CompletableFuture<ChunkResult<LevelChunk>> getEntityTickingChunkFuture() {
      return this.entityTickingChunkFuture;
   }

   public CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture() {
      return this.fullChunkFuture;
   }

   @Nullable
   public LevelChunk getTickingChunk() {
      return this.getTickingChunkFuture().getNow(UNLOADED_LEVEL_CHUNK).orElse(null);
   }

   @Nullable
   public LevelChunk getChunkToSend() {
      return !this.sendSync.isDone() ? null : this.getTickingChunk();
   }

   public CompletableFuture<?> getSendSyncFuture() {
      return this.sendSync;
   }

   public void addSendDependency(CompletableFuture<?> p_301235_) {
      if (this.sendSync.isDone()) {
         this.sendSync = p_301235_;
      } else {
         this.sendSync = this.sendSync.thenCombine((CompletionStage<? extends Object>)p_301235_, (p_347036_, p_347037_) -> null);
      }
   }

   public CompletableFuture<?> getSaveSyncFuture() {
      return this.saveSync;
   }

   public boolean isReadyForSaving() {
      return this.getGenerationRefCount() == 0 && this.saveSync.isDone();
   }

   private void addSaveDependency(CompletableFuture<?> p_200418_) {
      if (this.saveSync.isDone()) {
         this.saveSync = p_200418_;
      } else {
         this.saveSync = this.saveSync.thenCombine((CompletionStage<? extends Object>)p_200418_, (p_300767_, p_300768_) -> null);
      }
   }

   public void blockChanged(BlockPos p_140057_) {
      LevelChunk $$1 = this.getTickingChunk();
      if ($$1 != null) {
         int $$2 = this.levelHeightAccessor.getSectionIndex(p_140057_.getY());
         if (this.changedBlocksPerSection[$$2] == null) {
            this.hasChangedSections = true;
            this.changedBlocksPerSection[$$2] = new ShortOpenHashSet();
         }

         this.changedBlocksPerSection[$$2].add(SectionPos.sectionRelativePos(p_140057_));
      }
   }

   public void sectionLightChanged(LightLayer p_140037_, int p_140038_) {
      ChunkAccess $$2 = this.getChunkIfPresent(ChunkStatus.INITIALIZE_LIGHT);
      if ($$2 != null) {
         $$2.setUnsaved(true);
         LevelChunk $$3 = this.getTickingChunk();
         if ($$3 != null) {
            int $$4 = this.lightEngine.getMinLightSection();
            int $$5 = this.lightEngine.getMaxLightSection();
            if (p_140038_ >= $$4 && p_140038_ <= $$5) {
               int $$6 = p_140038_ - $$4;
               if (p_140037_ == LightLayer.SKY) {
                  this.skyChangedLightSectionFilter.set($$6);
               } else {
                  this.blockChangedLightSectionFilter.set($$6);
               }
            }
         }
      }
   }

   public void broadcastChanges(LevelChunk p_140055_) {
      if (this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
         Level $$1 = p_140055_.getLevel();
         if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            List<ServerPlayer> $$2 = this.playerProvider.getPlayers(this.pos, true);
            if (!$$2.isEmpty()) {
               ClientboundLightUpdatePacket $$3 = new ClientboundLightUpdatePacket(
                  p_140055_.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter
               );
               this.broadcast($$2, $$3);
            }

            this.skyChangedLightSectionFilter.clear();
            this.blockChangedLightSectionFilter.clear();
         }

         if (this.hasChangedSections) {
            List<ServerPlayer> $$4 = this.playerProvider.getPlayers(this.pos, false);

            for (int $$5 = 0; $$5 < this.changedBlocksPerSection.length; $$5++) {
               ShortSet $$6 = this.changedBlocksPerSection[$$5];
               if ($$6 != null) {
                  this.changedBlocksPerSection[$$5] = null;
                  if (!$$4.isEmpty()) {
                     int $$7 = this.levelHeightAccessor.getSectionYFromSectionIndex($$5);
                     SectionPos $$8 = SectionPos.of(p_140055_.getPos(), $$7);
                     if ($$6.size() == 1) {
                        BlockPos $$9 = $$8.relativeToBlockPos($$6.iterator().nextShort());
                        BlockState $$10 = $$1.getBlockState($$9);
                        this.broadcast($$4, new ClientboundBlockUpdatePacket($$9, $$10));
                        this.broadcastBlockEntityIfNeeded($$4, $$1, $$9, $$10);
                     } else {
                        LevelChunkSection $$11 = p_140055_.getSection($$5);
                        ClientboundSectionBlocksUpdatePacket $$12 = new ClientboundSectionBlocksUpdatePacket($$8, $$6, $$11);
                        this.broadcast($$4, $$12);
                        $$12.runUpdates((p_288761_, p_288762_) -> this.broadcastBlockEntityIfNeeded($$4, $$1, p_288761_, p_288762_));
                     }
                  }
               }
            }

            this.hasChangedSections = false;
         }
      }
   }

   private void broadcastBlockEntityIfNeeded(List<ServerPlayer> p_288982_, Level p_289011_, BlockPos p_288969_, BlockState p_288973_) {
      if (p_288973_.hasBlockEntity()) {
         this.broadcastBlockEntity(p_288982_, p_289011_, p_288969_);
      }
   }

   private void broadcastBlockEntity(List<ServerPlayer> p_288988_, Level p_289005_, BlockPos p_288981_) {
      BlockEntity $$3 = p_289005_.getBlockEntity(p_288981_);
      if ($$3 != null) {
         Packet<?> $$4 = $$3.getUpdatePacket();
         if ($$4 != null) {
            this.broadcast(p_288988_, $$4);
         }
      }
   }

   private void broadcast(List<ServerPlayer> p_288998_, Packet<?> p_289013_) {
      p_288998_.forEach(p_293798_ -> p_293798_.connection.send(p_289013_));
   }

   @Override
   public int getTicketLevel() {
      return this.ticketLevel;
   }

   @Override
   public int getQueueLevel() {
      return this.queueLevel;
   }

   private void setQueueLevel(int p_140087_) {
      this.queueLevel = p_140087_;
   }

   public void setTicketLevel(int p_140028_) {
      this.ticketLevel = p_140028_;
   }

   private void scheduleFullChunkPromotion(
      ChunkMap p_142999_, CompletableFuture<ChunkResult<LevelChunk>> p_143000_, Executor p_143001_, FullChunkStatus p_287621_
   ) {
      this.pendingFullStateConfirmation.cancel(false);
      CompletableFuture<Void> $$4 = new CompletableFuture<>();
      $$4.thenRunAsync(() -> p_142999_.onFullChunkStatusChange(this.pos, p_287621_), p_143001_);
      this.pendingFullStateConfirmation = $$4;
      p_143000_.thenAccept(p_329909_ -> p_329909_.ifSuccess(p_200424_ -> $$4.complete(null)));
   }

   private void demoteFullChunk(ChunkMap p_287599_, FullChunkStatus p_287649_) {
      this.pendingFullStateConfirmation.cancel(false);
      p_287599_.onFullChunkStatusChange(this.pos, p_287649_);
   }

   protected void updateFutures(ChunkMap p_143004_, Executor p_143005_) {
      FullChunkStatus $$2 = ChunkLevel.fullStatus(this.oldTicketLevel);
      FullChunkStatus $$3 = ChunkLevel.fullStatus(this.ticketLevel);
      boolean $$4 = $$2.isOrAfter(FullChunkStatus.FULL);
      boolean $$5 = $$3.isOrAfter(FullChunkStatus.FULL);
      this.wasAccessibleSinceLastSave |= $$5;
      if (!$$4 && $$5) {
         this.fullChunkFuture = p_143004_.prepareAccessibleChunk(this);
         this.scheduleFullChunkPromotion(p_143004_, this.fullChunkFuture, p_143005_, FullChunkStatus.FULL);
         this.addSaveDependency(this.fullChunkFuture);
      }

      if ($$4 && !$$5) {
         this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      boolean $$6 = $$2.isOrAfter(FullChunkStatus.BLOCK_TICKING);
      boolean $$7 = $$3.isOrAfter(FullChunkStatus.BLOCK_TICKING);
      if (!$$6 && $$7) {
         this.tickingChunkFuture = p_143004_.prepareTickingChunk(this);
         this.scheduleFullChunkPromotion(p_143004_, this.tickingChunkFuture, p_143005_, FullChunkStatus.BLOCK_TICKING);
         this.addSaveDependency(this.tickingChunkFuture);
      }

      if ($$6 && !$$7) {
         this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      boolean $$8 = $$2.isOrAfter(FullChunkStatus.ENTITY_TICKING);
      boolean $$9 = $$3.isOrAfter(FullChunkStatus.ENTITY_TICKING);
      if (!$$8 && $$9) {
         if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
         }

         this.entityTickingChunkFuture = p_143004_.prepareEntityTickingChunk(this);
         this.scheduleFullChunkPromotion(p_143004_, this.entityTickingChunkFuture, p_143005_, FullChunkStatus.ENTITY_TICKING);
         this.addSaveDependency(this.entityTickingChunkFuture);
      }

      if ($$8 && !$$9) {
         this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      if (!$$3.isOrAfter($$2)) {
         this.demoteFullChunk(p_143004_, $$3);
      }

      this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
      this.oldTicketLevel = this.ticketLevel;
   }

   public boolean wasAccessibleSinceLastSave() {
      return this.wasAccessibleSinceLastSave;
   }

   public void refreshAccessibility() {
      this.wasAccessibleSinceLastSave = ChunkLevel.fullStatus(this.ticketLevel).isOrAfter(FullChunkStatus.FULL);
   }

   @FunctionalInterface
   public interface LevelChangeListener {
      void onLevelChange(ChunkPos var1, IntSupplier var2, int var3, IntConsumer var4);
   }

   public interface PlayerProvider {
      List<ServerPlayer> getPlayers(ChunkPos var1, boolean var2);
   }
}
