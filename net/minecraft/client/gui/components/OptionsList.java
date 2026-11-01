package net.minecraft.client.gui.components;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OptionsList extends ContainerObjectSelectionList<OptionsList.Entry> {
   private static final int BIG_BUTTON_WIDTH = 310;
   private static final int DEFAULT_ITEM_HEIGHT = 25;
   private final OptionsSubScreen screen;

   public OptionsList(Minecraft p_94465_, int p_94466_, OptionsSubScreen p_345374_) {
      super(p_94465_, p_94466_, p_345374_.layout.getContentHeight(), p_345374_.layout.getHeaderHeight(), 25);
      this.centerListVertically = false;
      this.screen = p_345374_;
   }

   public void addBig(OptionInstance<?> p_232529_) {
      this.addEntry(OptionsList.OptionEntry.big(this.minecraft.options, p_232529_, this.screen));
   }

   public void addSmall(OptionInstance<?>... p_232534_) {
      for (int $$1 = 0; $$1 < p_232534_.length; $$1 += 2) {
         OptionInstance<?> $$2 = $$1 < p_232534_.length - 1 ? p_232534_[$$1 + 1] : null;
         this.addEntry(OptionsList.OptionEntry.small(this.minecraft.options, p_232534_[$$1], $$2, this.screen));
      }
   }

   public void addSmall(List<AbstractWidget> p_333735_) {
      for (int $$1 = 0; $$1 < p_333735_.size(); $$1 += 2) {
         this.addSmall(p_333735_.get($$1), $$1 < p_333735_.size() - 1 ? p_333735_.get($$1 + 1) : null);
      }
   }

   public void addSmall(AbstractWidget p_333843_, @Nullable AbstractWidget p_334089_) {
      this.addEntry(OptionsList.Entry.small(p_333843_, p_334089_, this.screen));
   }

   @Override
   public int getRowWidth() {
      return 310;
   }

   @Nullable
   public AbstractWidget findOption(OptionInstance<?> p_232536_) {
      for (OptionsList.Entry $$1 : this.children()) {
         if ($$1 instanceof OptionsList.OptionEntry $$2) {
            AbstractWidget $$3 = $$2.options.get(p_232536_);
            if ($$3 != null) {
               return $$3;
            }
         }
      }

      return null;
   }

   public void applyUnsavedChanges() {
      for (OptionsList.Entry $$0 : this.children()) {
         if ($$0 instanceof OptionsList.OptionEntry $$1) {
            for (AbstractWidget $$2 : $$1.options.values()) {
               if ($$2 instanceof OptionInstance.OptionInstanceSliderButton<?> $$3) {
                  $$3.applyUnsavedValue();
               }
            }
         }
      }
   }

   public Optional<GuiEventListener> getMouseOver(double p_94481_, double p_94482_) {
      for (OptionsList.Entry $$2 : this.children()) {
         for (GuiEventListener $$3 : $$2.children()) {
            if ($$3.isMouseOver(p_94481_, p_94482_)) {
               return Optional.of($$3);
            }
         }
      }

      return Optional.empty();
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Entry extends ContainerObjectSelectionList.Entry<OptionsList.Entry> {
      private final List<AbstractWidget> children;
      private final Screen screen;
      private static final int X_OFFSET = 160;

      Entry(List<AbstractWidget> p_333982_, Screen p_333707_) {
         this.children = ImmutableList.copyOf(p_333982_);
         this.screen = p_333707_;
      }

      public static OptionsList.Entry big(List<AbstractWidget> p_333829_, Screen p_334023_) {
         return new OptionsList.Entry(p_333829_, p_334023_);
      }

      public static OptionsList.Entry small(AbstractWidget p_333824_, @Nullable AbstractWidget p_333990_, Screen p_334077_) {
         return p_333990_ == null
            ? new OptionsList.Entry(ImmutableList.of(p_333824_), p_334077_)
            : new OptionsList.Entry(ImmutableList.of(p_333824_, p_333990_), p_334077_);
      }

      @Override
      public void render(
         GuiGraphics p_281311_,
         int p_94497_,
         int p_94498_,
         int p_94499_,
         int p_94500_,
         int p_94501_,
         int p_94502_,
         int p_94503_,
         boolean p_94504_,
         float p_94505_
      ) {
         int $$10 = 0;
         int $$11 = this.screen.width / 2 - 155;

         for (AbstractWidget $$12 : this.children) {
            $$12.setPosition($$11 + $$10, p_94498_);
            $$12.render(p_281311_, p_94502_, p_94503_, p_94505_);
            $$10 += 160;
         }
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

   @OnlyIn(Dist.CLIENT)
   protected static class OptionEntry extends OptionsList.Entry {
      final Map<OptionInstance<?>, AbstractWidget> options;

      private OptionEntry(Map<OptionInstance<?>, AbstractWidget> p_333718_, OptionsSubScreen p_345547_) {
         super(ImmutableList.copyOf(p_333718_.values()), p_345547_);
         this.options = p_333718_;
      }

      public static OptionsList.OptionEntry big(Options p_333804_, OptionInstance<?> p_333884_, OptionsSubScreen p_346169_) {
         return new OptionsList.OptionEntry(ImmutableMap.of(p_333884_, p_333884_.createButton(p_333804_, 0, 0, 310)), p_346169_);
      }

      public static OptionsList.OptionEntry small(
         Options p_333928_, OptionInstance<?> p_333848_, @Nullable OptionInstance<?> p_333717_, OptionsSubScreen p_344761_
      ) {
         AbstractWidget $$4 = p_333848_.createButton(p_333928_);
         return p_333717_ == null
            ? new OptionsList.OptionEntry(ImmutableMap.of(p_333848_, $$4), p_344761_)
            : new OptionsList.OptionEntry(ImmutableMap.of(p_333848_, $$4, p_333717_, p_333717_.createButton(p_333928_)), p_344761_);
      }
   }
}
