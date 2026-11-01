package net.minecraft.server.waypoints;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.Sets.SetView;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.waypoints.WaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class ServerWaypointManager implements WaypointManager<WaypointTransmitter> {
   private final Set<WaypointTransmitter> waypoints = new HashSet<>();
   private final Set<ServerPlayer> players = new HashSet<>();
   private final Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> connections = HashBasedTable.create();

   public void trackWaypoint(WaypointTransmitter waypointTransmitter) {
      this.waypoints.add(waypointTransmitter);

      for (ServerPlayer serverPlayer : this.players) {
         this.createConnection(serverPlayer, waypointTransmitter);
      }
   }

   public void updateWaypoint(WaypointTransmitter waypointTransmitter) {
      if (this.waypoints.contains(waypointTransmitter)) {
         Map<ServerPlayer, WaypointTransmitter.Connection> map = Tables.transpose(this.connections).row(waypointTransmitter);
         SetView<ServerPlayer> setView = Sets.difference(this.players, map.keySet());
         UnmodifiableIterator var4 = ImmutableSet.copyOf(map.entrySet()).iterator();

         while (var4.hasNext()) {
            Entry<ServerPlayer, WaypointTransmitter.Connection> entry = (Entry<ServerPlayer, WaypointTransmitter.Connection>)var4.next();
            this.updateConnection(entry.getKey(), waypointTransmitter, entry.getValue());
         }

         var4 = setView.iterator();

         while (var4.hasNext()) {
            ServerPlayer serverPlayer = (ServerPlayer)var4.next();
            this.createConnection(serverPlayer, waypointTransmitter);
         }
      }
   }

   public void untrackWaypoint(WaypointTransmitter waypointTransmitter) {
      this.connections.column(waypointTransmitter).forEach((serverPlayer, connection) -> connection.disconnect());
      Tables.transpose(this.connections).row(waypointTransmitter).clear();
      this.waypoints.remove(waypointTransmitter);
   }

   public void addPlayer(ServerPlayer player) {
      this.players.add(player);

      for (WaypointTransmitter waypointTransmitter : this.waypoints) {
         this.createConnection(player, waypointTransmitter);
      }

      if (player.isTransmittingWaypoint()) {
         this.trackWaypoint((WaypointTransmitter)player);
      }
   }

   public void updatePlayer(ServerPlayer player) {
      Map<WaypointTransmitter, WaypointTransmitter.Connection> map = this.connections.row(player);
      SetView<WaypointTransmitter> setView = Sets.difference(this.waypoints, map.keySet());
      UnmodifiableIterator var4 = ImmutableSet.copyOf(map.entrySet()).iterator();

      while (var4.hasNext()) {
         Entry<WaypointTransmitter, WaypointTransmitter.Connection> entry = (Entry<WaypointTransmitter, WaypointTransmitter.Connection>)var4.next();
         this.updateConnection(player, entry.getKey(), entry.getValue());
      }

      var4 = setView.iterator();

      while (var4.hasNext()) {
         WaypointTransmitter waypointTransmitter = (WaypointTransmitter)var4.next();
         this.createConnection(player, waypointTransmitter);
      }
   }

   public void removePlayer(ServerPlayer player) {
      this.connections.row(player).values().removeIf(connection -> {
         connection.disconnect();
         return true;
      });
      this.untrackWaypoint((WaypointTransmitter)player);
      this.players.remove(player);
   }

   public void breakAllConnections() {
      this.connections.values().forEach(WaypointTransmitter.Connection::disconnect);
      this.connections.clear();
   }

   public void remakeConnections(WaypointTransmitter waypoint) {
      for (ServerPlayer serverPlayer : this.players) {
         this.createConnection(serverPlayer, waypoint);
      }
   }

   public Set<WaypointTransmitter> transmitters() {
      return this.waypoints;
   }

   private static boolean isLocatorBarEnabledFor(ServerPlayer player) {
      return player.level().getGameRules().get(GameRules.LOCATOR_BAR);
   }

   private void createConnection(ServerPlayer player, WaypointTransmitter waypoint) {
      if (player != waypoint) {
         if (isLocatorBarEnabledFor(player)) {
            waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(connection -> {
               this.connections.put(player, waypoint, connection);
               connection.connect();
            }, () -> {
               WaypointTransmitter.Connection connection = (WaypointTransmitter.Connection)this.connections.remove(player, waypoint);
               if (connection != null) {
                  connection.disconnect();
               }
            });
         }
      }
   }

   private void updateConnection(ServerPlayer player, WaypointTransmitter waypoint, WaypointTransmitter.Connection connection) {
      if (player != waypoint) {
         if (isLocatorBarEnabledFor(player)) {
            if (!connection.isBroken()) {
               connection.update();
            } else {
               waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(connectionx -> {
                  connectionx.connect();
                  this.connections.put(player, waypoint, connectionx);
               }, () -> {
                  connection.disconnect();
                  this.connections.remove(player, waypoint);
               });
            }
         }
      }
   }
}
