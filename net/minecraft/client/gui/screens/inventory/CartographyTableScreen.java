package net.minecraft.client.gui.screens.inventory;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CartographyTableScreen extends AbstractContainerScreen<CartographyTableMenu> {
   private static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/error");
   private static final ResourceLocation SCALED_MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/scaled_map");
   private static final ResourceLocation DUPLICATED_MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/duplicated_map");
   private static final ResourceLocation MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/map");
   private static final ResourceLocation LOCKED_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/locked");
   private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/cartography_table.png");

   public CartographyTableScreen(CartographyTableMenu p_98349_, Inventory p_98350_, Component p_98351_) {
      super(p_98349_, p_98350_, p_98351_);
      this.titleLabelY -= 2;
   }

   @Override
   public void render(GuiGraphics p_281331_, int p_281706_, int p_282996_, float p_283037_) {
      super.render(p_281331_, p_281706_, p_282996_, p_283037_);
      this.renderTooltip(p_281331_, p_281706_, p_282996_);
   }

   @Override
   protected void renderBg(GuiGraphics p_282101_, float p_282697_, int p_282380_, int p_282327_) {
      int $$4 = this.leftPos;
      int $$5 = this.topPos;
      p_282101_.blit(BG_LOCATION, $$4, $$5, 0, 0, this.imageWidth, this.imageHeight);
      ItemStack $$6 = this.menu.getSlot(1).getItem();
      boolean $$7 = $$6.is(Items.MAP);
      boolean $$8 = $$6.is(Items.PAPER);
      boolean $$9 = $$6.is(Items.GLASS_PANE);
      ItemStack $$10 = this.menu.getSlot(0).getItem();
      MapId $$11 = $$10.get(DataComponents.MAP_ID);
      boolean $$12 = false;
      MapItemSavedData $$13;
      if ($$11 != null) {
         $$13 = MapItem.getSavedData($$11, this.minecraft.level);
         if ($$13 != null) {
            if ($$13.locked) {
               $$12 = true;
               if ($$8 || $$9) {
                  p_282101_.blitSprite(ERROR_SPRITE, $$4 + 35, $$5 + 31, 28, 21);
               }
            }

            if ($$8 && $$13.scale >= 4) {
               $$12 = true;
               p_282101_.blitSprite(ERROR_SPRITE, $$4 + 35, $$5 + 31, 28, 21);
            }
         }
      } else {
         $$13 = null;
      }

      this.renderResultingMap(p_282101_, $$11, $$13, $$7, $$8, $$9, $$12);
   }

   private void renderResultingMap(
      GuiGraphics p_282167_,
      @Nullable MapId p_323466_,
      @Nullable MapItemSavedData p_282045_,
      boolean p_282086_,
      boolean p_283531_,
      boolean p_282645_,
      boolean p_281646_
   ) {
      int $$7 = this.leftPos;
      int $$8 = this.topPos;
      if (p_283531_ && !p_281646_) {
         p_282167_.blitSprite(SCALED_MAP_SPRITE, $$7 + 67, $$8 + 13, 66, 66);
         this.renderMap(p_282167_, p_323466_, p_282045_, $$7 + 85, $$8 + 31, 0.226F);
      } else if (p_282086_) {
         p_282167_.blitSprite(DUPLICATED_MAP_SPRITE, $$7 + 67 + 16, $$8 + 13, 50, 66);
         this.renderMap(p_282167_, p_323466_, p_282045_, $$7 + 86, $$8 + 16, 0.34F);
         p_282167_.pose().pushPose();
         p_282167_.pose().translate(0.0F, 0.0F, 1.0F);
         p_282167_.blitSprite(DUPLICATED_MAP_SPRITE, $$7 + 67, $$8 + 13 + 16, 50, 66);
         this.renderMap(p_282167_, p_323466_, p_282045_, $$7 + 70, $$8 + 32, 0.34F);
         p_282167_.pose().popPose();
      } else if (p_282645_) {
         p_282167_.blitSprite(MAP_SPRITE, $$7 + 67, $$8 + 13, 66, 66);
         this.renderMap(p_282167_, p_323466_, p_282045_, $$7 + 71, $$8 + 17, 0.45F);
         p_282167_.pose().pushPose();
         p_282167_.pose().translate(0.0F, 0.0F, 1.0F);
         p_282167_.blitSprite(LOCKED_SPRITE, $$7 + 118, $$8 + 60, 10, 14);
         p_282167_.pose().popPose();
      } else {
         p_282167_.blitSprite(MAP_SPRITE, $$7 + 67, $$8 + 13, 66, 66);
         this.renderMap(p_282167_, p_323466_, p_282045_, $$7 + 71, $$8 + 17, 0.45F);
      }
   }

   private void renderMap(GuiGraphics p_282298_, @Nullable MapId p_324470_, @Nullable MapItemSavedData p_282897_, int p_281632_, int p_282115_, float p_283388_) {
      if (p_324470_ != null && p_282897_ != null) {
         p_282298_.pose().pushPose();
         p_282298_.pose().translate((float)p_281632_, (float)p_282115_, 1.0F);
         p_282298_.pose().scale(p_283388_, p_283388_, 1.0F);
         this.minecraft.gameRenderer.getMapRenderer().render(p_282298_.pose(), p_282298_.bufferSource(), p_324470_, p_282897_, true, 15728880);
         p_282298_.flush();
         p_282298_.pose().popPose();
      }
   }
}
