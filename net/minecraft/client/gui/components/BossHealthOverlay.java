package net.minecraft.client.gui.components;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.BossEvent;

@Environment(EnvType.CLIENT)
public class BossHealthOverlay {
   private static final int BAR_WIDTH = 182;
   private static final int BAR_HEIGHT = 5;
   private static final Identifier[] BAR_BACKGROUND_SPRITES = new Identifier[]{
      Identifier.withDefaultNamespace("boss_bar/pink_background"),
      Identifier.withDefaultNamespace("boss_bar/blue_background"),
      Identifier.withDefaultNamespace("boss_bar/red_background"),
      Identifier.withDefaultNamespace("boss_bar/green_background"),
      Identifier.withDefaultNamespace("boss_bar/yellow_background"),
      Identifier.withDefaultNamespace("boss_bar/purple_background"),
      Identifier.withDefaultNamespace("boss_bar/white_background")
   };
   private static final Identifier[] BAR_PROGRESS_SPRITES = new Identifier[]{
      Identifier.withDefaultNamespace("boss_bar/pink_progress"),
      Identifier.withDefaultNamespace("boss_bar/blue_progress"),
      Identifier.withDefaultNamespace("boss_bar/red_progress"),
      Identifier.withDefaultNamespace("boss_bar/green_progress"),
      Identifier.withDefaultNamespace("boss_bar/yellow_progress"),
      Identifier.withDefaultNamespace("boss_bar/purple_progress"),
      Identifier.withDefaultNamespace("boss_bar/white_progress")
   };
   private static final Identifier[] OVERLAY_BACKGROUND_SPRITES = new Identifier[]{
      Identifier.withDefaultNamespace("boss_bar/notched_6_background"),
      Identifier.withDefaultNamespace("boss_bar/notched_10_background"),
      Identifier.withDefaultNamespace("boss_bar/notched_12_background"),
      Identifier.withDefaultNamespace("boss_bar/notched_20_background")
   };
   private static final Identifier[] OVERLAY_PROGRESS_SPRITES = new Identifier[]{
      Identifier.withDefaultNamespace("boss_bar/notched_6_progress"),
      Identifier.withDefaultNamespace("boss_bar/notched_10_progress"),
      Identifier.withDefaultNamespace("boss_bar/notched_12_progress"),
      Identifier.withDefaultNamespace("boss_bar/notched_20_progress")
   };
   private final Minecraft minecraft;
   final Map<UUID, LerpingBossEvent> events = Maps.newLinkedHashMap();

   public BossHealthOverlay(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(GuiGraphics guiGraphics) {
      if (!this.events.isEmpty()) {
         guiGraphics.nextStratum();
         ProfilerFiller profilerFiller = Profiler.get();
         profilerFiller.push("bossHealth");
         int i = guiGraphics.guiWidth();
         int j = 12;

         for (LerpingBossEvent lerpingBossEvent : this.events.values()) {
            int k = i / 2 - 91;
            this.drawBar(guiGraphics, k, j, lerpingBossEvent);
            Component component = lerpingBossEvent.getName();
            int m = this.minecraft.font.width(component);
            int n = i / 2 - m / 2;
            int o = j - 9;
            guiGraphics.drawString(this.minecraft.font, component, n, o, -1);
            j += 10 + 9;
            if (j >= guiGraphics.guiHeight() / 3) {
               break;
            }
         }

         profilerFiller.pop();
      }
   }

   private void drawBar(GuiGraphics guiGraphics, int x, int y, BossEvent bossEvent) {
      this.drawBar(guiGraphics, x, y, bossEvent, 182, BAR_BACKGROUND_SPRITES, OVERLAY_BACKGROUND_SPRITES);
      int i = Mth.lerpDiscrete(bossEvent.getProgress(), 0, 182);
      if (i > 0) {
         this.drawBar(guiGraphics, x, y, bossEvent, i, BAR_PROGRESS_SPRITES, OVERLAY_PROGRESS_SPRITES);
      }
   }

   private void drawBar(
      GuiGraphics guiGraphics, int x, int y, BossEvent bossEvent, int progress, Identifier[] barProgressSprites, Identifier[] overlayProgressSprites
   ) {
      guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, barProgressSprites[bossEvent.getColor().ordinal()], 182, 5, 0, 0, x, y, progress, 5);
      if (bossEvent.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, overlayProgressSprites[bossEvent.getOverlay().ordinal() - 1], 182, 5, 0, 0, x, y, progress, 5);
      }
   }

   public void update(ClientboundBossEventPacket packet) {
      packet.dispatch(
         new ClientboundBossEventPacket.Handler() {
            @Override
            public void add(
               UUID id,
               Component name,
               float progress,
               BossEvent.BossBarColor color,
               BossEvent.BossBarOverlay overlay,
               boolean darkenScreen,
               boolean playMusic,
               boolean createWorldFog
            ) {
               BossHealthOverlay.this.events.put(id, new LerpingBossEvent(id, name, progress, color, overlay, darkenScreen, playMusic, createWorldFog));
            }

            @Override
            public void remove(UUID id) {
               BossHealthOverlay.this.events.remove(id);
            }

            @Override
            public void updateProgress(UUID id, float progress) {
               BossHealthOverlay.this.events.get(id).setProgress(progress);
            }

            @Override
            public void updateName(UUID id, Component name) {
               BossHealthOverlay.this.events.get(id).setName(name);
            }

            @Override
            public void updateStyle(UUID id, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
               LerpingBossEvent lerpingBossEvent = BossHealthOverlay.this.events.get(id);
               lerpingBossEvent.setColor(color);
               lerpingBossEvent.setOverlay(overlay);
            }

            @Override
            public void updateProperties(UUID id, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
               LerpingBossEvent lerpingBossEvent = BossHealthOverlay.this.events.get(id);
               lerpingBossEvent.setDarkenScreen(darkenScreen);
               lerpingBossEvent.setPlayBossMusic(playMusic);
               lerpingBossEvent.setCreateWorldFog(createWorldFog);
            }
         }
      );
   }

   public void reset() {
      this.events.clear();
   }

   public boolean shouldPlayMusic() {
      if (!this.events.isEmpty()) {
         for (BossEvent bossEvent : this.events.values()) {
            if (bossEvent.shouldPlayBossMusic()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldDarkenScreen() {
      if (!this.events.isEmpty()) {
         for (BossEvent bossEvent : this.events.values()) {
            if (bossEvent.shouldDarkenScreen()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldCreateWorldFog() {
      if (!this.events.isEmpty()) {
         for (BossEvent bossEvent : this.events.values()) {
            if (bossEvent.shouldCreateWorldFog()) {
               return true;
            }
         }
      }

      return false;
   }
}
