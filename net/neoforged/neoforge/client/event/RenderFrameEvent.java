package net.neoforged.neoforge.client.event;

import net.minecraft.client.DeltaTracker;
import net.neoforged.bus.api.Event;

public abstract class RenderFrameEvent extends Event {
   protected final DeltaTracker partialTick;

   protected RenderFrameEvent(DeltaTracker partialTick) {
      this.partialTick = partialTick;
   }

   public DeltaTracker getPartialTick() {
      return this.partialTick;
   }

   public static class Post extends RenderFrameEvent {
      public Post(DeltaTracker partialTick) {
         super(partialTick);
      }
   }

   public static class Pre extends RenderFrameEvent {
      public Pre(DeltaTracker partialTick) {
         super(partialTick);
      }
   }
}
