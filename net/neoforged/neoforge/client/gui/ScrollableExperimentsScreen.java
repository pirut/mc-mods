package net.neoforged.neoforge.client.gui;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.ExperimentsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ScrollableExperimentsScreen extends ExperimentsScreen {
   private static final int DEFAULT_LIST_HEIGHT = 121;
   private static final int ROW_PADDING = 4;
   private static final int LIST_PADDING = 38;
   private static final int ENTRY_HEIGHT = 42;
   @Nullable
   private ScrollableExperimentsScreen.ExperimentSelectionList selectionList;
   @Nullable
   private LinearLayout listLayout;

   public ScrollableExperimentsScreen(Screen parent, PackRepository packRepository, Consumer<PackRepository> output) {
      super(parent, packRepository, output);
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      LinearLayout contentLayout = this.layout.addToContents(LinearLayout.vertical(), LayoutSettings::alignVerticallyTop);
      contentLayout.addChild(new MultiLineTextWidget(INFO, this.font).setMaxWidth(310), settings -> settings.paddingBottom(15).alignHorizontallyCenter());
      this.listLayout = contentLayout.addChild(LinearLayout.vertical());
      this.selectionList = new ScrollableExperimentsScreen.ExperimentSelectionList(this.minecraft);
      this.packs
         .forEach(
            (pack, selected) -> this.selectionList
               .addEntry(
                  new ScrollableExperimentsScreen.ExperimentSelectionList.ExperimentEntry(
                     getHumanReadableTitle(pack), () -> this.packs.getBoolean(pack), flag -> this.packs.put(pack, flag), pack.getDescription()
                  )
               )
         );
      this.listLayout.addChild(this.selectionList);
      LinearLayout footerLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      footerLayout.addChild(Button.builder(CommonComponents.GUI_DONE, btn -> this.onDone()).build());
      footerLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose()).build());
      this.layout.visitWidgets(x$0 -> {
         AbstractWidget var10000 = this.addRenderableWidget(x$0);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      if (this.selectionList != null) {
         this.selectionList.setHeight(121);
      }

      super.repositionElements();
      if (this.selectionList != null && this.listLayout != null) {
         this.selectionList.setHeight(this.layout.getContentHeight() - this.listLayout.getY());
         this.selectionList.setPosition(this.listLayout.getX(), this.listLayout.getY());
         this.selectionList.clampScrollAmount();
      }
   }

   private static class ExperimentSelectionList extends ContainerObjectSelectionList<ScrollableExperimentsScreen.ExperimentSelectionList.ExperimentEntry> {
      public ExperimentSelectionList(Minecraft mc) {
         super(mc, 348, 121, 0, 42);
      }

      @Override
      public int getRowWidth() {
         return 314;
      }

      protected int addEntry(ScrollableExperimentsScreen.ExperimentSelectionList.ExperimentEntry entry) {
         return super.addEntry(entry);
      }

      private static class ExperimentEntry extends ContainerObjectSelectionList.Entry<ScrollableExperimentsScreen.ExperimentSelectionList.ExperimentEntry> {
         private static final int BUTTON_WIDTH = 44;
         private static final int TITLE_Y = 6;
         private static final int DESCRIPTION_Y = 20;
         private final StringWidget titleWidget;
         private final MultiLineTextWidget descriptionWidget;
         private final CycleButton<Boolean> button;
         private final List<AbstractWidget> children;

         public ExperimentEntry(Component title, BooleanSupplier selectedSupplier, Consumer<Boolean> selectedSetter, Component description) {
            this.titleWidget = new StringWidget(title, Minecraft.getInstance().font).alignLeft();
            this.descriptionWidget = new MultiLineTextWidget(description.copy().withStyle(ChatFormatting.GRAY), Minecraft.getInstance().font).setMaxRows(2);
            this.button = CycleButton.onOffBuilder(selectedSupplier.getAsBoolean())
               .displayOnlyValue()
               .withCustomNarration(btn -> CommonComponents.joinForNarration(title, btn.createDefaultNarrationMessage(), description))
               .create(0, 0, 44, 20, Component.empty(), (btn, val) -> selectedSetter.accept(val));
            this.children = List.of(this.titleWidget, this.descriptionWidget, this.button);
         }

         @Override
         public void render(
            GuiGraphics graphics, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
         ) {
            this.titleWidget.setPosition(left, top + 6);
            this.descriptionWidget.setPosition(left, top + 20);
            this.descriptionWidget.setMaxWidth(entryWidth - this.button.getWidth());
            this.button.setPosition(left + entryWidth - this.button.getWidth() - 4, top);
            this.titleWidget.render(graphics, mouseX, mouseY, partialTick);
            this.descriptionWidget.render(graphics, mouseX, mouseY, partialTick);
            this.button.render(graphics, mouseX, mouseY, partialTick);
         }

         @Override
         public List<? extends GuiEventListener> children() {
            return this.children;
         }

         @Override
         public List<? extends NarratableEntry> narratables() {
            return this.children;
         }
      }
   }
}
