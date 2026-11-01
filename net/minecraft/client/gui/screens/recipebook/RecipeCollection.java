package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeCollection {
   private final RegistryAccess registryAccess;
   private final List<RecipeHolder<?>> recipes;
   private final boolean singleResultItem;
   private final Set<RecipeHolder<?>> craftable = Sets.newHashSet();
   private final Set<RecipeHolder<?>> fitsDimensions = Sets.newHashSet();
   private final Set<RecipeHolder<?>> known = Sets.newHashSet();

   public RecipeCollection(RegistryAccess p_266782_, List<RecipeHolder<?>> p_267051_) {
      this.registryAccess = p_266782_;
      this.recipes = ImmutableList.copyOf(p_267051_);
      if (p_267051_.size() <= 1) {
         this.singleResultItem = true;
      } else {
         this.singleResultItem = allRecipesHaveSameResult(p_266782_, p_267051_);
      }
   }

   private static boolean allRecipesHaveSameResult(RegistryAccess p_267210_, List<RecipeHolder<?>> p_100509_) {
      int $$2 = p_100509_.size();
      ItemStack $$3 = p_100509_.get(0).value().getResultItem(p_267210_);

      for (int $$4 = 1; $$4 < $$2; $$4++) {
         ItemStack $$5 = p_100509_.get($$4).value().getResultItem(p_267210_);
         if (!ItemStack.isSameItemSameComponents($$3, $$5)) {
            return false;
         }
      }

      return true;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public boolean hasKnownRecipes() {
      return !this.known.isEmpty();
   }

   public void updateKnownRecipes(RecipeBook p_100500_) {
      for (RecipeHolder<?> $$1 : this.recipes) {
         if (p_100500_.contains($$1)) {
            this.known.add($$1);
         }
      }
   }

   public void canCraft(StackedContents p_100502_, int p_100503_, int p_100504_, RecipeBook p_100505_) {
      for (RecipeHolder<?> $$4 : this.recipes) {
         boolean $$5 = $$4.value().canCraftInDimensions(p_100503_, p_100504_) && p_100505_.contains($$4);
         if ($$5) {
            this.fitsDimensions.add($$4);
         } else {
            this.fitsDimensions.remove($$4);
         }

         if ($$5 && p_100502_.canCraft($$4.value(), null)) {
            this.craftable.add($$4);
         } else {
            this.craftable.remove($$4);
         }
      }
   }

   public boolean isCraftable(RecipeHolder<?> p_301167_) {
      return this.craftable.contains(p_301167_);
   }

   public boolean hasCraftable() {
      return !this.craftable.isEmpty();
   }

   public boolean hasFitting() {
      return !this.fitsDimensions.isEmpty();
   }

   public List<RecipeHolder<?>> getRecipes() {
      return this.recipes;
   }

   public List<RecipeHolder<?>> getRecipes(boolean p_100511_) {
      List<RecipeHolder<?>> $$1 = Lists.newArrayList();
      Set<RecipeHolder<?>> $$2 = p_100511_ ? this.craftable : this.fitsDimensions;

      for (RecipeHolder<?> $$3 : this.recipes) {
         if ($$2.contains($$3)) {
            $$1.add($$3);
         }
      }

      return $$1;
   }

   public List<RecipeHolder<?>> getDisplayRecipes(boolean p_100514_) {
      List<RecipeHolder<?>> $$1 = Lists.newArrayList();

      for (RecipeHolder<?> $$2 : this.recipes) {
         if (this.fitsDimensions.contains($$2) && this.craftable.contains($$2) == p_100514_) {
            $$1.add($$2);
         }
      }

      return $$1;
   }

   public boolean hasSingleResultItem() {
      return this.singleResultItem;
   }
}
