package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OverlayRecipeComponent implements Renderable, GuiEventListener {
   private static final Identifier OVERLAY_RECIPE_SPRITE = Identifier.withDefaultNamespace("recipe_book/overlay_recipe");
   private static final int MAX_ROW = 4;
   private static final int MAX_ROW_LARGE = 5;
   private static final float ITEM_RENDER_SCALE = 0.375F;
   public static final int BUTTON_SIZE = 25;
   private final List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons = Lists.newArrayList();
   private boolean isVisible;
   private int x;
   private int y;
   private RecipeCollection collection = RecipeCollection.EMPTY;
   @Nullable
   private RecipeDisplayId lastRecipeClicked;
   final SlotSelectTime slotSelectTime;
   private final boolean isFurnaceMenu;

   public OverlayRecipeComponent(SlotSelectTime p_365007_, boolean p_364041_) {
      this.slotSelectTime = p_365007_;
      this.isFurnaceMenu = p_364041_;
   }

   public void init(
      RecipeCollection p_100196_, ContextMap p_381080_, boolean p_361594_, int p_100197_, int p_100198_, int p_100199_, int p_100200_, float p_100201_
   ) {
      this.collection = p_100196_;
      List<RecipeDisplayEntry> list = p_100196_.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
      List<RecipeDisplayEntry> list1 = p_361594_ ? Collections.emptyList() : p_100196_.getSelectedRecipes(RecipeCollection.CraftableStatus.NOT_CRAFTABLE);
      int i = list.size();
      int j = i + list1.size();
      int k = j <= 16 ? 4 : 5;
      int l = (int)Math.ceil((float)j / k);
      this.x = p_100197_;
      this.y = p_100198_;
      float f = this.x + Math.min(j, k) * 25;
      float f1 = p_100199_ + 50;
      if (f > f1) {
         this.x = (int)(this.x - p_100201_ * (int)((f - f1) / p_100201_));
      }

      float f2 = this.y + l * 25;
      float f3 = p_100200_ + 50;
      if (f2 > f3) {
         this.y = (int)(this.y - p_100201_ * Mth.ceil((f2 - f3) / p_100201_));
      }

      float f4 = this.y;
      float f5 = p_100200_ - 100;
      if (f4 < f5) {
         this.y = (int)(this.y - p_100201_ * Mth.ceil((f4 - f5) / p_100201_));
      }

      this.isVisible = true;
      this.recipeButtons.clear();

      for (int i1 = 0; i1 < j; i1++) {
         boolean flag = i1 < i;
         RecipeDisplayEntry recipedisplayentry = flag ? list.get(i1) : list1.get(i1 - i);
         int j1 = this.x + 4 + 25 * (i1 % k);
         int k1 = this.y + 5 + 25 * (i1 / k);
         if (this.isFurnaceMenu) {
            this.recipeButtons
               .add(new OverlayRecipeComponent.OverlaySmeltingRecipeButton(j1, k1, recipedisplayentry.id(), recipedisplayentry.display(), p_381080_, flag));
         } else {
            this.recipeButtons
               .add(new OverlayRecipeComponent.OverlayCraftingRecipeButton(j1, k1, recipedisplayentry.id(), recipedisplayentry.display(), p_381080_, flag));
         }
      }

      this.lastRecipeClicked = null;
   }

   public RecipeCollection getRecipeCollection() {
      return this.collection;
   }

   @Nullable
   public RecipeDisplayId getLastRecipeClicked() {
      return this.lastRecipeClicked;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_446742_, boolean p_435819_) {
      if (p_446742_.button() != 0) {
         return false;
      } else {
         for (OverlayRecipeComponent.OverlayRecipeButton overlayrecipecomponent$overlayrecipebutton : this.recipeButtons) {
            if (overlayrecipecomponent$overlayrecipebutton.mouseClicked(p_446742_, p_435819_)) {
               this.lastRecipeClicked = overlayrecipecomponent$overlayrecipebutton.recipe;
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
         int i = this.recipeButtons.size() <= 16 ? 4 : 5;
         int j = Math.min(this.recipeButtons.size(), i);
         int k = Mth.ceil((float)this.recipeButtons.size() / i);
         int l = 4;
         p_281618_.blitSprite(RenderPipelines.GUI_TEXTURED, OVERLAY_RECIPE_SPRITE, this.x, this.y, j * 25 + 8, k * 25 + 8);

         for (OverlayRecipeComponent.OverlayRecipeButton overlayrecipecomponent$overlayrecipebutton : this.recipeButtons) {
            overlayrecipecomponent$overlayrecipebutton.render(p_281618_, p_282646_, p_283687_, p_283147_);
         }
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
   class OverlayCraftingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      private static final Identifier ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay");
      private static final Identifier HIGHLIGHTED_ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
      private static final Identifier DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
      private static final Identifier HIGHLIGHTED_DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_disabled_highlighted");
      private static final int GRID_WIDTH = 3;
      private static final int GRID_HEIGHT = 3;

      public OverlayCraftingRecipeButton(
         int p_365371_, int p_362152_, RecipeDisplayId p_379848_, RecipeDisplay p_379906_, ContextMap p_381098_, boolean p_361389_
      ) {
         super(p_365371_, p_362152_, p_379848_, p_361389_, calculateIngredientsPositions(p_379906_, p_381098_));
      }

      private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay p_380007_, ContextMap p_381009_) {
         List<OverlayRecipeComponent.OverlayRecipeButton.Pos> list = new ArrayList<>();
         switch (p_380007_) {
            case ShapedCraftingRecipeDisplay shapedcraftingrecipedisplay:
               PlaceRecipeHelper.placeRecipe(
                  3,
                  3,
                  shapedcraftingrecipedisplay.width(),
                  shapedcraftingrecipedisplay.height(),
                  shapedcraftingrecipedisplay.ingredients(),
                  (p_380792_, p_380793_, p_380794_, p_380795_) -> {
                     List<ItemStack> list3 = p_380792_.resolveForStacks(p_381009_);
                     if (!list3.isEmpty()) {
                        list.add(createGridPos(p_380794_, p_380795_, list3));
                     }
                  }
               );
               break;
            case ShapelessCraftingRecipeDisplay shapelesscraftingrecipedisplay:
               label19: {
                  List<SlotDisplay> list1 = shapelesscraftingrecipedisplay.ingredients();

                  for (int i = 0; i < list1.size(); i++) {
                     List<ItemStack> list2 = list1.get(i).resolveForStacks(p_381009_);
                     if (!list2.isEmpty()) {
                        list.add(createGridPos(i % 3, i / 3, list2));
                     }
                  }
                  break label19;
               }
            default:
         }

         return list;
      }

      @Override
      protected Identifier getSprite(boolean p_363013_) {
         if (p_363013_) {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
         } else {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract class OverlayRecipeButton extends AbstractWidget {
      final RecipeDisplayId recipe;
      private final boolean isCraftable;
      private final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> slots;

      public OverlayRecipeButton(
         int p_100232_, int p_100233_, RecipeDisplayId p_380153_, boolean p_100235_, List<OverlayRecipeComponent.OverlayRecipeButton.Pos> p_360663_
      ) {
         super(p_100232_, p_100233_, 24, 24, CommonComponents.EMPTY);
         this.slots = p_360663_;
         this.recipe = p_380153_;
         this.isCraftable = p_100235_;
      }

      protected static OverlayRecipeComponent.OverlayRecipeButton.Pos createGridPos(int p_364955_, int p_365288_, List<ItemStack> p_362300_) {
         return new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + p_364955_ * 7, 3 + p_365288_ * 7, p_362300_);
      }

      protected abstract Identifier getSprite(boolean var1);

      @Override
      public void updateWidgetNarration(NarrationElementOutput p_259646_) {
         this.defaultButtonNarrationText(p_259646_);
      }

      @Override
      public void renderWidget(GuiGraphics p_283557_, int p_283483_, int p_282919_, float p_282165_) {
         p_283557_.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(this.isCraftable), this.getX(), this.getY(), this.width, this.height);
         float f = this.getX() + 2;
         float f1 = this.getY() + 2;

         for (OverlayRecipeComponent.OverlayRecipeButton.Pos overlayrecipecomponent$overlayrecipebutton$pos : this.slots) {
            p_283557_.pose().pushMatrix();
            p_283557_.pose().translate(f + overlayrecipecomponent$overlayrecipebutton$pos.x, f1 + overlayrecipecomponent$overlayrecipebutton$pos.y);
            p_283557_.pose().scale(0.375F, 0.375F);
            p_283557_.pose().translate(-8.0F, -8.0F);
            p_283557_.renderItem(
               overlayrecipecomponent$overlayrecipebutton$pos.selectIngredient(OverlayRecipeComponent.this.slotSelectTime.currentIndex()), 0, 0
            );
            p_283557_.pose().popMatrix();
         }
      }

      @OnlyIn(Dist.CLIENT)
      protected record Pos(int x, int y, List<ItemStack> ingredients) {
         public Pos(int x, int y, List<ItemStack> ingredients) {
            if (ingredients.isEmpty()) {
               throw new IllegalArgumentException("Ingredient list must be non-empty");
            } else {
               this.x = x;
               this.y = y;
               this.ingredients = ingredients;
            }
         }

         public ItemStack selectIngredient(int p_361926_) {
            return this.ingredients.get(p_361926_ % this.ingredients.size());
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      private static final Identifier ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay");
      private static final Identifier HIGHLIGHTED_ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
      private static final Identifier DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
      private static final Identifier HIGHLIGHTED_DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_disabled_highlighted");

      public OverlaySmeltingRecipeButton(
         int p_100262_, int p_100263_, RecipeDisplayId p_379828_, RecipeDisplay p_380374_, ContextMap p_381025_, boolean p_100265_
      ) {
         super(p_100262_, p_100263_, p_379828_, p_100265_, calculateIngredientsPositions(p_380374_, p_381025_));
      }

      private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay p_379813_, ContextMap p_381001_) {
         if (p_379813_ instanceof FurnaceRecipeDisplay furnacerecipedisplay) {
            List<ItemStack> list = furnacerecipedisplay.ingredient().resolveForStacks(p_381001_);
            if (!list.isEmpty()) {
               return List.of(createGridPos(1, 1, list));
            }
         }

         return List.of();
      }

      @Override
      protected Identifier getSprite(boolean p_363404_) {
         if (p_363404_) {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
         } else {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
         }
      }
   }
}
