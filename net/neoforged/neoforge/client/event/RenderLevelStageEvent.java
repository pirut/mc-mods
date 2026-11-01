package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.IRenderableSection;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public abstract class RenderLevelStageEvent extends Event {
   private final LevelRenderer levelRenderer;
   private final LevelRenderState levelRenderState;
   private final PoseStack poseStack;
   private final Matrix4f modelViewMatrix;
   private final Iterable<? extends IRenderableSection> renderableSections;

   public RenderLevelStageEvent(
      LevelRenderer levelRenderer,
      LevelRenderState levelRenderState,
      @Nullable PoseStack poseStack,
      Matrix4f modelViewMatrix,
      Iterable<? extends IRenderableSection> renderableSections
   ) {
      this.levelRenderer = levelRenderer;
      this.levelRenderState = levelRenderState;
      this.poseStack = poseStack != null ? poseStack : new PoseStack();
      this.modelViewMatrix = modelViewMatrix;
      this.renderableSections = renderableSections;
   }

   public LevelRenderer getLevelRenderer() {
      return this.levelRenderer;
   }

   public LevelRenderState getLevelRenderState() {
      return this.levelRenderState;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public Matrix4f getModelViewMatrix() {
      return this.modelViewMatrix;
   }

   public Iterable<? extends IRenderableSection> getRenderableSections() {
      return this.renderableSections;
   }

   public static class AfterEntities extends RenderLevelStageEvent {
      public AfterEntities(
         LevelRenderer levelRenderer,
         LevelRenderState levelRenderState,
         @Nullable PoseStack poseStack,
         Matrix4f modelViewMatrix,
         Iterable<? extends IRenderableSection> renderableSections
      ) {
         super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
      }
   }

   public static class AfterLevel extends RenderLevelStageEvent {
      public AfterLevel(
         LevelRenderer levelRenderer,
         LevelRenderState levelRenderState,
         @Nullable PoseStack poseStack,
         Matrix4f modelViewMatrix,
         Iterable<? extends IRenderableSection> renderableSections
      ) {
         super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
      }
   }

   public static class AfterOpaqueBlocks extends RenderLevelStageEvent {
      public AfterOpaqueBlocks(
         LevelRenderer levelRenderer,
         LevelRenderState levelRenderState,
         @Nullable PoseStack poseStack,
         Matrix4f modelViewMatrix,
         Iterable<? extends IRenderableSection> renderableSections
      ) {
         super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
      }
   }

   public static class AfterParticles extends RenderLevelStageEvent {
      public AfterParticles(
         LevelRenderer levelRenderer,
         LevelRenderState levelRenderState,
         @Nullable PoseStack poseStack,
         Matrix4f modelViewMatrix,
         Iterable<? extends IRenderableSection> renderableSections
      ) {
         super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
      }
   }

   public static class AfterSky extends RenderLevelStageEvent {
      public AfterSky(
         LevelRenderer levelRenderer,
         LevelRenderState levelRenderState,
         @Nullable PoseStack poseStack,
         Matrix4f modelViewMatrix,
         Iterable<? extends IRenderableSection> renderableSections
      ) {
         super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
      }
   }

   public static class AfterTranslucentBlocks extends RenderLevelStageEvent {
      public AfterTranslucentBlocks(
         LevelRenderer levelRenderer,
         LevelRenderState levelRenderState,
         @Nullable PoseStack poseStack,
         Matrix4f modelViewMatrix,
         Iterable<? extends IRenderableSection> renderableSections
      ) {
         super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
      }
   }

   public static class AfterTripwireBlocks extends RenderLevelStageEvent {
      public AfterTripwireBlocks(
         LevelRenderer levelRenderer,
         LevelRenderState levelRenderState,
         @Nullable PoseStack poseStack,
         Matrix4f modelViewMatrix,
         Iterable<? extends IRenderableSection> renderableSections
      ) {
         super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
      }
   }

   public static class AfterWeather extends RenderLevelStageEvent {
      public AfterWeather(
         LevelRenderer levelRenderer,
         LevelRenderState levelRenderState,
         @Nullable PoseStack poseStack,
         Matrix4f modelViewMatrix,
         Iterable<? extends IRenderableSection> renderableSections
      ) {
         super(levelRenderer, levelRenderState, poseStack, modelViewMatrix, renderableSections);
      }
   }
}
