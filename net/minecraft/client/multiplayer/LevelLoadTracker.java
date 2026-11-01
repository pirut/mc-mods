package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LevelLoadProgressTracker;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LevelLoadTracker implements LevelLoadListener {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final long CLIENT_WAIT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30L);
   public static final long LEVEL_LOAD_CLOSE_DELAY_MS = 500L;
   private final LevelLoadProgressTracker serverProgressTracker = new LevelLoadProgressTracker(true);
   @Nullable
   private ChunkLoadStatusView serverChunkStatusView;
   @Nullable
   private volatile LevelLoadListener.Stage serverStage;
   @Nullable
   private LevelLoadTracker.ClientState clientState;
   private final long closeDelayMs;

   public LevelLoadTracker() {
      this(0L);
   }

   public LevelLoadTracker(long p_436741_) {
      this.closeDelayMs = p_436741_;
   }

   public void setServerChunkStatusView(ChunkLoadStatusView p_434823_) {
      this.serverChunkStatusView = p_434823_;
   }

   public void startClientLoad(LocalPlayer p_433955_, ClientLevel p_435777_, LevelRenderer p_434302_) {
      this.clientState = new LevelLoadTracker.WaitingForServer(p_433955_, p_435777_, p_434302_, Util.getMillis() + CLIENT_WAIT_TIMEOUT_MS);
   }

   public void tickClientLoad() {
      if (this.clientState != null) {
         this.clientState = this.clientState.tick();
      }
   }

   public boolean isLevelReady() {
      if (this.clientState instanceof LevelLoadTracker.ClientLevelReady(long var8)) {
         long var5 = var8;
         if (Util.getMillis() >= var5 + this.closeDelayMs) {
            return true;
         }
      }

      return false;
   }

   public void loadingPacketsReceived() {
      if (this.clientState != null) {
         this.clientState = this.clientState.loadingPacketsReceived();
      }
   }

   @Override
   public void start(LevelLoadListener.Stage p_435695_, int p_434558_) {
      this.serverProgressTracker.start(p_435695_, p_434558_);
      this.serverStage = p_435695_;
   }

   @Override
   public void update(LevelLoadListener.Stage p_432756_, int p_435430_, int p_435518_) {
      this.serverProgressTracker.update(p_432756_, p_435430_, p_435518_);
   }

   @Override
   public void finish(LevelLoadListener.Stage p_435115_) {
      this.serverProgressTracker.finish(p_435115_);
   }

   @Override
   public void updateFocus(ResourceKey<Level> p_435425_, ChunkPos p_435535_) {
      if (this.serverChunkStatusView != null) {
         this.serverChunkStatusView.moveTo(p_435425_, p_435535_);
      }
   }

   @Nullable
   public ChunkLoadStatusView statusView() {
      return this.serverChunkStatusView;
   }

   public float serverProgress() {
      return this.serverProgressTracker.get();
   }

   public boolean hasProgress() {
      return this.serverStage != null;
   }

   @OnlyIn(Dist.CLIENT)
   record ClientLevelReady(long readyAt) implements LevelLoadTracker.ClientState {
   }

   @OnlyIn(Dist.CLIENT)
   sealed interface ClientState permits LevelLoadTracker.WaitingForServer, LevelLoadTracker.WaitingForPlayerChunk, LevelLoadTracker.ClientLevelReady {
      default LevelLoadTracker.ClientState tick() {
         return this;
      }

      default LevelLoadTracker.ClientState loadingPacketsReceived() {
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record WaitingForPlayerChunk(LocalPlayer player, ClientLevel level, LevelRenderer levelRenderer, long timeoutAfter) implements LevelLoadTracker.ClientState {
      @Override
      public LevelLoadTracker.ClientState tick() {
         return (LevelLoadTracker.ClientState)(this.isReady() ? new LevelLoadTracker.ClientLevelReady(Util.getMillis()) : this);
      }

      private boolean isReady() {
         if (Util.getMillis() > this.timeoutAfter) {
            LevelLoadTracker.LOGGER.warn("Timed out while waiting for the client to load chunks, letting the player into the world anyway");
            return true;
         } else {
            BlockPos blockpos = this.player.blockPosition();
            return !this.level.isOutsideBuildHeight(blockpos.getY()) && !this.player.isSpectator() && this.player.isAlive()
               ? this.levelRenderer.isSectionCompiledAndVisible(blockpos)
               : true;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record WaitingForServer(LocalPlayer player, ClientLevel level, LevelRenderer levelRenderer, long timeoutAfter) implements LevelLoadTracker.ClientState {
      @Override
      public LevelLoadTracker.ClientState loadingPacketsReceived() {
         return new LevelLoadTracker.WaitingForPlayerChunk(this.player, this.level, this.levelRenderer, this.timeoutAfter);
      }
   }
}
