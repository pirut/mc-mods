package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

public class ServerPlaceRecipe<I extends RecipeInput, R extends Recipe<I>> implements PlaceRecipe<Integer> {
   private static final int ITEM_NOT_FOUND = -1;
   protected final StackedContents stackedContents = new StackedContents();
   protected Inventory inventory;
   protected RecipeBookMenu<I, R> menu;

   public ServerPlaceRecipe(RecipeBookMenu<I, R> p_135431_) {
      this.menu = p_135431_;
   }

   public void recipeClicked(ServerPlayer p_135435_, @Nullable RecipeHolder<R> p_301150_, boolean p_135437_) {
      if (p_301150_ != null && p_135435_.getRecipeBook().contains(p_301150_)) {
         this.inventory = p_135435_.getInventory();
         if (this.testClearGrid() || p_135435_.isCreative()) {
            this.stackedContents.clear();
            p_135435_.getInventory().fillStackedContents(this.stackedContents);
            this.menu.fillCraftSlotsStackedContents(this.stackedContents);
            if (this.stackedContents.canCraft(p_301150_.value(), null)) {
               this.handleRecipeClicked(p_301150_, p_135437_);
            } else {
               this.clearGrid();
               p_135435_.connection.send(new ClientboundPlaceGhostRecipePacket(p_135435_.containerMenu.containerId, p_301150_));
            }

            p_135435_.getInventory().setChanged();
         }
      }
   }

   protected void clearGrid() {
      for (int $$0 = 0; $$0 < this.menu.getSize(); $$0++) {
         if (this.menu.shouldMoveToInventory($$0)) {
            ItemStack $$1 = this.menu.getSlot($$0).getItem().copy();
            this.inventory.placeItemBackInInventory($$1, false);
            this.menu.getSlot($$0).set($$1);
         }
      }

      this.menu.clearCraftingContent();
   }

   protected void handleRecipeClicked(RecipeHolder<R> p_301187_, boolean p_135442_) {
      boolean $$2 = this.menu.recipeMatches(p_301187_);
      int $$3 = this.stackedContents.getBiggestCraftableStack(p_301187_, null);
      if ($$2) {
         for (int $$4 = 0; $$4 < this.menu.getGridHeight() * this.menu.getGridWidth() + 1; $$4++) {
            if ($$4 != this.menu.getResultSlotIndex()) {
               ItemStack $$5 = this.menu.getSlot($$4).getItem();
               if (!$$5.isEmpty() && Math.min($$3, $$5.getMaxStackSize()) < $$5.getCount() + 1) {
                  return;
               }
            }
         }
      }

      int $$6 = this.getStackSize(p_135442_, $$3, $$2);
      IntList $$7 = new IntArrayList();
      if (this.stackedContents.canCraft(p_301187_.value(), $$7, $$6)) {
         int $$8 = $$6;
         IntListIterator var8 = $$7.iterator();

         while (var8.hasNext()) {
            int $$9 = (Integer)var8.next();
            ItemStack $$10 = StackedContents.fromStackingIndex($$9);
            if (!$$10.isEmpty()) {
               int $$11 = $$10.getMaxStackSize();
               if ($$11 < $$8) {
                  $$8 = $$11;
               }
            }
         }

         if (this.stackedContents.canCraft(p_301187_.value(), $$7, $$8)) {
            this.clearGrid();
            this.placeRecipe(this.menu.getGridWidth(), this.menu.getGridHeight(), this.menu.getResultSlotIndex(), p_301187_, $$7.iterator(), $$8);
         }
      }
   }

   public void addItemToSlot(Integer p_346390_, int p_346229_, int p_345733_, int p_345812_, int p_346351_) {
      Slot $$5 = this.menu.getSlot(p_346229_);
      ItemStack $$6 = StackedContents.fromStackingIndex(p_346390_);
      if (!$$6.isEmpty()) {
         int $$7 = p_345733_;

         while ($$7 > 0) {
            $$7 = this.moveItemToGrid($$5, $$6, $$7);
            if ($$7 == -1) {
               return;
            }
         }
      }
   }

   protected int getStackSize(boolean p_135450_, int p_135451_, boolean p_135452_) {
      int $$3 = 1;
      if (p_135450_) {
         $$3 = p_135451_;
      } else if (p_135452_) {
         $$3 = Integer.MAX_VALUE;

         for (int $$4 = 0; $$4 < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; $$4++) {
            if ($$4 != this.menu.getResultSlotIndex()) {
               ItemStack $$5 = this.menu.getSlot($$4).getItem();
               if (!$$5.isEmpty() && $$3 > $$5.getCount()) {
                  $$3 = $$5.getCount();
               }
            }
         }

         if ($$3 != Integer.MAX_VALUE) {
            $$3++;
         }
      }

      return $$3;
   }

   protected int moveItemToGrid(Slot p_135439_, ItemStack p_135440_, int p_346157_) {
      int $$3 = this.inventory.findSlotMatchingUnusedItem(p_135440_);
      if ($$3 == -1) {
         return -1;
      } else {
         ItemStack $$4 = this.inventory.getItem($$3);
         int $$5;
         if (p_346157_ < $$4.getCount()) {
            this.inventory.removeItem($$3, p_346157_);
            $$5 = p_346157_;
         } else {
            this.inventory.removeItemNoUpdate($$3);
            $$5 = $$4.getCount();
         }

         if (p_135439_.getItem().isEmpty()) {
            p_135439_.set($$4.copyWithCount($$5));
         } else {
            p_135439_.getItem().grow($$5);
         }

         return p_346157_ - $$5;
      }
   }

   private boolean testClearGrid() {
      List<ItemStack> $$0 = Lists.newArrayList();
      int $$1 = this.getAmountOfFreeSlotsInInventory();

      for (int $$2 = 0; $$2 < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; $$2++) {
         if ($$2 != this.menu.getResultSlotIndex()) {
            ItemStack $$3 = this.menu.getSlot($$2).getItem().copy();
            if (!$$3.isEmpty()) {
               int $$4 = this.inventory.getSlotWithRemainingSpace($$3);
               if ($$4 == -1 && $$0.size() <= $$1) {
                  for (ItemStack $$5 : $$0) {
                     if (ItemStack.isSameItem($$5, $$3) && $$5.getCount() != $$5.getMaxStackSize() && $$5.getCount() + $$3.getCount() <= $$5.getMaxStackSize()) {
                        $$5.grow($$3.getCount());
                        $$3.setCount(0);
                        break;
                     }
                  }

                  if (!$$3.isEmpty()) {
                     if ($$0.size() >= $$1) {
                        return false;
                     }

                     $$0.add($$3);
                  }
               } else if ($$4 == -1) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private int getAmountOfFreeSlotsInInventory() {
      int $$0 = 0;

      for (ItemStack $$1 : this.inventory.items) {
         if ($$1.isEmpty()) {
            $$0++;
         }
      }

      return $$0;
   }
}
