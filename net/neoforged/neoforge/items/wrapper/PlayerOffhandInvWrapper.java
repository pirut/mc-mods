package net.neoforged.neoforge.items.wrapper;

import net.minecraft.world.entity.player.Inventory;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class PlayerOffhandInvWrapper extends RangedWrapper {
   public PlayerOffhandInvWrapper(Inventory inv) {
      super(new InvWrapper(inv), 40, 41);
   }
}
