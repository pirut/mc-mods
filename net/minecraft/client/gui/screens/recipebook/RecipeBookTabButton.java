package net.minecraft.client.gui.screens.recipebook;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeBookTabButton extends StateSwitchingButton {
   private static final WidgetSprites SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("recipe_book/tab"), ResourceLocation.withDefaultNamespace("recipe_book/tab_selected")
   );
   private final RecipeBookCategories category;
   private static final float ANIMATION_TIME = 15.0F;
   private float animationTime;

   public RecipeBookTabButton(RecipeBookCategories p_100448_) {
      super(0, 0, 35, 27, false);
      this.category = p_100448_;
      this.initTextureValues(SPRITES);
   }

   public void startAnimation(Minecraft p_100452_) {
      ClientRecipeBook $$1 = p_100452_.player.getRecipeBook();
      List<RecipeCollection> $$2 = $$1.getCollection(this.category);
      if (p_100452_.player.containerMenu instanceof RecipeBookMenu) {
         for (RecipeCollection $$3 : $$2) {
            for (RecipeHolder<?> $$4 : $$3.getRecipes($$1.isFiltering((RecipeBookMenu<?, ?>)p_100452_.player.containerMenu))) {
               if ($$1.willHighlight($$4)) {
                  this.animationTime = 15.0F;
                  return;
               }
            }
         }
      }
   }

   @Override
   public void renderWidget(GuiGraphics p_283195_, int p_283508_, int p_281788_, float p_283269_) {
      if (this.sprites != null) {
         if (this.animationTime > 0.0F) {
            float $$4 = 1.0F + 0.1F * (float)Math.sin(this.animationTime / 15.0F * (float) Math.PI);
            p_283195_.pose().pushPose();
            p_283195_.pose().translate((float)(this.getX() + 8), (float)(this.getY() + 12), 0.0F);
            p_283195_.pose().scale(1.0F, $$4, 1.0F);
            p_283195_.pose().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)), 0.0F);
         }

         Minecraft $$5 = Minecraft.getInstance();
         RenderSystem.disableDepthTest();
         ResourceLocation $$6 = this.sprites.get(true, this.isStateTriggered);
         int $$7 = this.getX();
         if (this.isStateTriggered) {
            $$7 -= 2;
         }

         p_283195_.blitSprite($$6, $$7, this.getY(), this.width, this.height);
         RenderSystem.enableDepthTest();
         this.renderIcon(p_283195_, $$5.getItemRenderer());
         if (this.animationTime > 0.0F) {
            p_283195_.pose().popPose();
            this.animationTime -= p_283269_;
         }
      }
   }

   private void renderIcon(GuiGraphics p_281802_, ItemRenderer p_282499_) {
      List<ItemStack> $$2 = this.category.getIconItems();
      int $$3 = this.isStateTriggered ? -2 : 0;
      if ($$2.size() == 1) {
         p_281802_.renderFakeItem($$2.get(0), this.getX() + 9 + $$3, this.getY() + 5);
      } else if ($$2.size() == 2) {
         p_281802_.renderFakeItem($$2.get(0), this.getX() + 3 + $$3, this.getY() + 5);
         p_281802_.renderFakeItem($$2.get(1), this.getX() + 14 + $$3, this.getY() + 5);
      }
   }

   public RecipeBookCategories getCategory() {
      return this.category;
   }

   public boolean updateVisibility(ClientRecipeBook p_100450_) {
      List<RecipeCollection> $$1 = p_100450_.getCollection(this.category);
      this.visible = false;
      if ($$1 != null) {
         for (RecipeCollection $$2 : $$1) {
            if ($$2.hasKnownRecipes() && $$2.hasFitting()) {
               this.visible = true;
               break;
            }
         }
      }

      return this.visible;
   }
}
