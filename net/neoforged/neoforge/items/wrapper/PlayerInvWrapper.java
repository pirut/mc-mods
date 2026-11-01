package net.neoforged.neoforge.items.wrapper;

import net.minecraft.world.entity.player.Inventory;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class PlayerInvWrapper extends CombinedInvWrapper {
   public PlayerInvWrapper(Inventory inv) {
      super(new PlayerMainInvWrapper(inv), new PlayerArmorInvWrapper(inv), new PlayerOffhandInvWrapper(inv));
   }
}
