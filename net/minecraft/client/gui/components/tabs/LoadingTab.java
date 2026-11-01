package net.minecraft.client.gui.components.tabs;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class LoadingTab implements Tab {
   private final Component title;
   private final Component loadingTitle;
   protected final LinearLayout layout = LinearLayout.vertical();

   public LoadingTab(Font font, Component title, Component loadingTitle) {
      this.title = title;
      this.loadingTitle = loadingTitle;
      LoadingDotsWidget loadingDotsWidget = new LoadingDotsWidget(font, loadingTitle);
      this.layout.defaultCellSetting().alignVerticallyMiddle().alignHorizontallyCenter();
      this.layout.addChild(loadingDotsWidget, layoutSettings -> layoutSettings.paddingBottom(30));
   }

   @Override
   public Component getTabTitle() {
      return this.title;
   }

   @Override
   public Component getTabExtraNarration() {
      return this.loadingTitle;
   }

   @Override
   public void visitChildren(Consumer<AbstractWidget> consumer) {
      this.layout.visitWidgets(consumer);
   }

   @Override
   public void doLayout(ScreenRectangle rectangle) {
      this.layout.arrangeElements();
      FrameLayout.alignInRectangle(this.layout, rectangle, 0.5F, 0.5F);
   }
}
