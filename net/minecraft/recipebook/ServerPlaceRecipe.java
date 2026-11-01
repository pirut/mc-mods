package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

public class ServerPlaceRecipe<R extends Recipe<?>> {
   private static final int ITEM_NOT_FOUND = -1;
   private final Inventory inventory;
   private final ServerPlaceRecipe.CraftingMenuAccess<R> menu;
   private final boolean useMaxItems;
   private final int gridWidth;
   private final int gridHeight;
   private final List<Slot> inputGridSlots;
   private final List<Slot> slotsToClear;

   public static <I extends RecipeInput, R extends Recipe<I>> RecipeBookMenu.PostPlaceAction placeRecipe(
      ServerPlaceRecipe.CraftingMenuAccess<R> menu,
      int gridWidth,
      int gridHeight,
      List<Slot> inputGridSlots,
      List<Slot> slotsToClear,
      Inventory inventory,
      RecipeHolder<R> recipe,
      boolean useMaxItems,
      boolean isCreative
   ) {
      ServerPlaceRecipe<R> serverPlaceRecipe = new ServerPlaceRecipe<>(menu, inventory, useMaxItems, gridWidth, gridHeight, inputGridSlots, slotsToClear);
      if (!isCreative && !serverPlaceRecipe.testClearGrid()) {
         return RecipeBookMenu.PostPlaceAction.NOTHING;
      } else {
         StackedItemContents stackedItemContents = new StackedItemContents();
         inventory.fillStackedContents(stackedItemContents);
         menu.fillCraftSlotsStackedContents(stackedItemContents);
         return serverPlaceRecipe.tryPlaceRecipe(recipe, stackedItemContents);
      }
   }

   private ServerPlaceRecipe(
      ServerPlaceRecipe.CraftingMenuAccess<R> menu,
      Inventory inventory,
      boolean useMaxItems,
      int gridWidth,
      int gridHeight,
      List<Slot> inputGridSlots,
      List<Slot> slotsToClear
   ) {
      this.menu = menu;
      this.inventory = inventory;
      this.useMaxItems = useMaxItems;
      this.gridWidth = gridWidth;
      this.gridHeight = gridHeight;
      this.inputGridSlots = inputGridSlots;
      this.slotsToClear = slotsToClear;
   }

   private RecipeBookMenu.PostPlaceAction tryPlaceRecipe(RecipeHolder<R> recipe, StackedItemContents stackedItemContents) {
      if (stackedItemContents.canCraft(recipe.value(), null)) {
         this.placeRecipe(recipe, stackedItemContents);
         this.inventory.setChanged();
         return RecipeBookMenu.PostPlaceAction.NOTHING;
      } else {
         this.clearGrid();
         this.inventory.setChanged();
         return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
      }
   }

   private void clearGrid() {
      for (Slot slot : this.slotsToClear) {
         ItemStack itemStack = slot.getItem().copy();
         this.inventory.placeItemBackInInventory(itemStack, false);
         slot.set(itemStack);
      }

      this.menu.clearCraftingContent();
   }

   private void placeRecipe(RecipeHolder<R> recipe, StackedItemContents stackedItemContents) {
      boolean bl = this.menu.recipeMatches(recipe);
      int i = stackedItemContents.getBiggestCraftableStack(recipe.value(), null);
      if (bl) {
         for (Slot slot : this.inputGridSlots) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty() && Math.min(i, itemStack.getMaxStackSize()) < itemStack.getCount() + 1) {
               return;
            }
         }
      }

      int j = this.calculateAmountToCraft(i, bl);
      List<Holder<Item>> list = new ArrayList<>();
      if (stackedItemContents.canCraft(recipe.value(), j, list::add)) {
         int k = clampToMaxStackSize(j, list);
         if (k != j) {
            list.clear();
            if (!stackedItemContents.canCraft(recipe.value(), k, list::add)) {
               return;
            }
         }

         this.clearGrid();
         PlaceRecipeHelper.placeRecipe(
            this.gridWidth, this.gridHeight, recipe.value(), recipe.value().placementInfo().slotsToIngredientIndex(), (integer, jx, kx, l) -> {
               if (integer != -1) {
                  Slot slotx = this.inputGridSlots.get(jx);
                  Holder<Item> holder = list.get(integer);
                  int m = k;

                  while (m > 0) {
                     m = this.moveItemToGrid(slotx, holder, m);
                     if (m == -1) {
                        return;
                     }
                  }
               }
            }
         );
      }
   }

   private static int clampToMaxStackSize(int amount, List<Holder<Item>> items) {
      for (Holder<Item> holder : items) {
         amount = Math.min(amount, holder.value().getDefaultMaxStackSize());
      }

      return amount;
   }

   private int calculateAmountToCraft(int max, boolean recipeMatches) {
      if (this.useMaxItems) {
         return max;
      } else if (recipeMatches) {
         int i = Integer.MAX_VALUE;

         for (Slot slot : this.inputGridSlots) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty() && i > itemStack.getCount()) {
               i = itemStack.getCount();
            }
         }

         if (i != Integer.MAX_VALUE) {
            i++;
         }

         return i;
      } else {
         return 1;
      }
   }

   private int moveItemToGrid(Slot slot, Holder<Item> item, int count) {
      ItemStack itemStack = slot.getItem();
      int i = this.inventory.findSlotMatchingCraftingIngredient(item, itemStack);
      if (i == -1) {
         return -1;
      } else {
         ItemStack itemStack2 = this.inventory.getItem(i);
         ItemStack itemStack3;
         if (count < itemStack2.getCount()) {
            itemStack3 = this.inventory.removeItem(i, count);
         } else {
            itemStack3 = this.inventory.removeItemNoUpdate(i);
         }

         int j = itemStack3.getCount();
         if (itemStack.isEmpty()) {
            slot.set(itemStack3);
         } else {
            itemStack.grow(j);
         }

         return count - j;
      }
   }

   private boolean testClearGrid() {
      List<ItemStack> list = Lists.newArrayList();
      int i = this.getAmountOfFreeSlotsInInventory();

      for (Slot slot : this.inputGridSlots) {
         ItemStack itemStack = slot.getItem().copy();
         if (!itemStack.isEmpty()) {
            int j = this.inventory.getSlotWithRemainingSpace(itemStack);
            if (j == -1 && list.size() <= i) {
               for (ItemStack itemStack2 : list) {
                  if (ItemStack.isSameItem(itemStack2, itemStack)
                     && itemStack2.getCount() != itemStack2.getMaxStackSize()
                     && itemStack2.getCount() + itemStack.getCount() <= itemStack2.getMaxStackSize()) {
                     itemStack2.grow(itemStack.getCount());
                     itemStack.setCount(0);
                     break;
                  }
               }

               if (!itemStack.isEmpty()) {
                  if (list.size() >= i) {
                     return false;
                  }

                  list.add(itemStack);
               }
            } else if (j == -1) {
               return false;
            }
         }
      }

      return true;
   }

   private int getAmountOfFreeSlotsInInventory() {
      int i = 0;

      for (ItemStack itemStack : this.inventory.getNonEquipmentItems()) {
         if (itemStack.isEmpty()) {
            i++;
         }
      }

      return i;
   }

   public interface CraftingMenuAccess<T extends Recipe<?>> {
      void fillCraftSlotsStackedContents(StackedItemContents stackedItemContents);

      void clearCraftingContent();

      boolean recipeMatches(RecipeHolder<T> recipe);
   }
}
