package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LevelLoadingScreen extends Screen {
   private static final Component DOWNLOADING_TERRAIN_TEXT = Component.translatable("multiplayer.downloadingTerrain");
   private static final Component READY_TO_PLAY_TEXT = Component.translatable("narrator.ready_to_play");
   private static final long NARRATION_DELAY_MS = 2000L;
   private static final int PROGRESS_BAR_WIDTH = 200;
   private LevelLoadTracker loadTracker;
   private float smoothedProgress;
   private long lastNarration = -1L;
   private LevelLoadingScreen.Reason reason;
   @Nullable
   private TextureAtlasSprite cachedNetherPortalSprite;
   private static final Object2IntMap<ChunkStatus> COLORS = Util.make(new Object2IntOpenHashMap(), object2IntOpenHashMap -> {
      object2IntOpenHashMap.defaultReturnValue(0);
      object2IntOpenHashMap.put(ChunkStatus.EMPTY, 5526612);
      object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_STARTS, 10066329);
      object2IntOpenHashMap.put(ChunkStatus.STRUCTURE_REFERENCES, 6250897);
      object2IntOpenHashMap.put(ChunkStatus.BIOMES, 8434258);
      object2IntOpenHashMap.put(ChunkStatus.NOISE, 13750737);
      object2IntOpenHashMap.put(ChunkStatus.SURFACE, 7497737);
      object2IntOpenHashMap.put(ChunkStatus.CARVERS, 3159410);
      object2IntOpenHashMap.put(ChunkStatus.FEATURES, 2213376);
      object2IntOpenHashMap.put(ChunkStatus.INITIALIZE_LIGHT, 13421772);
      object2IntOpenHashMap.put(ChunkStatus.LIGHT, 16769184);
      object2IntOpenHashMap.put(ChunkStatus.SPAWN, 15884384);
      object2IntOpenHashMap.put(ChunkStatus.FULL, 16777215);
   });

   public LevelLoadingScreen(LevelLoadTracker loadTracker, LevelLoadingScreen.Reason reason) {
      super(GameNarrator.NO_TITLE);
      this.loadTracker = loadTracker;
      this.reason = reason;
   }

   public void update(LevelLoadTracker laodTracker, LevelLoadingScreen.Reason reason) {
      this.loadTracker = laodTracker;
      this.reason = reason;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected boolean shouldNarrateNavigation() {
      return false;
   }

   @Override
   protected void updateNarratedWidget(NarrationElementOutput narrationElementOutput) {
      if (this.loadTracker.hasProgress()) {
         narrationElementOutput.add(
            NarratedElementType.TITLE, Component.translatable("loading.progress", Mth.floor(this.loadTracker.serverProgress() * 100.0F))
         );
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.smoothedProgress = this.smoothedProgress + (this.loadTracker.serverProgress() - this.smoothedProgress) * 0.2F;
      if (this.loadTracker.isLevelReady()) {
         this.onClose();
      }
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      long l = Util.getMillis();
      if (l - this.lastNarration > 2000L) {
         this.lastNarration = l;
         this.triggerImmediateNarration(true);
      }

      int i = this.width / 2;
      int j = this.height / 2;
      ChunkLoadStatusView chunkLoadStatusView = this.loadTracker.statusView();
      int m;
      if (chunkLoadStatusView != null) {
         int k = 2;
         renderChunks(guiGraphics, i, j, 2, 0, chunkLoadStatusView);
         m = j - chunkLoadStatusView.radius() * 2 - 9 * 3;
      } else {
         m = j - 50;
      }

      guiGraphics.drawCenteredString(this.font, DOWNLOADING_TERRAIN_TEXT, i, m, -1);
      if (this.loadTracker.hasProgress()) {
         this.drawProgressBar(guiGraphics, i - 100, m + 9 + 3, 200, 2, this.smoothedProgress);
      }
   }

   private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress) {
      guiGraphics.fill(x, y, x + width, y + height, -16777216);
      guiGraphics.fill(x, y, x + Math.round(progress * width), y + height, -16711936);
   }

   public static void renderChunks(GuiGraphics guiGraphics, int x, int y, int size, int spacing, ChunkLoadStatusView statusView) {
      int i = size + spacing;
      int j = statusView.radius() * 2 + 1;
      int k = j * i - spacing;
      int l = x - k / 2;
      int m = y - k / 2;
      if (Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
         int n = i / 2 + 1;
         guiGraphics.fill(x - n, y - n, x + n, y + n, -65536);
      }

      for (int n = 0; n < j; n++) {
         for (int o = 0; o < j; o++) {
            ChunkStatus chunkStatus = statusView.get(n, o);
            int p = l + n * i;
            int q = m + o * i;
            guiGraphics.fill(p, q, p + size, q + size, ARGB.opaque(COLORS.getInt(chunkStatus)));
         }
      }
   }

   @Override
   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      switch (this.reason) {
         case NETHER_PORTAL:
            guiGraphics.blitSprite(
               RenderPipelines.GUI_OPAQUE_TEXTURED_BACKGROUND, this.getNetherPortalSprite(), 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()
            );
            break;
         case END_PORTAL:
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstractTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION);
            AbstractTexture abstractTexture2 = textureManager.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION);
            TextureSetup textureSetup = TextureSetup.doubleTexture(
               abstractTexture.getTextureView(), abstractTexture.getSampler(), abstractTexture2.getTextureView(), abstractTexture2.getSampler()
            );
            guiGraphics.fill(RenderPipelines.END_PORTAL, textureSetup, 0, 0, this.width, this.height);
            break;
         case OTHER:
            this.renderPanorama(guiGraphics, partialTick);
            this.renderBlurredBackground(guiGraphics);
            this.renderMenuBackground(guiGraphics);
      }
   }

   private TextureAtlasSprite getNetherPortalSprite() {
      if (this.cachedNetherPortalSprite != null) {
         return this.cachedNetherPortalSprite;
      } else {
         this.cachedNetherPortalSprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
         return this.cachedNetherPortalSprite;
      }
   }

   @Override
   public void onClose() {
      this.minecraft.getNarrator().saySystemNow(READY_TO_PLAY_TEXT);
      super.onClose();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   public static enum Reason {
      NETHER_PORTAL,
      END_PORTAL,
      OTHER;
   }
}
