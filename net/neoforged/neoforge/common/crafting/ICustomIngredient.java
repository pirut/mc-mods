package net.neoforged.neoforge.common.crafting;

import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface ICustomIngredient {
   boolean test(ItemStack var1);

   Stream<ItemStack> getItems();

   boolean isSimple();

   IngredientType<?> getType();

   @NonExtendable
   default Ingredient toVanilla() {
      return new Ingredient(this);
   }
}
