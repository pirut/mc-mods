package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BrewingStandScreen extends AbstractContainerScreen<BrewingStandMenu> {
   private static final ResourceLocation FUEL_LENGTH_SPRITE = ResourceLocation.withDefaultNamespace("container/brewing_stand/fuel_length");
   private static final ResourceLocation BREW_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/brewing_stand/brew_progress");
   private static final ResourceLocation BUBBLES_SPRITE = ResourceLocation.withDefaultNamespace("container/brewing_stand/bubbles");
   private static final ResourceLocation BREWING_STAND_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/brewing_stand.png");
   private static final int[] BUBBLELENGTHS = new int[]{29, 24, 20, 16, 11, 6, 0};

   public BrewingStandScreen(BrewingStandMenu p_98332_, Inventory p_98333_, Component p_98334_) {
      super(p_98332_, p_98333_, p_98334_);
   }

   @Override
   protected void init() {
      super.init();
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   public void render(GuiGraphics p_283297_, int p_283600_, int p_282033_, float p_283410_) {
      super.render(p_283297_, p_283600_, p_282033_, p_283410_);
      this.renderTooltip(p_283297_, p_283600_, p_282033_);
   }

   @Override
   protected void renderBg(GuiGraphics p_282963_, float p_282080_, int p_283365_, int p_283150_) {
      int $$4 = (this.width - this.imageWidth) / 2;
      int $$5 = (this.height - this.imageHeight) / 2;
      p_282963_.blit(BREWING_STAND_LOCATION, $$4, $$5, 0, 0, this.imageWidth, this.imageHeight);
      int $$6 = this.menu.getFuel();
      int $$7 = Mth.clamp((18 * $$6 + 20 - 1) / 20, 0, 18);
      if ($$7 > 0) {
         p_282963_.blitSprite(FUEL_LENGTH_SPRITE, 18, 4, 0, 0, $$4 + 60, $$5 + 44, $$7, 4);
      }

      int $$8 = this.menu.getBrewingTicks();
      if ($$8 > 0) {
         int $$9 = (int)(28.0F * (1.0F - $$8 / 400.0F));
         if ($$9 > 0) {
            p_282963_.blitSprite(BREW_PROGRESS_SPRITE, 9, 28, 0, 0, $$4 + 97, $$5 + 16, 9, $$9);
         }

         $$9 = BUBBLELENGTHS[$$8 / 2 % 7];
         if ($$9 > 0) {
            p_282963_.blitSprite(BUBBLES_SPRITE, 12, 29, 0, 29 - $$9, $$4 + 63, $$5 + 14 + 29 - $$9, 12, $$9);
         }
      }
   }
}
