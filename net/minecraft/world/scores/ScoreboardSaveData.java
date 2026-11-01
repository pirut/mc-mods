package net.minecraft.world.scores;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class ScoreboardSaveData extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String FILE_ID = "scoreboard";
   private final Scoreboard scoreboard;

   public ScoreboardSaveData(Scoreboard p_166101_) {
      this.scoreboard = p_166101_;
   }

   public ScoreboardSaveData load(CompoundTag p_166103_, HolderLookup.Provider p_332093_) {
      this.loadObjectives(p_166103_.getList("Objectives", 10), p_332093_);
      this.scoreboard.loadPlayerScores(p_166103_.getList("PlayerScores", 10), p_332093_);
      if (p_166103_.contains("DisplaySlots", 10)) {
         this.loadDisplaySlots(p_166103_.getCompound("DisplaySlots"));
      }

      if (p_166103_.contains("Teams", 9)) {
         this.loadTeams(p_166103_.getList("Teams", 10), p_332093_);
      }

      return this;
   }

   private void loadTeams(ListTag p_83525_, HolderLookup.Provider p_331819_) {
      for (int $$2 = 0; $$2 < p_83525_.size(); $$2++) {
         CompoundTag $$3 = p_83525_.getCompound($$2);
         String $$4 = $$3.getString("Name");
         PlayerTeam $$5 = this.scoreboard.addPlayerTeam($$4);
         Component $$6 = Component.Serializer.fromJson($$3.getString("DisplayName"), p_331819_);
         if ($$6 != null) {
            $$5.setDisplayName($$6);
         }

         if ($$3.contains("TeamColor", 8)) {
            $$5.setColor(ChatFormatting.getByName($$3.getString("TeamColor")));
         }

         if ($$3.contains("AllowFriendlyFire", 99)) {
            $$5.setAllowFriendlyFire($$3.getBoolean("AllowFriendlyFire"));
         }

         if ($$3.contains("SeeFriendlyInvisibles", 99)) {
            $$5.setSeeFriendlyInvisibles($$3.getBoolean("SeeFriendlyInvisibles"));
         }

         if ($$3.contains("MemberNamePrefix", 8)) {
            Component $$7 = Component.Serializer.fromJson($$3.getString("MemberNamePrefix"), p_331819_);
            if ($$7 != null) {
               $$5.setPlayerPrefix($$7);
            }
         }

         if ($$3.contains("MemberNameSuffix", 8)) {
            Component $$8 = Component.Serializer.fromJson($$3.getString("MemberNameSuffix"), p_331819_);
            if ($$8 != null) {
               $$5.setPlayerSuffix($$8);
            }
         }

         if ($$3.contains("NameTagVisibility", 8)) {
            Team.Visibility $$9 = Team.Visibility.byName($$3.getString("NameTagVisibility"));
            if ($$9 != null) {
               $$5.setNameTagVisibility($$9);
            }
         }

         if ($$3.contains("DeathMessageVisibility", 8)) {
            Team.Visibility $$10 = Team.Visibility.byName($$3.getString("DeathMessageVisibility"));
            if ($$10 != null) {
               $$5.setDeathMessageVisibility($$10);
            }
         }

         if ($$3.contains("CollisionRule", 8)) {
            Team.CollisionRule $$11 = Team.CollisionRule.byName($$3.getString("CollisionRule"));
            if ($$11 != null) {
               $$5.setCollisionRule($$11);
            }
         }

         this.loadTeamPlayers($$5, $$3.getList("Players", 8));
      }
   }

   private void loadTeamPlayers(PlayerTeam p_83515_, ListTag p_83516_) {
      for (int $$2 = 0; $$2 < p_83516_.size(); $$2++) {
         this.scoreboard.addPlayerToTeam(p_83516_.getString($$2), p_83515_);
      }
   }

   private void loadDisplaySlots(CompoundTag p_83531_) {
      for (String $$1 : p_83531_.getAllKeys()) {
         DisplaySlot $$2 = DisplaySlot.CODEC.byName($$1);
         if ($$2 != null) {
            String $$3 = p_83531_.getString($$1);
            Objective $$4 = this.scoreboard.getObjective($$3);
            this.scoreboard.setDisplayObjective($$2, $$4);
         }
      }
   }

   private void loadObjectives(ListTag p_83529_, HolderLookup.Provider p_331574_) {
      for (int $$2 = 0; $$2 < p_83529_.size(); $$2++) {
         CompoundTag $$3 = p_83529_.getCompound($$2);
         String $$4 = $$3.getString("CriteriaName");
         ObjectiveCriteria $$5 = ObjectiveCriteria.byName($$4).orElseGet(() -> {
            LOGGER.warn("Unknown scoreboard criteria {}, replacing with {}", $$4, ObjectiveCriteria.DUMMY.getName());
            return ObjectiveCriteria.DUMMY;
         });
         String $$6 = $$3.getString("Name");
         Component $$7 = Component.Serializer.fromJson($$3.getString("DisplayName"), p_331574_);
         ObjectiveCriteria.RenderType $$8 = ObjectiveCriteria.RenderType.byId($$3.getString("RenderType"));
         boolean $$9 = $$3.getBoolean("display_auto_update");
         NumberFormat $$10 = (NumberFormat)NumberFormatTypes.CODEC
            .parse(p_331574_.createSerializationContext(NbtOps.INSTANCE), $$3.get("format"))
            .result()
            .orElse(null);
         this.scoreboard.addObjective($$6, $$5, $$7, $$8, $$9, $$10);
      }
   }

   @Override
   public CompoundTag save(CompoundTag p_83527_, HolderLookup.Provider p_324474_) {
      p_83527_.put("Objectives", this.saveObjectives(p_324474_));
      p_83527_.put("PlayerScores", this.scoreboard.savePlayerScores(p_324474_));
      p_83527_.put("Teams", this.saveTeams(p_324474_));
      this.saveDisplaySlots(p_83527_);
      return p_83527_;
   }

   private ListTag saveTeams(HolderLookup.Provider p_331247_) {
      ListTag $$1 = new ListTag();

      for (PlayerTeam $$3 : this.scoreboard.getPlayerTeams()) {
         CompoundTag $$4 = new CompoundTag();
         $$4.putString("Name", $$3.getName());
         $$4.putString("DisplayName", Component.Serializer.toJson($$3.getDisplayName(), p_331247_));
         if ($$3.getColor().getId() >= 0) {
            $$4.putString("TeamColor", $$3.getColor().getName());
         }

         $$4.putBoolean("AllowFriendlyFire", $$3.isAllowFriendlyFire());
         $$4.putBoolean("SeeFriendlyInvisibles", $$3.canSeeFriendlyInvisibles());
         $$4.putString("MemberNamePrefix", Component.Serializer.toJson($$3.getPlayerPrefix(), p_331247_));
         $$4.putString("MemberNameSuffix", Component.Serializer.toJson($$3.getPlayerSuffix(), p_331247_));
         $$4.putString("NameTagVisibility", $$3.getNameTagVisibility().name);
         $$4.putString("DeathMessageVisibility", $$3.getDeathMessageVisibility().name);
         $$4.putString("CollisionRule", $$3.getCollisionRule().name);
         ListTag $$5 = new ListTag();

         for (String $$6 : $$3.getPlayers()) {
            $$5.add(StringTag.valueOf($$6));
         }

         $$4.put("Players", $$5);
         $$1.add($$4);
      }

      return $$1;
   }

   private void saveDisplaySlots(CompoundTag p_83533_) {
      CompoundTag $$1 = new CompoundTag();

      for (DisplaySlot $$2 : DisplaySlot.values()) {
         Objective $$3 = this.scoreboard.getDisplayObjective($$2);
         if ($$3 != null) {
            $$1.putString($$2.getSerializedName(), $$3.getName());
         }
      }

      if (!$$1.isEmpty()) {
         p_83533_.put("DisplaySlots", $$1);
      }
   }

   private ListTag saveObjectives(HolderLookup.Provider p_330495_) {
      ListTag $$1 = new ListTag();

      for (Objective $$3 : this.scoreboard.getObjectives()) {
         CompoundTag $$4 = new CompoundTag();
         $$4.putString("Name", $$3.getName());
         $$4.putString("CriteriaName", $$3.getCriteria().getName());
         $$4.putString("DisplayName", Component.Serializer.toJson($$3.getDisplayName(), p_330495_));
         $$4.putString("RenderType", $$3.getRenderType().getId());
         $$4.putBoolean("display_auto_update", $$3.displayAutoUpdate());
         NumberFormat $$5 = $$3.numberFormat();
         if ($$5 != null) {
            NumberFormatTypes.CODEC
               .encodeStart(p_330495_.createSerializationContext(NbtOps.INSTANCE), $$5)
               .ifSuccess(p_313685_ -> $$4.put("format", p_313685_));
         }

         $$1.add($$4);
      }

      return $$1;
   }
}
