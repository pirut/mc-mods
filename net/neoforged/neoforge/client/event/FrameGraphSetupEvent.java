package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;

public final class FrameGraphSetupEvent extends Event {
   private final FrameGraphBuilder builder;
   private final LevelTargetBundle targets;
   private final RenderTargetDescriptor renderTargetDescriptor;
   private final Frustum frustum;
   private final Camera camera;
   private final Matrix4f modelViewMatrix;
   private final Matrix4f projectionMatrix;
   private final DeltaTracker deltaTracker;
   private final ProfilerFiller profiler;
   private boolean enableOutline;

   @Internal
   public FrameGraphSetupEvent(
      FrameGraphBuilder builder,
      LevelTargetBundle targets,
      RenderTargetDescriptor renderTargetDescriptor,
      Frustum frustum,
      Camera camera,
      Matrix4f modelViewMatrix,
      Matrix4f projectionMatrix,
      DeltaTracker deltaTracker,
      ProfilerFiller profiler
   ) {
      this.builder = builder;
      this.targets = targets;
      this.renderTargetDescriptor = renderTargetDescriptor;
      this.frustum = frustum;
      this.camera = camera;
      this.modelViewMatrix = modelViewMatrix;
      this.projectionMatrix = projectionMatrix;
      this.deltaTracker = deltaTracker;
      this.profiler = profiler;
   }

   public FrameGraphBuilder getFrameGrapBuilder() {
      return this.builder;
   }

   public LevelTargetBundle getTargetBundle() {
      return this.targets;
   }

   public RenderTargetDescriptor getRenderTargetDescriptor() {
      return this.renderTargetDescriptor;
   }

   public Frustum getFrustum() {
      return this.frustum;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public Matrix4f getModelViewMatrix() {
      return this.modelViewMatrix;
   }

   public Matrix4f getProjectionMatrix() {
      return this.projectionMatrix;
   }

   public DeltaTracker getDeltaTracker() {
      return this.deltaTracker;
   }

   public ProfilerFiller getProfiler() {
      return this.profiler;
   }

   public void enableOutlineProcessing() {
      this.enableOutline = true;
   }

   public boolean isOutlineProcessingEnabled() {
      return this.enableOutline;
   }
}
