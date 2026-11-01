package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class OptionsList extends ContainerObjectSelectionList<OptionsList.AbstractEntry> {
   private static final int BIG_BUTTON_WIDTH = 310;
   private static final int DEFAULT_ITEM_HEIGHT = 25;
   private final OptionsSubScreen screen;

   public OptionsList(Minecraft minecraft, int width, OptionsSubScreen screen) {
      super(minecraft, width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 25);
      this.centerListVertically = false;
      this.screen = screen;
   }

   public void addBig(OptionInstance<?> option) {
      this.addEntry(OptionsList.Entry.big(this.minecraft.options, option, this.screen));
   }

   public void addSmall(OptionInstance<?>... options) {
      for (int i = 0; i < options.length; i += 2) {
         OptionInstance<?> optionInstance = i < options.length - 1 ? options[i + 1] : null;
         this.addEntry(OptionsList.Entry.small(this.minecraft.options, options[i], optionInstance, this.screen));
      }
   }

   public void addSmall(List<AbstractWidget> options) {
      for (int i = 0; i < options.size(); i += 2) {
         this.addSmall(options.get(i), i < options.size() - 1 ? options.get(i + 1) : null);
      }
   }

   public void addSmall(AbstractWidget leftOption, @Nullable AbstractWidget rightOption) {
      this.addEntry(OptionsList.Entry.small(leftOption, rightOption, this.screen));
   }

   public void addSmall(AbstractWidget leftOption, OptionInstance<?> leftOptionInstance, @Nullable AbstractWidget rightOption) {
      this.addEntry(OptionsList.Entry.small(leftOption, leftOptionInstance, rightOption, this.screen));
   }

   public void addHeader(Component text) {
      int i = 9;
      int j = this.children().isEmpty() ? 0 : i * 2;
      this.addEntry(new OptionsList.HeaderEntry(this.screen, text, j), j + i + 4);
   }

   @Override
   public int getRowWidth() {
      return 310;
   }

   @Nullable
   public AbstractWidget findOption(OptionInstance<?> option) {
      for (OptionsList.AbstractEntry abstractEntry : this.children()) {
         if (abstractEntry instanceof OptionsList.Entry entry) {
            AbstractWidget abstractWidget = entry.findOption(option);
            if (abstractWidget != null) {
               return abstractWidget;
            }
         }
      }

      return null;
   }

   public void applyUnsavedChanges() {
      for (OptionsList.AbstractEntry abstractEntry : this.children()) {
         if (abstractEntry instanceof OptionsList.Entry entry) {
            for (OptionsList.OptionInstanceWidget optionInstanceWidget : entry.children) {
               if (optionInstanceWidget.optionInstance() != null
                  && optionInstanceWidget.widget() instanceof OptionInstance.OptionInstanceSliderButton<?> optionInstanceSliderButton) {
                  optionInstanceSliderButton.applyUnsavedValue();
               }
            }
         }
      }
   }

   public void resetOption(OptionInstance<?> option) {
      for (OptionsList.AbstractEntry abstractEntry : this.children()) {
         if (abstractEntry instanceof OptionsList.Entry entry) {
            for (OptionsList.OptionInstanceWidget optionInstanceWidget : entry.children) {
               if (optionInstanceWidget.optionInstance() == option && optionInstanceWidget.widget() instanceof ResettableOptionWidget resettableOptionWidget) {
                  resettableOptionWidget.resetValue();
                  return;
               }
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   protected abstract static class AbstractEntry extends ContainerObjectSelectionList.Entry<OptionsList.AbstractEntry> {
   }

   @Environment(EnvType.CLIENT)
   protected static class Entry extends OptionsList.AbstractEntry {
      final List<OptionsList.OptionInstanceWidget> children;
      private final Screen screen;
      private static final int X_OFFSET = 160;

      private Entry(List<OptionsList.OptionInstanceWidget> children, Screen screen) {
         this.children = children;
         this.screen = screen;
      }

      public static OptionsList.Entry big(Options options, OptionInstance<?> option, Screen screen) {
         return new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(option.createButton(options, 0, 0, 310), option)), screen);
      }

      public static OptionsList.Entry small(AbstractWidget leftOption, @Nullable AbstractWidget rightOption, Screen screen) {
         return rightOption == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftOption)), screen)
            : new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftOption), new OptionsList.OptionInstanceWidget(rightOption)), screen);
      }

      public static OptionsList.Entry small(
         AbstractWidget leftOption, OptionInstance<?> leftOptionInstance, @Nullable AbstractWidget rightOption, Screen screen
      ) {
         return rightOption == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftOption, leftOptionInstance)), screen)
            : new OptionsList.Entry(
               List.of(new OptionsList.OptionInstanceWidget(leftOption, leftOptionInstance), new OptionsList.OptionInstanceWidget(rightOption)), screen
            );
      }

      public static OptionsList.Entry small(Options options, OptionInstance<?> leftOption, @Nullable OptionInstance<?> rightOption, OptionsSubScreen screen) {
         AbstractWidget abstractWidget = leftOption.createButton(options);
         return rightOption == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(abstractWidget, leftOption)), screen)
            : new OptionsList.Entry(
               List.of(
                  new OptionsList.OptionInstanceWidget(abstractWidget, leftOption),
                  new OptionsList.OptionInstanceWidget(rightOption.createButton(options), rightOption)
               ),
               screen
            );
      }

      @Override
      public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
         int i = 0;
         int j = this.screen.width / 2 - 155;

         for (OptionsList.OptionInstanceWidget optionInstanceWidget : this.children) {
            optionInstanceWidget.widget().setPosition(j + i, this.getContentY());
            optionInstanceWidget.widget().render(guiGraphics, mouseX, mouseY, partialTick);
            i += 160;
         }
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return Lists.transform(this.children, OptionsList.OptionInstanceWidget::widget);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return Lists.transform(this.children, OptionsList.OptionInstanceWidget::widget);
      }

      @Nullable
      public AbstractWidget findOption(OptionInstance<?> optionInstance) {
         for (OptionsList.OptionInstanceWidget optionInstanceWidget : this.children) {
            if (optionInstanceWidget.optionInstance == optionInstance) {
               return optionInstanceWidget.widget();
            }
         }

         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   protected static class HeaderEntry extends OptionsList.AbstractEntry {
      private final Screen screen;
      private final int paddingTop;
      private final StringWidget widget;

      protected HeaderEntry(Screen screen, Component text, int paddingTop) {
         this.screen = screen;
         this.paddingTop = paddingTop;
         this.widget = new StringWidget(text, screen.getFont());
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return List.of(this.widget);
      }

      @Override
      public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
         this.widget.setPosition(this.screen.width / 2 - 155, this.getContentY() + this.paddingTop);
         this.widget.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return List.of(this.widget);
      }
   }

   @Environment(EnvType.CLIENT)
   public record OptionInstanceWidget(AbstractWidget widget, @Nullable OptionInstance<?> optionInstance) {

      public OptionInstanceWidget(AbstractWidget widget) {
         this(widget, null);
      }
   }
}
