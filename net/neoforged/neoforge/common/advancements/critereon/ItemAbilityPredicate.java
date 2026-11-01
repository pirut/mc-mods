package net.neoforged.neoforge.common.advancements.critereon;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbility;

public record ItemAbilityPredicate(ItemAbility action) implements ItemSubPredicate {
   public static final Codec<ItemAbilityPredicate> CODEC = ItemAbility.CODEC.xmap(ItemAbilityPredicate::new, ItemAbilityPredicate::action);
   public static final ItemSubPredicate.Type<ItemAbilityPredicate> TYPE = new ItemSubPredicate.Type<>(CODEC);

   @Override
   public boolean matches(ItemStack stack) {
      return stack.canPerformAction(this.action);
   }
}
