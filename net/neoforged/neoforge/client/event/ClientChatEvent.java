package net.neoforged.neoforge.client.event;

import com.google.common.base.Strings;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ClientChatEvent extends Event implements ICancellableEvent {
   private String message;
   private final String originalMessage;

   @Internal
   public ClientChatEvent(String message) {
      this.setMessage(message);
      this.originalMessage = Strings.nullToEmpty(message);
      this.message = this.originalMessage;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = Strings.nullToEmpty(message);
   }

   public String getOriginalMessage() {
      return this.originalMessage;
   }
}
