package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class SpriteIconButton extends Button {
   protected final WidgetSprites sprite;
   protected final int spriteWidth;
   protected final int spriteHeight;

   SpriteIconButton(
      int p_294218_,
      int p_294461_,
      Component p_294760_,
      int p_296342_,
      int p_296291_,
      WidgetSprites p_440029_,
      Button.OnPress p_295667_,
      @Nullable Component p_440489_,
      @Nullable Button.CreateNarration p_332100_
   ) {
      super(0, 0, p_294218_, p_294461_, p_294760_, p_295667_, p_332100_ == null ? DEFAULT_NARRATION : p_332100_);
      if (p_440489_ != null) {
         this.setTooltip(Tooltip.create(p_440489_));
      }

      this.spriteWidth = p_296342_;
      this.spriteHeight = p_296291_;
      this.sprite = p_440029_;
   }

   protected void renderSprite(GuiGraphics p_458109_, int p_457736_, int p_457843_) {
      p_458109_.blitSprite(
         RenderPipelines.GUI_TEXTURED,
         this.sprite.get(this.isActive(), this.isHoveredOrFocused()),
         p_457736_,
         p_457843_,
         this.spriteWidth,
         this.spriteHeight,
         this.alpha
      );
   }

   public static SpriteIconButton.Builder builder(Component p_294639_, Button.OnPress p_295155_, boolean p_295622_) {
      return new SpriteIconButton.Builder(p_294639_, p_295155_, p_295622_);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Component message;
      private final Button.OnPress onPress;
      private final boolean iconOnly;
      private int width = 150;
      private int height = 20;
      @Nullable
      private WidgetSprites sprite;
      private int spriteWidth;
      private int spriteHeight;
      @Nullable
      private Component tooltip;
      @Nullable
      private Button.CreateNarration narration;

      public Builder(Component p_294568_, Button.OnPress p_294520_, boolean p_294559_) {
         this.message = p_294568_;
         this.onPress = p_294520_;
         this.iconOnly = p_294559_;
      }

      public SpriteIconButton.Builder width(int p_295136_) {
         this.width = p_295136_;
         return this;
      }

      public SpriteIconButton.Builder size(int p_295812_, int p_296135_) {
         this.width = p_295812_;
         this.height = p_296135_;
         return this;
      }

      public SpriteIconButton.Builder sprite(Identifier p_469861_, int p_296046_, int p_295188_) {
         this.sprite = new WidgetSprites(p_469861_);
         this.spriteWidth = p_296046_;
         this.spriteHeight = p_295188_;
         return this;
      }

      public SpriteIconButton.Builder sprite(WidgetSprites p_439332_, int p_440349_, int p_440074_) {
         this.sprite = p_439332_;
         this.spriteWidth = p_440349_;
         this.spriteHeight = p_440074_;
         return this;
      }

      public SpriteIconButton.Builder withTootip() {
         this.tooltip = this.message;
         return this;
      }

      public SpriteIconButton.Builder narration(Button.CreateNarration p_331295_) {
         this.narration = p_331295_;
         return this;
      }

      public SpriteIconButton build() {
         if (this.sprite == null) {
            throw new IllegalStateException("Sprite not set");
         } else {
            return (SpriteIconButton)(this.iconOnly
               ? new SpriteIconButton.CenteredIcon(
                  this.width, this.height, this.message, this.spriteWidth, this.spriteHeight, this.sprite, this.onPress, this.tooltip, this.narration
               )
               : new SpriteIconButton.TextAndIcon(
                  this.width, this.height, this.message, this.spriteWidth, this.spriteHeight, this.sprite, this.onPress, this.tooltip, this.narration
               ));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class CenteredIcon extends SpriteIconButton {
      protected CenteredIcon(
         int p_295914_,
         int p_294852_,
         Component p_295609_,
         int p_294922_,
         int p_296462_,
         WidgetSprites p_440019_,
         Button.OnPress p_294427_,
         @Nullable Component p_440407_,
         @Nullable Button.CreateNarration p_330653_
      ) {
         super(p_295914_, p_294852_, p_295609_, p_294922_, p_296462_, p_440019_, p_294427_, p_440407_, p_330653_);
      }

      @Override
      public void renderContents(GuiGraphics p_458138_, int p_457527_, int p_457926_, float p_457737_) {
         this.renderDefaultSprite(p_458138_);
         int i = this.getX() + this.getWidth() / 2 - this.spriteWidth / 2;
         int j = this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
         this.renderSprite(p_458138_, i, j);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TextAndIcon extends SpriteIconButton {
      protected TextAndIcon(
         int p_296442_,
         int p_294340_,
         Component p_296265_,
         int p_294900_,
         int p_295900_,
         WidgetSprites p_439719_,
         Button.OnPress p_295566_,
         @Nullable Component p_439158_,
         @Nullable Button.CreateNarration p_330735_
      ) {
         super(p_296442_, p_294340_, p_296265_, p_294900_, p_295900_, p_439719_, p_295566_, p_439158_, p_330735_);
      }

      @Override
      public void renderContents(GuiGraphics p_458096_, int p_457648_, int p_457987_, float p_457713_) {
         this.renderDefaultSprite(p_458096_);
         int i = this.getX() + 2;
         int j = this.getX() + this.getWidth() - this.spriteWidth - 4;
         int k = this.getX() + this.getWidth() / 2;
         ActiveTextCollector activetextcollector = p_458096_.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
         activetextcollector.acceptScrolling(this.getMessage(), k, i, j, this.getY(), this.getY() + this.getHeight());
         int l = this.getX() + this.getWidth() - this.spriteWidth - 2;
         int i1 = this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
         this.renderSprite(p_458096_, l, i1);
      }
   }
}
