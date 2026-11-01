package net.minecraft.world.item;

import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class PickaxeItem extends DiggerItem {
   public PickaxeItem(Tier p_42961_, Item.Properties p_42964_) {
      super(p_42961_, BlockTags.MINEABLE_WITH_PICKAXE, p_42964_);
   }

   @Override
   public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
      return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility);
   }
}
