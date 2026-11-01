package net.neoforged.neoforge.common.crafting;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface ICustomIngredient {
   boolean test(ItemStack var1);

   Stream<Holder<Item>> items();

   boolean isSimple();

   IngredientType<?> getType();

   default SlotDisplay display() {
      return new SlotDisplay.Composite(this.items().map(Ingredient::displayForSingleItem).toList());
   }

   @NonExtendable
   default Ingredient toVanilla() {
      return new Ingredient(this);
   }
}
