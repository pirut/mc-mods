package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.time.Duration;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractWidget implements Renderable, GuiEventListener, LayoutElement, NarratableEntry {
   protected int width;
   protected int height;
   private int x;
   private int y;
   protected Component message;
   protected boolean isHovered;
   public boolean active = true;
   public boolean visible = true;
   protected float alpha = 1.0F;
   private int tabOrderGroup;
   private boolean focused;
   private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

   public AbstractWidget(int x, int y, int width, int height, Component message) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.message = message;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public final void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      if (this.visible) {
         this.isHovered = guiGraphics.containsPointInScissor(mouseX, mouseY) && this.areCoordinatesInRectangle(mouseX, mouseY);
         this.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
         this.tooltip.refreshTooltipForNextRenderPass(guiGraphics, mouseX, mouseY, this.isHovered(), this.isFocused(), this.getRectangle());
      }
   }

   protected void handleCursor(GuiGraphics guiGraphics) {
      if (this.isHovered()) {
         guiGraphics.requestCursor(this.isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
      }
   }

   public void setTooltip(@Nullable Tooltip tooltip) {
      this.tooltip.set(tooltip);
   }

   public void setTooltipDelay(Duration tooltipDelay) {
      this.tooltip.setDelay(tooltipDelay);
   }

   protected MutableComponent createNarrationMessage() {
      return wrapDefaultNarrationMessage(this.getMessage());
   }

   public static MutableComponent wrapDefaultNarrationMessage(Component message) {
      return Component.translatable("gui.narrate.button", message);
   }

   protected abstract void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

   protected void renderScrollingStringOverContents(ActiveTextCollector activeTextCollector, Component text, int padding) {
      int i = this.getX() + padding;
      int j = this.getX() + this.getWidth() - padding;
      int k = this.getY();
      int l = this.getY() + this.getHeight();
      activeTextCollector.acceptScrollingWithDefaultCenter(text, i, j, k, l);
   }

   public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
   }

   public void onRelease(MouseButtonEvent event) {
   }

   protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      if (!this.isActive()) {
         return false;
      } else {
         if (this.isValidClickButton(event.buttonInfo())) {
            boolean bl = this.isMouseOver(event.x(), event.y());
            if (bl) {
               this.playDownSound(Minecraft.getInstance().getSoundManager());
               this.onClick(event, isDoubleClick);
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (this.isValidClickButton(event.buttonInfo())) {
         this.onRelease(event);
         return true;
      } else {
         return false;
      }
   }

   protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
      return buttonInfo.button() == 0;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
      if (this.isValidClickButton(event.buttonInfo())) {
         this.onDrag(event, mouseX, mouseY);
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      if (!this.isActive()) {
         return null;
      } else {
         return !this.isFocused() ? ComponentPath.leaf(this) : null;
      }
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.isActive() && this.areCoordinatesInRectangle(mouseX, mouseY);
   }

   public void playDownSound(SoundManager handler) {
      playButtonClickSound(handler);
   }

   public static void playButtonClickSound(SoundManager soundManager) {
      soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public void setAlpha(float alpha) {
      this.alpha = alpha;
   }

   public float getAlpha() {
      return this.alpha;
   }

   public void setMessage(Component message) {
      this.message = message;
   }

   public Component getMessage() {
      return this.message;
   }

   @Override
   public boolean isFocused() {
      return this.focused;
   }

   public boolean isHovered() {
      return this.isHovered;
   }

   public boolean isHoveredOrFocused() {
      return this.isHovered() || this.isFocused();
   }

   @Override
   public boolean isActive() {
      return this.visible && this.active;
   }

   @Override
   public void setFocused(boolean focused) {
      this.focused = focused;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      if (this.isFocused()) {
         return NarratableEntry.NarrationPriority.FOCUSED;
      } else {
         return this.isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
      }
   }

   @Override
   public final void updateNarration(NarrationElementOutput narrationElementOutput) {
      this.updateWidgetNarration(narrationElementOutput);
      this.tooltip.updateNarration(narrationElementOutput);
   }

   protected abstract void updateWidgetNarration(NarrationElementOutput narrationElementOutput);

   protected void defaultButtonNarrationText(NarrationElementOutput narrationElementOutput) {
      narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
         } else {
            narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
         }
      }
   }

   @Override
   public int getX() {
      return this.x;
   }

   @Override
   public void setX(int x) {
      this.x = x;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public void setY(int y) {
      this.y = y;
   }

   public int getRight() {
      return this.getX() + this.getWidth();
   }

   public int getBottom() {
      return this.getY() + this.getHeight();
   }

   @Override
   public void visitWidgets(Consumer<AbstractWidget> consumer) {
      consumer.accept(this);
   }

   public void setSize(int width, int height) {
      this.width = width;
      this.height = height;
   }

   @Override
   public ScreenRectangle getRectangle() {
      return LayoutElement.super.getRectangle();
   }

   private boolean areCoordinatesInRectangle(double x, double y) {
      return x >= this.getX() && y >= this.getY() && x < this.getRight() && y < this.getBottom();
   }

   public void setRectangle(int width, int height, int x, int y) {
      this.setSize(width, height);
      this.setPosition(x, y);
   }

   @Override
   public int getTabOrderGroup() {
      return this.tabOrderGroup;
   }

   public void setTabOrderGroup(int tabOrderGroup) {
      this.tabOrderGroup = tabOrderGroup;
   }

   @Environment(EnvType.CLIENT)
   public abstract static class WithInactiveMessage extends AbstractWidget {
      private Component inactiveMessage;

      public static Component defaultInactiveMessage(Component message) {
         return ComponentUtils.mergeStyles(message, Style.EMPTY.withColor(-6250336));
      }

      public WithInactiveMessage(int i, int j, int k, int l, Component component) {
         super(i, j, k, l, component);
         this.inactiveMessage = defaultInactiveMessage(component);
      }

      @Override
      public Component getMessage() {
         return this.active ? super.getMessage() : this.inactiveMessage;
      }

      @Override
      public void setMessage(Component message) {
         super.setMessage(message);
         this.inactiveMessage = defaultInactiveMessage(message);
      }
   }
}
