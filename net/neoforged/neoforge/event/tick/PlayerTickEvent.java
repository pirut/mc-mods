package net.neoforged.neoforge.event.tick;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public abstract class PlayerTickEvent extends PlayerEvent {
   protected PlayerTickEvent(Player player) {
      super(player);
   }

   public static class Post extends PlayerTickEvent {
      public Post(Player player) {
         super(player);
      }
   }

   public static class Pre extends PlayerTickEvent {
      public Pre(Player player) {
         super(player);
      }
   }
}
