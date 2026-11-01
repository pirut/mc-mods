package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class SpriteIconButton extends Button {
   protected final WidgetSprites sprite;
   protected final int spriteWidth;
   protected final int spriteHeight;

   SpriteIconButton(
      int width,
      int height,
      Component message,
      int spriteWidth,
      int spriteHeight,
      WidgetSprites sprite,
      Button.OnPress onPress,
      @Nullable Component tooltip,
      @Nullable Button.CreateNarration createNarration
   ) {
      super(0, 0, width, height, message, onPress, createNarration == null ? DEFAULT_NARRATION : createNarration);
      if (tooltip != null) {
         this.setTooltip(Tooltip.create(tooltip));
      }

      this.spriteWidth = spriteWidth;
      this.spriteHeight = spriteHeight;
      this.sprite = sprite;
   }

   protected void renderSprite(GuiGraphics guiGraphics, int x, int y) {
      guiGraphics.blitSprite(
         RenderPipelines.GUI_TEXTURED, this.sprite.get(this.isActive(), this.isHoveredOrFocused()), x, y, this.spriteWidth, this.spriteHeight, this.alpha
      );
   }

   public static SpriteIconButton.Builder builder(Component message, Button.OnPress onPress, boolean iconOnly) {
      return new SpriteIconButton.Builder(message, onPress, iconOnly);
   }

   @Environment(EnvType.CLIENT)
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

      public Builder(Component message, Button.OnPress onPress, boolean iconOnly) {
         this.message = message;
         this.onPress = onPress;
         this.iconOnly = iconOnly;
      }

      public SpriteIconButton.Builder width(int width) {
         this.width = width;
         return this;
      }

      public SpriteIconButton.Builder size(int width, int height) {
         this.width = width;
         this.height = height;
         return this;
      }

      public SpriteIconButton.Builder sprite(Identifier sprite, int spriteWidth, int spriteHeight) {
         this.sprite = new WidgetSprites(sprite);
         this.spriteWidth = spriteWidth;
         this.spriteHeight = spriteHeight;
         return this;
      }

      public SpriteIconButton.Builder sprite(WidgetSprites sprite, int spriteWidth, int spriteHeight) {
         this.sprite = sprite;
         this.spriteWidth = spriteWidth;
         this.spriteHeight = spriteHeight;
         return this;
      }

      public SpriteIconButton.Builder withTootip() {
         this.tooltip = this.message;
         return this;
      }

      public SpriteIconButton.Builder narration(Button.CreateNarration narration) {
         this.narration = narration;
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

   @Environment(EnvType.CLIENT)
   public static class CenteredIcon extends SpriteIconButton {
      protected CenteredIcon(
         int i,
         int j,
         Component component,
         int k,
         int l,
         WidgetSprites widgetSprites,
         Button.OnPress onPress,
         @Nullable Component component2,
         @Nullable Button.CreateNarration createNarration
      ) {
         super(i, j, component, k, l, widgetSprites, onPress, component2, createNarration);
      }

      @Override
      public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         this.renderDefaultSprite(guiGraphics);
         int i = this.getX() + this.getWidth() / 2 - this.spriteWidth / 2;
         int j = this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
         this.renderSprite(guiGraphics, i, j);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class TextAndIcon extends SpriteIconButton {
      protected TextAndIcon(
         int i,
         int j,
         Component component,
         int k,
         int l,
         WidgetSprites widgetSprites,
         Button.OnPress onPress,
         @Nullable Component component2,
         @Nullable Button.CreateNarration createNarration
      ) {
         super(i, j, component, k, l, widgetSprites, onPress, component2, createNarration);
      }

      @Override
      public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         this.renderDefaultSprite(guiGraphics);
         int i = this.getX() + 2;
         int j = this.getX() + this.getWidth() - this.spriteWidth - 4;
         int k = this.getX() + this.getWidth() / 2;
         ActiveTextCollector activeTextCollector = guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
         activeTextCollector.acceptScrolling(this.getMessage(), k, i, j, this.getY(), this.getY() + this.getHeight());
         int l = this.getX() + this.getWidth() - this.spriteWidth - 2;
         int m = this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
         this.renderSprite(guiGraphics, l, m);
      }
   }
}
