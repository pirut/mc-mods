package net.minecraft.world.entity.player;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public class StackedItemContents {
   private final StackedContents<Holder<Item>> raw = new StackedContents<>();

   public void accountSimpleStack(ItemStack p_363120_) {
      if (Inventory.isUsableForCrafting(p_363120_)) {
         this.accountStack(p_363120_);
      }
   }

   public void accountStack(ItemStack p_364939_) {
      this.accountStack(p_364939_, p_364939_.getMaxStackSize());
   }

   public void accountStack(ItemStack p_365466_, int p_365492_) {
      if (!p_365466_.isEmpty()) {
         int i = Math.min(p_365492_, p_365466_.getCount());
         this.raw.account(p_365466_.getItemHolder(), i);
      }
   }

   public boolean canCraft(Recipe<?> p_362014_, @Nullable StackedContents.Output<Holder<Item>> p_362141_) {
      return this.canCraft(p_362014_, 1, p_362141_);
   }

   public boolean canCraft(Recipe<?> p_364477_, int p_360429_, @Nullable StackedContents.Output<Holder<Item>> p_362813_) {
      PlacementInfo placementinfo = p_364477_.placementInfo();
      return placementinfo.isImpossibleToPlace() ? false : this.canCraft(placementinfo.ingredients(), p_360429_, p_362813_);
   }

   public boolean canCraft(List<? extends StackedContents.IngredientInfo<Holder<Item>>> p_379390_, @Nullable StackedContents.Output<Holder<Item>> p_379587_) {
      return this.canCraft(p_379390_, 1, p_379587_);
   }

   private boolean canCraft(
      List<? extends StackedContents.IngredientInfo<Holder<Item>>> p_379516_, int p_380017_, @Nullable StackedContents.Output<Holder<Item>> p_380092_
   ) {
      return this.raw.tryPick(p_379516_, p_380017_, p_380092_);
   }

   public int getBiggestCraftableStack(Recipe<?> p_363539_, @Nullable StackedContents.Output<Holder<Item>> p_363083_) {
      return this.getBiggestCraftableStack(p_363539_, Integer.MAX_VALUE, p_363083_);
   }

   public int getBiggestCraftableStack(Recipe<?> p_362520_, int p_361940_, @Nullable StackedContents.Output<Holder<Item>> p_362354_) {
      return this.raw.tryPickAll(p_362520_.placementInfo().ingredients(), p_361940_, p_362354_);
   }

   public void clear() {
      this.raw.clear();
   }
}
