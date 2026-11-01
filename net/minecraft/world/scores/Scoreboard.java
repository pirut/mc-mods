package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public class Scoreboard {
   public static final String HIDDEN_SCORE_PREFIX = "#";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<String, Objective> objectivesByName = new Object2ObjectOpenHashMap(16, 0.5F);
   private final Reference2ObjectMap<ObjectiveCriteria, List<Objective>> objectivesByCriteria = new Reference2ObjectOpenHashMap();
   private final Map<String, PlayerScores> playerScores = new Object2ObjectOpenHashMap(16, 0.5F);
   private final Map<DisplaySlot, Objective> displayObjectives = new EnumMap<>(DisplaySlot.class);
   private final Object2ObjectMap<String, PlayerTeam> teamsByName = new Object2ObjectOpenHashMap();
   private final Object2ObjectMap<String, PlayerTeam> teamsByPlayer = new Object2ObjectOpenHashMap();

   @Nullable
   public Objective getObjective(@Nullable String p_83478_) {
      return (Objective)this.objectivesByName.get(p_83478_);
   }

   public Objective addObjective(
      String p_83437_,
      ObjectiveCriteria p_83438_,
      Component p_83439_,
      ObjectiveCriteria.RenderType p_83440_,
      boolean p_313815_,
      @Nullable NumberFormat p_313909_
   ) {
      if (this.objectivesByName.containsKey(p_83437_)) {
         throw new IllegalArgumentException("An objective with the name '" + p_83437_ + "' already exists!");
      } else {
         Objective $$6 = new Objective(this, p_83437_, p_83438_, p_83439_, p_83440_, p_313815_, p_313909_);
         ((List)this.objectivesByCriteria.computeIfAbsent(p_83438_, p_314722_ -> Lists.newArrayList())).add($$6);
         this.objectivesByName.put(p_83437_, $$6);
         this.onObjectiveAdded($$6);
         return $$6;
      }
   }

   public final void forAllObjectives(ObjectiveCriteria p_83428_, ScoreHolder p_313805_, Consumer<ScoreAccess> p_83430_) {
      ((List)this.objectivesByCriteria.getOrDefault(p_83428_, Collections.emptyList()))
         .forEach(p_313676_ -> p_83430_.accept(this.getOrCreatePlayerScore(p_313805_, p_313676_, true)));
   }

   private PlayerScores getOrCreatePlayerInfo(String p_313892_) {
      return this.playerScores.computeIfAbsent(p_313892_, p_313683_ -> new PlayerScores());
   }

   public ScoreAccess getOrCreatePlayerScore(ScoreHolder p_313714_, Objective p_313948_) {
      return this.getOrCreatePlayerScore(p_313714_, p_313948_, false);
   }

   public ScoreAccess getOrCreatePlayerScore(final ScoreHolder p_313717_, final Objective p_83473_, boolean p_313939_) {
      final boolean $$3 = p_313939_ || !p_83473_.getCriteria().isReadOnly();
      PlayerScores $$4 = this.getOrCreatePlayerInfo(p_313717_.getScoreboardName());
      final MutableBoolean $$5 = new MutableBoolean();
      final Score $$6 = $$4.getOrCreate(p_83473_, p_313682_ -> $$5.setTrue());
      return new ScoreAccess() {
         @Override
         public int get() {
            return $$6.value();
         }

         @Override
         public void set(int p_313831_) {
            if (!$$3) {
               throw new IllegalStateException("Cannot modify read-only score");
            } else {
               boolean $$1 = $$5.isTrue();
               if (p_83473_.displayAutoUpdate()) {
                  Component $$2 = p_313717_.getDisplayName();
                  if ($$2 != null && !$$2.equals($$6.display())) {
                     $$6.display($$2);
                     $$1 = true;
                  }
               }

               if (p_313831_ != $$6.value()) {
                  $$6.value(p_313831_);
                  $$1 = true;
               }

               if ($$1) {
                  this.sendScoreToPlayers();
               }
            }
         }

         @Nullable
         @Override
         public Component display() {
            return $$6.display();
         }

         @Override
         public void display(@Nullable Component p_313826_) {
            if ($$5.isTrue() || !Objects.equals(p_313826_, $$6.display())) {
               $$6.display(p_313826_);
               this.sendScoreToPlayers();
            }
         }

         @Override
         public void numberFormatOverride(@Nullable NumberFormat p_313875_) {
            $$6.numberFormat(p_313875_);
            this.sendScoreToPlayers();
         }

         @Override
         public boolean locked() {
            return $$6.isLocked();
         }

         @Override
         public void unlock() {
            this.setLocked(false);
         }

         @Override
         public void lock() {
            this.setLocked(true);
         }

         private void setLocked(boolean p_313822_) {
            $$6.setLocked(p_313822_);
            if ($$5.isTrue()) {
               this.sendScoreToPlayers();
            }

            Scoreboard.this.onScoreLockChanged(p_313717_, p_83473_);
         }

         private void sendScoreToPlayers() {
            Scoreboard.this.onScoreChanged(p_313717_, p_83473_, $$6);
            $$5.setFalse();
         }
      };
   }

   @Nullable
   public ReadOnlyScoreInfo getPlayerScoreInfo(ScoreHolder p_313711_, Objective p_313813_) {
      PlayerScores $$2 = this.playerScores.get(p_313711_.getScoreboardName());
      return $$2 != null ? $$2.get(p_313813_) : null;
   }

   public Collection<PlayerScoreEntry> listPlayerScores(Objective p_313802_) {
      List<PlayerScoreEntry> $$1 = new ArrayList<>();
      this.playerScores.forEach((p_313669_, p_313670_) -> {
         Score $$4 = p_313670_.get(p_313802_);
         if ($$4 != null) {
            $$1.add(new PlayerScoreEntry(p_313669_, $$4.value(), $$4.display(), $$4.numberFormat()));
         }
      });
      return $$1;
   }

   public Collection<Objective> getObjectives() {
      return this.objectivesByName.values();
   }

   public Collection<String> getObjectiveNames() {
      return this.objectivesByName.keySet();
   }

   public Collection<ScoreHolder> getTrackedPlayers() {
      return this.playerScores.keySet().stream().map(ScoreHolder::forNameOnly).toList();
   }

   public void resetAllPlayerScores(ScoreHolder p_313823_) {
      PlayerScores $$1 = this.playerScores.remove(p_313823_.getScoreboardName());
      if ($$1 != null) {
         this.onPlayerRemoved(p_313823_);
      }
   }

   public void resetSinglePlayerScore(ScoreHolder p_313783_, Objective p_313928_) {
      PlayerScores $$2 = this.playerScores.get(p_313783_.getScoreboardName());
      if ($$2 != null) {
         boolean $$3 = $$2.remove(p_313928_);
         if (!$$2.hasScores()) {
            PlayerScores $$4 = this.playerScores.remove(p_313783_.getScoreboardName());
            if ($$4 != null) {
               this.onPlayerRemoved(p_313783_);
            }
         } else if ($$3) {
            this.onPlayerScoreRemoved(p_313783_, p_313928_);
         }
      }
   }

   public Object2IntMap<Objective> listPlayerScores(ScoreHolder p_313893_) {
      PlayerScores $$1 = this.playerScores.get(p_313893_.getScoreboardName());
      return $$1 != null ? $$1.listScores() : Object2IntMaps.emptyMap();
   }

   public void removeObjective(Objective p_83503_) {
      this.objectivesByName.remove(p_83503_.getName());

      for (DisplaySlot $$1 : DisplaySlot.values()) {
         if (this.getDisplayObjective($$1) == p_83503_) {
            this.setDisplayObjective($$1, null);
         }
      }

      List<Objective> $$2 = (List<Objective>)this.objectivesByCriteria.get(p_83503_.getCriteria());
      if ($$2 != null) {
         $$2.remove(p_83503_);
      }

      for (PlayerScores $$3 : this.playerScores.values()) {
         $$3.remove(p_83503_);
      }

      this.onObjectiveRemoved(p_83503_);
   }

   public void setDisplayObjective(DisplaySlot p_296205_, @Nullable Objective p_83419_) {
      this.displayObjectives.put(p_296205_, p_83419_);
   }

   @Nullable
   public Objective getDisplayObjective(DisplaySlot p_295165_) {
      return this.displayObjectives.get(p_295165_);
   }

   @Nullable
   public PlayerTeam getPlayerTeam(String p_83490_) {
      return (PlayerTeam)this.teamsByName.get(p_83490_);
   }

   public PlayerTeam addPlayerTeam(String p_83493_) {
      PlayerTeam $$1 = this.getPlayerTeam(p_83493_);
      if ($$1 != null) {
         LOGGER.warn("Requested creation of existing team '{}'", p_83493_);
         return $$1;
      } else {
         $$1 = new PlayerTeam(this, p_83493_);
         this.teamsByName.put(p_83493_, $$1);
         this.onTeamAdded($$1);
         return $$1;
      }
   }

   public void removePlayerTeam(PlayerTeam p_83476_) {
      this.teamsByName.remove(p_83476_.getName());

      for (String $$1 : p_83476_.getPlayers()) {
         this.teamsByPlayer.remove($$1);
      }

      this.onTeamRemoved(p_83476_);
   }

   public boolean addPlayerToTeam(String p_83434_, PlayerTeam p_83435_) {
      if (this.getPlayersTeam(p_83434_) != null) {
         this.removePlayerFromTeam(p_83434_);
      }

      this.teamsByPlayer.put(p_83434_, p_83435_);
      return p_83435_.getPlayers().add(p_83434_);
   }

   public boolean removePlayerFromTeam(String p_83496_) {
      PlayerTeam $$1 = this.getPlayersTeam(p_83496_);
      if ($$1 != null) {
         this.removePlayerFromTeam(p_83496_, $$1);
         return true;
      } else {
         return false;
      }
   }

   public void removePlayerFromTeam(String p_83464_, PlayerTeam p_83465_) {
      if (this.getPlayersTeam(p_83464_) != p_83465_) {
         throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + p_83465_.getName() + "'.");
      } else {
         this.teamsByPlayer.remove(p_83464_);
         p_83465_.getPlayers().remove(p_83464_);
      }
   }

   public Collection<String> getTeamNames() {
      return this.teamsByName.keySet();
   }

   public Collection<PlayerTeam> getPlayerTeams() {
      return this.teamsByName.values();
   }

   @Nullable
   public PlayerTeam getPlayersTeam(String p_83501_) {
      return (PlayerTeam)this.teamsByPlayer.get(p_83501_);
   }

   public void onObjectiveAdded(Objective p_83422_) {
   }

   public void onObjectiveChanged(Objective p_83455_) {
   }

   public void onObjectiveRemoved(Objective p_83467_) {
   }

   protected void onScoreChanged(ScoreHolder p_313729_, Objective p_313930_, Score p_83424_) {
   }

   protected void onScoreLockChanged(ScoreHolder p_313727_, Objective p_313708_) {
   }

   public void onPlayerRemoved(ScoreHolder p_313917_) {
   }

   public void onPlayerScoreRemoved(ScoreHolder p_313899_, Objective p_83433_) {
   }

   public void onTeamAdded(PlayerTeam p_83423_) {
   }

   public void onTeamChanged(PlayerTeam p_83456_) {
   }

   public void onTeamRemoved(PlayerTeam p_83468_) {
   }

   public void entityRemoved(Entity p_83421_) {
      if (!(p_83421_ instanceof Player) && !p_83421_.isAlive()) {
         this.resetAllPlayerScores(p_83421_);
         this.removePlayerFromTeam(p_83421_.getScoreboardName());
      }
   }

   protected ListTag savePlayerScores(HolderLookup.Provider p_331535_) {
      ListTag $$1 = new ListTag();
      this.playerScores.forEach((p_330203_, p_330204_) -> p_330204_.listRawScores().forEach((p_330199_, p_330200_) -> {
         CompoundTag $$5 = p_330200_.write(p_331535_);
         $$5.putString("Name", p_330203_);
         $$5.putString("Objective", p_330199_.getName());
         $$1.add($$5);
      }));
      return $$1;
   }

   protected void loadPlayerScores(ListTag p_83446_, HolderLookup.Provider p_330625_) {
      for (int $$2 = 0; $$2 < p_83446_.size(); $$2++) {
         CompoundTag $$3 = p_83446_.getCompound($$2);
         Score $$4 = Score.read($$3, p_330625_);
         String $$5 = $$3.getString("Name");
         String $$6 = $$3.getString("Objective");
         Objective $$7 = this.getObjective($$6);
         if ($$7 == null) {
            LOGGER.error("Unknown objective {} for name {}, ignoring", $$6, $$5);
         } else {
            this.getOrCreatePlayerInfo($$5).setScore($$7, $$4);
         }
      }
   }
}
