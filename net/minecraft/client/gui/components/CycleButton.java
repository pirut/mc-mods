package net.minecraft.client.gui.components;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CycleButton<T> extends AbstractButton implements ResettableOptionWidget {
   public static final BooleanSupplier DEFAULT_ALT_LIST_SELECTOR = () -> Minecraft.getInstance().hasAltDown();
   private static final List<Boolean> BOOLEAN_OPTIONS = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);
   private final Supplier<T> defaultValueSupplier;
   private final Component name;
   private int index;
   private T value;
   private final CycleButton.ValueListSupplier<T> values;
   private final Function<T, Component> valueStringifier;
   private final Function<CycleButton<T>, MutableComponent> narrationProvider;
   private final CycleButton.OnValueChange<T> onValueChange;
   private final CycleButton.DisplayState displayState;
   private final OptionInstance.TooltipSupplier<T> tooltipSupplier;
   private final CycleButton.SpriteSupplier<T> spriteSupplier;

   CycleButton(
      int x,
      int y,
      int width,
      int height,
      Component message,
      Component name,
      int index,
      T value,
      Supplier<T> defaultValueSupplier,
      CycleButton.ValueListSupplier<T> values,
      Function<T, Component> valueStringifier,
      Function<CycleButton<T>, MutableComponent> narrationProvider,
      CycleButton.OnValueChange<T> onValueChange,
      OptionInstance.TooltipSupplier<T> tooltipSupplier,
      CycleButton.DisplayState displayState,
      CycleButton.SpriteSupplier<T> spriteSupplier
   ) {
      super(x, y, width, height, message);
      this.name = name;
      this.index = index;
      this.defaultValueSupplier = defaultValueSupplier;
      this.value = value;
      this.values = values;
      this.valueStringifier = valueStringifier;
      this.narrationProvider = narrationProvider;
      this.onValueChange = onValueChange;
      this.displayState = displayState;
      this.tooltipSupplier = tooltipSupplier;
      this.spriteSupplier = spriteSupplier;
      this.updateTooltip();
   }

   @Override
   protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      Identifier identifier = this.spriteSupplier.apply(this, this.getValue());
      if (identifier != null) {
         guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.getWidth(), this.getHeight());
      } else {
         this.renderDefaultSprite(guiGraphics);
      }

      if (this.displayState != CycleButton.DisplayState.HIDE) {
         this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
      }
   }

   private void updateTooltip() {
      this.setTooltip(this.tooltipSupplier.apply(this.value));
   }

   @Override
   public void onPress(InputWithModifiers input) {
      if (input.hasShiftDown()) {
         this.cycleValue(-1);
      } else {
         this.cycleValue(1);
      }
   }

   private void cycleValue(int delta) {
      List<T> list = this.values.getSelectedList();
      this.index = Mth.positiveModulo(this.index + delta, list.size());
      T object = list.get(this.index);
      this.updateValue(object);
      this.onValueChange.onValueChange(this, object);
   }

   private T getCycledValue(int delta) {
      List<T> list = this.values.getSelectedList();
      return list.get(Mth.positiveModulo(this.index + delta, list.size()));
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY > 0.0) {
         this.cycleValue(-1);
      } else if (scrollY < 0.0) {
         this.cycleValue(1);
      }

      return true;
   }

   public void setValue(T value) {
      List<T> list = this.values.getSelectedList();
      int i = list.indexOf(value);
      if (i != -1) {
         this.index = i;
      }

      this.updateValue(value);
   }

   @Override
   public void resetValue() {
      this.setValue(this.defaultValueSupplier.get());
   }

   private void updateValue(T value) {
      Component component = this.createLabelForValue(value);
      this.setMessage(component);
      this.value = value;
      this.updateTooltip();
   }

   private Component createLabelForValue(T value) {
      return (Component)(this.displayState == CycleButton.DisplayState.VALUE ? this.valueStringifier.apply(value) : this.createFullName(value));
   }

   private MutableComponent createFullName(T value) {
      return CommonComponents.optionNameValue(this.name, this.valueStringifier.apply(value));
   }

   public T getValue() {
      return this.value;
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return this.narrationProvider.apply(this);
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         T object = this.getCycledValue(1);
         Component component = this.createLabelForValue(object);
         if (this.isFocused()) {
            narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.focused", component));
         } else {
            narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.hovered", component));
         }
      }
   }

   public MutableComponent createDefaultNarrationMessage() {
      return wrapDefaultNarrationMessage((Component)(this.displayState == CycleButton.DisplayState.VALUE ? this.createFullName(this.value) : this.getMessage()));
   }

   public static <T> CycleButton.Builder<T> builder(Function<T, Component> valueStringifier, Supplier<T> defaultValueSupplier) {
      return new CycleButton.Builder<>(valueStringifier, defaultValueSupplier);
   }

   public static <T> CycleButton.Builder<T> builder(Function<T, Component> valueStringifier, T defaultValue) {
      return new CycleButton.Builder<>(valueStringifier, () -> defaultValue);
   }

   public static CycleButton.Builder<Boolean> booleanBuilder(Component trueText, Component falseText, boolean defaultValue) {
      return new CycleButton.Builder<>(boolean_ -> boolean_ == Boolean.TRUE ? trueText : falseText, () -> defaultValue).withValues(BOOLEAN_OPTIONS);
   }

   public static CycleButton.Builder<Boolean> onOffBuilder(boolean defaultValue) {
      return new CycleButton.Builder<>(boolean_ -> boolean_ == Boolean.TRUE ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, () -> defaultValue)
         .withValues(BOOLEAN_OPTIONS);
   }

   @Environment(EnvType.CLIENT)
   public static class Builder<T> {
      private final Supplier<T> defaultValueSupplier;
      private final Function<T, Component> valueStringifier;
      private OptionInstance.TooltipSupplier<T> tooltipSupplier = object -> null;
      private CycleButton.SpriteSupplier<T> spriteSupplier = (cycleButton, object) -> null;
      private Function<CycleButton<T>, MutableComponent> narrationProvider = CycleButton::createDefaultNarrationMessage;
      private CycleButton.ValueListSupplier<T> values = CycleButton.ValueListSupplier.create(ImmutableList.of());
      private CycleButton.DisplayState displayState = CycleButton.DisplayState.NAME_AND_VALUE;

      public Builder(Function<T, Component> valueStringifier, Supplier<T> defaultValueSupplier) {
         this.valueStringifier = valueStringifier;
         this.defaultValueSupplier = defaultValueSupplier;
      }

      public CycleButton.Builder<T> withValues(Collection<T> values) {
         return this.withValues(CycleButton.ValueListSupplier.create(values));
      }

      @SafeVarargs
      public final CycleButton.Builder<T> withValues(T... values) {
         return this.withValues(ImmutableList.copyOf(values));
      }

      public CycleButton.Builder<T> withValues(List<T> defaultList, List<T> selectedList) {
         return this.withValues(CycleButton.ValueListSupplier.create(CycleButton.DEFAULT_ALT_LIST_SELECTOR, defaultList, selectedList));
      }

      public CycleButton.Builder<T> withValues(BooleanSupplier altListSelector, List<T> defaultList, List<T> selectedList) {
         return this.withValues(CycleButton.ValueListSupplier.create(altListSelector, defaultList, selectedList));
      }

      public CycleButton.Builder<T> withValues(CycleButton.ValueListSupplier<T> values) {
         this.values = values;
         return this;
      }

      public CycleButton.Builder<T> withTooltip(OptionInstance.TooltipSupplier<T> tooltipSupplier) {
         this.tooltipSupplier = tooltipSupplier;
         return this;
      }

      public CycleButton.Builder<T> withCustomNarration(Function<CycleButton<T>, MutableComponent> narrationProvider) {
         this.narrationProvider = narrationProvider;
         return this;
      }

      public CycleButton.Builder<T> withSprite(CycleButton.SpriteSupplier<T> spriteSupplier) {
         this.spriteSupplier = spriteSupplier;
         return this;
      }

      public CycleButton.Builder<T> displayState(CycleButton.DisplayState displayState) {
         this.displayState = displayState;
         return this;
      }

      public CycleButton.Builder<T> displayOnlyValue() {
         return this.displayState(CycleButton.DisplayState.VALUE);
      }

      public CycleButton<T> create(Component message, CycleButton.OnValueChange<T> onValueChange) {
         return this.create(0, 0, 150, 20, message, onValueChange);
      }

      public CycleButton<T> create(int x, int y, int width, int height, Component name) {
         return this.create(x, y, width, height, name, (cycleButton, object) -> {});
      }

      public CycleButton<T> create(int x, int y, int width, int height, Component name, CycleButton.OnValueChange<T> onValueChange) {
         List<T> list = this.values.getDefaultList();
         if (list.isEmpty()) {
            throw new IllegalStateException("No values for cycle button");
         } else {
            T object = this.defaultValueSupplier.get();
            int i = list.indexOf(object);
            Component component = this.valueStringifier.apply(object);
            Component component2 = (Component)(this.displayState == CycleButton.DisplayState.VALUE
               ? component
               : CommonComponents.optionNameValue(name, component));
            return new CycleButton<>(
               x,
               y,
               width,
               height,
               component2,
               name,
               i,
               object,
               this.defaultValueSupplier,
               this.values,
               this.valueStringifier,
               this.narrationProvider,
               onValueChange,
               this.tooltipSupplier,
               this.displayState,
               this.spriteSupplier
            );
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum DisplayState {
      NAME_AND_VALUE,
      VALUE,
      HIDE;
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface OnValueChange<T> {
      void onValueChange(CycleButton<T> cycleButton, T object);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface SpriteSupplier<T> {
      @Nullable
      Identifier apply(CycleButton<T> cycleButton, T object);
   }

   @Environment(EnvType.CLIENT)
   public interface ValueListSupplier<T> {
      List<T> getSelectedList();

      List<T> getDefaultList();

      static <T> CycleButton.ValueListSupplier<T> create(Collection<T> values) {
         final List<T> list = ImmutableList.copyOf(values);
         return new CycleButton.ValueListSupplier<T>() {
            @Override
            public List<T> getSelectedList() {
               return list;
            }

            @Override
            public List<T> getDefaultList() {
               return list;
            }
         };
      }

      static <T> CycleButton.ValueListSupplier<T> create(BooleanSupplier altListSelector, List<T> defaultList, List<T> selectedList) {
         final List<T> list = ImmutableList.copyOf(defaultList);
         final List<T> list2 = ImmutableList.copyOf(selectedList);
         return new CycleButton.ValueListSupplier<T>() {
            @Override
            public List<T> getSelectedList() {
               return altListSelector.getAsBoolean() ? list2 : list;
            }

            @Override
            public List<T> getDefaultList() {
               return list;
            }
         };
      }
   }
}
