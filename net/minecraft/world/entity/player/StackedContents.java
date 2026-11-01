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

   boolean hasAtLeast(T p_362841_, int p_364786_) {
      return this.amounts.getInt(p_362841_) >= p_364786_;
   }

   void take(T p_364608_, int p_36457_) {
      int i = this.amounts.addTo(p_364608_, -p_36457_);
      if (i < p_36457_) {
         throw new IllegalStateException("Took " + p_36457_ + " items, but only had " + i);
      }
   }

   void put(T p_364631_, int p_36485_) {
      this.amounts.addTo(p_364631_, p_36485_);
   }

   public boolean tryPick(List<? extends StackedContents.IngredientInfo<T>> p_363544_, int p_364228_, @Nullable StackedContents.Output<T> p_362098_) {
      return new StackedContents.RecipePicker(p_363544_).tryPick(p_364228_, p_362098_);
   }

   public int tryPickAll(List<? extends StackedContents.IngredientInfo<T>> p_363263_, int p_362732_, @Nullable StackedContents.Output<T> p_361155_) {
      return new StackedContents.RecipePicker(p_363263_).tryPickAll(p_362732_, p_361155_);
   }

   public void clear() {
      this.amounts.clear();
   }

   public void account(T p_362247_, int p_362975_) {
      this.put(p_362247_, p_362975_);
   }

   List<T> getUniqueAvailableIngredientItems(Iterable<? extends StackedContents.IngredientInfo<T>> p_389607_) {
      List<T> list = new ArrayList<>();
      ObjectIterator var3 = Reference2IntMaps.fastIterable(this.amounts).iterator();

      while (var3.hasNext()) {
         Entry<T> entry = (Entry<T>)var3.next();
         if (entry.getIntValue() > 0 && anyIngredientMatches(p_389607_, (T)entry.getKey())) {
            list.add((T)entry.getKey());
         }
      }

      return list;
   }

   private static <T> boolean anyIngredientMatches(Iterable<? extends StackedContents.IngredientInfo<T>> p_389661_, T p_389709_) {
      for (StackedContents.IngredientInfo<T> ingredientinfo : p_389661_) {
         if (ingredientinfo.acceptsItem(p_389709_)) {
            return true;
         }
      }

      return false;
   }

   @VisibleForTesting
   public int getResultUpperBound(List<? extends StackedContents.IngredientInfo<T>> p_389547_) {
      int i = Integer.MAX_VALUE;
      ObjectIterable<Entry<T>> objectiterable = Reference2IntMaps.fastIterable(this.amounts);

      label30:
      for (StackedContents.IngredientInfo<T> ingredientinfo : p_389547_) {
         int j = 0;
         ObjectIterator var7 = objectiterable.iterator();

         while (var7.hasNext()) {
            Entry<T> entry = (Entry<T>)var7.next();
            int k = entry.getIntValue();
            if (k > j) {
               if (ingredientinfo.acceptsItem((T)entry.getKey())) {
                  j = k;
               }

               if (j >= i) {
                  continue label30;
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
      boolean acceptsItem(T var1);
   }

   @FunctionalInterface
   public interface Output<T> {
      void accept(T var1);
   }

   class RecipePicker {
      private final List<? extends StackedContents.IngredientInfo<T>> ingredients;
      private final int ingredientCount;
      private final List<T> items;
      private final int itemCount;
      private final BitSet data;
      private final IntList path = new IntArrayList();

      public RecipePicker(List<? extends StackedContents.IngredientInfo<T>> p_363428_) {
         this.ingredients = p_363428_;
         this.ingredientCount = p_363428_.size();
         this.items = StackedContents.this.getUniqueAvailableIngredientItems(p_363428_);
         this.itemCount = this.items.size();
         this.data = new BitSet(this.visitedIngredientCount() + this.visitedItemCount() + this.satisfiedCount() + this.connectionCount() + this.residualCount());
         this.setInitialConnections();
      }

      private void setInitialConnections() {
         for (int i = 0; i < this.ingredientCount; i++) {
            StackedContents.IngredientInfo<T> ingredientinfo = (StackedContents.IngredientInfo<T>)this.ingredients.get(i);

            for (int j = 0; j < this.itemCount; j++) {
               if (ingredientinfo.acceptsItem(this.items.get(j))) {
                  this.setConnection(j, i);
               }
            }
         }
      }

      public boolean tryPick(int p_36513_, @Nullable StackedContents.Output<T> p_360727_) {
         if (p_36513_ <= 0) {
            return true;
         } else {
            int i = 0;

            while (true) {
               IntList intlist = this.tryAssigningNewItem(p_36513_);
               if (intlist == null) {
                  boolean flag = i == this.ingredientCount;
                  boolean flag1 = flag && p_360727_ != null;
                  this.clearAllVisited();
                  this.clearSatisfied();

                  for (int k1 = 0; k1 < this.ingredientCount; k1++) {
                     for (int l1 = 0; l1 < this.itemCount; l1++) {
                        if (this.isAssigned(l1, k1)) {
                           this.unassign(l1, k1);
                           StackedContents.this.put(this.items.get(l1), p_36513_);
                           if (flag1) {
                              p_360727_.accept(this.items.get(l1));
                           }
                           break;
                        }
                     }
                  }

                  assert this.data.get(this.residualOffset(), this.residualOffset() + this.residualCount()).isEmpty();

                  return flag;
               }

               int j = intlist.getInt(0);
               StackedContents.this.take(this.items.get(j), p_36513_);
               int k = intlist.size() - 1;
               this.setSatisfied(intlist.getInt(k));
               i++;

               for (int l = 0; l < intlist.size() - 1; l++) {
                  if (isPathIndexItem(l)) {
                     int i1 = intlist.getInt(l);
                     int j1 = intlist.getInt(l + 1);
                     this.assign(i1, j1);
                  } else {
                     int i2 = intlist.getInt(l + 1);
                     int j2 = intlist.getInt(l);
                     this.unassign(i2, j2);
                  }
               }
            }
         }
      }

      private static boolean isPathIndexItem(int p_363458_) {
         return (p_363458_ & 1) == 0;
      }

      @Nullable
      private IntList tryAssigningNewItem(int p_361858_) {
         this.clearAllVisited();

         for (int i = 0; i < this.itemCount; i++) {
            if (StackedContents.this.hasAtLeast(this.items.get(i), p_361858_)) {
               IntList intlist = this.findNewItemAssignmentPath(i);
               if (intlist != null) {
                  return intlist;
               }
            }
         }

         return null;
      }

      @Nullable
      private IntList findNewItemAssignmentPath(int p_361628_) {
         this.path.clear();
         this.visitItem(p_361628_);
         this.path.add(p_361628_);

         while (!this.path.isEmpty()) {
            int i = this.path.size();
            if (isPathIndexItem(i - 1)) {
               int l = this.path.getInt(i - 1);

               for (int j1 = 0; j1 < this.ingredientCount; j1++) {
                  if (!this.hasVisitedIngredient(j1) && this.hasConnection(l, j1) && !this.isAssigned(l, j1)) {
                     this.visitIngredient(j1);
                     this.path.add(j1);
                     break;
                  }
               }
            } else {
               int j = this.path.getInt(i - 1);
               if (!this.isSatisfied(j)) {
                  return this.path;
               }

               for (int k = 0; k < this.itemCount; k++) {
                  if (!this.hasVisitedItem(k) && this.isAssigned(k, j)) {
                     assert this.hasConnection(k, j);

                     this.visitItem(k);
                     this.path.add(k);
                     break;
                  }
               }
            }

            int i1 = this.path.size();
            if (i1 == i) {
               this.path.removeInt(i1 - 1);
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

      private boolean isSatisfied(int p_36524_) {
         return this.data.get(this.getSatisfiedIndex(p_36524_));
      }

      private void setSatisfied(int p_36536_) {
         this.data.set(this.getSatisfiedIndex(p_36536_));
      }

      private int getSatisfiedIndex(int p_36545_) {
         assert p_36545_ >= 0 && p_36545_ < this.ingredientCount;

         return this.satisfiedOffset() + p_36545_;
      }

      private void clearSatisfied() {
         this.clearRange(this.satisfiedOffset(), this.satisfiedCount());
      }

      private void setConnection(int p_363270_, int p_365139_) {
         this.data.set(this.getConnectionIndex(p_363270_, p_365139_));
      }

      private boolean hasConnection(int p_36520_, int p_36521_) {
         return this.data.get(this.getConnectionIndex(p_36520_, p_36521_));
      }

      private int getConnectionIndex(int p_361468_, int p_361569_) {
         assert p_361468_ >= 0 && p_361468_ < this.itemCount;

         assert p_361569_ >= 0 && p_361569_ < this.ingredientCount;

         return this.connectionOffset() + p_361468_ * this.ingredientCount + p_361569_;
      }

      private boolean isAssigned(int p_363705_, int p_362618_) {
         return this.data.get(this.getResidualIndex(p_363705_, p_362618_));
      }

      private void assign(int p_364941_, int p_363422_) {
         int i = this.getResidualIndex(p_364941_, p_363422_);

         assert !this.data.get(i);

         this.data.set(i);
      }

      private void unassign(int p_364332_, int p_363922_) {
         int i = this.getResidualIndex(p_364332_, p_363922_);

         assert this.data.get(i);

         this.data.clear(i);
      }

      private int getResidualIndex(int p_364133_, int p_363861_) {
         assert p_364133_ >= 0 && p_364133_ < this.itemCount;

         assert p_363861_ >= 0 && p_363861_ < this.ingredientCount;

         return this.residualOffset() + p_364133_ * this.ingredientCount + p_363861_;
      }

      private void visitIngredient(int p_364543_) {
         this.data.set(this.getVisitedIngredientIndex(p_364543_));
      }

      private boolean hasVisitedIngredient(int p_362970_) {
         return this.data.get(this.getVisitedIngredientIndex(p_362970_));
      }

      private int getVisitedIngredientIndex(int p_360807_) {
         assert p_360807_ >= 0 && p_360807_ < this.ingredientCount;

         return this.visitedIngredientOffset() + p_360807_;
      }

      private void visitItem(int p_363443_) {
         this.data.set(this.getVisitiedItemIndex(p_363443_));
      }

      private boolean hasVisitedItem(int p_360782_) {
         return this.data.get(this.getVisitiedItemIndex(p_360782_));
      }

      private int getVisitiedItemIndex(int p_363564_) {
         assert p_363564_ >= 0 && p_363564_ < this.itemCount;

         return this.visitedItemOffset() + p_363564_;
      }

      private void clearAllVisited() {
         this.clearRange(this.visitedIngredientOffset(), this.visitedIngredientCount());
         this.clearRange(this.visitedItemOffset(), this.visitedItemCount());
      }

      private void clearRange(int p_363225_, int p_363424_) {
         this.data.clear(p_363225_, p_363225_ + p_363424_);
      }

      public int tryPickAll(int p_36526_, @Nullable StackedContents.Output<T> p_361994_) {
         int i = 0;
         int j = Math.min(p_36526_, StackedContents.this.getResultUpperBound(this.ingredients)) + 1;

         while (true) {
            int k = (i + j) / 2;
            if (this.tryPick(k, null)) {
               if (j - i <= 1) {
                  if (k > 0) {
                     this.tryPick(k, p_361994_);
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
