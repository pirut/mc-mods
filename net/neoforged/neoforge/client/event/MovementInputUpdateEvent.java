package net.neoforged.neoforge.client.event;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class MovementInputUpdateEvent extends PlayerEvent {
   private final ClientInput input;

   @Internal
   public MovementInputUpdateEvent(Player player, ClientInput input) {
      super(player);
      this.input = input;
   }

   public ClientInput getInput() {
      return this.input;
   }
}
