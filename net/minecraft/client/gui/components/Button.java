package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class Button extends AbstractButton {
   public static final int SMALL_WIDTH = 120;
   public static final int DEFAULT_WIDTH = 150;
   public static final int BIG_WIDTH = 200;
   public static final int DEFAULT_HEIGHT = 20;
   public static final int DEFAULT_SPACING = 8;
   protected static final Button.CreateNarration DEFAULT_NARRATION = supplier -> supplier.get();
   protected final Button.OnPress onPress;
   protected final Button.CreateNarration createNarration;

   public static Button.Builder builder(Component message, Button.OnPress onPress) {
      return new Button.Builder(message, onPress);
   }

   protected Button(int x, int y, int width, int height, Component message, Button.OnPress onPress, Button.CreateNarration createNarration) {
      super(x, y, width, height, message);
      this.onPress = onPress;
      this.createNarration = createNarration;
   }

   @Override
   public void onPress(InputWithModifiers input) {
      this.onPress.onPress(this);
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return this.createNarration.createNarrationMessage(() -> super.createNarrationMessage());
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      this.defaultButtonNarrationText(narrationElementOutput);
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final Component message;
      private final Button.OnPress onPress;
      @Nullable
      private Tooltip tooltip;
      private int x;
      private int y;
      private int width = 150;
      private int height = 20;
      private Button.CreateNarration createNarration = Button.DEFAULT_NARRATION;

      public Builder(Component message, Button.OnPress onPress) {
         this.message = message;
         this.onPress = onPress;
      }

      public Button.Builder pos(int x, int y) {
         this.x = x;
         this.y = y;
         return this;
      }

      public Button.Builder width(int width) {
         this.width = width;
         return this;
      }

      public Button.Builder size(int width, int height) {
         this.width = width;
         this.height = height;
         return this;
      }

      public Button.Builder bounds(int x, int y, int width, int height) {
         return this.pos(x, y).size(width, height);
      }

      public Button.Builder tooltip(@Nullable Tooltip tooltip) {
         this.tooltip = tooltip;
         return this;
      }

      public Button.Builder createNarration(Button.CreateNarration createNarration) {
         this.createNarration = createNarration;
         return this;
      }

      public Button build() {
         Button button = new Button.Plain(this.x, this.y, this.width, this.height, this.message, this.onPress, this.createNarration);
         button.setTooltip(this.tooltip);
         return button;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface CreateNarration {
      MutableComponent createNarrationMessage(Supplier<MutableComponent> supplier);
   }

   @Environment(EnvType.CLIENT)
   public interface OnPress {
      void onPress(Button button);
   }

   @Environment(EnvType.CLIENT)
   public static class Plain extends Button {
      protected Plain(int i, int j, int k, int l, Component component, Button.OnPress onPress, Button.CreateNarration createNarration) {
         super(i, j, k, l, component, onPress, createNarration);
      }

      @Override
      protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         this.renderDefaultSprite(guiGraphics);
         this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
      }
   }
}
