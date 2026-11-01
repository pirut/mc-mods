package net.minecraft.client.gui.components.events;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ContainerEventHandler extends GuiEventListener {
   List<? extends GuiEventListener> children();

   default Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
      for (GuiEventListener guiEventListener : this.children()) {
         if (guiEventListener.isMouseOver(mouseX, mouseY)) {
            return Optional.of(guiEventListener);
         }
      }

      return Optional.empty();
   }

   @Override
   default boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
      Optional<GuiEventListener> optional = this.getChildAt(event.x(), event.y());
      if (optional.isEmpty()) {
         return false;
      } else {
         GuiEventListener guiEventListener = optional.get();
         if (guiEventListener.mouseClicked(event, isDoubleClick) && guiEventListener.shouldTakeFocusAfterInteraction()) {
            this.setFocused(guiEventListener);
            if (event.button() == 0) {
               this.setDragging(true);
            }
         }

         return true;
      }
   }

   @Override
   default boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 0 && this.isDragging()) {
         this.setDragging(false);
         if (this.getFocused() != null) {
            return this.getFocused().mouseReleased(event);
         }
      }

      return false;
   }

   @Override
   default boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
      return this.getFocused() != null && this.isDragging() && event.button() == 0 ? this.getFocused().mouseDragged(event, mouseX, mouseY) : false;
   }

   boolean isDragging();

   void setDragging(boolean isDragging);

   @Override
   default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return this.getChildAt(mouseX, mouseY).filter(guiEventListener -> guiEventListener.mouseScrolled(mouseX, mouseY, scrollX, scrollY)).isPresent();
   }

   @Override
   default boolean keyPressed(KeyEvent event) {
      return this.getFocused() != null && this.getFocused().keyPressed(event);
   }

   @Override
   default boolean keyReleased(KeyEvent event) {
      return this.getFocused() != null && this.getFocused().keyReleased(event);
   }

   @Override
   default boolean charTyped(CharacterEvent event) {
      return this.getFocused() != null && this.getFocused().charTyped(event);
   }

   @Nullable
   GuiEventListener getFocused();

   void setFocused(@Nullable GuiEventListener focused);

   @Override
   default void setFocused(boolean focused) {
   }

   @Override
   default boolean isFocused() {
      return this.getFocused() != null;
   }

   @Nullable
   @Override
   default ComponentPath getCurrentFocusPath() {
      GuiEventListener guiEventListener = this.getFocused();
      return guiEventListener != null ? ComponentPath.path(this, guiEventListener.getCurrentFocusPath()) : null;
   }

   @Nullable
   @Override
   default ComponentPath nextFocusPath(FocusNavigationEvent event) {
      GuiEventListener guiEventListener = this.getFocused();
      if (guiEventListener != null) {
         ComponentPath componentPath = guiEventListener.nextFocusPath(event);
         if (componentPath != null) {
            return ComponentPath.path(this, componentPath);
         }
      }

      if (event instanceof FocusNavigationEvent.TabNavigation tabNavigation) {
         return this.handleTabNavigation(tabNavigation);
      } else {
         return event instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation ? this.handleArrowNavigation(arrowNavigation) : null;
      }
   }

   @Nullable
   private ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation tabNavigation) {
      boolean bl = tabNavigation.forward();
      GuiEventListener guiEventListener = this.getFocused();
      List<? extends GuiEventListener> list = new ArrayList<>(this.children());
      Collections.sort(list, Comparator.comparingInt(guiEventListenerx -> guiEventListenerx.getTabOrderGroup()));
      int i = list.indexOf(guiEventListener);
      int j;
      if (guiEventListener != null && i >= 0) {
         j = i + (bl ? 1 : 0);
      } else if (bl) {
         j = 0;
      } else {
         j = list.size();
      }

      ListIterator<? extends GuiEventListener> listIterator = list.listIterator(j);
      BooleanSupplier booleanSupplier = bl ? listIterator::hasNext : listIterator::hasPrevious;
      Supplier<? extends GuiEventListener> supplier = bl ? listIterator::next : listIterator::previous;

      while (booleanSupplier.getAsBoolean()) {
         GuiEventListener guiEventListener2 = supplier.get();
         ComponentPath componentPath = guiEventListener2.nextFocusPath(tabNavigation);
         if (componentPath != null) {
            return ComponentPath.path(this, componentPath);
         }
      }

      return null;
   }

   @Nullable
   private ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation arrowNavigation) {
      GuiEventListener guiEventListener = this.getFocused();
      if (guiEventListener == null) {
         ScreenDirection screenDirection = arrowNavigation.direction();
         ScreenRectangle screenRectangle = this.getBorderForArrowNavigation(screenDirection.getOpposite());
         return ComponentPath.path(this, this.nextFocusPathInDirection(screenRectangle, screenDirection, null, arrowNavigation));
      } else {
         ScreenRectangle screenRectangle2 = guiEventListener.getRectangle();
         return ComponentPath.path(this, this.nextFocusPathInDirection(screenRectangle2, arrowNavigation.direction(), guiEventListener, arrowNavigation));
      }
   }

   @Nullable
   private ComponentPath nextFocusPathInDirection(
      ScreenRectangle rectangle, ScreenDirection direction, @Nullable GuiEventListener listener, FocusNavigationEvent event
   ) {
      ScreenAxis screenAxis = direction.getAxis();
      ScreenAxis screenAxis2 = screenAxis.orthogonal();
      ScreenDirection screenDirection = screenAxis2.getPositive();
      int i = rectangle.getBoundInDirection(direction.getOpposite());
      List<GuiEventListener> list = new ArrayList<>();

      for (GuiEventListener guiEventListener : this.children()) {
         if (guiEventListener != listener) {
            ScreenRectangle screenRectangle = guiEventListener.getRectangle();
            if (screenRectangle.overlapsInAxis(rectangle, screenAxis2)) {
               int j = screenRectangle.getBoundInDirection(direction.getOpposite());
               if (direction.isAfter(j, i)) {
                  list.add(guiEventListener);
               } else if (j == i && direction.isAfter(screenRectangle.getBoundInDirection(direction), rectangle.getBoundInDirection(direction))) {
                  list.add(guiEventListener);
               }
            }
         }
      }

      Comparator<GuiEventListener> comparator = Comparator.comparing(
         guiEventListenerx -> guiEventListenerx.getRectangle().getBoundInDirection(direction.getOpposite()), direction.coordinateValueComparator()
      );
      Comparator<GuiEventListener> comparator2 = Comparator.comparing(
         guiEventListenerx -> guiEventListenerx.getRectangle().getBoundInDirection(screenDirection.getOpposite()), screenDirection.coordinateValueComparator()
      );
      list.sort(comparator.thenComparing(comparator2));

      for (GuiEventListener guiEventListener2 : list) {
         ComponentPath componentPath = guiEventListener2.nextFocusPath(event);
         if (componentPath != null) {
            return componentPath;
         }
      }

      return this.nextFocusPathVaguelyInDirection(rectangle, direction, listener, event);
   }

   @Nullable
   private ComponentPath nextFocusPathVaguelyInDirection(
      ScreenRectangle rectangle, ScreenDirection direction, @Nullable GuiEventListener listener, FocusNavigationEvent event
   ) {
      ScreenAxis screenAxis = direction.getAxis();
      ScreenAxis screenAxis2 = screenAxis.orthogonal();
      List<Pair<GuiEventListener, Long>> list = new ArrayList<>();
      ScreenPosition screenPosition = ScreenPosition.of(screenAxis, rectangle.getBoundInDirection(direction), rectangle.getCenterInAxis(screenAxis2));

      for (GuiEventListener guiEventListener : this.children()) {
         if (guiEventListener != listener) {
            ScreenRectangle screenRectangle = guiEventListener.getRectangle();
            ScreenPosition screenPosition2 = ScreenPosition.of(
               screenAxis, screenRectangle.getBoundInDirection(direction.getOpposite()), screenRectangle.getCenterInAxis(screenAxis2)
            );
            if (direction.isAfter(screenPosition2.getCoordinate(screenAxis), screenPosition.getCoordinate(screenAxis))) {
               long l = Vector2i.distanceSquared(screenPosition.x(), screenPosition.y(), screenPosition2.x(), screenPosition2.y());
               list.add(Pair.of(guiEventListener, l));
            }
         }
      }

      list.sort(Comparator.comparingDouble(Pair::getSecond));

      for (Pair<GuiEventListener, Long> pair : list) {
         ComponentPath componentPath = ((GuiEventListener)pair.getFirst()).nextFocusPath(event);
         if (componentPath != null) {
            return componentPath;
         }
      }

      return null;
   }
}
