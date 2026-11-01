package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StonecutterScreen extends AbstractContainerScreen<StonecutterMenu> {
   private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller");
   private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller_disabled");
   private static final ResourceLocation RECIPE_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_selected");
   private static final ResourceLocation RECIPE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_highlighted");
   private static final ResourceLocation RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe");
   private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   private static final int RECIPES_COLUMNS = 4;
   private static final int RECIPES_ROWS = 3;
   private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
   private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
   private static final int SCROLLER_FULL_HEIGHT = 54;
   private static final int RECIPES_X = 52;
   private static final int RECIPES_Y = 14;
   private float scrollOffs;
   private boolean scrolling;
   private int startIndex;
   private boolean displayRecipes;

   public StonecutterScreen(StonecutterMenu p_99310_, Inventory p_99311_, Component p_99312_) {
      super(p_99310_, p_99311_, p_99312_);
      p_99310_.registerUpdateListener(this::containerChanged);
      this.titleLabelY--;
   }

   @Override
   public void render(GuiGraphics p_281735_, int p_282517_, int p_282840_, float p_282389_) {
      super.render(p_281735_, p_282517_, p_282840_, p_282389_);
      this.renderTooltip(p_281735_, p_282517_, p_282840_);
   }

   @Override
   protected void renderBg(GuiGraphics p_283115_, float p_282453_, int p_282940_, int p_282328_) {
      int $$4 = this.leftPos;
      int $$5 = this.topPos;
      p_283115_.blit(BG_LOCATION, $$4, $$5, 0, 0, this.imageWidth, this.imageHeight);
      int $$6 = (int)(41.0F * this.scrollOffs);
      ResourceLocation $$7 = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
      p_283115_.blitSprite($$7, $$4 + 119, $$5 + 15 + $$6, 12, 15);
      int $$8 = this.leftPos + 52;
      int $$9 = this.topPos + 14;
      int $$10 = this.startIndex + 12;
      this.renderButtons(p_283115_, p_282940_, p_282328_, $$8, $$9, $$10);
      this.renderRecipes(p_283115_, $$8, $$9, $$10);
   }

   @Override
   protected void renderTooltip(GuiGraphics p_282396_, int p_283157_, int p_282258_) {
      super.renderTooltip(p_282396_, p_283157_, p_282258_);
      if (this.displayRecipes) {
         int $$3 = this.leftPos + 52;
         int $$4 = this.topPos + 14;
         int $$5 = this.startIndex + 12;
         List<RecipeHolder<StonecutterRecipe>> $$6 = this.menu.getRecipes();

         for (int $$7 = this.startIndex; $$7 < $$5 && $$7 < this.menu.getNumRecipes(); $$7++) {
            int $$8 = $$7 - this.startIndex;
            int $$9 = $$3 + $$8 % 4 * 16;
            int $$10 = $$4 + $$8 / 4 * 18 + 2;
            if (p_283157_ >= $$9 && p_283157_ < $$9 + 16 && p_282258_ >= $$10 && p_282258_ < $$10 + 18) {
               p_282396_.renderTooltip(this.font, $$6.get($$7).value().getResultItem(this.minecraft.level.registryAccess()), p_283157_, p_282258_);
            }
         }
      }
   }

   private void renderButtons(GuiGraphics p_282733_, int p_282136_, int p_282147_, int p_281987_, int p_281276_, int p_282688_) {
      for (int $$6 = this.startIndex; $$6 < p_282688_ && $$6 < this.menu.getNumRecipes(); $$6++) {
         int $$7 = $$6 - this.startIndex;
         int $$8 = p_281987_ + $$7 % 4 * 16;
         int $$9 = $$7 / 4;
         int $$10 = p_281276_ + $$9 * 18 + 2;
         ResourceLocation $$11;
         if ($$6 == this.menu.getSelectedRecipeIndex()) {
            $$11 = RECIPE_SELECTED_SPRITE;
         } else if (p_282136_ >= $$8 && p_282147_ >= $$10 && p_282136_ < $$8 + 16 && p_282147_ < $$10 + 18) {
            $$11 = RECIPE_HIGHLIGHTED_SPRITE;
         } else {
            $$11 = RECIPE_SPRITE;
         }

         p_282733_.blitSprite($$11, $$8, $$10 - 1, 16, 18);
      }
   }

   private void renderRecipes(GuiGraphics p_281999_, int p_282658_, int p_282563_, int p_283352_) {
      List<RecipeHolder<StonecutterRecipe>> $$4 = this.menu.getRecipes();

      for (int $$5 = this.startIndex; $$5 < p_283352_ && $$5 < this.menu.getNumRecipes(); $$5++) {
         int $$6 = $$5 - this.startIndex;
         int $$7 = p_282658_ + $$6 % 4 * 16;
         int $$8 = $$6 / 4;
         int $$9 = p_282563_ + $$8 * 18 + 2;
         p_281999_.renderItem($$4.get($$5).value().getResultItem(this.minecraft.level.registryAccess()), $$7, $$9);
      }
   }

   @Override
   public boolean mouseClicked(double p_99318_, double p_99319_, int p_99320_) {
      this.scrolling = false;
      if (this.displayRecipes) {
         int $$3 = this.leftPos + 52;
         int $$4 = this.topPos + 14;
         int $$5 = this.startIndex + 12;

         for (int $$6 = this.startIndex; $$6 < $$5; $$6++) {
            int $$7 = $$6 - this.startIndex;
            double $$8 = p_99318_ - ($$3 + $$7 % 4 * 16);
            double $$9 = p_99319_ - ($$4 + $$7 / 4 * 18);
            if ($$8 >= 0.0 && $$9 >= 0.0 && $$8 < 16.0 && $$9 < 18.0 && this.menu.clickMenuButton(this.minecraft.player, $$6)) {
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
               this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, $$6);
               return true;
            }
         }

         $$3 = this.leftPos + 119;
         $$4 = this.topPos + 9;
         if (p_99318_ >= $$3 && p_99318_ < $$3 + 12 && p_99319_ >= $$4 && p_99319_ < $$4 + 54) {
            this.scrolling = true;
         }
      }

      return super.mouseClicked(p_99318_, p_99319_, p_99320_);
   }

   @Override
   public boolean mouseDragged(double p_99322_, double p_99323_, int p_99324_, double p_99325_, double p_99326_) {
      if (this.scrolling && this.isScrollBarActive()) {
         int $$5 = this.topPos + 14;
         int $$6 = $$5 + 54;
         this.scrollOffs = ((float)p_99323_ - $$5 - 7.5F) / ($$6 - $$5 - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.startIndex = (int)(this.scrollOffs * this.getOffscreenRows() + 0.5) * 4;
         return true;
      } else {
         return super.mouseDragged(p_99322_, p_99323_, p_99324_, p_99325_, p_99326_);
      }
   }

   @Override
   public boolean mouseScrolled(double p_99314_, double p_99315_, double p_99316_, double p_295672_) {
      if (this.isScrollBarActive()) {
         int $$4 = this.getOffscreenRows();
         float $$5 = (float)p_295672_ / $$4;
         this.scrollOffs = Mth.clamp(this.scrollOffs - $$5, 0.0F, 1.0F);
         this.startIndex = (int)(this.scrollOffs * $$4 + 0.5) * 4;
      }

      return true;
   }

   private boolean isScrollBarActive() {
      return this.displayRecipes && this.menu.getNumRecipes() > 12;
   }

   protected int getOffscreenRows() {
      return (this.menu.getNumRecipes() + 4 - 1) / 4 - 3;
   }

   private void containerChanged() {
      this.displayRecipes = this.menu.hasInputItem();
      if (!this.displayRecipes) {
         this.scrollOffs = 0.0F;
         this.startIndex = 0;
      }
   }
}
