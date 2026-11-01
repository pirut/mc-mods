package net.neoforged.neoforge.client.event;

import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerHeartTypeEvent extends PlayerEvent {
   private final Gui.HeartType originalType;
   private Gui.HeartType type;

   public PlayerHeartTypeEvent(Player player, Gui.HeartType type) {
      super(player);
      this.type = type;
      this.originalType = type;
   }

   public Gui.HeartType getOriginalType() {
      return this.originalType;
   }

   public Gui.HeartType getType() {
      return this.type;
   }

   public void setType(Gui.HeartType type) {
      this.type = type;
   }
}
