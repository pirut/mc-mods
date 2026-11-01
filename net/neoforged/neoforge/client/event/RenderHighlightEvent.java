package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class RenderHighlightEvent extends Event {
   private final LevelRenderer levelRenderer;
   private final Camera camera;
   private final HitResult target;
   private final DeltaTracker deltaTracker;
   private final PoseStack poseStack;
   private final MultiBufferSource multiBufferSource;

   @Internal
   protected RenderHighlightEvent(
      LevelRenderer levelRenderer, Camera camera, HitResult target, DeltaTracker deltaTracker, PoseStack poseStack, MultiBufferSource multiBufferSource
   ) {
      this.levelRenderer = levelRenderer;
      this.camera = camera;
      this.target = target;
      this.deltaTracker = deltaTracker;
      this.poseStack = poseStack;
      this.multiBufferSource = multiBufferSource;
   }

   public LevelRenderer getLevelRenderer() {
      return this.levelRenderer;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public HitResult getTarget() {
      return this.target;
   }

   public DeltaTracker getDeltaTracker() {
      return this.deltaTracker;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public MultiBufferSource getMultiBufferSource() {
      return this.multiBufferSource;
   }

   public static class Block extends RenderHighlightEvent implements ICancellableEvent {
      @Internal
      public Block(
         LevelRenderer levelRenderer, Camera camera, BlockHitResult target, DeltaTracker deltaTracker, PoseStack poseStack, MultiBufferSource bufferSource
      ) {
         super(levelRenderer, camera, target, deltaTracker, poseStack, bufferSource);
      }

      public BlockHitResult getTarget() {
         return (BlockHitResult)super.target;
      }
   }

   public static class Entity extends RenderHighlightEvent {
      @Internal
      public Entity(
         LevelRenderer levelRenderer, Camera camera, EntityHitResult target, DeltaTracker deltaTracker, PoseStack poseStack, MultiBufferSource bufferSource
      ) {
         super(levelRenderer, camera, target, deltaTracker, poseStack, bufferSource);
      }

      public EntityHitResult getTarget() {
         return (EntityHitResult)super.target;
      }
   }
}
