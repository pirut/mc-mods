package net.minecraft.client.gui.components;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ContainerObjectSelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
   public ContainerObjectSelectionList(Minecraft p_94010_, int p_94011_, int p_94012_, int p_94013_, int p_94014_) {
      super(p_94010_, p_94011_, p_94012_, p_94013_, p_94014_);
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent p_265385_) {
      if (this.getItemCount() == 0) {
         return null;
      } else if (!(p_265385_ instanceof FocusNavigationEvent.ArrowNavigation $$1)) {
         return super.nextFocusPath(p_265385_);
      } else {
         E $$2 = this.getFocused();
         if ($$1.direction().getAxis() == ScreenAxis.HORIZONTAL && $$2 != null) {
            return ComponentPath.path(this, $$2.nextFocusPath(p_265385_));
         } else {
            int $$3 = -1;
            ScreenDirection $$4 = $$1.direction();
            if ($$2 != null) {
               $$3 = $$2.children().indexOf($$2.getFocused());
            }

            if ($$3 == -1) {
               switch ($$4) {
                  case LEFT:
                     $$3 = Integer.MAX_VALUE;
                     $$4 = ScreenDirection.DOWN;
                     break;
                  case RIGHT:
                     $$3 = 0;
                     $$4 = ScreenDirection.DOWN;
                     break;
                  default:
                     $$3 = 0;
               }
            }

            E $$5 = $$2;

            ComponentPath $$6;
            do {
               $$5 = this.nextEntry($$4, p_351636_ -> !p_351636_.children().isEmpty(), $$5);
               if ($$5 == null) {
                  return null;
               }

               $$6 = $$5.focusPathAtIndex($$1, $$3);
            } while ($$6 == null);

            return ComponentPath.path(this, $$6);
         }
      }
   }

   @Override
   public void setFocused(@Nullable GuiEventListener p_265559_) {
      if (this.getFocused() != p_265559_) {
         super.setFocused(p_265559_);
         if (p_265559_ == null) {
            this.setSelected(null);
         }
      }
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.isFocused() ? NarratableEntry.NarrationPriority.FOCUSED : super.narrationPriority();
   }

   @Override
   protected boolean isSelectedItem(int p_94019_) {
      return false;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_313707_) {
      E $$1 = this.getHovered();
      if ($$1 != null) {
         $$1.updateNarration(p_313707_.nest());
         this.narrateListElementPosition(p_313707_, $$1);
      } else {
         E $$2 = this.getFocused();
         if ($$2 != null) {
            $$2.updateNarration(p_313707_.nest());
            this.narrateListElementPosition(p_313707_, $$2);
         }
      }

      p_313707_.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements ContainerEventHandler {
      @Nullable
      private GuiEventListener focused;
      @Nullable
      private NarratableEntry lastNarratable;
      private boolean dragging;

      @Override
      public boolean isDragging() {
         return this.dragging;
      }

      @Override
      public void setDragging(boolean p_94028_) {
         this.dragging = p_94028_;
      }

      @Override
      public boolean mouseClicked(double p_265453_, double p_265297_, int p_265697_) {
         return ContainerEventHandler.super.mouseClicked(p_265453_, p_265297_, p_265697_);
      }

      @Override
      public void setFocused(@Nullable GuiEventListener p_94024_) {
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         if (p_94024_ != null) {
            p_94024_.setFocused(true);
         }

         this.focused = p_94024_;
      }

      @Nullable
      @Override
      public GuiEventListener getFocused() {
         return this.focused;
      }

      @Nullable
      public ComponentPath focusPathAtIndex(FocusNavigationEvent p_265435_, int p_265432_) {
         if (this.children().isEmpty()) {
            return null;
         } else {
            ComponentPath $$2 = this.children().get(Math.min(p_265432_, this.children().size() - 1)).nextFocusPath(p_265435_);
            return ComponentPath.path(this, $$2);
         }
      }

      @Nullable
      @Override
      public ComponentPath nextFocusPath(FocusNavigationEvent p_265672_) {
         if (p_265672_ instanceof FocusNavigationEvent.ArrowNavigation $$1) {
            int $$2 = switch ($$1.direction()) {
               case LEFT -> -1;
               case RIGHT -> 1;
               case UP, DOWN -> 0;
            };
            if ($$2 == 0) {
               return null;
            }

            int $$3 = Mth.clamp($$2 + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1);

            for (int $$4 = $$3; $$4 >= 0 && $$4 < this.children().size(); $$4 += $$2) {
               GuiEventListener $$5 = this.children().get($$4);
               ComponentPath $$6 = $$5.nextFocusPath(p_265672_);
               if ($$6 != null) {
                  return ComponentPath.path(this, $$6);
               }
            }
         }

         return ContainerEventHandler.super.nextFocusPath(p_265672_);
      }

      public abstract List<? extends NarratableEntry> narratables();

      void updateNarration(NarrationElementOutput p_168855_) {
         List<? extends NarratableEntry> $$1 = this.narratables();
         Screen.NarratableSearchResult $$2 = Screen.findNarratableWidget($$1, this.lastNarratable);
         if ($$2 != null) {
            if ($$2.priority.isTerminal()) {
               this.lastNarratable = $$2.entry;
            }

            if ($$1.size() > 1) {
               p_168855_.add(NarratedElementType.POSITION, Component.translatable("narrator.position.object_list", $$2.index + 1, $$1.size()));
               if ($$2.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                  p_168855_.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
               }
            }

            $$2.entry.updateNarration(p_168855_.nest());
         }
      }
   }
}
