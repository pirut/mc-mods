package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class RenderPlayerEvent<T extends Avatar & ClientAvatarEntity> extends RenderLivingEvent<T, AvatarRenderState, PlayerModel> {
   @Internal
   protected RenderPlayerEvent(
      AvatarRenderState renderState, AvatarRenderer<T> renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector
   ) {
      super(renderState, renderer, partialTick, poseStack, submitNodeCollector);
   }

   public AvatarRenderer<T> getRenderer() {
      return (AvatarRenderer<T>)super.getRenderer();
   }

   public static class Post<T extends Avatar & ClientAvatarEntity> extends RenderPlayerEvent<T> {
      @Internal
      public Post(AvatarRenderState renderState, AvatarRenderer<T> renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
         super(renderState, renderer, partialTick, poseStack, submitNodeCollector);
      }
   }

   public static class Pre<T extends Avatar & ClientAvatarEntity> extends RenderPlayerEvent<T> implements ICancellableEvent {
      @Internal
      public Pre(AvatarRenderState renderState, AvatarRenderer<T> renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
         super(renderState, renderer, partialTick, poseStack, submitNodeCollector);
      }
   }
}
