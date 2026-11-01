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
      ServerPlaceRecipe.CraftingMenuAccess<R> p_361341_,
      int p_361512_,
      int p_362929_,
      List<Slot> p_364825_,
      List<Slot> p_364661_,
      Inventory p_364618_,
      RecipeHolder<R> p_363024_,
      boolean p_360549_,
      boolean p_361381_
   ) {
      ServerPlaceRecipe<R> serverplacerecipe = new ServerPlaceRecipe<>(p_361341_, p_364618_, p_360549_, p_361512_, p_362929_, p_364825_, p_364661_);
      if (!p_361381_ && !serverplacerecipe.testClearGrid()) {
         return RecipeBookMenu.PostPlaceAction.NOTHING;
      } else {
         StackedItemContents stackeditemcontents = new StackedItemContents();
         p_364618_.fillStackedContents(stackeditemcontents);
         p_361341_.fillCraftSlotsStackedContents(stackeditemcontents);
         return serverplacerecipe.tryPlaceRecipe(p_363024_, stackeditemcontents);
      }
   }

   private ServerPlaceRecipe(
      ServerPlaceRecipe.CraftingMenuAccess<R> p_363275_,
      Inventory p_361371_,
      boolean p_364633_,
      int p_362980_,
      int p_361285_,
      List<Slot> p_364759_,
      List<Slot> p_361301_
   ) {
      this.menu = p_363275_;
      this.inventory = p_361371_;
      this.useMaxItems = p_364633_;
      this.gridWidth = p_362980_;
      this.gridHeight = p_361285_;
      this.inputGridSlots = p_364759_;
      this.slotsToClear = p_361301_;
   }

   private RecipeBookMenu.PostPlaceAction tryPlaceRecipe(RecipeHolder<R> p_362187_, StackedItemContents p_361415_) {
      if (p_361415_.canCraft(p_362187_.value(), null)) {
         this.placeRecipe(p_362187_, p_361415_);
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
         ItemStack itemstack = slot.getItem().copy();
         this.inventory.placeItemBackInInventory(itemstack, false);
         slot.set(itemstack);
      }

      this.menu.clearCraftingContent();
   }

   private void placeRecipe(RecipeHolder<R> p_364551_, StackedItemContents p_362416_) {
      boolean flag = this.menu.recipeMatches(p_364551_);
      int i = p_362416_.getBiggestCraftableStack(p_364551_.value(), null);
      if (flag) {
         for (Slot slot : this.inputGridSlots) {
            ItemStack itemstack = slot.getItem();
            if (!itemstack.isEmpty() && Math.min(i, itemstack.getMaxStackSize()) < itemstack.getCount() + 1) {
               return;
            }
         }
      }

      int j = this.calculateAmountToCraft(i, flag);
      List<Holder<Item>> list = new ArrayList<>();
      if (p_362416_.canCraft(p_364551_.value(), j, list::add)) {
         int k = clampToMaxStackSize(j, list);
         if (k != j) {
            list.clear();
            if (!p_362416_.canCraft(p_364551_.value(), k, list::add)) {
               return;
            }
         }

         this.clearGrid();
         PlaceRecipeHelper.placeRecipe(
            this.gridWidth,
            this.gridHeight,
            p_364551_.value(),
            p_364551_.value().placementInfo().slotsToIngredientIndex(),
            (p_389375_, p_389376_, p_389377_, p_389378_) -> {
               if (p_389375_ != -1) {
                  Slot slot1 = this.inputGridSlots.get(p_389376_);
                  Holder<Item> holder = list.get(p_389375_);
                  int l = k;

                  while (l > 0) {
                     l = this.moveItemToGrid(slot1, holder, l);
                     if (l == -1) {
                        return;
                     }
                  }
               }
            }
         );
      }
   }

   private static int clampToMaxStackSize(int p_389711_, List<Holder<Item>> p_389467_) {
      for (Holder<Item> holder : p_389467_) {
         p_389711_ = Math.min(p_389711_, holder.value().getDefaultMaxStackSize());
      }

      return p_389711_;
   }

   private int calculateAmountToCraft(int p_364430_, boolean p_362919_) {
      if (this.useMaxItems) {
         return p_364430_;
      } else if (p_362919_) {
         int i = Integer.MAX_VALUE;

         for (Slot slot : this.inputGridSlots) {
            ItemStack itemstack = slot.getItem();
            if (!itemstack.isEmpty() && i > itemstack.getCount()) {
               i = itemstack.getCount();
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

   private int moveItemToGrid(Slot p_135439_, Holder<Item> p_360762_, int p_346157_) {
      ItemStack itemstack = p_135439_.getItem();
      int i = this.inventory.findSlotMatchingCraftingIngredient(p_360762_, itemstack);
      if (i == -1) {
         return -1;
      } else {
         ItemStack itemstack1 = this.inventory.getItem(i);
         ItemStack itemstack2;
         if (p_346157_ < itemstack1.getCount()) {
            itemstack2 = this.inventory.removeItem(i, p_346157_);
         } else {
            itemstack2 = this.inventory.removeItemNoUpdate(i);
         }

         int j = itemstack2.getCount();
         if (itemstack.isEmpty()) {
            p_135439_.set(itemstack2);
         } else {
            itemstack.grow(j);
         }

         return p_346157_ - j;
      }
   }

   private boolean testClearGrid() {
      List<ItemStack> list = Lists.newArrayList();
      int i = this.getAmountOfFreeSlotsInInventory();

      for (Slot slot : this.inputGridSlots) {
         ItemStack itemstack = slot.getItem().copy();
         if (!itemstack.isEmpty()) {
            int j = this.inventory.getSlotWithRemainingSpace(itemstack);
            if (j == -1 && list.size() <= i) {
               for (ItemStack itemstack1 : list) {
                  if (ItemStack.isSameItem(itemstack1, itemstack)
                     && itemstack1.getCount() != itemstack1.getMaxStackSize()
                     && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()) {
                     itemstack1.grow(itemstack.getCount());
                     itemstack.setCount(0);
                     break;
                  }
               }

               if (!itemstack.isEmpty()) {
                  if (list.size() >= i) {
                     return false;
                  }

                  list.add(itemstack);
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

      for (ItemStack itemstack : this.inventory.getNonEquipmentItems()) {
         if (itemstack.isEmpty()) {
            i++;
         }
      }

      return i;
   }

   public interface CraftingMenuAccess<T extends Recipe<?>> {
      void fillCraftSlotsStackedContents(StackedItemContents var1);

      void clearCraftingContent();

      boolean recipeMatches(RecipeHolder<T> var1);
   }
}
