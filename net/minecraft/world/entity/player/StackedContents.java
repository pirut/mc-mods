package net.minecraft.world.entity.player;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectIterable;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class StackedContents<T> {
   public final Reference2IntOpenHashMap<T> amounts = new Reference2IntOpenHashMap();

   boolean hasAtLeast(T item, int amount) {
      return this.amounts.getInt(item) >= amount;
   }

   void take(T item, int amount) {
      int i = this.amounts.addTo(item, -amount);
      if (i < amount) {
         throw new IllegalStateException("Took " + amount + " items, but only had " + i);
      }
   }

   void put(T item, int amount) {
      this.amounts.addTo(item, amount);
   }

   public boolean tryPick(List<? extends StackedContents.IngredientInfo<T>> ingredients, int amount, @Nullable StackedContents.Output<T> output) {
      return new StackedContents.RecipePicker(ingredients).tryPick(amount, output);
   }

   public int tryPickAll(List<? extends StackedContents.IngredientInfo<T>> ingredients, int amount, @Nullable StackedContents.Output<T> output) {
      return new StackedContents.RecipePicker(ingredients).tryPickAll(amount, output);
   }

   public void clear() {
      this.amounts.clear();
   }

   public void account(T item, int amount) {
      this.put(item, amount);
   }

   List<T> getUniqueAvailableIngredientItems(Iterable<? extends StackedContents.IngredientInfo<T>> ingredients) {
      List<T> list = new ArrayList<>();
      ObjectIterator var3 = Reference2IntMaps.fastIterable(this.amounts).iterator();

      while (var3.hasNext()) {
         Entry<T> entry = (Entry<T>)var3.next();
         if (entry.getIntValue() > 0 && anyIngredientMatches(ingredients, (T)entry.getKey())) {
            list.add((T)entry.getKey());
         }
      }

      return list;
   }

   private static <T> boolean anyIngredientMatches(Iterable<? extends StackedContents.IngredientInfo<T>> ingredients, T item) {
      for (StackedContents.IngredientInfo<T> ingredientInfo : ingredients) {
         if (ingredientInfo.acceptsItem(item)) {
            return true;
         }
      }

      return false;
   }

   @VisibleForTesting
   public int getResultUpperBound(List<? extends StackedContents.IngredientInfo<T>> ingredients) {
      int i = Integer.MAX_VALUE;
      ObjectIterable<Entry<T>> objectIterable = Reference2IntMaps.fastIterable(this.amounts);

      label31:
      for (StackedContents.IngredientInfo<T> ingredientInfo : ingredients) {
         int j = 0;
         ObjectIterator var7 = objectIterable.iterator();

         while (var7.hasNext()) {
            Entry<T> entry = (Entry<T>)var7.next();
            int k = entry.getIntValue();
            if (k > j) {
               if (ingredientInfo.acceptsItem((T)entry.getKey())) {
                  j = k;
               }

               if (j >= i) {
                  continue label31;
               }
            }
         }

         i = j;
         if (j == 0) {
            break;
         }
      }

      return i;
   }

   @FunctionalInterface
   public interface IngredientInfo<T> {
      boolean acceptsItem(T object);
   }

   @FunctionalInterface
   public interface Output<T> {
      void accept(T object);
   }

   class RecipePicker {
      private final List<? extends StackedContents.IngredientInfo<T>> ingredients;
      private final int ingredientCount;
      private final List<T> items;
      private final int itemCount;
      private final BitSet data;
      private final IntList path = new IntArrayList();

      public RecipePicker(final List<? extends StackedContents.IngredientInfo<T>> ingredients) {
         this.ingredients = ingredients;
         this.ingredientCount = ingredients.size();
         this.items = StackedContents.this.getUniqueAvailableIngredientItems(ingredients);
         this.itemCount = this.items.size();
         this.data = new BitSet(this.visitedIngredientCount() + this.visitedItemCount() + this.satisfiedCount() + this.connectionCount() + this.residualCount());
         this.setInitialConnections();
      }

      private void setInitialConnections() {
         for (int i = 0; i < this.ingredientCount; i++) {
            StackedContents.IngredientInfo<T> ingredientInfo = (StackedContents.IngredientInfo<T>)this.ingredients.get(i);

            for (int j = 0; j < this.itemCount; j++) {
               if (ingredientInfo.acceptsItem(this.items.get(j))) {
                  this.setConnection(j, i);
               }
            }
         }
      }

      public boolean tryPick(int amount, @Nullable StackedContents.Output<T> output) {
         if (amount <= 0) {
            return true;
         } else {
            int i = 0;

            while (true) {
               IntList intList = this.tryAssigningNewItem(amount);
               if (intList == null) {
                  boolean bl = i == this.ingredientCount;
                  boolean bl2 = bl && output != null;
                  this.clearAllVisited();
                  this.clearSatisfied();

                  for (int k = 0; k < this.ingredientCount; k++) {
                     for (int l = 0; l < this.itemCount; l++) {
                        if (this.isAssigned(l, k)) {
                           this.unassign(l, k);
                           StackedContents.this.put(this.items.get(l), amount);
                           if (bl2) {
                              output.accept(this.items.get(l));
                           }
                           break;
                        }
                     }
                  }

                  assert this.data.get(this.residualOffset(), this.residualOffset() + this.residualCount()).isEmpty();

                  return bl;
               }

               int j = intList.getInt(0);
               StackedContents.this.take(this.items.get(j), amount);
               int k = intList.size() - 1;
               this.setSatisfied(intList.getInt(k));
               i++;

               for (int lx = 0; lx < intList.size() - 1; lx++) {
                  if (isPathIndexItem(lx)) {
                     int m = intList.getInt(lx);
                     int n = intList.getInt(lx + 1);
                     this.assign(m, n);
                  } else {
                     int m = intList.getInt(lx + 1);
                     int n = intList.getInt(lx);
                     this.unassign(m, n);
                  }
               }
            }
         }
      }

      private static boolean isPathIndexItem(int index) {
         return (index & 1) == 0;
      }

      @Nullable
      private IntList tryAssigningNewItem(int amount) {
         this.clearAllVisited();

         for (int i = 0; i < this.itemCount; i++) {
            if (StackedContents.this.hasAtLeast(this.items.get(i), amount)) {
               IntList intList = this.findNewItemAssignmentPath(i);
               if (intList != null) {
                  return intList;
               }
            }
         }

         return null;
      }

      @Nullable
      private IntList findNewItemAssignmentPath(int amount) {
         this.path.clear();
         this.visitItem(amount);
         this.path.add(amount);

         while (!this.path.isEmpty()) {
            int i = this.path.size();
            if (isPathIndexItem(i - 1)) {
               int j = this.path.getInt(i - 1);

               for (int k = 0; k < this.ingredientCount; k++) {
                  if (!this.hasVisitedIngredient(k) && this.hasConnection(j, k) && !this.isAssigned(j, k)) {
                     this.visitIngredient(k);
                     this.path.add(k);
                     break;
                  }
               }
            } else {
               int j = this.path.getInt(i - 1);
               if (!this.isSatisfied(j)) {
                  return this.path;
               }

               for (int kx = 0; kx < this.itemCount; kx++) {
                  if (!this.hasVisitedItem(kx) && this.isAssigned(kx, j)) {
                     assert this.hasConnection(kx, j);

                     this.visitItem(kx);
                     this.path.add(kx);
                     break;
                  }
               }
            }

            int j = this.path.size();
            if (j == i) {
               this.path.removeInt(j - 1);
            }
         }

         return null;
      }

      private int visitedIngredientOffset() {
         return 0;
      }

      private int visitedIngredientCount() {
         return this.ingredientCount;
      }

      private int visitedItemOffset() {
         return this.visitedIngredientOffset() + this.visitedIngredientCount();
      }

      private int visitedItemCount() {
         return this.itemCount;
      }

      private int satisfiedOffset() {
         return this.visitedItemOffset() + this.visitedItemCount();
      }

      private int satisfiedCount() {
         return this.ingredientCount;
      }

      private int connectionOffset() {
         return this.satisfiedOffset() + this.satisfiedCount();
      }

      private int connectionCount() {
         return this.ingredientCount * this.itemCount;
      }

      private int residualOffset() {
         return this.connectionOffset() + this.connectionCount();
      }

      private int residualCount() {
         return this.ingredientCount * this.itemCount;
      }

      private boolean isSatisfied(int stackingIndex) {
         return this.data.get(this.getSatisfiedIndex(stackingIndex));
      }

      private void setSatisfied(int stackingIndex) {
         this.data.set(this.getSatisfiedIndex(stackingIndex));
      }

      private int getSatisfiedIndex(int stackingIndex) {
         assert stackingIndex >= 0 && stackingIndex < this.ingredientCount;

         return this.satisfiedOffset() + stackingIndex;
      }

      private void clearSatisfied() {
         this.clearRange(this.satisfiedOffset(), this.satisfiedCount());
      }

      private void setConnection(int itemIndex, int ingredientIndex) {
         this.data.set(this.getConnectionIndex(itemIndex, ingredientIndex));
      }

      private boolean hasConnection(int itemIndex, int ingredientIndex) {
         return this.data.get(this.getConnectionIndex(itemIndex, ingredientIndex));
      }

      private int getConnectionIndex(int itemIndex, int ingredientIndex) {
         assert itemIndex >= 0 && itemIndex < this.itemCount;

         assert ingredientIndex >= 0 && ingredientIndex < this.ingredientCount;

         return this.connectionOffset() + itemIndex * this.ingredientCount + ingredientIndex;
      }

      private boolean isAssigned(int itemIndex, int ingredientIndex) {
         return this.data.get(this.getResidualIndex(itemIndex, ingredientIndex));
      }

      private void assign(int itemIndex, int ingredientIndex) {
         int i = this.getResidualIndex(itemIndex, ingredientIndex);

         assert !this.data.get(i);

         this.data.set(i);
      }

      private void unassign(int itemIndex, int ingredientIndex) {
         int i = this.getResidualIndex(itemIndex, ingredientIndex);

         assert this.data.get(i);

         this.data.clear(i);
      }

      private int getResidualIndex(int itemIndex, int ingredientIndex) {
         assert itemIndex >= 0 && itemIndex < this.itemCount;

         assert ingredientIndex >= 0 && ingredientIndex < this.ingredientCount;

         return this.residualOffset() + itemIndex * this.ingredientCount + ingredientIndex;
      }

      private void visitIngredient(int ingredientIndex) {
         this.data.set(this.getVisitedIngredientIndex(ingredientIndex));
      }

      private boolean hasVisitedIngredient(int ingredientIndex) {
         return this.data.get(this.getVisitedIngredientIndex(ingredientIndex));
      }

      private int getVisitedIngredientIndex(int ingredientIndex) {
         assert ingredientIndex >= 0 && ingredientIndex < this.ingredientCount;

         return this.visitedIngredientOffset() + ingredientIndex;
      }

      private void visitItem(int itemIndex) {
         this.data.set(this.getVisitiedItemIndex(itemIndex));
      }

      private boolean hasVisitedItem(int itemIndex) {
         return this.data.get(this.getVisitiedItemIndex(itemIndex));
      }

      private int getVisitiedItemIndex(int itemIndex) {
         assert itemIndex >= 0 && itemIndex < this.itemCount;

         return this.visitedItemOffset() + itemIndex;
      }

      private void clearAllVisited() {
         this.clearRange(this.visitedIngredientOffset(), this.visitedIngredientCount());
         this.clearRange(this.visitedItemOffset(), this.visitedItemCount());
      }

      private void clearRange(int offset, int count) {
         this.data.clear(offset, offset + count);
      }

      public int tryPickAll(int amount, @Nullable StackedContents.Output<T> output) {
         int i = 0;
         int j = Math.min(amount, StackedContents.this.getResultUpperBound(this.ingredients)) + 1;

         while (true) {
            int k = (i + j) / 2;
            if (this.tryPick(k, null)) {
               if (j - i <= 1) {
                  if (k > 0) {
                     this.tryPick(k, output);
                  }

                  return k;
               }

               i = k;
            } else {
               j = k;
            }
         }
      }
   }
}
