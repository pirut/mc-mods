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
import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public interface ContainerEventHandler extends GuiEventListener {
   List<? extends GuiEventListener> children();

   default Optional<GuiEventListener> getChildAt(double p_94730_, double p_94731_) {
      for (GuiEventListener $$2 : this.children()) {
         if ($$2.isMouseOver(p_94730_, p_94731_)) {
            return Optional.of($$2);
         }
      }

      return Optional.empty();
   }

   @Override
   default boolean mouseClicked(double p_94695_, double p_94696_, int p_94697_) {
      for (GuiEventListener $$3 : this.children()) {
         if ($$3.mouseClicked(p_94695_, p_94696_, p_94697_)) {
            this.setFocused($$3);
            if (p_94697_ == 0) {
               this.setDragging(true);
            }

            return true;
         }
      }

      return false;
   }

   @Override
   default boolean mouseReleased(double p_94722_, double p_94723_, int p_94724_) {
      if (p_94724_ == 0 && this.isDragging()) {
         this.setDragging(false);
         if (this.getFocused() != null) {
            return this.getFocused().mouseReleased(p_94722_, p_94723_, p_94724_);
         }
      }

      return this.getChildAt(p_94722_, p_94723_).filter(p_94708_ -> p_94708_.mouseReleased(p_94722_, p_94723_, p_94724_)).isPresent();
   }

   @Override
   default boolean mouseDragged(double p_94699_, double p_94700_, int p_94701_, double p_94702_, double p_94703_) {
      return this.getFocused() != null && this.isDragging() && p_94701_ == 0
         ? this.getFocused().mouseDragged(p_94699_, p_94700_, p_94701_, p_94702_, p_94703_)
         : false;
   }

   boolean isDragging();

   void setDragging(boolean var1);

   @Override
   default boolean mouseScrolled(double p_94686_, double p_94687_, double p_94688_, double p_294830_) {
      return this.getChildAt(p_94686_, p_94687_).filter(p_293596_ -> p_293596_.mouseScrolled(p_94686_, p_94687_, p_94688_, p_294830_)).isPresent();
   }

   @Override
   default boolean keyPressed(int p_94710_, int p_94711_, int p_94712_) {
      return this.getFocused() != null && this.getFocused().keyPressed(p_94710_, p_94711_, p_94712_);
   }

   @Override
   default boolean keyReleased(int p_94715_, int p_94716_, int p_94717_) {
      return this.getFocused() != null && this.getFocused().keyReleased(p_94715_, p_94716_, p_94717_);
   }

   @Override
   default boolean charTyped(char p_94683_, int p_94684_) {
      return this.getFocused() != null && this.getFocused().charTyped(p_94683_, p_94684_);
   }

   @Nullable
   GuiEventListener getFocused();

   void setFocused(@Nullable GuiEventListener var1);

   @Override
   default void setFocused(boolean p_265504_) {
   }

   @Override
   default boolean isFocused() {
      return this.getFocused() != null;
   }

   @Nullable
   @Override
   default ComponentPath getCurrentFocusPath() {
      GuiEventListener $$0 = this.getFocused();
      return $$0 != null ? ComponentPath.path(this, $$0.getCurrentFocusPath()) : null;
   }

   @Nullable
   @Override
   default ComponentPath nextFocusPath(FocusNavigationEvent p_265668_) {
      GuiEventListener $$1 = this.getFocused();
      if ($$1 != null) {
         ComponentPath $$2 = $$1.nextFocusPath(p_265668_);
         if ($$2 != null) {
            return ComponentPath.path(this, $$2);
         }
      }

      if (p_265668_ instanceof FocusNavigationEvent.TabNavigation $$3) {
         return this.handleTabNavigation($$3);
      } else {
         return p_265668_ instanceof FocusNavigationEvent.ArrowNavigation $$4 ? this.handleArrowNavigation($$4) : null;
      }
   }

   @Nullable
   private ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation p_265354_) {
      boolean $$1 = p_265354_.forward();
      GuiEventListener $$2 = this.getFocused();
      List<? extends GuiEventListener> $$3 = new ArrayList<>(this.children());
      Collections.sort($$3, Comparator.comparingInt(p_344153_ -> p_344153_.getTabOrderGroup()));
      int $$4 = $$3.indexOf($$2);
      int $$5;
      if ($$2 != null && $$4 >= 0) {
         $$5 = $$4 + ($$1 ? 1 : 0);
      } else if ($$1) {
         $$5 = 0;
      } else {
         $$5 = $$3.size();
      }

      ListIterator<? extends GuiEventListener> $$8 = $$3.listIterator($$5);
      BooleanSupplier $$9 = $$1 ? $$8::hasNext : $$8::hasPrevious;
      Supplier<? extends GuiEventListener> $$10 = $$1 ? $$8::next : $$8::previous;

      while ($$9.getAsBoolean()) {
         GuiEventListener $$11 = $$10.get();
         ComponentPath $$12 = $$11.nextFocusPath(p_265354_);
         if ($$12 != null) {
            return ComponentPath.path(this, $$12);
         }
      }

      return null;
   }

   @Nullable
   private ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation p_265760_) {
      GuiEventListener $$1 = this.getFocused();
      if ($$1 == null) {
         ScreenDirection $$2 = p_265760_.direction();
         ScreenRectangle $$3 = this.getRectangle().getBorder($$2.getOpposite());
         return ComponentPath.path(this, this.nextFocusPathInDirection($$3, $$2, null, p_265760_));
      } else {
         ScreenRectangle $$4 = $$1.getRectangle();
         return ComponentPath.path(this, this.nextFocusPathInDirection($$4, p_265760_.direction(), $$1, p_265760_));
      }
   }

   @Nullable
   private ComponentPath nextFocusPathInDirection(
      ScreenRectangle p_265054_, ScreenDirection p_265167_, @Nullable GuiEventListener p_265476_, FocusNavigationEvent p_265762_
   ) {
      ScreenAxis $$4 = p_265167_.getAxis();
      ScreenAxis $$5 = $$4.orthogonal();
      ScreenDirection $$6 = $$5.getPositive();
      int $$7 = p_265054_.getBoundInDirection(p_265167_.getOpposite());
      List<GuiEventListener> $$8 = new ArrayList<>();

      for (GuiEventListener $$9 : this.children()) {
         if ($$9 != p_265476_) {
            ScreenRectangle $$10 = $$9.getRectangle();
            if ($$10.overlapsInAxis(p_265054_, $$5)) {
               int $$11 = $$10.getBoundInDirection(p_265167_.getOpposite());
               if (p_265167_.isAfter($$11, $$7)) {
                  $$8.add($$9);
               } else if ($$11 == $$7 && p_265167_.isAfter($$10.getBoundInDirection(p_265167_), p_265054_.getBoundInDirection(p_265167_))) {
                  $$8.add($$9);
               }
            }
         }
      }

      Comparator<GuiEventListener> $$12 = Comparator.comparing(
         p_264674_ -> p_264674_.getRectangle().getBoundInDirection(p_265167_.getOpposite()), p_265167_.coordinateValueComparator()
      );
      Comparator<GuiEventListener> $$13 = Comparator.comparing(
         p_264676_ -> p_264676_.getRectangle().getBoundInDirection($$6.getOpposite()), $$6.coordinateValueComparator()
      );
      $$8.sort($$12.thenComparing($$13));

      for (GuiEventListener $$14 : $$8) {
         ComponentPath $$15 = $$14.nextFocusPath(p_265762_);
         if ($$15 != null) {
            return $$15;
         }
      }

      return this.nextFocusPathVaguelyInDirection(p_265054_, p_265167_, p_265476_, p_265762_);
   }

   @Nullable
   private ComponentPath nextFocusPathVaguelyInDirection(
      ScreenRectangle p_265390_, ScreenDirection p_265687_, @Nullable GuiEventListener p_265498_, FocusNavigationEvent p_265048_
   ) {
      ScreenAxis $$4 = p_265687_.getAxis();
      ScreenAxis $$5 = $$4.orthogonal();
      List<Pair<GuiEventListener, Long>> $$6 = new ArrayList<>();
      ScreenPosition $$7 = ScreenPosition.of($$4, p_265390_.getBoundInDirection(p_265687_), p_265390_.getCenterInAxis($$5));

      for (GuiEventListener $$8 : this.children()) {
         if ($$8 != p_265498_) {
            ScreenRectangle $$9 = $$8.getRectangle();
            ScreenPosition $$10 = ScreenPosition.of($$4, $$9.getBoundInDirection(p_265687_.getOpposite()), $$9.getCenterInAxis($$5));
            if (p_265687_.isAfter($$10.getCoordinate($$4), $$7.getCoordinate($$4))) {
               long $$11 = Vector2i.distanceSquared($$7.x(), $$7.y(), $$10.x(), $$10.y());
               $$6.add(Pair.of($$8, $$11));
            }
         }
      }

      $$6.sort(Comparator.comparingDouble(Pair::getSecond));

      for (Pair<GuiEventListener, Long> $$12 : $$6) {
         ComponentPath $$13 = ((GuiEventListener)$$12.getFirst()).nextFocusPath(p_265048_);
         if ($$13 != null) {
            return $$13;
         }
      }

      return null;
   }
}
