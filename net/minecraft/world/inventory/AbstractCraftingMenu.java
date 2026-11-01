package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public abstract class AbstractCraftingMenu extends RecipeBookMenu {
   private final int width;
   private final int height;
   protected final CraftingContainer craftSlots;
   protected final ResultContainer resultSlots = new ResultContainer();

   public AbstractCraftingMenu(MenuType<?> p_362493_, int p_360673_, int p_364200_, int p_363034_) {
      super(p_362493_, p_360673_);
      this.width = p_364200_;
      this.height = p_363034_;
      this.craftSlots = new TransientCraftingContainer(this, p_364200_, p_363034_);
   }

   protected Slot addResultSlot(Player p_362550_, int p_361054_, int p_363126_) {
      return this.addSlot(new ResultSlot(p_362550_, this.craftSlots, this.resultSlots, 0, p_361054_, p_363126_));
   }

   protected void addCraftingGridSlots(int p_360345_, int p_361544_) {
      for (int i = 0; i < this.width; i++) {
         for (int j = 0; j < this.height; j++) {
            this.addSlot(new Slot(this.craftSlots, j + i * this.width, p_360345_ + j * 18, p_361544_ + i * 18));
         }
      }
   }

   @Override
   public RecipeBookMenu.PostPlaceAction handlePlacement(
      boolean p_361638_, boolean p_361841_, RecipeHolder<?> p_364981_, ServerLevel p_379885_, Inventory p_361078_
   ) {
      RecipeHolder<CraftingRecipe> recipeholder = (RecipeHolder<CraftingRecipe>)p_364981_;
      this.beginPlacingRecipe();

      RecipeBookMenu.PostPlaceAction recipebookmenu$postplaceaction;
      try {
         List<Slot> list = this.getInputGridSlots();
         recipebookmenu$postplaceaction = ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe>() {
            @Override
            public void fillCraftSlotsStackedContents(StackedItemContents p_363395_) {
               AbstractCraftingMenu.this.fillCraftSlotsStackedContents(p_363395_);
            }

            @Override
            public void clearCraftingContent() {
               AbstractCraftingMenu.this.resultSlots.clearContent();
               AbstractCraftingMenu.this.craftSlots.clearContent();
            }

            @Override
            public boolean recipeMatches(RecipeHolder<CraftingRecipe> p_365206_) {
               return p_365206_.value().matches(AbstractCraftingMenu.this.craftSlots.asCraftInput(), AbstractCraftingMenu.this.owner().level());
            }
         }, this.width, this.height, list, list, p_361078_, recipeholder, p_361638_, p_361841_);
      } finally {
         this.finishPlacingRecipe(p_379885_, (RecipeHolder<CraftingRecipe>)p_364981_);
      }

      return recipebookmenu$postplaceaction;
   }

   protected void beginPlacingRecipe() {
   }

   protected void finishPlacingRecipe(ServerLevel p_379946_, RecipeHolder<CraftingRecipe> p_360568_) {
   }

   public abstract Slot getResultSlot();

   public abstract List<Slot> getInputGridSlots();

   public int getGridWidth() {
      return this.width;
   }

   public int getGridHeight() {
      return this.height;
   }

   protected abstract Player owner();

   @Override
   public void fillCraftSlotsStackedContents(StackedItemContents p_360753_) {
      this.craftSlots.fillStackedContents(p_360753_);
   }
}
