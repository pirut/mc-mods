package net.neoforged.neoforge.client.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ClientPauseChangeEvent extends Event {
   private final boolean pause;

   public ClientPauseChangeEvent(boolean pause) {
      this.pause = pause;
   }

   public boolean isPaused() {
      return this.pause;
   }

   public static class Post extends ClientPauseChangeEvent {
      public Post(boolean pause) {
         super(pause);
      }
   }

   public static class Pre extends ClientPauseChangeEvent implements ICancellableEvent {
      public Pre(boolean pause) {
         super(pause);
      }
   }
}
