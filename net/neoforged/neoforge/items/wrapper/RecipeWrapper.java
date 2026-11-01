package net.neoforged.neoforge.items.wrapper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.IItemHandler;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class RecipeWrapper implements RecipeInput {
   protected final IItemHandler inv;

   public RecipeWrapper(IItemHandler inv) {
      this.inv = inv;
   }

   @Override
   public int size() {
      return this.inv.getSlots();
   }

   @Override
   public ItemStack getItem(int slot) {
      return this.inv.getStackInSlot(slot);
   }
}
