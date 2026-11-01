package net.neoforged.neoforge.client.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class RenderGuiLayerEvent extends Event {
   private final GuiGraphics guiGraphics;
   private final DeltaTracker partialTick;
   private final ResourceLocation name;
   private final LayeredDraw.Layer layer;

   @Internal
   protected RenderGuiLayerEvent(GuiGraphics guiGraphics, DeltaTracker partialTick, ResourceLocation name, LayeredDraw.Layer layer) {
      this.guiGraphics = guiGraphics;
      this.partialTick = partialTick;
      this.name = name;
      this.layer = layer;
   }

   public GuiGraphics getGuiGraphics() {
      return this.guiGraphics;
   }

   public DeltaTracker getPartialTick() {
      return this.partialTick;
   }

   public ResourceLocation getName() {
      return this.name;
   }

   public LayeredDraw.Layer getLayer() {
      return this.layer;
   }

   public static class Post extends RenderGuiLayerEvent {
      @Internal
      public Post(GuiGraphics guiGraphics, DeltaTracker partialTick, ResourceLocation name, LayeredDraw.Layer layer) {
         super(guiGraphics, partialTick, name, layer);
      }
   }

   public static class Pre extends RenderGuiLayerEvent implements ICancellableEvent {
      @Internal
      public Pre(GuiGraphics guiGraphics, DeltaTracker partialTick, ResourceLocation name, LayeredDraw.Layer layer) {
         super(guiGraphics, partialTick, name, layer);
      }
   }
}
