package net.minecraft.world.entity.player;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class StackedContents {
   private static final int EMPTY = 0;
   public final Int2IntMap contents = new Int2IntOpenHashMap();

   public void accountSimpleStack(ItemStack p_36467_) {
      if (!p_36467_.isDamaged() && !p_36467_.isEnchanted() && !p_36467_.has(DataComponents.CUSTOM_NAME)) {
         this.accountStack(p_36467_);
      }
   }

   public void accountStack(ItemStack p_36492_) {
      this.accountStack(p_36492_, p_36492_.getMaxStackSize());
   }

   public void accountStack(ItemStack p_36469_, int p_36470_) {
      if (!p_36469_.isEmpty()) {
         int $$2 = getStackingIndex(p_36469_);
         int $$3 = Math.min(p_36470_, p_36469_.getCount());
         this.put($$2, $$3);
      }
   }

   public static int getStackingIndex(ItemStack p_36497_) {
      return BuiltInRegistries.ITEM.getId(p_36497_.getItem());
   }

   boolean has(int p_36483_) {
      return this.contents.get(p_36483_) > 0;
   }

   int take(int p_36457_, int p_36458_) {
      int $$2 = this.contents.get(p_36457_);
      if ($$2 >= p_36458_) {
         this.contents.put(p_36457_, $$2 - p_36458_);
         return p_36457_;
      } else {
         return 0;
      }
   }

   void put(int p_36485_, int p_36486_) {
      this.contents.put(p_36485_, this.contents.get(p_36485_) + p_36486_);
   }

   public boolean canCraft(Recipe<?> p_36476_, @Nullable IntList p_36477_) {
      return this.canCraft(p_36476_, p_36477_, 1);
   }

   public boolean canCraft(Recipe<?> p_36479_, @Nullable IntList p_36480_, int p_36481_) {
      return new StackedContents.RecipePicker(p_36479_).tryPick(p_36481_, p_36480_);
   }

   public int getBiggestCraftableStack(RecipeHolder<?> p_301005_, @Nullable IntList p_36474_) {
      return this.getBiggestCraftableStack(p_301005_, Integer.MAX_VALUE, p_36474_);
   }

   public int getBiggestCraftableStack(RecipeHolder<?> p_300888_, int p_300980_, @Nullable IntList p_36495_) {
      return new StackedContents.RecipePicker(p_300888_.value()).tryPickAll(p_300980_, p_36495_);
   }

   public static ItemStack fromStackingIndex(int p_36455_) {
      return p_36455_ == 0 ? ItemStack.EMPTY : new ItemStack(Item.byId(p_36455_));
   }

   public void clear() {
      this.contents.clear();
   }

   class RecipePicker {
      private final Recipe<?> recipe;
      private final List<Ingredient> ingredients = Lists.newArrayList();
      private final int ingredientCount;
      private final int[] items;
      private final int itemCount;
      private final BitSet data;
      private final IntList path = new IntArrayList();

      public RecipePicker(Recipe<?> p_36508_) {
         this.recipe = p_36508_;
         this.ingredients.addAll(p_36508_.getIngredients());
         this.ingredients.removeIf(Ingredient::isEmpty);
         this.ingredientCount = this.ingredients.size();
         this.items = this.getUniqueAvailableIngredientItems();
         this.itemCount = this.items.length;
         this.data = new BitSet(this.ingredientCount + this.itemCount + this.ingredientCount + this.ingredientCount * this.itemCount);

         for (int $$1 = 0; $$1 < this.ingredients.size(); $$1++) {
            IntList $$2 = this.ingredients.get($$1).getStackingIds();

            for (int $$3 = 0; $$3 < this.itemCount; $$3++) {
               if ($$2.contains(this.items[$$3])) {
                  this.data.set(this.getIndex(true, $$3, $$1));
               }
            }
         }
      }

      public boolean tryPick(int p_36513_, @Nullable IntList p_36514_) {
         if (p_36513_ <= 0) {
            return true;
         } else {
            int $$2;
            for ($$2 = 0; this.dfs(p_36513_); $$2++) {
               StackedContents.this.take(this.items[this.path.getInt(0)], p_36513_);
               int $$3 = this.path.size() - 1;
               this.setSatisfied(this.path.getInt($$3));

               for (int $$4 = 0; $$4 < $$3; $$4++) {
                  this.toggleResidual(($$4 & 1) == 0, this.path.get($$4), this.path.get($$4 + 1));
               }

               this.path.clear();
               this.data.clear(0, this.ingredientCount + this.itemCount);
            }

            boolean $$5 = $$2 == this.ingredientCount;
            boolean $$6 = $$5 && p_36514_ != null;
            if ($$6) {
               p_36514_.clear();
            }

            this.data.clear(0, this.ingredientCount + this.itemCount + this.ingredientCount);
            int $$7 = 0;

            for (Ingredient $$9 : this.recipe.getIngredients()) {
               if ($$6 && $$9.isEmpty()) {
                  p_36514_.add(0);
               } else {
                  for (int $$10 = 0; $$10 < this.itemCount; $$10++) {
                     if (this.hasResidual(false, $$7, $$10)) {
                        this.toggleResidual(true, $$10, $$7);
                        StackedContents.this.put(this.items[$$10], p_36513_);
                        if ($$6) {
                           p_36514_.add(this.items[$$10]);
                        }
                     }
                  }

                  $$7++;
               }
            }

            return $$5;
         }
      }

      private int[] getUniqueAvailableIngredientItems() {
         IntCollection $$0 = new IntAVLTreeSet();

         for (Ingredient $$1 : this.ingredients) {
            $$0.addAll($$1.getStackingIds());
         }

         IntIterator $$2 = $$0.iterator();

         while ($$2.hasNext()) {
            if (!StackedContents.this.has($$2.nextInt())) {
               $$2.remove();
            }
         }

         return $$0.toIntArray();
      }

      private boolean dfs(int p_36511_) {
         int $$1 = this.itemCount;

         for (int $$2 = 0; $$2 < $$1; $$2++) {
            if (StackedContents.this.contents.get(this.items[$$2]) >= p_36511_) {
               this.visit(false, $$2);

               while (!this.path.isEmpty()) {
                  int $$3 = this.path.size();
                  boolean $$4 = ($$3 & 1) == 1;
                  int $$5 = this.path.getInt($$3 - 1);
                  if (!$$4 && !this.isSatisfied($$5)) {
                     break;
                  }

                  int $$6 = $$4 ? this.ingredientCount : $$1;
                  int $$7 = 0;

                  while (true) {
                     if ($$7 < $$6) {
                        if (this.hasVisited($$4, $$7) || !this.hasConnection($$4, $$5, $$7) || !this.hasResidual($$4, $$5, $$7)) {
                           $$7++;
                           continue;
                        }

                        this.visit($$4, $$7);
                     }

                     $$7 = this.path.size();
                     if ($$7 == $$3) {
                        this.path.removeInt($$7 - 1);
                     }
                     break;
                  }
               }

               if (!this.path.isEmpty()) {
                  return true;
               }
            }
         }

         return false;
      }

      private boolean isSatisfied(int p_36524_) {
         return this.data.get(this.getSatisfiedIndex(p_36524_));
      }

      private void setSatisfied(int p_36536_) {
         this.data.set(this.getSatisfiedIndex(p_36536_));
      }

      private int getSatisfiedIndex(int p_36545_) {
         return this.ingredientCount + this.itemCount + p_36545_;
      }

      private boolean hasConnection(boolean p_36519_, int p_36520_, int p_36521_) {
         return this.data.get(this.getIndex(p_36519_, p_36520_, p_36521_));
      }

      private boolean hasResidual(boolean p_36532_, int p_36533_, int p_36534_) {
         return p_36532_ != this.data.get(1 + this.getIndex(p_36532_, p_36533_, p_36534_));
      }

      private void toggleResidual(boolean p_36541_, int p_36542_, int p_36543_) {
         this.data.flip(1 + this.getIndex(p_36541_, p_36542_, p_36543_));
      }

      private int getIndex(boolean p_36547_, int p_36548_, int p_36549_) {
         int $$3 = p_36547_ ? p_36548_ * this.ingredientCount + p_36549_ : p_36549_ * this.ingredientCount + p_36548_;
         return this.ingredientCount + this.itemCount + this.ingredientCount + 2 * $$3;
      }

      private void visit(boolean p_36516_, int p_36517_) {
         this.data.set(this.getVisitedIndex(p_36516_, p_36517_));
         this.path.add(p_36517_);
      }

      private boolean hasVisited(boolean p_36529_, int p_36530_) {
         return this.data.get(this.getVisitedIndex(p_36529_, p_36530_));
      }

      private int getVisitedIndex(boolean p_36538_, int p_36539_) {
         return (p_36538_ ? 0 : this.ingredientCount) + p_36539_;
      }

      public int tryPickAll(int p_36526_, @Nullable IntList p_36527_) {
         int $$2 = 0;
         int $$3 = Math.min(p_36526_, this.getMinIngredientCount()) + 1;

         while (true) {
            int $$4 = ($$2 + $$3) / 2;
            if (this.tryPick($$4, null)) {
               if ($$3 - $$2 <= 1) {
                  if ($$4 > 0) {
                     this.tryPick($$4, p_36527_);
                  }

                  return $$4;
               }

               $$2 = $$4;
            } else {
               $$3 = $$4;
            }
         }
      }

      private int getMinIngredientCount() {
         int $$0 = Integer.MAX_VALUE;

         for (Ingredient $$1 : this.ingredients) {
            int $$2 = 0;
            IntListIterator var5 = $$1.getStackingIds().iterator();

            while (var5.hasNext()) {
               int $$3 = (Integer)var5.next();
               $$2 = Math.max($$2, StackedContents.this.contents.get($$3));
            }

            if ($$0 > 0) {
               $$0 = Math.min($$0, $$2);
            }
         }

         return $$0;
      }
   }
}
