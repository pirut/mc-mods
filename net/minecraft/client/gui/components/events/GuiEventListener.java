package net.minecraft.client.gui.components.events;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface GuiEventListener extends TabOrderedElement {
   default void mouseMoved(double mouseX, double mouseY) {
   }

   default boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      return false;
   }

   default boolean mouseReleased(MouseButtonEvent event) {
      return false;
   }

   default boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
      return false;
   }

   default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return false;
   }

   default boolean keyPressed(KeyEvent event) {
      return false;
   }

   default boolean keyReleased(KeyEvent event) {
      return false;
   }

   default boolean charTyped(CharacterEvent event) {
      return false;
   }

   @Nullable
   default ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return null;
   }

   default boolean isMouseOver(double mouseX, double mouseY) {
      return false;
   }

   void setFocused(boolean focused);

   boolean isFocused();

   default boolean shouldTakeFocusAfterInteraction() {
      return true;
   }

   @Nullable
   default ComponentPath getCurrentFocusPath() {
      return this.isFocused() ? ComponentPath.leaf(this) : null;
   }

   default ScreenRectangle getRectangle() {
      return ScreenRectangle.empty();
   }

   default ScreenRectangle getBorderForArrowNavigation(ScreenDirection direction) {
      return this.getRectangle().getBorder(direction);
   }
}
