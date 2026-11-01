package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;

public class CraftingInput implements RecipeInput {
   public static final CraftingInput EMPTY = new CraftingInput(0, 0, List.of());
   private final int width;
   private final int height;
   private final List<ItemStack> items;
   private final StackedContents stackedContents = new StackedContents();
   private final int ingredientCount;

   private CraftingInput(int p_346099_, int p_344783_, List<ItemStack> p_345241_) {
      this.width = p_346099_;
      this.height = p_344783_;
      this.items = p_345241_;
      int $$3 = 0;

      for (ItemStack $$4 : p_345241_) {
         if (!$$4.isEmpty()) {
            $$3++;
            this.stackedContents.accountStack($$4, 1);
         }
      }

      this.ingredientCount = $$3;
   }

   public static CraftingInput of(int p_346122_, int p_344877_, List<ItemStack> p_345183_) {
      return ofPositioned(p_346122_, p_344877_, p_345183_).input();
   }

   public static CraftingInput.Positioned ofPositioned(int p_347479_, int p_347466_, List<ItemStack> p_347585_) {
      if (p_347479_ != 0 && p_347466_ != 0) {
         int $$3 = p_347479_ - 1;
         int $$4 = 0;
         int $$5 = p_347466_ - 1;
         int $$6 = 0;

         for (int $$7 = 0; $$7 < p_347466_; $$7++) {
            boolean $$8 = true;

            for (int $$9 = 0; $$9 < p_347479_; $$9++) {
               ItemStack $$10 = p_347585_.get($$9 + $$7 * p_347479_);
               if (!$$10.isEmpty()) {
                  $$3 = Math.min($$3, $$9);
                  $$4 = Math.max($$4, $$9);
                  $$8 = false;
               }
            }

            if (!$$8) {
               $$5 = Math.min($$5, $$7);
               $$6 = Math.max($$6, $$7);
            }
         }

         int $$11 = $$4 - $$3 + 1;
         int $$12 = $$6 - $$5 + 1;
         if ($$11 <= 0 || $$12 <= 0) {
            return CraftingInput.Positioned.EMPTY;
         } else if ($$11 == p_347479_ && $$12 == p_347466_) {
            return new CraftingInput.Positioned(new CraftingInput(p_347479_, p_347466_, p_347585_), $$3, $$5);
         } else {
            List<ItemStack> $$13 = new ArrayList<>($$11 * $$12);

            for (int $$14 = 0; $$14 < $$12; $$14++) {
               for (int $$15 = 0; $$15 < $$11; $$15++) {
                  int $$16 = $$15 + $$3 + ($$14 + $$5) * p_347479_;
                  $$13.add(p_347585_.get($$16));
               }
            }

            return new CraftingInput.Positioned(new CraftingInput($$11, $$12, $$13), $$3, $$5);
         }
      } else {
         return CraftingInput.Positioned.EMPTY;
      }
   }

   @Override
   public ItemStack getItem(int p_345667_) {
      return this.items.get(p_345667_);
   }

   public ItemStack getItem(int p_346237_, int p_345556_) {
      return this.items.get(p_346237_ + p_345556_ * this.width);
   }

   @Override
   public int size() {
      return this.items.size();
   }

   @Override
   public boolean isEmpty() {
      return this.ingredientCount == 0;
   }

   public StackedContents stackedContents() {
      return this.stackedContents;
   }

   public List<ItemStack> items() {
      return this.items;
   }

   public int ingredientCount() {
      return this.ingredientCount;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   @Override
   public boolean equals(Object p_345299_) {
      if (p_345299_ == this) {
         return true;
      } else {
         return !(p_345299_ instanceof CraftingInput $$1)
            ? false
            : this.width == $$1.width
               && this.height == $$1.height
               && this.ingredientCount == $$1.ingredientCount
               && ItemStack.listMatches(this.items, $$1.items);
      }
   }

   @Override
   public int hashCode() {
      int $$0 = ItemStack.hashStackList(this.items);
      $$0 = 31 * $$0 + this.width;
      return 31 * $$0 + this.height;
   }

   public record Positioned(CraftingInput input, int left, int top) {
      public static final CraftingInput.Positioned EMPTY = new CraftingInput.Positioned(CraftingInput.EMPTY, 0, 0);
   }
}
