package net.minecraft.client.gui.components.tabs;

import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TabManager {
   private final Consumer<AbstractWidget> addWidget;
   private final Consumer<AbstractWidget> removeWidget;
   private final Consumer<Tab> onSelected;
   private final Consumer<Tab> onDeselected;
   @Nullable
   private Tab currentTab;
   @Nullable
   private ScreenRectangle tabArea;

   public TabManager(Consumer<AbstractWidget> addWidget, Consumer<AbstractWidget> removeWidget) {
      this(addWidget, removeWidget, tab -> {}, tab -> {});
   }

   public TabManager(Consumer<AbstractWidget> addWidget, Consumer<AbstractWidget> removeWidget, Consumer<Tab> onSelected, Consumer<Tab> onDeselected) {
      this.addWidget = addWidget;
      this.removeWidget = removeWidget;
      this.onSelected = onSelected;
      this.onDeselected = onDeselected;
   }

   public void setTabArea(ScreenRectangle tabArea) {
      this.tabArea = tabArea;
      Tab tab = this.getCurrentTab();
      if (tab != null) {
         tab.doLayout(tabArea);
      }
   }

   public void setCurrentTab(Tab tab, boolean playClickSound) {
      if (!Objects.equals(this.currentTab, tab)) {
         if (this.currentTab != null) {
            this.currentTab.visitChildren(this.removeWidget);
         }

         Tab tab2 = this.currentTab;
         this.currentTab = tab;
         tab.visitChildren(this.addWidget);
         if (this.tabArea != null) {
            tab.doLayout(this.tabArea);
         }

         if (playClickSound) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }

         this.onDeselected.accept(tab2);
         this.onSelected.accept(this.currentTab);
      }
   }

   @Nullable
   public Tab getCurrentTab() {
      return this.currentTab;
   }
}
