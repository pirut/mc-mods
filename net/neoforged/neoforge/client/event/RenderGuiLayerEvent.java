package net.neoforged.neoforge.client.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class RenderGuiLayerEvent extends Event {
   private final GuiGraphics guiGraphics;
   private final DeltaTracker partialTick;
   private final Identifier name;
   private final GuiLayer layer;

   @Internal
   protected RenderGuiLayerEvent(GuiGraphics guiGraphics, DeltaTracker partialTick, Identifier name, GuiLayer layer) {
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

   public Identifier getName() {
      return this.name;
   }

   public GuiLayer getLayer() {
      return this.layer;
   }

   public static class Post extends RenderGuiLayerEvent {
      @Internal
      public Post(GuiGraphics guiGraphics, DeltaTracker partialTick, Identifier name, GuiLayer layer) {
         super(guiGraphics, partialTick, name, layer);
      }
   }

   public static class Pre extends RenderGuiLayerEvent implements ICancellableEvent {
      @Internal
      public Pre(GuiGraphics guiGraphics, DeltaTracker partialTick, Identifier name, GuiLayer layer) {
         super(guiGraphics, partialTick, name, layer);
      }
   }
}
