package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class RenderLivingEvent<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends Event {
   private final S renderState;
   private final LivingEntityRenderer<T, S, M> renderer;
   private final float partialTick;
   private final PoseStack poseStack;
   private final SubmitNodeCollector submitNodeCollector;

   @Internal
   protected RenderLivingEvent(
      S renderState, LivingEntityRenderer<T, S, M> renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector
   ) {
      this.renderState = renderState;
      this.renderer = renderer;
      this.partialTick = partialTick;
      this.poseStack = poseStack;
      this.submitNodeCollector = submitNodeCollector;
   }

   public S getRenderState() {
      return this.renderState;
   }

   public LivingEntityRenderer<T, S, M> getRenderer() {
      return this.renderer;
   }

   public float getPartialTick() {
      return this.partialTick;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public SubmitNodeCollector getSubmitNodeCollector() {
      return this.submitNodeCollector;
   }

   public static class Post<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLivingEvent<T, S, M> {
      @Internal
      public Post(S renderState, LivingEntityRenderer<T, S, M> renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
         super(renderState, renderer, partialTick, poseStack, submitNodeCollector);
      }
   }

   public static class Pre<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
      extends RenderLivingEvent<T, S, M>
      implements ICancellableEvent {
      @Internal
      public Pre(S renderState, LivingEntityRenderer<T, S, M> renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
         super(renderState, renderer, partialTick, poseStack, submitNodeCollector);
      }
   }
}
