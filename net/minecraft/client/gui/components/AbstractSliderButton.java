package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public abstract class AbstractSliderButton extends AbstractWidget.WithInactiveMessage {
   private static final Identifier SLIDER_SPRITE = Identifier.withDefaultNamespace("widget/slider");
   private static final Identifier HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/slider_highlighted");
   private static final Identifier SLIDER_HANDLE_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle");
   private static final Identifier SLIDER_HANDLE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
   protected static final int TEXT_MARGIN = 2;
   public static final int DEFAULT_HEIGHT = 20;
   protected static final int HANDLE_WIDTH = 8;
   private static final int HANDLE_HALF_WIDTH = 4;
   protected double value;
   protected boolean canChangeValue;
   private boolean dragging;

   public AbstractSliderButton(int x, int y, int width, int height, Component message, double value) {
      super(x, y, width, height, message);
      this.value = value;
   }

   private Identifier getSprite() {
      return this.isActive() && this.isFocused() && !this.canChangeValue ? HIGHLIGHTED_SPRITE : SLIDER_SPRITE;
   }

   private Identifier getHandleSprite() {
      return !this.isActive() || !this.isHovered && !this.canChangeValue ? SLIDER_HANDLE_SPRITE : SLIDER_HANDLE_HIGHLIGHTED_SPRITE;
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return Component.translatable("gui.narrate.slider", this.getMessage());
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            if (this.canChangeValue) {
               narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.focused"));
            } else {
               narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.focused.keyboard_cannot_change_value"));
            }
         } else {
            narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.hovered"));
         }
      }
   }

   @Override
   public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.blitSprite(
         RenderPipelines.GUI_TEXTURED, this.getSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha)
      );
      guiGraphics.blitSprite(
         RenderPipelines.GUI_TEXTURED,
         this.getHandleSprite(),
         this.getX() + (int)(this.value * (this.width - 8)),
         this.getY(),
         8,
         this.getHeight(),
         ARGB.white(this.alpha)
      );
      this.renderScrollingStringOverContents(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE), this.getMessage(), 2);
      if (this.isHovered()) {
         guiGraphics.requestCursor(this.dragging ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND);
      }
   }

   @Override
   public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
      this.dragging = this.active;
      this.setValueFromMouse(event);
   }

   @Override
   public void setFocused(boolean focused) {
      super.setFocused(focused);
      if (!focused) {
         this.canChangeValue = false;
      } else {
         InputType inputType = Minecraft.getInstance().getLastInputType();
         if (inputType == InputType.MOUSE || inputType == InputType.KEYBOARD_TAB) {
            this.canChangeValue = true;
         }
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.isSelection()) {
         this.canChangeValue = !this.canChangeValue;
         return true;
      } else {
         if (this.canChangeValue) {
            boolean bl = event.isLeft();
            boolean bl2 = event.isRight();
            if (bl || bl2) {
               float f = bl ? -1.0F : 1.0F;
               this.setValue(this.value + f / (this.width - 8));
               return true;
            }
         }

         return false;
      }
   }

   private void setValueFromMouse(MouseButtonEvent event) {
      this.setValue((event.x() - (this.getX() + 4)) / (this.width - 8));
   }

   protected void setValue(double value) {
      double d = this.value;
      this.value = Mth.clamp(value, 0.0, 1.0);
      if (d != this.value) {
         this.applyValue();
      }

      this.updateMessage();
   }

   @Override
   protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
      this.setValueFromMouse(event);
      super.onDrag(event, mouseX, mouseY);
   }

   @Override
   public void playDownSound(SoundManager handler) {
   }

   @Override
   public void onRelease(MouseButtonEvent event) {
      this.dragging = false;
      super.playDownSound(Minecraft.getInstance().getSoundManager());
   }

   protected abstract void updateMessage();

   protected abstract void applyValue();
}
