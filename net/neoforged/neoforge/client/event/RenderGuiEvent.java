package net.neoforged.neoforge.client.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class RenderGuiEvent extends Event {
   private final GuiGraphics guiGraphics;
   private final DeltaTracker partialTick;

   @Internal
   protected RenderGuiEvent(GuiGraphics guiGraphics, DeltaTracker partialTick) {
      this.guiGraphics = guiGraphics;
      this.partialTick = partialTick;
   }

   public GuiGraphics getGuiGraphics() {
      return this.guiGraphics;
   }

   public DeltaTracker getPartialTick() {
      return this.partialTick;
   }

   public static class Post extends RenderGuiEvent {
      @Internal
      public Post(GuiGraphics guiGraphics, DeltaTracker partialTick) {
         super(guiGraphics, partialTick);
      }
   }

   public static class Pre extends RenderGuiEvent implements ICancellableEvent {
      @Internal
      public Pre(GuiGraphics guiGraphics, DeltaTracker partialTick) {
         super(guiGraphics, partialTick);
      }
   }
}
