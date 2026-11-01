package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GhostRecipe {
   @Nullable
   private RecipeHolder<?> recipe;
   private final List<GhostRecipe.GhostIngredient> ingredients = Lists.newArrayList();
   float time;

   public void clear() {
      this.recipe = null;
      this.ingredients.clear();
      this.time = 0.0F;
   }

   public void addIngredient(Ingredient p_100144_, int p_100145_, int p_100146_) {
      this.ingredients.add(new GhostRecipe.GhostIngredient(p_100144_, p_100145_, p_100146_));
   }

   public GhostRecipe.GhostIngredient get(int p_100142_) {
      return this.ingredients.get(p_100142_);
   }

   public int size() {
      return this.ingredients.size();
   }

   @Nullable
   public RecipeHolder<?> getRecipe() {
      return this.recipe;
   }

   public void setRecipe(RecipeHolder<?> p_300899_) {
      this.recipe = p_300899_;
   }

   public void render(GuiGraphics p_282081_, Minecraft p_281341_, int p_283169_, int p_282326_, boolean p_282174_, float p_282256_) {
      if (!Screen.hasControlDown()) {
         this.time += p_282256_;
      }

      for (int $$6 = 0; $$6 < this.ingredients.size(); $$6++) {
         GhostRecipe.GhostIngredient $$7 = this.ingredients.get($$6);
         int $$8 = $$7.getX() + p_283169_;
         int $$9 = $$7.getY() + p_282326_;
         if ($$6 == 0 && p_282174_) {
            p_282081_.fill($$8 - 4, $$9 - 4, $$8 + 20, $$9 + 20, 822018048);
         } else {
            p_282081_.fill($$8, $$9, $$8 + 16, $$9 + 16, 822018048);
         }

         ItemStack $$10 = $$7.getItem();
         p_282081_.renderFakeItem($$10, $$8, $$9);
         p_282081_.fill(RenderType.guiGhostRecipeOverlay(), $$8, $$9, $$8 + 16, $$9 + 16, 822083583);
         if ($$6 == 0) {
            p_282081_.renderItemDecorations(p_281341_.font, $$10, $$8, $$9);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class GhostIngredient {
      private final Ingredient ingredient;
      private final int x;
      private final int y;

      public GhostIngredient(Ingredient p_100166_, int p_100167_, int p_100168_) {
         this.ingredient = p_100166_;
         this.x = p_100167_;
         this.y = p_100168_;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public ItemStack getItem() {
         ItemStack[] $$0 = this.ingredient.getItems();
         return $$0.length == 0 ? ItemStack.EMPTY : $$0[Mth.floor(GhostRecipe.this.time / 30.0F) % $$0.length];
      }
   }
}
