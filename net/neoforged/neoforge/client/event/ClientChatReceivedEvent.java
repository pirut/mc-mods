package net.neoforged.neoforge.client.event;

import java.util.UUID;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.Util;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class ClientChatReceivedEvent extends Event implements ICancellableEvent {
   private Component message;
   @Nullable
   private final ChatType.Bound boundChatType;
   private final UUID sender;

   @Internal
   public ClientChatReceivedEvent(@Nullable ChatType.Bound boundChatType, Component message, UUID sender) {
      this.boundChatType = boundChatType;
      this.message = message;
      this.sender = sender;
   }

   public Component getMessage() {
      return this.message;
   }

   public void setMessage(Component message) {
      this.message = message;
   }

   @Nullable
   public ChatType.Bound getBoundChatType() {
      return this.boundChatType;
   }

   public UUID getSender() {
      return this.sender;
   }

   public boolean isSystem() {
      return this.sender.equals(Util.NIL_UUID);
   }

   public static class Player extends ClientChatReceivedEvent {
      private final PlayerChatMessage playerChatMessage;

      @Internal
      public Player(ChatType.Bound boundChatType, Component message, PlayerChatMessage playerChatMessage, UUID sender) {
         super(boundChatType, message, sender);
         this.playerChatMessage = playerChatMessage;
      }

      public PlayerChatMessage getPlayerChatMessage() {
         return this.playerChatMessage;
      }
   }

   public static class System extends ClientChatReceivedEvent {
      private final boolean overlay;

      @Internal
      public System(Component message, boolean overlay) {
         super(null, message, Util.NIL_UUID);
         this.overlay = overlay;
      }

      public boolean isOverlay() {
         return this.overlay;
      }
   }
}
