package net.minecraft.client.gui.screens.recipebook;

import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeBookTabButton extends ImageButton {
   private static final WidgetSprites SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/tab"), Identifier.withDefaultNamespace("recipe_book/tab_selected")
   );
   public static final int WIDTH = 35;
   public static final int HEIGHT = 27;
   private final RecipeBookComponent.TabInfo tabInfo;
   private static final float ANIMATION_TIME = 15.0F;
   private float animationTime;
   private boolean selected = false;

   public RecipeBookTabButton(int p_470644_, int p_470574_, RecipeBookComponent.TabInfo p_379856_, Button.OnPress p_470697_) {
      super(p_470644_, p_470574_, 35, 27, SPRITES, p_470697_);
      this.tabInfo = p_379856_;
   }

   public void startAnimation(ClientRecipeBook p_361190_, boolean p_362968_) {
      RecipeCollection.CraftableStatus recipecollection$craftablestatus = p_362968_
         ? RecipeCollection.CraftableStatus.CRAFTABLE
         : RecipeCollection.CraftableStatus.ANY;

      for (RecipeCollection recipecollection : p_361190_.getCollection(this.tabInfo.category())) {
         for (RecipeDisplayEntry recipedisplayentry : recipecollection.getSelectedRecipes(recipecollection$craftablestatus)) {
            if (p_361190_.willHighlight(recipedisplayentry.id())) {
               this.animationTime = 15.0F;
               return;
            }
         }
      }
   }

   @Override
   public void renderContents(GuiGraphics p_470692_, int p_470672_, int p_470801_, float p_470592_) {
      if (this.animationTime > 0.0F) {
         float f = 1.0F + 0.1F * (float)Math.sin(this.animationTime / 15.0F * (float) Math.PI);
         p_470692_.pose().pushMatrix();
         p_470692_.pose().translate(this.getX() + 8, this.getY() + 12);
         p_470692_.pose().scale(1.0F, f);
         p_470692_.pose().translate(-(this.getX() + 8), -(this.getY() + 12));
      }

      Identifier identifier = this.sprites.get(true, this.selected);
      int i = this.getX();
      if (this.selected) {
         i -= 2;
      }

      p_470692_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, i, this.getY(), this.width, this.height);
      this.renderIcon(p_470692_);
      if (this.animationTime > 0.0F) {
         p_470692_.pose().popMatrix();
         this.animationTime -= p_470592_;
      }
   }

   @Override
   protected void handleCursor(GuiGraphics p_470607_) {
      if (!this.selected) {
         super.handleCursor(p_470607_);
      }
   }

   private void renderIcon(GuiGraphics p_281802_) {
      int i = this.selected ? -2 : 0;
      if (this.tabInfo.secondaryIcon().isPresent()) {
         p_281802_.renderFakeItem(this.tabInfo.primaryIcon(), this.getX() + 3 + i, this.getY() + 5);
         p_281802_.renderFakeItem(this.tabInfo.secondaryIcon().get(), this.getX() + 14 + i, this.getY() + 5);
      } else {
         p_281802_.renderFakeItem(this.tabInfo.primaryIcon(), this.getX() + 9 + i, this.getY() + 5);
      }
   }

   public ExtendedRecipeBookCategory getCategory() {
      return this.tabInfo.category();
   }

   public boolean updateVisibility(ClientRecipeBook p_100450_) {
      List<RecipeCollection> list = p_100450_.getCollection(this.tabInfo.category());
      this.visible = false;

      for (RecipeCollection recipecollection : list) {
         if (recipecollection.hasAnySelected()) {
            this.visible = true;
            break;
         }
      }

      return this.visible;
   }

   public void select() {
      this.selected = true;
   }

   public void unselect() {
      this.selected = false;
   }
}
