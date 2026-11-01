package net.neoforged.neoforge.event;

import java.util.stream.Stream;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

public class OnDatapackSyncEvent extends Event {
   private final PlayerList playerList;
   @Nullable
   private final ServerPlayer player;

   public OnDatapackSyncEvent(PlayerList playerList, @Nullable ServerPlayer player) {
      this.playerList = playerList;
      this.player = player;
   }

   public PlayerList getPlayerList() {
      return this.playerList;
   }

   public Stream<ServerPlayer> getRelevantPlayers() {
      return this.player == null ? this.playerList.getPlayers().stream() : Stream.of(this.player);
   }

   @Nullable
   public ServerPlayer getPlayer() {
      return this.player;
   }
}
