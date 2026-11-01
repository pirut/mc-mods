package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RenderItemInFrameEvent extends Event implements ICancellableEvent {
   private final ItemStackRenderState itemStack;
   private final ItemFrameRenderState frameRenderState;
   private final ItemFrameRenderer<?> renderer;
   private final PoseStack poseStack;
   private final SubmitNodeCollector submitNodeCollector;

   @Internal
   public RenderItemInFrameEvent(
      ItemFrameRenderState frameRenderState, ItemFrameRenderer<?> renderItemFrame, PoseStack poseStack, SubmitNodeCollector submitNodeCollector
   ) {
      this.itemStack = frameRenderState.item;
      this.frameRenderState = frameRenderState;
      this.renderer = renderItemFrame;
      this.poseStack = poseStack;
      this.submitNodeCollector = submitNodeCollector;
   }

   public ItemStackRenderState getItemStackRenderState() {
      return this.itemStack;
   }

   public ItemFrameRenderState getItemFrameRenderState() {
      return this.frameRenderState;
   }

   public ItemFrameRenderer<?> getRenderer() {
      return this.renderer;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public SubmitNodeCollector getSubmitNodeCollector() {
      return this.submitNodeCollector;
   }
}
