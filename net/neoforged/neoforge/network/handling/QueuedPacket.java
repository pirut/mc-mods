package net.neoforged.neoforge.network.handling;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface QueuedPacket {
   void handle();

   public record CustomPayload(Runnable task) implements QueuedPacket {
      @Override
      public void handle() {
         this.task.run();
      }
   }
}
