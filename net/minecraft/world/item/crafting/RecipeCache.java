package net.minecraft.world.item.crafting;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class RecipeCache {
   private final RecipeCache.Entry[] entries;
   private WeakReference<RecipeManager> cachedRecipeManager = new WeakReference<>(null);

   public RecipeCache(int p_307489_) {
      this.entries = new RecipeCache.Entry[p_307489_];
   }

   public Optional<RecipeHolder<CraftingRecipe>> get(ServerLevel p_379692_, CraftingInput p_345726_) {
      if (p_345726_.isEmpty()) {
         return Optional.empty();
      } else {
         this.validateRecipeManager(p_379692_);

         for (int i = 0; i < this.entries.length; i++) {
            RecipeCache.Entry recipecache$entry = this.entries[i];
            if (recipecache$entry != null && recipecache$entry.matches(p_345726_)) {
               this.moveEntryToFront(i);
               return Optional.ofNullable(recipecache$entry.value());
            }
         }

         return this.compute(p_345726_, p_379692_);
      }
   }

   private void validateRecipeManager(ServerLevel p_379315_) {
      RecipeManager recipemanager = p_379315_.recipeAccess();
      if (recipemanager != this.cachedRecipeManager.get()) {
         this.cachedRecipeManager = new WeakReference<>(recipemanager);
         Arrays.fill(this.entries, null);
      }
   }

   private Optional<RecipeHolder<CraftingRecipe>> compute(CraftingInput p_345136_, ServerLevel p_379915_) {
      Optional<RecipeHolder<CraftingRecipe>> optional = p_379915_.recipeAccess().getRecipeFor(RecipeType.CRAFTING, p_345136_, p_379915_);
      this.insert(p_345136_, optional.orElse(null));
      return optional;
   }

   private void moveEntryToFront(int p_307277_) {
      if (p_307277_ > 0) {
         RecipeCache.Entry recipecache$entry = this.entries[p_307277_];
         System.arraycopy(this.entries, 0, this.entries, 1, p_307277_);
         this.entries[0] = recipecache$entry;
      }
   }

   private void insert(CraftingInput p_348525_, @Nullable RecipeHolder<CraftingRecipe> p_336146_) {
      NonNullList<ItemStack> nonnulllist = NonNullList.withSize(p_348525_.size(), ItemStack.EMPTY);

      for (int i = 0; i < p_348525_.size(); i++) {
         nonnulllist.set(i, p_348525_.getItem(i).copyWithCount(1));
      }

      System.arraycopy(this.entries, 0, this.entries, 1, this.entries.length - 1);
      this.entries[0] = new RecipeCache.Entry(nonnulllist, p_348525_.width(), p_348525_.height(), p_336146_);
   }

   record Entry(NonNullList<ItemStack> key, int width, int height, @Nullable RecipeHolder<CraftingRecipe> value) {
      public boolean matches(CraftingInput p_348535_) {
         if (this.width == p_348535_.width() && this.height == p_348535_.height()) {
            for (int i = 0; i < this.key.size(); i++) {
               if (!ItemStack.isSameItemSameComponents(this.key.get(i), p_348535_.getItem(i))) {
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
