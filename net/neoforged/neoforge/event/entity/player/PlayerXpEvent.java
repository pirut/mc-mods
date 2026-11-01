package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class PlayerXpEvent extends PlayerEvent {
   public PlayerXpEvent(Player player) {
      super(player);
   }

   public static class LevelChange extends PlayerXpEvent implements ICancellableEvent {
      private int levels;

      public LevelChange(Player player, int levels) {
         super(player);
         this.levels = levels;
      }

      public int getLevels() {
         return this.levels;
      }

      public void setLevels(int levels) {
         this.levels = levels;
      }
   }

   public static class PickupXp extends PlayerXpEvent implements ICancellableEvent {
      private final ExperienceOrb orb;

      public PickupXp(Player player, ExperienceOrb orb) {
         super(player);
         this.orb = orb;
      }

      public ExperienceOrb getOrb() {
         return this.orb;
      }
   }

   public static class XpChange extends PlayerXpEvent implements ICancellableEvent {
      private int amount;

      public XpChange(Player player, int amount) {
         super(player);
         this.amount = amount;
      }

      public int getAmount() {
         return this.amount;
      }

      public void setAmount(int amount) {
         this.amount = amount;
      }
   }
}
