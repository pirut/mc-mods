package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverlayRecipeComponent implements Renderable, GuiEventListener {
   private static final ResourceLocation OVERLAY_RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/overlay_recipe");
   static final ResourceLocation FURNACE_OVERLAY_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
   static final ResourceLocation FURNACE_OVERLAY_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay");
   static final ResourceLocation CRAFTING_OVERLAY_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
   static final ResourceLocation CRAFTING_OVERLAY_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay");
   static final ResourceLocation FURNACE_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace(
      "recipe_book/furnace_overlay_disabled_highlighted"
   );
   static final ResourceLocation FURNACE_OVERLAY_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
   static final ResourceLocation CRAFTING_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace(
      "recipe_book/crafting_overlay_disabled_highlighted"
   );
   static final ResourceLocation CRAFTING_OVERLAY_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
   private static final int MAX_ROW = 4;
   private static final int MAX_ROW_LARGE = 5;
   private static final float ITEM_RENDER_SCALE = 0.375F;
   public static final int BUTTON_SIZE = 25;
   private final List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons = Lists.newArrayList();
   private boolean isVisible;
   private int x;
   private int y;
   private Minecraft minecraft;
   private RecipeCollection collection;
   @Nullable
   private RecipeHolder<?> lastRecipeClicked;
   float time;
   boolean isFurnaceMenu;

   public void init(Minecraft p_100195_, RecipeCollection p_100196_, int p_100197_, int p_100198_, int p_100199_, int p_100200_, float p_100201_) {
      this.minecraft = p_100195_;
      this.collection = p_100196_;
      if (p_100195_.player.containerMenu instanceof AbstractFurnaceMenu) {
         this.isFurnaceMenu = true;
      }

      boolean $$7 = p_100195_.player.getRecipeBook().isFiltering((RecipeBookMenu<?, ?>)p_100195_.player.containerMenu);
      List<RecipeHolder<?>> $$8 = p_100196_.getDisplayRecipes(true);
      List<RecipeHolder<?>> $$9 = $$7 ? Collections.emptyList() : p_100196_.getDisplayRecipes(false);
      int $$10 = $$8.size();
      int $$11 = $$10 + $$9.size();
      int $$12 = $$11 <= 16 ? 4 : 5;
      int $$13 = (int)Math.ceil((float)$$11 / $$12);
      this.x = p_100197_;
      this.y = p_100198_;
      float $$14 = this.x + Math.min($$11, $$12) * 25;
      float $$15 = p_100199_ + 50;
      if ($$14 > $$15) {
         this.x = (int)(this.x - p_100201_ * (int)(($$14 - $$15) / p_100201_));
      }

      float $$16 = this.y + $$13 * 25;
      float $$17 = p_100200_ + 50;
      if ($$16 > $$17) {
         this.y = (int)(this.y - p_100201_ * Mth.ceil(($$16 - $$17) / p_100201_));
      }

      float $$18 = this.y;
      float $$19 = p_100200_ - 100;
      if ($$18 < $$19) {
         this.y = (int)(this.y - p_100201_ * Mth.ceil(($$18 - $$19) / p_100201_));
      }

      this.isVisible = true;
      this.recipeButtons.clear();

      for (int $$20 = 0; $$20 < $$11; $$20++) {
         boolean $$21 = $$20 < $$10;
         RecipeHolder<?> $$22 = $$21 ? $$8.get($$20) : $$9.get($$20 - $$10);
         int $$23 = this.x + 4 + 25 * ($$20 % $$12);
         int $$24 = this.y + 5 + 25 * ($$20 / $$12);
         if (this.isFurnaceMenu) {
            this.recipeButtons.add(new OverlayRecipeComponent.OverlaySmeltingRecipeButton($$23, $$24, $$22, $$21));
         } else {
            this.recipeButtons.add(new OverlayRecipeComponent.OverlayRecipeButton($$23, $$24, $$22, $$21));
         }
      }

      this.lastRecipeClicked = null;
   }

   public RecipeCollection getRecipeCollection() {
      return this.collection;
   }

   @Nullable
   public RecipeHolder<?> getLastRecipeClicked() {
      return this.lastRecipeClicked;
   }

   @Override
   public boolean mouseClicked(double p_100186_, double p_100187_, int p_100188_) {
      if (p_100188_ != 0) {
         return false;
      } else {
         for (OverlayRecipeComponent.OverlayRecipeButton $$3 : this.recipeButtons) {
            if ($$3.mouseClicked(p_100186_, p_100187_, p_100188_)) {
               this.lastRecipeClicked = $$3.recipe;
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean isMouseOver(double p_100208_, double p_100209_) {
      return false;
   }

   @Override
   public void render(GuiGraphics p_281618_, int p_282646_, int p_283687_, float p_283147_) {
      if (this.isVisible) {
         this.time += p_283147_;
         RenderSystem.enableBlend();
         p_281618_.pose().pushPose();
         p_281618_.pose().translate(0.0F, 0.0F, 1000.0F);
         int $$4 = this.recipeButtons.size() <= 16 ? 4 : 5;
         int $$5 = Math.min(this.recipeButtons.size(), $$4);
         int $$6 = Mth.ceil((float)this.recipeButtons.size() / $$4);
         int $$7 = 4;
         p_281618_.blitSprite(OVERLAY_RECIPE_SPRITE, this.x, this.y, $$5 * 25 + 8, $$6 * 25 + 8);
         RenderSystem.disableBlend();

         for (OverlayRecipeComponent.OverlayRecipeButton $$8 : this.recipeButtons) {
            $$8.render(p_281618_, p_282646_, p_283687_, p_283147_);
         }

         p_281618_.pose().popPose();
      }
   }

   public void setVisible(boolean p_100205_) {
      this.isVisible = p_100205_;
   }

   public boolean isVisible() {
      return this.isVisible;
   }

   @Override
   public void setFocused(boolean p_265597_) {
   }

   @Override
   public boolean isFocused() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   class OverlayRecipeButton extends AbstractWidget implements PlaceRecipe<Ingredient> {
      final RecipeHolder<?> recipe;
      private final boolean isCraftable;
      protected final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> ingredientPos = Lists.newArrayList();

      public OverlayRecipeButton(int p_100232_, int p_100233_, RecipeHolder<?> p_301240_, boolean p_100235_) {
         super(p_100232_, p_100233_, 200, 20, CommonComponents.EMPTY);
         this.width = 24;
         this.height = 24;
         this.recipe = p_301240_;
         this.isCraftable = p_100235_;
         this.calculateIngredientsPositions(p_301240_);
      }

      protected void calculateIngredientsPositions(RecipeHolder<?> p_301128_) {
         this.placeRecipe(3, 3, -1, p_301128_, p_301128_.value().getIngredients().iterator(), 0);
      }

      @Override
      public void updateWidgetNarration(NarrationElementOutput p_259646_) {
         this.defaultButtonNarrationText(p_259646_);
      }

      public void addItemToSlot(Ingredient p_344853_, int p_100241_, int p_100242_, int p_100243_, int p_100244_) {
         ItemStack[] $$5 = p_344853_.getItems();
         if ($$5.length != 0) {
            this.ingredientPos.add(new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + p_100243_ * 7, 3 + p_100244_ * 7, $$5));
         }
      }

      @Override
      public void renderWidget(GuiGraphics p_283557_, int p_283483_, int p_282919_, float p_282165_) {
         ResourceLocation $$4;
         if (this.isCraftable) {
            if (OverlayRecipeComponent.this.isFurnaceMenu) {
               $$4 = this.isHoveredOrFocused() ? OverlayRecipeComponent.FURNACE_OVERLAY_HIGHLIGHTED_SPRITE : OverlayRecipeComponent.FURNACE_OVERLAY_SPRITE;
            } else {
               $$4 = this.isHoveredOrFocused() ? OverlayRecipeComponent.CRAFTING_OVERLAY_HIGHLIGHTED_SPRITE : OverlayRecipeComponent.CRAFTING_OVERLAY_SPRITE;
            }
         } else if (OverlayRecipeComponent.this.isFurnaceMenu) {
            $$4 = this.isHoveredOrFocused()
               ? OverlayRecipeComponent.FURNACE_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE
               : OverlayRecipeComponent.FURNACE_OVERLAY_DISABLED_SPRITE;
         } else {
            $$4 = this.isHoveredOrFocused()
               ? OverlayRecipeComponent.CRAFTING_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE
               : OverlayRecipeComponent.CRAFTING_OVERLAY_DISABLED_SPRITE;
         }

         p_283557_.blitSprite($$4, this.getX(), this.getY(), this.width, this.height);
         p_283557_.pose().pushPose();
         p_283557_.pose().translate((double)(this.getX() + 2), (double)(this.getY() + 2), 150.0);

         for (OverlayRecipeComponent.OverlayRecipeButton.Pos $$8 : this.ingredientPos) {
            p_283557_.pose().pushPose();
            p_283557_.pose().translate((double)$$8.x, (double)$$8.y, 0.0);
            p_283557_.pose().scale(0.375F, 0.375F, 1.0F);
            p_283557_.pose().translate(-8.0, -8.0, 0.0);
            if ($$8.ingredients.length > 0) {
               p_283557_.renderItem($$8.ingredients[Mth.floor(OverlayRecipeComponent.this.time / 30.0F) % $$8.ingredients.length], 0, 0);
            }

            p_283557_.pose().popPose();
         }

         p_283557_.pose().popPose();
      }

      @OnlyIn(Dist.CLIENT)
      protected class Pos {
         public final ItemStack[] ingredients;
         public final int x;
         public final int y;

         public Pos(int p_100256_, int p_100257_, ItemStack[] p_100258_) {
            this.x = p_100256_;
            this.y = p_100257_;
            this.ingredients = p_100258_;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      public OverlaySmeltingRecipeButton(int p_100262_, int p_100263_, RecipeHolder<?> p_301104_, boolean p_100265_) {
         super(p_100262_, p_100263_, p_301104_, p_100265_);
      }

      @Override
      protected void calculateIngredientsPositions(RecipeHolder<?> p_301196_) {
         Ingredient $$1 = p_301196_.value().getIngredients().get(0);
         ItemStack[] $$2 = $$1.getItems();
         this.ingredientPos.add(new OverlayRecipeComponent.OverlayRecipeButton.Pos(10, 10, $$2));
      }
   }
}
