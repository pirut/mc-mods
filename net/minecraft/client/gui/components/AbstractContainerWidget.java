package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractContainerWidget extends AbstractScrollArea implements ContainerEventHandler {
   @Nullable
   private GuiEventListener focused;
   private boolean isDragging;

   public AbstractContainerWidget(int i, int j, int k, int l, Component component) {
      super(i, j, k, l, component);
   }

   @Override
   public final boolean isDragging() {
      return this.isDragging;
   }

   @Override
   public final void setDragging(boolean isDragging) {
      this.isDragging = isDragging;
   }

   @Nullable
   @Override
   public GuiEventListener getFocused() {
      return this.focused;
   }

   @Override
   public void setFocused(@Nullable GuiEventListener focused) {
      if (this.focused != null) {
         this.focused.setFocused(false);
      }

      if (focused != null) {
         focused.setFocused(true);
      }

      this.focused = focused;
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return ContainerEventHandler.super.nextFocusPath(event);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      boolean bl = this.updateScrolling(event);
      return ContainerEventHandler.super.mouseClicked(event, isDoubleClick) || bl;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      super.mouseReleased(event);
      return ContainerEventHandler.super.mouseReleased(event);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
      super.mouseDragged(event, mouseX, mouseY);
      return ContainerEventHandler.super.mouseDragged(event, mouseX, mouseY);
   }

   @Override
   public boolean isFocused() {
      return ContainerEventHandler.super.isFocused();
   }

   @Override
   public void setFocused(boolean focused) {
      ContainerEventHandler.super.setFocused(focused);
   }
}
