package net.neoforged.neoforge.event.furnace;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public class FurnaceFuelBurnTimeEvent extends Event implements ICancellableEvent {
   private final ItemStack itemStack;
   @Nullable
   private final RecipeType<?> recipeType;
   private int burnTime;

   public FurnaceFuelBurnTimeEvent(ItemStack itemStack, int burnTime, @Nullable RecipeType<?> recipeType) {
      this.itemStack = itemStack;
      this.burnTime = burnTime;
      this.recipeType = recipeType;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   @Nullable
   public RecipeType<?> getRecipeType() {
      return this.recipeType;
   }

   public void setBurnTime(int burnTime) {
      if (burnTime >= 0) {
         this.burnTime = burnTime;
         this.setCanceled(true);
      }
   }

   public int getBurnTime() {
      return this.burnTime;
   }
}
