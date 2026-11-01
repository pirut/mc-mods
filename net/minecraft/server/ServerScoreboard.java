package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.jspecify.annotations.Nullable;

public class ServerScoreboard extends Scoreboard {
   private final MinecraftServer server;
   private final Set<Objective> trackedObjectives = Sets.newHashSet();
   private boolean dirty;

   public ServerScoreboard(MinecraftServer server) {
      this.server = server;
   }

   public void load(ScoreboardSaveData.Packed data) {
      data.objectives().forEach(packed -> this.loadObjective(packed));
      data.scores().forEach(packedScore -> this.loadPlayerScore(packedScore));
      data.displaySlots().forEach((displaySlot, string) -> {
         Objective objective = this.getObjective(string);
         this.setDisplayObjective(displaySlot, objective);
      });
      data.teams().forEach(packed -> this.loadPlayerTeam(packed));
   }

   private ScoreboardSaveData.Packed store() {
      return new ScoreboardSaveData.Packed(this.packObjectives(), this.packPlayerScores(), this.packDisplaySlots(), this.packPlayerTeams());
   }

   @Override
   protected void onScoreChanged(ScoreHolder scoreHolder, Objective objective, Score score) {
      super.onScoreChanged(scoreHolder, objective, score);
      if (this.trackedObjectives.contains(objective)) {
         this.server
            .getPlayerList()
            .broadcastAll(
               new ClientboundSetScorePacket(
                  scoreHolder.getScoreboardName(),
                  objective.getName(),
                  score.value(),
                  Optional.ofNullable(score.display()),
                  Optional.ofNullable(score.numberFormat())
               )
            );
      }

      this.setDirty();
   }

   @Override
   protected void onScoreLockChanged(ScoreHolder scoreHolder, Objective objective) {
      super.onScoreLockChanged(scoreHolder, objective);
      this.setDirty();
   }

