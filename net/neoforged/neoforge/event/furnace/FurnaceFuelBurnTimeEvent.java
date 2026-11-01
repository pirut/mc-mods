package net.neoforged.neoforge.event.furnace;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FuelValues;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jspecify.annotations.Nullable;

public class FurnaceFuelBurnTimeEvent extends Event implements ICancellableEvent {
   private final ItemStack itemStack;
   @Nullable
   private final RecipeType<?> recipeType;
   private final FuelValues fuelValues;
   private int burnTime;

   public FurnaceFuelBurnTimeEvent(ItemStack itemStack, int burnTime, @Nullable RecipeType<?> recipeType, FuelValues fuelValues) {
      this.itemStack = itemStack;
      this.burnTime = burnTime;
      this.recipeType = recipeType;
      this.fuelValues = fuelValues;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   @Nullable
   public RecipeType<?> getRecipeType() {
      return this.recipeType;
   }

   public FuelValues getFuelValues() {
      return this.fuelValues;
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
