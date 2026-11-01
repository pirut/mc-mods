package net.minecraft.client.gui.components.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
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

   TabNavigationBar(int width, TabManager tabManager, Iterable<Tab> tabs) {
      this.width = width;
      this.tabManager = tabManager;
      this.tabs = ImmutableList.copyOf(tabs);
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      com.google.common.collect.ImmutableList.Builder<TabButton> builder = ImmutableList.builder();

      for (Tab tab : tabs) {
         builder.add(this.layout.addChild(new TabButton(tabManager, tab, 0, 24)));
      }

      this.tabButtons = builder.build();
   }

   public static TabNavigationBar.Builder builder(TabManager tabManager, int width) {
      return new TabNavigationBar.Builder(tabManager, width);
   }

   public void setWidth(int width) {
      this.width = width;
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return mouseX >= this.layout.getX()
         && mouseY >= this.layout.getY()
         && mouseX < this.layout.getX() + this.layout.getWidth()
         && mouseY < this.layout.getY() + this.layout.getHeight();
   }

   @Override
   public void setFocused(boolean focused) {
      super.setFocused(focused);
      if (this.getFocused() != null) {
         this.setFocused(null);
      }
   }

   @Override
   public void setFocused(@Nullable GuiEventListener focused) {
      super.setFocused(focused);
      if (focused instanceof TabButton tabButton && tabButton.isActive()) {
         this.tabManager.setCurrentTab(tabButton.tab(), true);
      }
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      if (!this.isFocused()) {
         TabButton tabButton = this.currentTabButton();
         if (tabButton != null) {
            return ComponentPath.path(this, ComponentPath.leaf(tabButton));
         }
      }

      return event instanceof FocusNavigationEvent.TabNavigation ? null : super.nextFocusPath(event);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return this.tabButtons;
   }

   public List<Tab> getTabs() {
      return this.tabs;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.tabButtons.stream().map(AbstractWidget::narrationPriority).max(Comparator.naturalOrder()).orElse(NarratableEntry.NarrationPriority.NONE);
   }

   @Override
   public void updateNarration(NarrationElementOutput narrationElementOutput) {
      Optional<TabButton> optional = this.tabButtons
         .stream()
         .filter(AbstractWidget::isHovered)
         .findFirst()
         .or(() -> Optional.ofNullable(this.currentTabButton()));
      optional.ifPresent(tabButton -> {
         this.narrateListElementPosition(narrationElementOutput.nest(), tabButton);
         tabButton.updateNarration(narrationElementOutput);
      });
      if (this.isFocused()) {
         narrationElementOutput.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }
   }

   protected void narrateListElementPosition(NarrationElementOutput narrationElementOutput, TabButton tabButton) {
      if (this.tabs.size() > 1) {
         int i = this.tabButtons.indexOf(tabButton);
         if (i != -1) {
            narrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", i + 1, this.tabs.size()));
         }
      }
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.blit(
         RenderPipelines.GUI_TEXTURED,
         Screen.HEADER_SEPARATOR,
         0,
         this.layout.getY() + this.layout.getHeight() - 2,
         0.0F,
         0.0F,
         ((TabButton)this.tabButtons.get(0)).getX(),
         2,
         32,
         2
      );
      int i = ((TabButton)this.tabButtons.get(this.tabButtons.size() - 1)).getRight();
      guiGraphics.blit(
         RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR, i, this.layout.getY() + this.layout.getHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2
      );
      UnmodifiableIterator var6 = this.tabButtons.iterator();

      while (var6.hasNext()) {
         TabButton tabButton = (TabButton)var6.next();
         tabButton.render(guiGraphics, mouseX, mouseY, partialTick);
      }
   }

   @Override
   public ScreenRectangle getRectangle() {
      return this.layout.getRectangle();
   }

   public void arrangeElements() {
      int i = Math.min(400, this.width) - 28;
      int j = Mth.roundToward(i / this.tabs.size(), 2);
      UnmodifiableIterator var3 = this.tabButtons.iterator();

      while (var3.hasNext()) {
         TabButton tabButton = (TabButton)var3.next();
         tabButton.setWidth(j);
      }

      this.layout.arrangeElements();
      this.layout.setX(Mth.roundToward((this.width - i) / 2, 2));
      this.layout.setY(0);
   }

   public void selectTab(int index, boolean playClickSound) {
      if (this.isFocused()) {
         this.setFocused((GuiEventListener)this.tabButtons.get(index));
      } else if (((TabButton)this.tabButtons.get(index)).isActive()) {
         this.tabManager.setCurrentTab((Tab)this.tabs.get(index), playClickSound);
      }
   }

   public void setTabActiveState(int index, boolean active) {
      if (index >= 0 && index < this.tabButtons.size()) {
         ((TabButton)this.tabButtons.get(index)).active = active;
      }
   }

   public void setTabTooltip(int index, @Nullable Tooltip tooltip) {
      if (index >= 0 && index < this.tabButtons.size()) {
         ((TabButton)this.tabButtons.get(index)).setTooltip(tooltip);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.hasControlDownWithQuirk()) {
         int i = this.getNextTabIndex(event);
         if (i != -1) {
            this.selectTab(Mth.clamp(i, 0, this.tabs.size() - 1), true);
            return true;
         }
      }

      return false;
   }

   private int getNextTabIndex(KeyEvent event) {
      return this.getNextTabIndex(this.currentTabIndex(), event);
   }

   private int getNextTabIndex(int tabIndex, KeyEvent event) {
      int i = event.getDigit();
      if (i != -1) {
         return Math.floorMod(i - 1, 10);
      } else if (event.isCycleFocus() && tabIndex != -1) {
         int j = event.hasShiftDown() ? tabIndex - 1 : tabIndex + 1;
         int k = Math.floorMod(j, this.tabs.size());
         return ((TabButton)this.tabButtons.get(k)).active ? k : this.getNextTabIndex(k, event);
      } else {
         return -1;
      }
   }

   private int currentTabIndex() {
      Tab tab = this.tabManager.getCurrentTab();
      int i = this.tabs.indexOf(tab);
      return i != -1 ? i : -1;
   }

   @Nullable
   private TabButton currentTabButton() {
      int i = this.currentTabIndex();
      return i != -1 ? (TabButton)this.tabButtons.get(i) : null;
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final int width;
      private final TabManager tabManager;
      private final List<Tab> tabs = new ArrayList<>();

      Builder(TabManager tabManager, int width) {
         this.tabManager = tabManager;
         this.width = width;
      }

      public TabNavigationBar.Builder addTabs(Tab... tabs) {
         Collections.addAll(this.tabs, tabs);
         return this;
      }

      public TabNavigationBar build() {
         return new TabNavigationBar(this.width, this.tabManager, this.tabs);
      }
   }
}
