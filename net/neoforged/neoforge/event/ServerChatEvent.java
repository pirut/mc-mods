package net.neoforged.neoforge.event;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ServerChatEvent extends Event implements ICancellableEvent {
   private final ServerPlayer player;
   private final String username;
   private final String rawText;
   private Component message;

   @Internal
   public ServerChatEvent(ServerPlayer player, String rawText, Component message) {
      this.player = player;
      this.username = player.getGameProfile().name();
      this.rawText = rawText;
      this.message = message;
   }

   public ServerPlayer getPlayer() {
      return this.player;
   }

   public String getUsername() {
      return this.username;
   }

   public String getRawText() {
      return this.rawText;
   }

   public void setMessage(Component message) {
      this.message = Objects.requireNonNull(message);
   }

   public Component getMessage() {
      return this.message;
   }
}
