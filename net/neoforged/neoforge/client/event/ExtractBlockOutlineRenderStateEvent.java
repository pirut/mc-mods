package net.neoforged.neoforge.client.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class ExtractBlockOutlineRenderStateEvent extends Event implements ICancellableEvent {
   private final LevelRenderer levelRenderer;
   private final ClientLevel level;
   private final BlockPos pos;
   private final BlockState state;
   private final BlockHitResult hitResult;
   private final CollisionContext collisionContext;
   private final boolean inTranslucentPass;
   private final boolean highContrast;
   private final Camera camera;
   private final LevelRenderState levelRenderState;
   private final List<CustomBlockOutlineRenderer> customRenderers = new ArrayList<>();

   @Internal
   public ExtractBlockOutlineRenderStateEvent(
      LevelRenderer levelRenderer,
      ClientLevel level,
      BlockPos pos,
      BlockState state,
      BlockHitResult hitResult,
      CollisionContext collisionContext,
      boolean inTranslucentPass,
      boolean highContrast,
      Camera camera,
      LevelRenderState levelRenderState
   ) {
      this.levelRenderer = levelRenderer;
      this.level = level;
      this.pos = pos;
      this.state = state;
      this.hitResult = hitResult;
      this.collisionContext = collisionContext;
      this.inTranslucentPass = inTranslucentPass;
      this.highContrast = highContrast;
      this.camera = camera;
      this.levelRenderState = levelRenderState;
   }

   public void addCustomRenderer(CustomBlockOutlineRenderer renderer) {
      this.customRenderers.add(renderer);
   }

   public LevelRenderer getLevelRenderer() {
      return this.levelRenderer;
   }

   public ClientLevel getLevel() {
      return this.level;
   }

   public BlockPos getBlockPos() {
      return this.pos;
   }

   public BlockState getBlockState() {
      return this.state;
   }

   public BlockHitResult getHitResult() {
      return this.hitResult;
   }

   public CollisionContext getCollisionContext() {
      return this.collisionContext;
   }

   public boolean isInTranslucentPass() {
      return this.inTranslucentPass;
   }

   public boolean isHighContrast() {
      return this.highContrast;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public LevelRenderState getLevelRenderState() {
      return this.levelRenderState;
   }

   @Internal
   public List<CustomBlockOutlineRenderer> getCustomRenderers() {
      return this.customRenderers;
   }
}
