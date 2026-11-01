package net.minecraft.client.gui.components.spectator;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenuListener;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpectatorGui implements SpectatorMenuListener {
   private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
   private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
   private static final long FADE_OUT_DELAY = 5000L;
   private static final long FADE_OUT_TIME = 2000L;
   private final Minecraft minecraft;
   private long lastSelectionTime;
   @Nullable
   private SpectatorMenu menu;

   public SpectatorGui(Minecraft p_94767_) {
      this.minecraft = p_94767_;
   }

   public void onHotbarSelected(int p_94772_) {
      this.lastSelectionTime = Util.getMillis();
      if (this.menu != null) {
         this.menu.selectSlot(p_94772_);
      } else {
         this.menu = new SpectatorMenu(this);
      }
   }

   private float getHotbarAlpha() {
      long $$0 = this.lastSelectionTime - Util.getMillis() + 5000L;
      return Mth.clamp((float)$$0 / 2000.0F, 0.0F, 1.0F);
   }

   public void renderHotbar(GuiGraphics p_281458_) {
      if (this.menu != null) {
         float $$1 = this.getHotbarAlpha();
         if ($$1 <= 0.0F) {
            this.menu.exit();
         } else {
            int $$2 = p_281458_.guiWidth() / 2;
            p_281458_.pose().pushPose();
            p_281458_.pose().translate(0.0F, 0.0F, -90.0F);
            int $$3 = Mth.floor(p_281458_.guiHeight() - 22.0F * $$1);
            SpectatorPage $$4 = this.menu.getCurrentPage();
            this.renderPage(p_281458_, $$1, $$2, $$3, $$4);
            p_281458_.pose().popPose();
         }
      }
   }

   protected void renderPage(GuiGraphics p_282945_, float p_281688_, int p_281726_, int p_281730_, SpectatorPage p_282361_) {
      RenderSystem.enableBlend();
      p_282945_.setColor(1.0F, 1.0F, 1.0F, p_281688_);
      p_282945_.blitSprite(HOTBAR_SPRITE, p_281726_ - 91, p_281730_, 182, 22);
      if (p_282361_.getSelectedSlot() >= 0) {
         p_282945_.blitSprite(HOTBAR_SELECTION_SPRITE, p_281726_ - 91 - 1 + p_282361_.getSelectedSlot() * 20, p_281730_ - 1, 24, 23);
      }

      p_282945_.setColor(1.0F, 1.0F, 1.0F, 1.0F);

      for (int $$5 = 0; $$5 < 9; $$5++) {
         this.renderSlot(p_282945_, $$5, p_282945_.guiWidth() / 2 - 90 + $$5 * 20 + 2, p_281730_ + 3, p_281688_, p_282361_.getItem($$5));
      }

      RenderSystem.disableBlend();
   }

   private void renderSlot(GuiGraphics p_281411_, int p_283536_, int p_281853_, float p_282693_, float p_281955_, SpectatorMenuItem p_283370_) {
      if (p_283370_ != SpectatorMenu.EMPTY_SLOT) {
         int $$6 = (int)(p_281955_ * 255.0F);
         p_281411_.pose().pushPose();
         p_281411_.pose().translate((float)p_281853_, p_282693_, 0.0F);
         float $$7 = p_283370_.isEnabled() ? 1.0F : 0.25F;
         p_281411_.setColor($$7, $$7, $$7, p_281955_);
         p_283370_.renderIcon(p_281411_, $$7, $$6);
         p_281411_.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         p_281411_.pose().popPose();
         if ($$6 > 3 && p_283370_.isEnabled()) {
            Component $$8 = this.minecraft.options.keyHotbarSlots[p_283536_].getTranslatedKeyMessage();
            p_281411_.drawString(this.minecraft.font, $$8, p_281853_ + 19 - 2 - this.minecraft.font.width($$8), (int)p_282693_ + 6 + 3, 16777215 + ($$6 << 24));
         }
      }
   }

   public void renderTooltip(GuiGraphics p_283107_) {
      int $$1 = (int)(this.getHotbarAlpha() * 255.0F);
      if ($$1 > 3 && this.menu != null) {
         SpectatorMenuItem $$2 = this.menu.getSelectedItem();
         Component $$3 = $$2 == SpectatorMenu.EMPTY_SLOT ? this.menu.getSelectedCategory().getPrompt() : $$2.getName();
         if ($$3 != null) {
            int $$4 = this.minecraft.font.width($$3);
            int $$5 = (p_283107_.guiWidth() - $$4) / 2;
            int $$6 = p_283107_.guiHeight() - 35;
            p_283107_.drawStringWithBackdrop(this.minecraft.font, $$3, $$5, $$6, $$4, FastColor.ARGB32.color($$1, -1));
         }
      }
   }

   @Override
   public void onSpectatorMenuClosed(SpectatorMenu p_94792_) {
      this.menu = null;
      this.lastSelectionTime = 0L;
   }

   public boolean isMenuActive() {
      return this.menu != null;
   }

   public void onMouseScrolled(int p_205381_) {
      int $$1 = this.menu.getSelectedSlot() + p_205381_;

      while ($$1 >= 0 && $$1 <= 8 && (this.menu.getItem($$1) == SpectatorMenu.EMPTY_SLOT || !this.menu.getItem($$1).isEnabled())) {
         $$1 += p_205381_;
      }

      if ($$1 >= 0 && $$1 <= 8) {
         this.menu.selectSlot($$1);
         this.lastSelectionTime = Util.getMillis();
      }
   }

   public void onMouseMiddleClick() {
      this.lastSelectionTime = Util.getMillis();
      if (this.isMenuActive()) {
         int $$0 = this.menu.getSelectedSlot();
         if ($$0 != -1) {
            this.menu.selectSlot($$0);
         }
      } else {
         this.menu = new SpectatorMenu(this);
      }
   }
}
