package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TabButton extends AbstractWidget {
   private static final WidgetSprites SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("widget/tab_selected"),
      ResourceLocation.withDefaultNamespace("widget/tab"),
      ResourceLocation.withDefaultNamespace("widget/tab_selected_highlighted"),
      ResourceLocation.withDefaultNamespace("widget/tab_highlighted")
   );
   private static final int SELECTED_OFFSET = 3;
   private static final int TEXT_MARGIN = 1;
   private static final int UNDERLINE_HEIGHT = 1;
   private static final int UNDERLINE_MARGIN_X = 4;
   private static final int UNDERLINE_MARGIN_BOTTOM = 2;
   private final TabManager tabManager;
   private final Tab tab;

   public TabButton(TabManager p_275399_, Tab p_275391_, int p_275340_, int p_275364_) {
      super(0, 0, p_275340_, p_275364_, p_275391_.getTabTitle());
      this.tabManager = p_275399_;
      this.tab = p_275391_;
   }

   @Override
   public void renderWidget(GuiGraphics p_283350_, int p_283437_, int p_281595_, float p_282117_) {
      RenderSystem.enableBlend();
      p_283350_.blitSprite(SPRITES.get(this.isSelected(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height);
      RenderSystem.disableBlend();
      Font $$4 = Minecraft.getInstance().font;
      int $$5 = this.active ? -1 : -6250336;
      this.renderString(p_283350_, $$4, $$5);
      if (this.isSelected()) {
         this.renderMenuBackground(p_283350_, this.getX() + 2, this.getY() + 2, this.getRight() - 2, this.getBottom());
         this.renderFocusUnderline(p_283350_, $$4, $$5);
      }
   }

   protected void renderMenuBackground(GuiGraphics p_334080_, int p_333889_, int p_333767_, int p_333724_, int p_333941_) {
      Screen.renderMenuBackgroundTexture(p_334080_, Screen.MENU_BACKGROUND, p_333889_, p_333767_, 0.0F, 0.0F, p_333724_ - p_333889_, p_333941_ - p_333767_);
   }

   public void renderString(GuiGraphics p_282917_, Font p_275208_, int p_275293_) {
      int $$3 = this.getX() + 1;
      int $$4 = this.getY() + (this.isSelected() ? 0 : 3);
      int $$5 = this.getX() + this.getWidth() - 1;
      int $$6 = this.getY() + this.getHeight();
      renderScrollingString(p_282917_, p_275208_, this.getMessage(), $$3, $$4, $$5, $$6, p_275293_);
   }

   private void renderFocusUnderline(GuiGraphics p_282383_, Font p_275475_, int p_275367_) {
      int $$3 = Math.min(p_275475_.width(this.getMessage()), this.getWidth() - 4);
      int $$4 = this.getX() + (this.getWidth() - $$3) / 2;
      int $$5 = this.getY() + this.getHeight() - 2;
      p_282383_.fill($$4, $$5, $$4 + $$3, $$5 + 1, p_275367_);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_275465_) {
      p_275465_.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", this.tab.getTabTitle()));
   }

   @Override
   public void playDownSound(SoundManager p_276302_) {
   }

   public Tab tab() {
      return this.tab;
   }

   public boolean isSelected() {
      return this.tabManager.getCurrentTab() == this.tab;
   }
}
