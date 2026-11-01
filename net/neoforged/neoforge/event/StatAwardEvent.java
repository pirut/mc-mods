package net.neoforged.neoforge.event;

import net.minecraft.stats.Stat;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class StatAwardEvent extends PlayerEvent implements ICancellableEvent {
   private Stat<?> stat;
   private int value;

   @Internal
   public StatAwardEvent(Player player, Stat<?> stat, int value) {
      super(player);
      this.stat = stat;
      this.value = value;
   }

   public Stat<?> getStat() {
      return this.stat;
   }

   public void setStat(Stat<?> stat) {
      this.stat = stat;
   }

   public int getValue() {
      return this.value;
   }

   public void setValue(int value) {
      this.value = value;
   }
}
