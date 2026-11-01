package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;

public class ServerScoreboard extends Scoreboard {
   private final MinecraftServer server;
   private final Set<Objective> trackedObjectives = Sets.newHashSet();
   private final List<Runnable> dirtyListeners = Lists.newArrayList();

   public ServerScoreboard(MinecraftServer p_136197_) {
      this.server = p_136197_;
   }

   @Override
   protected void onScoreChanged(ScoreHolder p_313858_, Objective p_313953_, Score p_136206_) {
      super.onScoreChanged(p_313858_, p_313953_, p_136206_);
      if (this.trackedObjectives.contains(p_313953_)) {
         this.server
            .getPlayerList()
            .broadcastAll(
               new ClientboundSetScorePacket(
                  p_313858_.getScoreboardName(),
                  p_313953_.getName(),
                  p_136206_.value(),
                  Optional.ofNullable(p_136206_.display()),
                  Optional.ofNullable(p_136206_.numberFormat())
               )
            );
      }

      this.setDirty();
   }

   @Override
   protected void onScoreLockChanged(ScoreHolder p_313796_, Objective p_313806_) {
      super.onScoreLockChanged(p_313796_, p_313806_);
      this.setDirty();
   }

   @Override
   public void onPlayerRemoved(ScoreHolder p_313870_) {
      super.onPlayerRemoved(p_313870_);
      this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(p_313870_.getScoreboardName(), null));
      this.setDirty();
   }

   @Override
   public void onPlayerScoreRemoved(ScoreHolder p_313777_, Objective p_136213_) {
      super.onPlayerScoreRemoved(p_313777_, p_136213_);
      if (this.trackedObjectives.contains(p_136213_)) {
         this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(p_313777_.getScoreboardName(), p_136213_.getName()));
      }

      this.setDirty();
   }

   @Override
   public void setDisplayObjective(DisplaySlot p_294118_, @Nullable Objective p_136200_) {
      Objective $$2 = this.getDisplayObjective(p_294118_);
      super.setDisplayObjective(p_294118_, p_136200_);
      if ($$2 != p_136200_ && $$2 != null) {
         if (this.getObjectiveDisplaySlotCount($$2) > 0) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(p_294118_, p_136200_));
         } else {
            this.stopTrackingObjective($$2);
         }
      }

      if (p_136200_ != null) {
         if (this.trackedObjectives.contains(p_136200_)) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(p_294118_, p_136200_));
         } else {
            this.startTrackingObjective(p_136200_);
         }
      }

      this.setDirty();
   }

   @Override
   public boolean addPlayerToTeam(String p_136215_, PlayerTeam p_136216_) {
      if (super.addPlayerToTeam(p_136215_, p_136216_)) {
         this.server
            .getPlayerList()
            .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(p_136216_, p_136215_, ClientboundSetPlayerTeamPacket.Action.ADD));
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void removePlayerFromTeam(String p_136223_, PlayerTeam p_136224_) {
      super.removePlayerFromTeam(p_136223_, p_136224_);
      this.server
         .getPlayerList()
         .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(p_136224_, p_136223_, ClientboundSetPlayerTeamPacket.Action.REMOVE));
      this.setDirty();
   }

   @Override
   public void onObjectiveAdded(Objective p_136202_) {
      super.onObjectiveAdded(p_136202_);
      this.setDirty();
   }

   @Override
   public void onObjectiveChanged(Objective p_136219_) {
      super.onObjectiveChanged(p_136219_);
      if (this.trackedObjectives.contains(p_136219_)) {
         this.server.getPlayerList().broadcastAll(new ClientboundSetObjectivePacket(p_136219_, 2));
      }

      this.setDirty();
   }

   @Override
   public void onObjectiveRemoved(Objective p_136226_) {
      super.onObjectiveRemoved(p_136226_);
      if (this.trackedObjectives.contains(p_136226_)) {
         this.stopTrackingObjective(p_136226_);
      }

      this.setDirty();
   }

   @Override
   public void onTeamAdded(PlayerTeam p_136204_) {
      super.onTeamAdded(p_136204_);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(p_136204_, true));
      this.setDirty();
   }

   @Override
   public void onTeamChanged(PlayerTeam p_136221_) {
      super.onTeamChanged(p_136221_);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(p_136221_, false));
      this.setDirty();
   }

   @Override
   public void onTeamRemoved(PlayerTeam p_136228_) {
      super.onTeamRemoved(p_136228_);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createRemovePacket(p_136228_));
      this.setDirty();
   }

   public void addDirtyListener(Runnable p_136208_) {
      this.dirtyListeners.add(p_136208_);
   }

   protected void setDirty() {
      for (Runnable $$0 : this.dirtyListeners) {
         $$0.run();
      }
   }

   public List<Packet<?>> getStartTrackingPackets(Objective p_136230_) {
      List<Packet<?>> $$1 = Lists.newArrayList();
      $$1.add(new ClientboundSetObjectivePacket(p_136230_, 0));

      for (DisplaySlot $$2 : DisplaySlot.values()) {
         if (this.getDisplayObjective($$2) == p_136230_) {
            $$1.add(new ClientboundSetDisplayObjectivePacket($$2, p_136230_));
         }
      }

      for (PlayerScoreEntry $$3 : this.listPlayerScores(p_136230_)) {
         $$1.add(
            new ClientboundSetScorePacket(
               $$3.owner(), p_136230_.getName(), $$3.value(), Optional.ofNullable($$3.display()), Optional.ofNullable($$3.numberFormatOverride())
            )
         );
      }

      return $$1;
   }

   public void startTrackingObjective(Objective p_136232_) {
      List<Packet<?>> $$1 = this.getStartTrackingPackets(p_136232_);

      for (ServerPlayer $$2 : this.server.getPlayerList().getPlayers()) {
         for (Packet<?> $$3 : $$1) {
            $$2.connection.send($$3);
         }
      }

      this.trackedObjectives.add(p_136232_);
   }

   public List<Packet<?>> getStopTrackingPackets(Objective p_136234_) {
      List<Packet<?>> $$1 = Lists.newArrayList();
      $$1.add(new ClientboundSetObjectivePacket(p_136234_, 1));

      for (DisplaySlot $$2 : DisplaySlot.values()) {
         if (this.getDisplayObjective($$2) == p_136234_) {
            $$1.add(new ClientboundSetDisplayObjectivePacket($$2, p_136234_));
         }
      }

      return $$1;
   }

   public void stopTrackingObjective(Objective p_136236_) {
      List<Packet<?>> $$1 = this.getStopTrackingPackets(p_136236_);

      for (ServerPlayer $$2 : this.server.getPlayerList().getPlayers()) {
         for (Packet<?> $$3 : $$1) {
            $$2.connection.send($$3);
         }
      }

      this.trackedObjectives.remove(p_136236_);
   }

   public int getObjectiveDisplaySlotCount(Objective p_136238_) {
      int $$1 = 0;

      for (DisplaySlot $$2 : DisplaySlot.values()) {
         if (this.getDisplayObjective($$2) == p_136238_) {
            $$1++;
         }
      }

      return $$1;
   }

   public SavedData.Factory<ScoreboardSaveData> dataFactory() {
      return new SavedData.Factory<>(this::createData, this::createData, DataFixTypes.SAVED_DATA_SCOREBOARD);
   }

   private ScoreboardSaveData createData() {
      ScoreboardSaveData $$0 = new ScoreboardSaveData(this);
      this.addDirtyListener($$0::setDirty);
      return $$0;
   }

   private ScoreboardSaveData createData(CompoundTag p_180014_, HolderLookup.Provider p_324580_) {
      return this.createData().load(p_180014_, p_324580_);
   }

   public static enum Method {
      CHANGE,
      REMOVE;
   }
}
