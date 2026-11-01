package net.minecraft.world.item.crafting;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RecipeCache {
   private final RecipeCache.Entry[] entries;
   private WeakReference<RecipeManager> cachedRecipeManager = new WeakReference<>(null);

   public RecipeCache(int p_307489_) {
      this.entries = new RecipeCache.Entry[p_307489_];
   }

   public Optional<RecipeHolder<CraftingRecipe>> get(Level p_307315_, CraftingInput p_345726_) {
      if (p_345726_.isEmpty()) {
         return Optional.empty();
      } else {
         this.validateRecipeManager(p_307315_);

         for (int $$2 = 0; $$2 < this.entries.length; $$2++) {
            RecipeCache.Entry $$3 = this.entries[$$2];
            if ($$3 != null && $$3.matches(p_345726_)) {
               this.moveEntryToFront($$2);
               return Optional.ofNullable($$3.value());
            }
         }

         return this.compute(p_345726_, p_307315_);
      }
   }

   private void validateRecipeManager(Level p_307638_) {
      RecipeManager $$1 = p_307638_.getRecipeManager();
      if ($$1 != this.cachedRecipeManager.get()) {
         this.cachedRecipeManager = new WeakReference<>($$1);
         Arrays.fill(this.entries, null);
      }
   }

   private Optional<RecipeHolder<CraftingRecipe>> compute(CraftingInput p_345136_, Level p_307202_) {
      Optional<RecipeHolder<CraftingRecipe>> $$2 = p_307202_.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, p_345136_, p_307202_);
      this.insert(p_345136_, $$2.orElse(null));
      return $$2;
   }

   private void moveEntryToFront(int p_307277_) {
      if (p_307277_ > 0) {
         RecipeCache.Entry $$1 = this.entries[p_307277_];
         System.arraycopy(this.entries, 0, this.entries, 1, p_307277_);
         this.entries[0] = $$1;
      }
   }

   private void insert(CraftingInput p_348525_, @Nullable RecipeHolder<CraftingRecipe> p_336146_) {
      NonNullList<ItemStack> $$2 = NonNullList.withSize(p_348525_.size(), ItemStack.EMPTY);

      for (int $$3 = 0; $$3 < p_348525_.size(); $$3++) {
         $$2.set($$3, p_348525_.getItem($$3).copyWithCount(1));
      }

      System.arraycopy(this.entries, 0, this.entries, 1, this.entries.length - 1);
      this.entries[0] = new RecipeCache.Entry($$2, p_348525_.width(), p_348525_.height(), p_336146_);
   }

   record Entry(NonNullList<ItemStack> key, int width, int height, @Nullable RecipeHolder<CraftingRecipe> value) {
      public boolean matches(CraftingInput p_348535_) {
         if (this.width == p_348535_.width() && this.height == p_348535_.height()) {
            for (int $$1 = 0; $$1 < this.key.size(); $$1++) {
               if (!ItemStack.isSameItemSameComponents(this.key.get($$1), p_348535_.getItem($$1))) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }
}
