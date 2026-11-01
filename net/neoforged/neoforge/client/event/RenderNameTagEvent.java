package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public abstract class RenderNameTagEvent extends Event {
   private final EntityRenderState renderState;
   private final EntityRenderer<?, ?> entityRenderer;
   private final float partialTick;

   @Internal
   public RenderNameTagEvent(EntityRenderState renderState, EntityRenderer<?, ?> entityRenderer, float partialTick) {
      this.renderState = renderState;
      this.entityRenderer = entityRenderer;
      this.partialTick = partialTick;
   }

   public EntityRenderState getEntityRenderState() {
      return this.renderState;
   }

   public EntityRenderer<?, ?> getEntityRenderer() {
      return this.entityRenderer;
   }

   public float getPartialTick() {
      return this.partialTick;
   }

   public static class CanRender extends RenderNameTagEvent {
      private final Entity entity;
      @Nullable
      private final Component originalContent;
      @Nullable
      private Component content;
      private TriState canRender = TriState.DEFAULT;

      public CanRender(Entity entity, EntityRenderState renderState, @Nullable Component content, EntityRenderer<?, ?> entityRenderer, float partialTick) {
         super(renderState, entityRenderer, partialTick);
         this.entity = entity;
         this.originalContent = content;
         this.content = content;
      }

      public Entity getEntity() {
         return this.entity;
      }

      @Nullable
      public Component getOriginalContent() {
         return this.originalContent;
      }

      public void setCanRender(TriState canRender) {
         this.canRender = canRender;
      }

      public TriState canRender() {
         return this.canRender;
      }

      public void setContent(Component contents) {
         this.content = contents;
      }

      @Nullable
      public Component getContent() {
         return this.content;
      }
   }

   public static class DoRender extends RenderNameTagEvent implements ICancellableEvent {
      private final Component content;
      private final PoseStack poseStack;
      private final SubmitNodeCollector submitNodeCollector;
      private final CameraRenderState cameraRenderState;

      public DoRender(
         EntityRenderState renderState,
         Component content,
         EntityRenderer<?, ?> entityRenderer,
         PoseStack poseStack,
         SubmitNodeCollector submitNodeCollector,
         CameraRenderState cameraRenderState,
         float partialTick
      ) {
         super(renderState, entityRenderer, partialTick);
         this.content = content;
         this.poseStack = poseStack;
         this.submitNodeCollector = submitNodeCollector;
         this.cameraRenderState = cameraRenderState;
      }

      public Component getContent() {
         return this.content;
      }

      public PoseStack getPoseStack() {
         return this.poseStack;
      }

      public SubmitNodeCollector getSubmitNodeCollector() {
         return this.submitNodeCollector;
      }

      public CameraRenderState getCameraRenderState() {
         return this.cameraRenderState;
      }
   }
}
