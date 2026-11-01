package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
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

   public OverlayRecipeComponent(SlotSelectTime slotSelectTime, boolean isFurnaceMenu) {
      this.slotSelectTime = slotSelectTime;
      this.isFurnaceMenu = isFurnaceMenu;
   }

   public void init(RecipeCollection collection, ContextMap contextMap, boolean isFiltering, int x, int y, int overlayX, int overlayY, float width) {
      this.collection = collection;
      List<RecipeDisplayEntry> list = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
      List<RecipeDisplayEntry> list2 = isFiltering ? Collections.emptyList() : collection.getSelectedRecipes(RecipeCollection.CraftableStatus.NOT_CRAFTABLE);
      int i = list.size();
      int j = i + list2.size();
      int k = j <= 16 ? 4 : 5;
      int l = (int)Math.ceil((float)j / k);
      this.x = x;
      this.y = y;
      float f = this.x + Math.min(j, k) * 25;
      float g = overlayX + 50;
      if (f > g) {
         this.x = (int)(this.x - width * (int)((f - g) / width));
      }

      float h = this.y + l * 25;
      float m = overlayY + 50;
      if (h > m) {
         this.y = (int)(this.y - width * Mth.ceil((h - m) / width));
      }

      float n = this.y;
      float o = overlayY - 100;
      if (n < o) {
         this.y = (int)(this.y - width * Mth.ceil((n - o) / width));
      }

      this.isVisible = true;
      this.recipeButtons.clear();

      for (int p = 0; p < j; p++) {
         boolean bl = p < i;
         RecipeDisplayEntry recipeDisplayEntry = bl ? list.get(p) : list2.get(p - i);
         int q = this.x + 4 + 25 * (p % k);
         int r = this.y + 5 + 25 * (p / k);
         if (this.isFurnaceMenu) {
            this.recipeButtons
               .add(new OverlayRecipeComponent.OverlaySmeltingRecipeButton(q, r, recipeDisplayEntry.id(), recipeDisplayEntry.display(), contextMap, bl));
         } else {
            this.recipeButtons
               .add(new OverlayRecipeComponent.OverlayCraftingRecipeButton(q, r, recipeDisplayEntry.id(), recipeDisplayEntry.display(), contextMap, bl));
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
   public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      if (event.button() != 0) {
         return false;
      } else {
         for (OverlayRecipeComponent.OverlayRecipeButton overlayRecipeButton : this.recipeButtons) {
            if (overlayRecipeButton.mouseClicked(event, isDoubleClick)) {
               this.lastRecipeClicked = overlayRecipeButton.recipe;
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return false;
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      if (this.isVisible) {
         int i = this.recipeButtons.size() <= 16 ? 4 : 5;
         int j = Math.min(this.recipeButtons.size(), i);
         int k = Mth.ceil((float)this.recipeButtons.size() / i);
         int l = 4;
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, OVERLAY_RECIPE_SPRITE, this.x, this.y, j * 25 + 8, k * 25 + 8);

         for (OverlayRecipeComponent.OverlayRecipeButton overlayRecipeButton : this.recipeButtons) {
            overlayRecipeButton.render(guiGraphics, mouseX, mouseY, partialTick);
         }
      }
   }

   public void setVisible(boolean isVisible) {
      this.isVisible = isVisible;
   }

   public boolean isVisible() {
      return this.isVisible;
   }

   @Override
   public void setFocused(boolean focused) {
   }

   @Override
   public boolean isFocused() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   class OverlayCraftingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      private static final Identifier ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay");
      private static final Identifier HIGHLIGHTED_ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
      private static final Identifier DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
      private static final Identifier HIGHLIGHTED_DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_disabled_highlighted");
      private static final int GRID_WIDTH = 3;
      private static final int GRID_HEIGHT = 3;

      public OverlayCraftingRecipeButton(
         final int x, final int y, final RecipeDisplayId recipe, final RecipeDisplay recipeDisplay, final ContextMap contextMap, final boolean isCraftable
      ) {
         super(x, y, recipe, isCraftable, calculateIngredientsPositions(recipeDisplay, contextMap));
      }

      private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay recipeDisplay, ContextMap contextMap) {
         List<OverlayRecipeComponent.OverlayRecipeButton.Pos> list = new ArrayList<>();
         switch (recipeDisplay) {
            case ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay:
               PlaceRecipeHelper.placeRecipe(
                  3,
                  3,
                  shapedCraftingRecipeDisplay.width(),
                  shapedCraftingRecipeDisplay.height(),
                  shapedCraftingRecipeDisplay.ingredients(),
                  (slotDisplay, ix, j, k) -> {
                     List<ItemStack> list2x = slotDisplay.resolveForStacks(contextMap);
                     if (!list2x.isEmpty()) {
                        list.add(createGridPos(j, k, list2x));
                     }
                  }
               );
               break;
            case ShapelessCraftingRecipeDisplay shapelessCraftingRecipeDisplay:
               label19: {
                  List<SlotDisplay> list2 = shapelessCraftingRecipeDisplay.ingredients();

                  for (int i = 0; i < list2.size(); i++) {
                     List<ItemStack> list3 = list2.get(i).resolveForStacks(contextMap);
                     if (!list3.isEmpty()) {
                        list.add(createGridPos(i % 3, i / 3, list3));
                     }
                  }
                  break label19;
               }
            default:
         }

         return list;
      }

      @Override
      protected Identifier getSprite(boolean enabled) {
         if (enabled) {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
         } else {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   abstract class OverlayRecipeButton extends AbstractWidget {
      final RecipeDisplayId recipe;
      private final boolean isCraftable;
      private final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> slots;

      public OverlayRecipeButton(
         final int x, final int y, final RecipeDisplayId recipe, final boolean isCraftable, final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> slots
      ) {
         super(x, y, 24, 24, CommonComponents.EMPTY);
         this.slots = slots;
         this.recipe = recipe;
         this.isCraftable = isCraftable;
      }

      protected static OverlayRecipeComponent.OverlayRecipeButton.Pos createGridPos(int x, int y, List<ItemStack> possibleItems) {
         return new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + x * 7, 3 + y * 7, possibleItems);
      }

      protected abstract Identifier getSprite(boolean enabled);

      @Override
      public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
         this.defaultButtonNarrationText(narrationElementOutput);
      }

      @Override
      public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(this.isCraftable), this.getX(), this.getY(), this.width, this.height);
         float f = this.getX() + 2;
         float g = this.getY() + 2;

         for (OverlayRecipeComponent.OverlayRecipeButton.Pos pos : this.slots) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(f + pos.x, g + pos.y);
            guiGraphics.pose().scale(0.375F, 0.375F);
            guiGraphics.pose().translate(-8.0F, -8.0F);
            guiGraphics.renderItem(pos.selectIngredient(OverlayRecipeComponent.this.slotSelectTime.currentIndex()), 0, 0);
            guiGraphics.pose().popMatrix();
         }
      }

      @Environment(EnvType.CLIENT)
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

         public ItemStack selectIngredient(int index) {
            return this.ingredients.get(index % this.ingredients.size());
         }
      }
   }

   @Environment(EnvType.CLIENT)
   class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      private static final Identifier ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay");
      private static final Identifier HIGHLIGHTED_ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
      private static final Identifier DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
      private static final Identifier HIGHLIGHTED_DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_disabled_highlighted");

      public OverlaySmeltingRecipeButton(
         final int x, final int y, final RecipeDisplayId recipe, final RecipeDisplay recipeDisplay, final ContextMap contextMap, final boolean isCraftable
      ) {
         super(x, y, recipe, isCraftable, calculateIngredientsPositions(recipeDisplay, contextMap));
      }

      private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay recipeDisplay, ContextMap contextMap) {
         if (recipeDisplay instanceof FurnaceRecipeDisplay furnaceRecipeDisplay) {
            List<ItemStack> list = furnaceRecipeDisplay.ingredient().resolveForStacks(contextMap);
            if (!list.isEmpty()) {
               return List.of(createGridPos(1, 1, list));
            }
         }

         return List.of();
      }

      @Override
      protected Identifier getSprite(boolean enabled) {
         if (enabled) {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
         } else {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
         }
      }
   }
}
