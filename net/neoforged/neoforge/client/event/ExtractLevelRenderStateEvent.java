package net.neoforged.neoforge.client.event;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class ExtractLevelRenderStateEvent extends Event {
   private final LevelRenderer levelRenderer;
   private final LevelRenderState renderState;
   private final ClientLevel level;
   private final Camera camera;
   private final Frustum frustum;
   private final DeltaTracker deltaTracker;
   private final int renderTick;

   @Internal
   public ExtractLevelRenderStateEvent(
      LevelRenderer levelRenderer, LevelRenderState renderState, ClientLevel level, Camera camera, Frustum frustum, DeltaTracker deltaTracker, int renderTick
   ) {
      this.levelRenderer = levelRenderer;
      this.renderState = renderState;
      this.level = level;
      this.camera = camera;
      this.frustum = frustum;
      this.deltaTracker = deltaTracker;
      this.renderTick = renderTick;
   }

   public LevelRenderer getLevelRenderer() {
      return this.levelRenderer;
   }

   public LevelRenderState getRenderState() {
      return this.renderState;
   }

   public ClientLevel getLevel() {
      return this.level;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public Frustum getFrustum() {
      return this.frustum;
   }

   public DeltaTracker getDeltaTracker() {
      return this.deltaTracker;
   }

   public int getRenderTick() {
      return this.renderTick;
   }
}
