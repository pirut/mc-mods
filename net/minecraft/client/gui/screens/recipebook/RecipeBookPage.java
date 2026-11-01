package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RecipeBookPage {
   public static final int ITEMS_PER_PAGE = 20;
   private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/page_forward"), Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted")
   );
   private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/page_backward"), Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted")
   );
   private static final Component NEXT_PAGE_TEXT = Component.translatable("gui.recipebook.next_page");
   private static final Component PREVIOUS_PAGE_TEXT = Component.translatable("gui.recipebook.previous_page");
   private static final int TURN_PAGE_SPRITE_WIDTH = 12;
   private static final int TURN_PAGE_SPRITE_HEIGHT = 17;
   private final List<RecipeButton> buttons = Lists.newArrayListWithCapacity(20);
   @Nullable
   private RecipeButton hoveredButton;
   private final OverlayRecipeComponent overlay;
   private Minecraft minecraft;
   private final RecipeBookComponent<?> parent;
   private List<RecipeCollection> recipeCollections = ImmutableList.of();
   @Nullable
   private ImageButton forwardButton;
   @Nullable
   private ImageButton backButton;
   private int totalPages;
   private int currentPage;
   private ClientRecipeBook recipeBook;
   @Nullable
   private RecipeDisplayId lastClickedRecipe;
   @Nullable
   private RecipeCollection lastClickedRecipeCollection;
   private boolean isFiltering;

   public RecipeBookPage(RecipeBookComponent<?> parent, SlotSelectTime slotSelectTime, boolean isFurnaceMenu) {
      this.parent = parent;
      this.overlay = new OverlayRecipeComponent(slotSelectTime, isFurnaceMenu);

      for (int i = 0; i < 20; i++) {
         this.buttons.add(new RecipeButton(slotSelectTime));
      }
   }

   public void init(Minecraft minecraft, int x, int y) {
      this.minecraft = minecraft;
      this.recipeBook = minecraft.player.getRecipeBook();

      for (int i = 0; i < this.buttons.size(); i++) {
         this.buttons.get(i).setPosition(x + 11 + 25 * (i % 5), y + 31 + 25 * (i / 5));
      }

      this.forwardButton = new ImageButton(x + 93, y + 137, 12, 17, PAGE_FORWARD_SPRITES, button -> this.updateArrowButtons(), NEXT_PAGE_TEXT);
      this.forwardButton.setTooltip(Tooltip.create(NEXT_PAGE_TEXT));
      this.backButton = new ImageButton(x + 38, y + 137, 12, 17, PAGE_BACKWARD_SPRITES, button -> this.updateArrowButtons(), PREVIOUS_PAGE_TEXT);
      this.backButton.setTooltip(Tooltip.create(PREVIOUS_PAGE_TEXT));
   }

   public void updateCollections(List<RecipeCollection> recipeCollections, boolean resetPageNumber, boolean isFiltering) {
      this.recipeCollections = recipeCollections;
      this.isFiltering = isFiltering;
      this.totalPages = (int)Math.ceil(recipeCollections.size() / 20.0);
      if (this.totalPages <= this.currentPage || resetPageNumber) {
         this.currentPage = 0;
      }

      this.updateButtonsForPage();
   }

   private void updateButtonsForPage() {
      int i = 20 * this.currentPage;
      ContextMap contextMap = SlotDisplayContext.fromLevel(this.minecraft.level);

      for (int j = 0; j < this.buttons.size(); j++) {
         RecipeButton recipeButton = this.buttons.get(j);
         if (i + j < this.recipeCollections.size()) {
            RecipeCollection recipeCollection = this.recipeCollections.get(i + j);
            recipeButton.init(recipeCollection, this.isFiltering, this, contextMap);
            recipeButton.visible = true;
         } else {
            recipeButton.visible = false;
         }
      }

      this.updateArrowButtons();
   }

   private void updateArrowButtons() {
      if (this.forwardButton != null) {
         this.forwardButton.visible = this.totalPages > 1 && this.currentPage < this.totalPages - 1;
      }

      if (this.backButton != null) {
         this.backButton.visible = this.totalPages > 1 && this.currentPage > 0;
      }
   }

   public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
      if (this.totalPages > 1) {
         Component component = Component.translatable("gui.recipebook.page", this.currentPage + 1, this.totalPages);
         int i = this.minecraft.font.width(component);
         guiGraphics.drawString(this.minecraft.font, component, x - i / 2 + 73, y + 141, -1);
      }

      this.hoveredButton = null;

      for (RecipeButton recipeButton : this.buttons) {
         recipeButton.render(guiGraphics, mouseX, mouseY, partialTick);
         if (recipeButton.visible && recipeButton.isHoveredOrFocused()) {
            this.hoveredButton = recipeButton;
         }
      }

      if (this.forwardButton != null) {
         this.forwardButton.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      if (this.backButton != null) {
         this.backButton.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      guiGraphics.nextStratum();
      this.overlay.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   public void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
      if (this.minecraft.screen != null && this.hoveredButton != null && !this.overlay.isVisible()) {
         ItemStack itemStack = this.hoveredButton.getDisplayStack();
         Identifier identifier = itemStack.get(DataComponents.TOOLTIP_STYLE);
         guiGraphics.setComponentTooltipForNextFrame(this.minecraft.font, this.hoveredButton.getTooltipText(itemStack), x, y, identifier);
      }
   }

   @Nullable
   public RecipeDisplayId getLastClickedRecipe() {
      return this.lastClickedRecipe;
   }

   @Nullable
   public RecipeCollection getLastClickedRecipeCollection() {
      return this.lastClickedRecipeCollection;
   }

   public void setInvisible() {
      this.overlay.setVisible(false);
   }

   public boolean mouseClicked(MouseButtonEvent event, int x, int y, int width, int height, boolean isDoubleClick) {
      this.lastClickedRecipe = null;
      this.lastClickedRecipeCollection = null;
      if (this.overlay.isVisible()) {
         if (this.overlay.mouseClicked(event, isDoubleClick)) {
            this.lastClickedRecipe = this.overlay.getLastRecipeClicked();
            this.lastClickedRecipeCollection = this.overlay.getRecipeCollection();
         } else {
            this.overlay.setVisible(false);
         }

         return true;
      } else if (this.forwardButton.mouseClicked(event, isDoubleClick)) {
         this.currentPage++;
         this.updateButtonsForPage();
         return true;
      } else if (this.backButton.mouseClicked(event, isDoubleClick)) {
         this.currentPage--;
         this.updateButtonsForPage();
         return true;
      } else {
         ContextMap contextMap = SlotDisplayContext.fromLevel(this.minecraft.level);

         for (RecipeButton recipeButton : this.buttons) {
            if (recipeButton.mouseClicked(event, isDoubleClick)) {
               if (event.button() == 0) {
                  this.lastClickedRecipe = recipeButton.getCurrentRecipe();
                  this.lastClickedRecipeCollection = recipeButton.getCollection();
               } else if (event.button() == 1 && !this.overlay.isVisible() && !recipeButton.isOnlyOption()) {
                  this.overlay
                     .init(
                        recipeButton.getCollection(),
                        contextMap,
                        this.isFiltering,
                        recipeButton.getX(),
                        recipeButton.getY(),
                        x + width / 2,
                        y + 13 + height / 2,
                        recipeButton.getWidth()
                     );
               }

               return true;
            }
         }

         return false;
      }
   }

   public void recipeShown(RecipeDisplayId recipe) {
      this.parent.recipeShown(recipe);
   }

   public ClientRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   protected void listButtons(Consumer<AbstractWidget> consumer) {
      this.buttons.forEach(consumer);
   }
}
