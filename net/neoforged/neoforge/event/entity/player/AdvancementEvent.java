package net.neoforged.neoforge.event.entity.player;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.world.entity.player.Player;

public abstract class AdvancementEvent extends PlayerEvent {
   private final AdvancementHolder advancement;

   public AdvancementEvent(Player player, AdvancementHolder advancement) {
      super(player);
      this.advancement = advancement;
   }

   public AdvancementHolder getAdvancement() {
      return this.advancement;
   }

   public static class AdvancementEarnEvent extends AdvancementEvent {
      public AdvancementEarnEvent(Player player, AdvancementHolder earned) {
         super(player, earned);
      }
   }

   public static class AdvancementProgressEvent extends AdvancementEvent {
      private final AdvancementProgress advancementProgress;
      private final String criterionName;
      private final AdvancementEvent.AdvancementProgressEvent.ProgressType progressType;

      public AdvancementProgressEvent(
         Player player,
         AdvancementHolder progressed,
         AdvancementProgress advancementProgress,
         String criterionName,
         AdvancementEvent.AdvancementProgressEvent.ProgressType progressType
      ) {
         super(player, progressed);
         this.advancementProgress = advancementProgress;
         this.criterionName = criterionName;
         this.progressType = progressType;
      }

      public AdvancementProgress getAdvancementProgress() {
         return this.advancementProgress;
      }

      public String getCriterionName() {
         return this.criterionName;
      }

      public AdvancementEvent.AdvancementProgressEvent.ProgressType getProgressType() {
         return this.progressType;
      }

      public static enum ProgressType {
         GRANT,
         REVOKE;
      }
   }
}
