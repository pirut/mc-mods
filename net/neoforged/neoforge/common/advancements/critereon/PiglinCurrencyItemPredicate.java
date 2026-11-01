package net.neoforged.neoforge.common.advancements.critereon;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.ItemStack;

public class PiglinCurrencyItemPredicate implements ItemSubPredicate {
   public static final PiglinCurrencyItemPredicate INSTANCE = new PiglinCurrencyItemPredicate();
   public static final Codec<PiglinCurrencyItemPredicate> CODEC = Codec.unit(INSTANCE);
   public static final ItemSubPredicate.Type<PiglinCurrencyItemPredicate> TYPE = new ItemSubPredicate.Type<>(CODEC);

   private PiglinCurrencyItemPredicate() {
   }

   @Override
   public boolean matches(ItemStack stack) {
      return stack.isPiglinCurrency();
   }
}
