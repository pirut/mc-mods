package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class ServerDebugSubscribers {
   private final MinecraftServer server;
   private final Map<DebugSubscription<?>, List<ServerPlayer>> enabledSubscriptions = new HashMap<>();

   public ServerDebugSubscribers(MinecraftServer p_449804_) {
      this.server = p_449804_;
   }

   private List<ServerPlayer> getSubscribersFor(DebugSubscription<?> p_449856_) {
      return this.enabledSubscriptions.getOrDefault(p_449856_, List.of());
   }

   public void tick() {
      this.enabledSubscriptions.values().forEach(List::clear);

      for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
         for (DebugSubscription<?> debugsubscription : serverplayer.debugSubscriptions()) {
            this.enabledSubscriptions.computeIfAbsent(debugsubscription, p_449260_ -> new ArrayList<>()).add(serverplayer);
         }
      }

      this.enabledSubscriptions.values().removeIf(List::isEmpty);
   }

   public void broadcastToAll(DebugSubscription<?> p_449018_, Packet<?> p_449659_) {
      for (ServerPlayer serverplayer : this.getSubscribersFor(p_449018_)) {
         serverplayer.connection.send(p_449659_);
      }
   }

   public Set<DebugSubscription<?>> enabledSubscriptions() {
      return Set.copyOf(this.enabledSubscriptions.keySet());
   }

   public boolean hasAnySubscriberFor(DebugSubscription<?> p_449811_) {
      return !this.getSubscribersFor(p_449811_).isEmpty();
   }

   public boolean hasRequiredPermissions(ServerPlayer p_449527_) {
      NameAndId nameandid = p_449527_.nameAndId();
      return SharedConstants.IS_RUNNING_IN_IDE && this.server.isSingleplayerOwner(nameandid) ? true : this.server.getPlayerList().isOp(nameandid);
   }
}
