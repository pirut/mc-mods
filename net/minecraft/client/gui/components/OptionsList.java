package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OptionsList extends ContainerObjectSelectionList<OptionsList.AbstractEntry> {
   private static final int BIG_BUTTON_WIDTH = 310;
   private static final int DEFAULT_ITEM_HEIGHT = 25;
   private final OptionsSubScreen screen;

   public OptionsList(Minecraft p_94465_, int p_94466_, OptionsSubScreen p_345374_) {
      super(p_94465_, p_94466_, p_345374_.layout.getContentHeight(), p_345374_.layout.getHeaderHeight(), 25);
      this.centerListVertically = false;
      this.screen = p_345374_;
   }

   public void addBig(OptionInstance<?> p_232529_) {
      this.addEntry(OptionsList.Entry.big(this.minecraft.options, p_232529_, this.screen));
   }

   public void addSmall(OptionInstance<?>... p_232534_) {
      for (int i = 0; i < p_232534_.length; i += 2) {
         OptionInstance<?> optioninstance = i < p_232534_.length - 1 ? p_232534_[i + 1] : null;
         this.addEntry(OptionsList.Entry.small(this.minecraft.options, p_232534_[i], optioninstance, this.screen));
      }
   }

   public void addSmall(List<AbstractWidget> p_333735_) {
      for (int i = 0; i < p_333735_.size(); i += 2) {
         this.addSmall(p_333735_.get(i), i < p_333735_.size() - 1 ? p_333735_.get(i + 1) : null);
      }
   }

   public void addSmall(AbstractWidget p_333843_, @Nullable AbstractWidget p_334089_) {
      this.addEntry(OptionsList.Entry.small(p_333843_, p_334089_, this.screen));
   }

   public void addSmall(AbstractWidget p_460679_, OptionInstance<?> p_460682_, @Nullable AbstractWidget p_460799_) {
      this.addEntry(OptionsList.Entry.small(p_460679_, p_460682_, p_460799_, this.screen));
   }

   public void addHeader(Component p_455792_) {
      int i = 9;
      int j = this.children().isEmpty() ? 0 : i * 2;
      this.addEntry(new OptionsList.HeaderEntry(this.screen, p_455792_, j), j + i + 4);
   }

   @Override
   public int getRowWidth() {
      return 310;
   }

   @Nullable
   public AbstractWidget findOption(OptionInstance<?> p_232536_) {
      for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
         if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
            AbstractWidget abstractwidget = optionslist$entry.findOption(p_232536_);
            if (abstractwidget != null) {
               return abstractwidget;
            }
         }
      }

      return null;
   }

   public void applyUnsavedChanges() {
      for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
         if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
            for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : optionslist$entry.children) {
               if (optionslist$optioninstancewidget.optionInstance() != null
                  && optionslist$optioninstancewidget.widget() instanceof OptionInstance.OptionInstanceSliderButton<?> optioninstancesliderbutton) {
                  optioninstancesliderbutton.applyUnsavedValue();
               }
            }
         }
      }
   }

   public void resetOption(OptionInstance<?> p_455548_) {
      for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
         if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
            for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : optionslist$entry.children) {
               if (optionslist$optioninstancewidget.optionInstance() == p_455548_
                  && optionslist$optioninstancewidget.widget() instanceof ResettableOptionWidget resettableoptionwidget) {
                  resettableoptionwidget.resetValue();
                  return;
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected abstract static class AbstractEntry extends ContainerObjectSelectionList.Entry<OptionsList.AbstractEntry> {
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Entry extends OptionsList.AbstractEntry {
      final List<OptionsList.OptionInstanceWidget> children;
      private final Screen screen;
      private static final int X_OFFSET = 160;

      private Entry(List<OptionsList.OptionInstanceWidget> p_333982_, Screen p_333707_) {
         this.children = p_333982_;
         this.screen = p_333707_;
      }

      public static OptionsList.Entry big(Options p_460791_, OptionInstance<?> p_460946_, Screen p_334023_) {
         return new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(p_460946_.createButton(p_460791_, 0, 0, 310), p_460946_)), p_334023_);
      }

      public static OptionsList.Entry small(AbstractWidget p_333824_, @Nullable AbstractWidget p_333990_, Screen p_334077_) {
         return p_333990_ == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(p_333824_)), p_334077_)
            : new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(p_333824_), new OptionsList.OptionInstanceWidget(p_333990_)), p_334077_);
      }

      public static OptionsList.Entry small(AbstractWidget p_460895_, OptionInstance<?> p_461260_, @Nullable AbstractWidget p_461098_, Screen p_460910_) {
         return p_461098_ == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(p_460895_, p_461260_)), p_460910_)
            : new OptionsList.Entry(
               List.of(new OptionsList.OptionInstanceWidget(p_460895_, p_461260_), new OptionsList.OptionInstanceWidget(p_461098_)), p_460910_
            );
      }

      public static OptionsList.Entry small(Options p_460845_, OptionInstance<?> p_460973_, @Nullable OptionInstance<?> p_460953_, OptionsSubScreen p_461070_) {
         AbstractWidget abstractwidget = p_460973_.createButton(p_460845_);
         return p_460953_ == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(abstractwidget, p_460973_)), p_461070_)
            : new OptionsList.Entry(
               List.of(
                  new OptionsList.OptionInstanceWidget(abstractwidget, p_460973_),
                  new OptionsList.OptionInstanceWidget(p_460953_.createButton(p_460845_), p_460953_)
               ),
               p_461070_
            );
      }

      @Override
      public void renderContent(GuiGraphics p_440603_, int p_440287_, int p_439324_, boolean p_439478_, float p_440210_) {
         int i = 0;
         int j = this.screen.width / 2 - 155;

         for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : this.children) {
            optionslist$optioninstancewidget.widget().setPosition(j + i, this.getContentY());
            optionslist$optioninstancewidget.widget().render(p_440603_, p_440287_, p_439324_, p_440210_);
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
      public AbstractWidget findOption(OptionInstance<?> p_460883_) {
         for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : this.children) {
            if (optionslist$optioninstancewidget.optionInstance == p_460883_) {
               return optionslist$optioninstancewidget.widget();
            }
         }

         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected static class HeaderEntry extends OptionsList.AbstractEntry {
      private final Screen screen;
      private final int paddingTop;
      private final StringWidget widget;

      protected HeaderEntry(Screen p_455974_, Component p_455529_, int p_455767_) {
         this.screen = p_455974_;
         this.paddingTop = p_455767_;
         this.widget = new StringWidget(p_455529_, p_455974_.getFont());
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return List.of(this.widget);
      }

      @Override
      public void renderContent(GuiGraphics p_455586_, int p_456280_, int p_454936_, boolean p_455018_, float p_455521_) {
         this.widget.setPosition(this.screen.width / 2 - 155, this.getContentY() + this.paddingTop);
         this.widget.render(p_455586_, p_456280_, p_454936_, p_455521_);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return List.of(this.widget);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record OptionInstanceWidget(AbstractWidget widget, @Nullable OptionInstance<?> optionInstance) {
      public OptionInstanceWidget(AbstractWidget p_460993_) {
         this(p_460993_, null);
      }
   }
}
