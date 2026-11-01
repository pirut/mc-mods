package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoomScreen extends AbstractContainerScreen<LoomMenu> {
   private static final ResourceLocation BANNER_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/banner_slot");
   private static final ResourceLocation DYE_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/dye_slot");
   private static final ResourceLocation PATTERN_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern_slot");
   private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/scroller");
   private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/scroller_disabled");
   private static final ResourceLocation PATTERN_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern_selected");
   private static final ResourceLocation PATTERN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern_highlighted");
   private static final ResourceLocation PATTERN_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern");
   private static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/error");
   private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/loom.png");
   private static final int PATTERN_COLUMNS = 4;
   private static final int PATTERN_ROWS = 4;
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   private static final int PATTERN_IMAGE_SIZE = 14;
   private static final int SCROLLER_FULL_HEIGHT = 56;
   private static final int PATTERNS_X = 60;
   private static final int PATTERNS_Y = 13;
   private ModelPart flag;
   @Nullable
   private BannerPatternLayers resultBannerPatterns;
   private ItemStack bannerStack = ItemStack.EMPTY;
   private ItemStack dyeStack = ItemStack.EMPTY;
   private ItemStack patternStack = ItemStack.EMPTY;
   private boolean displayPatterns;
   private boolean hasMaxPatterns;
   private float scrollOffs;
   private boolean scrolling;
   private int startRow;

   public LoomScreen(LoomMenu p_99075_, Inventory p_99076_, Component p_99077_) {
      super(p_99075_, p_99076_, p_99077_);
      p_99075_.registerUpdateListener(this::containerChanged);
      this.titleLabelY -= 2;
   }

   @Override
   protected void init() {
      super.init();
      this.flag = this.minecraft.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
   }

   @Override
   public void render(GuiGraphics p_283513_, int p_282700_, int p_282637_, float p_281433_) {
      super.render(p_283513_, p_282700_, p_282637_, p_281433_);
      this.renderTooltip(p_283513_, p_282700_, p_282637_);
   }

   private int totalRowCount() {
      return Mth.positiveCeilDiv(this.menu.getSelectablePatterns().size(), 4);
   }

   @Override
   protected void renderBg(GuiGraphics p_282870_, float p_281777_, int p_283331_, int p_283087_) {
      int $$4 = this.leftPos;
      int $$5 = this.topPos;
      p_282870_.blit(BG_LOCATION, $$4, $$5, 0, 0, this.imageWidth, this.imageHeight);
      Slot $$6 = this.menu.getBannerSlot();
      Slot $$7 = this.menu.getDyeSlot();
      Slot $$8 = this.menu.getPatternSlot();
      Slot $$9 = this.menu.getResultSlot();
      if (!$$6.hasItem()) {
         p_282870_.blitSprite(BANNER_SLOT_SPRITE, $$4 + $$6.x, $$5 + $$6.y, 16, 16);
      }

      if (!$$7.hasItem()) {
         p_282870_.blitSprite(DYE_SLOT_SPRITE, $$4 + $$7.x, $$5 + $$7.y, 16, 16);
      }

      if (!$$8.hasItem()) {
         p_282870_.blitSprite(PATTERN_SLOT_SPRITE, $$4 + $$8.x, $$5 + $$8.y, 16, 16);
      }

      int $$10 = (int)(41.0F * this.scrollOffs);
      ResourceLocation $$11 = this.displayPatterns ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
      p_282870_.blitSprite($$11, $$4 + 119, $$5 + 13 + $$10, 12, 15);
      Lighting.setupForFlatItems();
      if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
         p_282870_.pose().pushPose();
         p_282870_.pose().translate((float)($$4 + 139), (float)($$5 + 52), 0.0F);
         p_282870_.pose().scale(24.0F, 24.0F, 1.0F);
         p_282870_.pose().translate(0.5F, -0.5F, 0.5F);
         float $$12 = 0.6666667F;
         p_282870_.pose().scale(0.6666667F, 0.6666667F, -0.6666667F);
         this.flag.xRot = 0.0F;
         this.flag.y = -32.0F;
         DyeColor $$13 = ((BannerItem)$$9.getItem().getItem()).getColor();
         BannerRenderer.renderPatterns(
            p_282870_.pose(),
            p_282870_.bufferSource(),
            15728880,
            OverlayTexture.NO_OVERLAY,
            this.flag,
            ModelBakery.BANNER_BASE,
            true,
            $$13,
            this.resultBannerPatterns
         );
         p_282870_.pose().popPose();
         p_282870_.flush();
      } else if (this.hasMaxPatterns) {
         p_282870_.blitSprite(ERROR_SPRITE, $$4 + $$9.x - 5, $$5 + $$9.y - 5, 26, 26);
      }

      if (this.displayPatterns) {
         int $$14 = $$4 + 60;
         int $$15 = $$5 + 13;
         List<Holder<BannerPattern>> $$16 = this.menu.getSelectablePatterns();

         label64:
         for (int $$17 = 0; $$17 < 4; $$17++) {
            for (int $$18 = 0; $$18 < 4; $$18++) {
               int $$19 = $$17 + this.startRow;
               int $$20 = $$19 * 4 + $$18;
               if ($$20 >= $$16.size()) {
                  break label64;
               }

               int $$21 = $$14 + $$18 * 14;
               int $$22 = $$15 + $$17 * 14;
               boolean $$23 = p_283331_ >= $$21 && p_283087_ >= $$22 && p_283331_ < $$21 + 14 && p_283087_ < $$22 + 14;
               ResourceLocation $$24;
               if ($$20 == this.menu.getSelectedBannerPatternIndex()) {
                  $$24 = PATTERN_SELECTED_SPRITE;
               } else if ($$23) {
                  $$24 = PATTERN_HIGHLIGHTED_SPRITE;
               } else {
                  $$24 = PATTERN_SPRITE;
               }

               p_282870_.blitSprite($$24, $$21, $$22, 14, 14);
               this.renderPattern(p_282870_, $$16.get($$20), $$21, $$22);
            }
         }
      }

      Lighting.setupFor3DItems();
   }

   private void renderPattern(GuiGraphics p_282452_, Holder<BannerPattern> p_281940_, int p_281872_, int p_282995_) {
      PoseStack $$4 = new PoseStack();
      $$4.pushPose();
      $$4.translate(p_281872_ + 0.5F, (float)(p_282995_ + 16), 0.0F);
      $$4.scale(6.0F, -6.0F, 1.0F);
      $$4.translate(0.5F, 0.5F, 0.0F);
      $$4.translate(0.5F, 0.5F, 0.5F);
      float $$5 = 0.6666667F;
      $$4.scale(0.6666667F, -0.6666667F, -0.6666667F);
      this.flag.xRot = 0.0F;
      this.flag.y = -32.0F;
      BannerPatternLayers $$6 = new BannerPatternLayers.Builder().add(p_281940_, DyeColor.WHITE).build();
      BannerRenderer.renderPatterns(
         $$4, p_282452_.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, this.flag, ModelBakery.BANNER_BASE, true, DyeColor.GRAY, $$6
      );
      $$4.popPose();
      p_282452_.flush();
   }

   @Override
   public boolean mouseClicked(double p_99083_, double p_99084_, int p_99085_) {
      this.scrolling = false;
      if (this.displayPatterns) {
         int $$3 = this.leftPos + 60;
         int $$4 = this.topPos + 13;

         for (int $$5 = 0; $$5 < 4; $$5++) {
            for (int $$6 = 0; $$6 < 4; $$6++) {
               double $$7 = p_99083_ - ($$3 + $$6 * 14);
               double $$8 = p_99084_ - ($$4 + $$5 * 14);
               int $$9 = $$5 + this.startRow;
               int $$10 = $$9 * 4 + $$6;
               if ($$7 >= 0.0 && $$8 >= 0.0 && $$7 < 14.0 && $$8 < 14.0 && this.menu.clickMenuButton(this.minecraft.player, $$10)) {
                  Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                  this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, $$10);
                  return true;
               }
            }
         }

         $$3 = this.leftPos + 119;
         $$4 = this.topPos + 9;
         if (p_99083_ >= $$3 && p_99083_ < $$3 + 12 && p_99084_ >= $$4 && p_99084_ < $$4 + 56) {
            this.scrolling = true;
         }
      }

      return super.mouseClicked(p_99083_, p_99084_, p_99085_);
   }

   @Override
   public boolean mouseDragged(double p_99087_, double p_99088_, int p_99089_, double p_99090_, double p_99091_) {
      int $$5 = this.totalRowCount() - 4;
      if (this.scrolling && this.displayPatterns && $$5 > 0) {
         int $$6 = this.topPos + 13;
         int $$7 = $$6 + 56;
         this.scrollOffs = ((float)p_99088_ - $$6 - 7.5F) / ($$7 - $$6 - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.startRow = Math.max((int)(this.scrollOffs * $$5 + 0.5), 0);
         return true;
      } else {
         return super.mouseDragged(p_99087_, p_99088_, p_99089_, p_99090_, p_99091_);
      }
   }

   @Override
   public boolean mouseScrolled(double p_99079_, double p_99080_, double p_99081_, double p_295430_) {
      int $$4 = this.totalRowCount() - 4;
      if (this.displayPatterns && $$4 > 0) {
         float $$5 = (float)p_295430_ / $$4;
         this.scrollOffs = Mth.clamp(this.scrollOffs - $$5, 0.0F, 1.0F);
         this.startRow = Math.max((int)(this.scrollOffs * $$4 + 0.5F), 0);
      }

      return true;
   }

   @Override
   protected boolean hasClickedOutside(double p_99093_, double p_99094_, int p_99095_, int p_99096_, int p_99097_) {
      return p_99093_ < p_99095_ || p_99094_ < p_99096_ || p_99093_ >= p_99095_ + this.imageWidth || p_99094_ >= p_99096_ + this.imageHeight;
   }

   private void containerChanged() {
      ItemStack $$0 = this.menu.getResultSlot().getItem();
      if ($$0.isEmpty()) {
         this.resultBannerPatterns = null;
      } else {
         this.resultBannerPatterns = $$0.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      }

      ItemStack $$1 = this.menu.getBannerSlot().getItem();
      ItemStack $$2 = this.menu.getDyeSlot().getItem();
      ItemStack $$3 = this.menu.getPatternSlot().getItem();
      BannerPatternLayers $$4 = $$1.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      this.hasMaxPatterns = $$4.layers().size() >= 6;
      if (this.hasMaxPatterns) {
         this.resultBannerPatterns = null;
      }

      if (!ItemStack.matches($$1, this.bannerStack) || !ItemStack.matches($$2, this.dyeStack) || !ItemStack.matches($$3, this.patternStack)) {
         this.displayPatterns = !$$1.isEmpty() && !$$2.isEmpty() && !this.hasMaxPatterns && !this.menu.getSelectablePatterns().isEmpty();
      }

      if (this.startRow >= this.totalRowCount()) {
         this.startRow = 0;
         this.scrollOffs = 0.0F;
      }

      this.bannerStack = $$1.copy();
      this.dyeStack = $$2.copy();
      this.patternStack = $$3.copy();
   }
}
