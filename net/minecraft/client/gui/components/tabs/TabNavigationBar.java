package net.minecraft.client.gui.components.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TabNavigationBar extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
   private static final int NO_TAB = -1;
   private static final int MAX_WIDTH = 400;
   private static final int HEIGHT = 24;
   private static final int MARGIN = 14;
   private static final Component USAGE_NARRATION = Component.translatable("narration.tab_navigation.usage");
   private final LinearLayout layout = LinearLayout.horizontal();
   private int width;
   private final TabManager tabManager;
   private final ImmutableList<Tab> tabs;
   private final ImmutableList<TabButton> tabButtons;

   TabNavigationBar(int p_275379_, TabManager p_275624_, Iterable<Tab> p_275279_) {
      this.width = p_275379_;
      this.tabManager = p_275624_;
      this.tabs = ImmutableList.copyOf(p_275279_);
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      com.google.common.collect.ImmutableList.Builder<TabButton> $$3 = ImmutableList.builder();

      for (Tab $$4 : p_275279_) {
         $$3.add(this.layout.addChild(new TabButton(p_275624_, $$4, 0, 24)));
      }

      this.tabButtons = $$3.build();
   }

   public static TabNavigationBar.Builder builder(TabManager p_268126_, int p_268070_) {
      return new TabNavigationBar.Builder(p_268126_, p_268070_);
   }

   public void setWidth(int p_268094_) {
      this.width = p_268094_;
   }

   @Override
   public void setFocused(boolean p_275488_) {
      super.setFocused(p_275488_);
      if (this.getFocused() != null) {
         this.getFocused().setFocused(p_275488_);
      }
   }

   @Override
   public void setFocused(@Nullable GuiEventListener p_275675_) {
      super.setFocused(p_275675_);
      if (p_275675_ instanceof TabButton $$1) {
         this.tabManager.setCurrentTab($$1.tab(), true);
      }
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent p_275418_) {
      if (!this.isFocused()) {
         TabButton $$1 = this.currentTabButton();
         if ($$1 != null) {
            return ComponentPath.path(this, ComponentPath.leaf($$1));
         }
      }

      return p_275418_ instanceof FocusNavigationEvent.TabNavigation ? null : super.nextFocusPath(p_275418_);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return this.tabButtons;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.tabButtons.stream().map(AbstractWidget::narrationPriority).max(Comparator.naturalOrder()).orElse(NarratableEntry.NarrationPriority.NONE);
   }

   @Override
   public void updateNarration(NarrationElementOutput p_275583_) {
      Optional<TabButton> $$1 = this.tabButtons.stream().filter(AbstractWidget::isHovered).findFirst().or(() -> Optional.ofNullable(this.currentTabButton()));
      $$1.ifPresent(p_274663_ -> {
         this.narrateListElementPosition(p_275583_.nest(), p_274663_);
         p_274663_.updateNarration(p_275583_);
      });
      if (this.isFocused()) {
         p_275583_.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }
   }

   protected void narrateListElementPosition(NarrationElementOutput p_275386_, TabButton p_275397_) {
      if (this.tabs.size() > 1) {
         int $$2 = this.tabButtons.indexOf(p_275397_);
         if ($$2 != -1) {
            p_275386_.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", $$2 + 1, this.tabs.size()));
         }
      }
   }

   @Override
   public void render(GuiGraphics p_281720_, int p_282085_, int p_281687_, float p_283048_) {
      RenderSystem.enableBlend();
      p_281720_.blit(
         Screen.HEADER_SEPARATOR, 0, this.layout.getY() + this.layout.getHeight() - 2, 0.0F, 0.0F, ((TabButton)this.tabButtons.get(0)).getX(), 2, 32, 2
      );
      int $$4 = ((TabButton)this.tabButtons.get(this.tabButtons.size() - 1)).getRight();
      p_281720_.blit(Screen.HEADER_SEPARATOR, $$4, this.layout.getY() + this.layout.getHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
      RenderSystem.disableBlend();
      UnmodifiableIterator var6 = this.tabButtons.iterator();

      while (var6.hasNext()) {
         TabButton $$5 = (TabButton)var6.next();
         $$5.render(p_281720_, p_282085_, p_281687_, p_283048_);
      }
   }

   @Override
   public ScreenRectangle getRectangle() {
      return this.layout.getRectangle();
   }

   public void arrangeElements() {
      int $$0 = Math.min(400, this.width) - 28;
      int $$1 = Mth.roundToward($$0 / this.tabs.size(), 2);
      UnmodifiableIterator var3 = this.tabButtons.iterator();

      while (var3.hasNext()) {
         TabButton $$2 = (TabButton)var3.next();
         $$2.setWidth($$1);
      }

      this.layout.arrangeElements();
      this.layout.setX(Mth.roundToward((this.width - $$0) / 2, 2));
      this.layout.setY(0);
   }

   public void selectTab(int p_276107_, boolean p_276125_) {
      if (this.isFocused()) {
         this.setFocused((GuiEventListener)this.tabButtons.get(p_276107_));
      } else {
         this.tabManager.setCurrentTab((Tab)this.tabs.get(p_276107_), p_276125_);
      }
   }

   public boolean keyPressed(int p_270495_) {
      if (Screen.hasControlDown()) {
         int $$1 = this.getNextTabIndex(p_270495_);
         if ($$1 != -1) {
            this.selectTab(Mth.clamp($$1, 0, this.tabs.size() - 1), true);
            return true;
         }
      }

      return false;
   }

   private int getNextTabIndex(int p_270508_) {
      if (p_270508_ >= 49 && p_270508_ <= 57) {
         return p_270508_ - 49;
      } else {
         if (p_270508_ == 258) {
            int $$1 = this.currentTabIndex();
            if ($$1 != -1) {
               int $$2 = Screen.hasShiftDown() ? $$1 - 1 : $$1 + 1;
               return Math.floorMod($$2, this.tabs.size());
            }
         }

         return -1;
      }
   }

   private int currentTabIndex() {
      Tab $$0 = this.tabManager.getCurrentTab();
      int $$1 = this.tabs.indexOf($$0);
      return $$1 != -1 ? $$1 : -1;
   }

   @Nullable
   private TabButton currentTabButton() {
      int $$0 = this.currentTabIndex();
      return $$0 != -1 ? (TabButton)this.tabButtons.get($$0) : null;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final int width;
      private final TabManager tabManager;
      private final List<Tab> tabs = new ArrayList<>();

      Builder(TabManager p_268334_, int p_267986_) {
         this.tabManager = p_268334_;
         this.width = p_267986_;
      }

      public TabNavigationBar.Builder addTabs(Tab... p_268144_) {
         Collections.addAll(this.tabs, p_268144_);
         return this;
      }

      public TabNavigationBar build() {
         return new TabNavigationBar(this.width, this.tabManager, this.tabs);
      }
   }
}