   @Override
   public void onPlayerRemoved(ScoreHolder scoreHolder) {
      super.onPlayerRemoved(scoreHolder);
      this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(scoreHolder.getScoreboardName(), null));
      this.setDirty();
   }

   @Override
   public void onPlayerScoreRemoved(ScoreHolder scoreHolder, Objective objective) {
      super.onPlayerScoreRemoved(scoreHolder, objective);
      if (this.trackedObjectives.contains(objective)) {
         this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(scoreHolder.getScoreboardName(), objective.getName()));
      }

      this.setDirty();
   }

   @Override
   public void setDisplayObjective(DisplaySlot slot, @Nullable Objective objective) {
      Objective objective2 = this.getDisplayObjective(slot);
      super.setDisplayObjective(slot, objective);
      if (objective2 != objective && objective2 != null) {
         if (this.getObjectiveDisplaySlotCount(objective2) > 0) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(slot, objective));
         } else {
            this.stopTrackingObjective(objective2);
         }
      }

      if (objective != null) {
         if (this.trackedObjectives.contains(objective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(slot, objective));
         } else {
            this.startTrackingObjective(objective);
         }
      }

      this.setDirty();
   }

   @Override
   public boolean addPlayerToTeam(String playerName, PlayerTeam team) {
      if (super.addPlayerToTeam(playerName, team)) {
         this.server
            .getPlayerList()
            .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, playerName, ClientboundSetPlayerTeamPacket.Action.ADD));
         this.updatePlayerWaypoint(playerName);
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void removePlayerFromTeam(String username, PlayerTeam playerTeam) {
      super.removePlayerFromTeam(username, playerTeam);
      this.server
         .getPlayerList()
         .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, username, ClientboundSetPlayerTeamPacket.Action.REMOVE));
      this.updatePlayerWaypoint(username);
      this.setDirty();
   }

   @Override
   public void onObjectiveAdded(Objective objective) {
      super.onObjectiveAdded(objective);
      this.setDirty();
   }

   @Override
   public void onObjectiveChanged(Objective objective) {
      super.onObjectiveChanged(objective);
      if (this.trackedObjectives.contains(objective)) {
         this.server.getPlayerList().broadcastAll(new ClientboundSetObjectivePacket(objective, 2));
      }

      this.setDirty();
   }

   @Override
   public void onObjectiveRemoved(Objective objective) {
      super.onObjectiveRemoved(objective);
      if (this.trackedObjectives.contains(objective)) {
         this.stopTrackingObjective(objective);
      }

      this.setDirty();
   }

   @Override
   public void onTeamAdded(PlayerTeam playerTeam) {
      super.onTeamAdded(playerTeam);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true));
      this.setDirty();
   }

   @Override
   public void onTeamChanged(PlayerTeam playerTeam) {
      super.onTeamChanged(playerTeam);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, false));
      this.updateTeamWaypoints(playerTeam);
      this.setDirty();
   }

   @Override
   public void onTeamRemoved(PlayerTeam playerTeam) {
      super.onTeamRemoved(playerTeam);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createRemovePacket(playerTeam));
      this.updateTeamWaypoints(playerTeam);
      this.setDirty();
   }

   protected void setDirty() {
      this.dirty = true;
   }

   public void storeToSaveDataIfDirty(ScoreboardSaveData saveData) {
      if (this.dirty) {
         this.dirty = false;
         saveData.setData(this.store());
      }
   }

   public List<Packet<?>> getStartTrackingPackets(Objective objective) {
      List<Packet<?>> list = Lists.newArrayList();
      list.add(new ClientboundSetObjectivePacket(objective, 0));

      for (DisplaySlot displaySlot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displaySlot) == objective) {
            list.add(new ClientboundSetDisplayObjectivePacket(displaySlot, objective));
         }
      }

      for (PlayerScoreEntry playerScoreEntry : this.listPlayerScores(objective)) {
         list.add(
            new ClientboundSetScorePacket(
               playerScoreEntry.owner(),
               objective.getName(),
               playerScoreEntry.value(),
               Optional.ofNullable(playerScoreEntry.display()),
               Optional.ofNullable(playerScoreEntry.numberFormatOverride())
            )
         );
      }

      return list;
   }

   public void startTrackingObjective(Objective objective) {
      List<Packet<?>> list = this.getStartTrackingPackets(objective);

      for (ServerPlayer serverPlayer : this.server.getPlayerList().getPlayers()) {
         for (Packet<?> packet : list) {
            serverPlayer.connection.send(packet);
         }
      }

      this.trackedObjectives.add(objective);
   }

   public List<Packet<?>> getStopTrackingPackets(Objective objective) {
      List<Packet<?>> list = Lists.newArrayList();
      list.add(new ClientboundSetObjectivePacket(objective, 1));

      for (DisplaySlot displaySlot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displaySlot) == objective) {
            list.add(new ClientboundSetDisplayObjectivePacket(displaySlot, objective));
         }
      }

      return list;
   }

   public void stopTrackingObjective(Objective objective) {
      List<Packet<?>> list = this.getStopTrackingPackets(objective);

      for (ServerPlayer serverPlayer : this.server.getPlayerList().getPlayers()) {
         for (Packet<?> packet : list) {
            serverPlayer.connection.send(packet);
         }
      }

      this.trackedObjectives.remove(objective);
   }

   public int getObjectiveDisplaySlotCount(Objective objective) {
      int i = 0;

      for (DisplaySlot displaySlot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displaySlot) == objective) {
            i++;
         }
      }

      return i;
   }

   private void updatePlayerWaypoint(String playerName) {
      ServerPlayer serverPlayer = this.server.getPlayerList().getPlayerByName(playerName);
      if (serverPlayer != null) {
         serverPlayer.level().getWaypointManager().remakeConnections(serverPlayer);
      }
   }

   private void updateTeamWaypoints(PlayerTeam team) {
      for (ServerLevel serverLevel : this.server.getAllLevels()) {
         team.getPlayers()
            .stream()
            .map(string -> this.server.getPlayerList().getPlayerByName(string))
            .filter(Objects::nonNull)
            .forEach(serverPlayer -> serverLevel.getWaypointManager().remakeConnections(serverPlayer));
      }
   }
}
