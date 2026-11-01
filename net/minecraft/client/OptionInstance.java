package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ResettableOptionWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public final class OptionInstance<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final OptionInstance.Enum<Boolean> BOOLEAN_VALUES = new OptionInstance.Enum<>(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL);
   public static final OptionInstance.CaptionBasedToString<Boolean> BOOLEAN_TO_STRING = (component, boolean_) -> boolean_
      ? CommonComponents.OPTION_ON
      : CommonComponents.OPTION_OFF;
   private final OptionInstance.TooltipSupplier<T> tooltip;
   final Function<T, Component> toString;
   private final OptionInstance.ValueSet<T> values;
   private final Codec<T> codec;
   private final T initialValue;
   private final Consumer<T> onValueUpdate;
   final Component caption;
   private T value;

   public static OptionInstance<Boolean> createBoolean(String key, boolean initialValue, Consumer<Boolean> onValueUpdate) {
      return createBoolean(key, noTooltip(), initialValue, onValueUpdate);
   }

   public static OptionInstance<Boolean> createBoolean(String key, boolean initialValue) {
      return createBoolean(key, noTooltip(), initialValue, boolean_ -> {});
   }

   public static OptionInstance<Boolean> createBoolean(String caption, OptionInstance.TooltipSupplier<Boolean> tooltip, boolean initialValue) {
      return createBoolean(caption, tooltip, initialValue, boolean_ -> {});
   }

   public static OptionInstance<Boolean> createBoolean(
      String caption, OptionInstance.TooltipSupplier<Boolean> tooltip, boolean initialValue, Consumer<Boolean> onValueUpdate
   ) {
      return createBoolean(caption, tooltip, BOOLEAN_TO_STRING, initialValue, onValueUpdate);
   }

   public static OptionInstance<Boolean> createBoolean(
      String caption,
      OptionInstance.TooltipSupplier<Boolean> tooltip,
      OptionInstance.CaptionBasedToString<Boolean> valueStringifier,
      boolean initialValue,
      Consumer<Boolean> onValueUpdate
   ) {
      return new OptionInstance<>(caption, tooltip, valueStringifier, BOOLEAN_VALUES, initialValue, onValueUpdate);
   }

   public OptionInstance(
      String caption,
      OptionInstance.TooltipSupplier<T> tooltip,
      OptionInstance.CaptionBasedToString<T> valueStringifier,
      OptionInstance.ValueSet<T> values,
      T initialValue,
      Consumer<T> onValueUpdate
   ) {
      this(caption, tooltip, valueStringifier, values, values.codec(), initialValue, onValueUpdate);
   }

   public OptionInstance(
      String caption,
      OptionInstance.TooltipSupplier<T> tooltip,
      OptionInstance.CaptionBasedToString<T> valueStringifier,
      OptionInstance.ValueSet<T> values,
      Codec<T> codec,
      T initialValue,
      Consumer<T> onValueUpdate
   ) {
      this.caption = Component.translatable(caption);
      this.tooltip = tooltip;
      this.toString = object -> valueStringifier.toString(this.caption, object);
      this.values = values;
      this.codec = codec;
      this.initialValue = initialValue;
      this.onValueUpdate = onValueUpdate;
      this.value = this.initialValue;
   }

   public static <T> OptionInstance.TooltipSupplier<T> noTooltip() {
      return object -> null;
   }

   public static <T> OptionInstance.TooltipSupplier<T> cachedConstantTooltip(Component message) {
      return object -> Tooltip.create(message);
   }

   public AbstractWidget createButton(Options options) {
      return this.createButton(options, 0, 0, 150);
   }

   public AbstractWidget createButton(Options options, int x, int y, int width) {
      return this.createButton(options, x, y, width, object -> {});
   }

   public AbstractWidget createButton(Options options, int x, int y, int width, Consumer<T> onValueChanged) {
      return this.values.createButton(this.tooltip, options, x, y, width, onValueChanged).apply(this);
   }

   public T get() {
      return this.value;
   }

   public Codec<T> codec() {
      return this.codec;
   }

   @Override
   public String toString() {
      return this.caption.getString();
   }

   public void set(T value) {
      T object = this.values.validateValue(value).orElseGet(() -> {
         LOGGER.error("Illegal option value {} for {}", value, this.caption.getString());
         return this.initialValue;
      });
      if (!Minecraft.getInstance().isRunning()) {
         this.value = object;
      } else {
         if (!Objects.equals(this.value, object)) {
            this.value = object;
            this.onValueUpdate.accept(this.value);
         }
      }
   }

   public OptionInstance.ValueSet<T> values() {
      return this.values;
   }

   @Environment(EnvType.CLIENT)
   public record AltEnum<T>(
      List<T> values, List<T> altValues, BooleanSupplier altCondition, OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter, Codec<T> codec
   ) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.altCondition, this.values, this.altValues);
      }

      @Override
      public Optional<T> validateValue(T value) {
         return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(value) ? Optional.of(value) : Optional.empty();
      }
   }

   @Environment(EnvType.CLIENT)
   public interface CaptionBasedToString<T> {
      Component toString(Component component, T object);
   }

   @Environment(EnvType.CLIENT)
   public record ClampingLazyMaxIntRange(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive)
      implements OptionInstance.IntRangeBase,
      OptionInstance.SliderableOrCyclableValueSet<Integer> {
      public Optional<Integer> validateValue(Integer integer) {
         return Optional.of(Mth.clamp(integer, this.minInclusive(), this.maxInclusive()));
      }

      @Override
      public int maxInclusive() {
         return this.maxSupplier.getAsInt();
      }

      @Override
      public Codec<Integer> codec() {
         return Codec.INT
            .validate(
               integer -> {
                  int i = this.encodableMaxInclusive + 1;
                  return integer.compareTo(this.minInclusive) >= 0 && integer.compareTo(i) <= 0
                     ? DataResult.success(integer)
                     : DataResult.error(() -> "Value " + integer + " outside of range [" + this.minInclusive + ":" + i + "]", integer);
               }
            );
      }

      @Override
      public boolean createCycleButton() {
         return true;
      }

      @Override
      public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
      }
   }

   @Environment(EnvType.CLIENT)
   interface CycleableValueSet<T> extends OptionInstance.ValueSet<T> {
      CycleButton.ValueListSupplier<T> valueListSupplier();

      default OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
         return OptionInstance::set;
      }

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         OptionInstance.TooltipSupplier<T> tooltipSupplier, Options options, int x, int y, int width, Consumer<T> onValueChanged
      ) {
         return optionInstance -> CycleButton.builder(optionInstance.toString, optionInstance::get)
            .withValues(this.valueListSupplier())
            .withTooltip(tooltipSupplier)
            .create(x, y, width, 20, optionInstance.caption, (cycleButton, object) -> {
               this.valueSetter().set(optionInstance, object);
               options.save();
               onValueChanged.accept(object);
            });
      }

      @Environment(EnvType.CLIENT)
      public interface ValueSetter<T> {
         void set(OptionInstance<T> optionInstance, T object);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Enum<T>(List<T> values, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public Optional<T> validateValue(T value) {
         return this.values.contains(value) ? Optional.of(value) : Optional.empty();
      }

      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.values);
      }
   }

   @Environment(EnvType.CLIENT)
   public record IntRange(int minInclusive, int maxInclusive, boolean applyValueImmediately) implements OptionInstance.IntRangeBase {
      public IntRange(int minInclusive, int maxInclusive) {
         this(minInclusive, maxInclusive, true);
      }

      public Optional<Integer> validateValue(Integer integer) {
         return integer.compareTo(this.minInclusive()) >= 0 && integer.compareTo(this.maxInclusive()) <= 0 ? Optional.of(integer) : Optional.empty();
      }

      @Override
      public Codec<Integer> codec() {
         return Codec.intRange(this.minInclusive, this.maxInclusive + 1);
      }
   }

   @Environment(EnvType.CLIENT)
   interface IntRangeBase extends OptionInstance.SliderableValueSet<Integer> {
      int minInclusive();

      int maxInclusive();

      default Optional<Integer> next(Integer integer) {
         return Optional.of(integer + 1);
      }

      default Optional<Integer> previous(Integer integer) {
         return Optional.of(integer - 1);
      }

      default double toSliderValue(Integer integer) {
         if (integer == this.minInclusive()) {
            return 0.0;
         } else {
            return integer == this.maxInclusive() ? 1.0 : Mth.map(integer.intValue() + 0.5, (double)this.minInclusive(), this.maxInclusive() + 1.0, 0.0, 1.0);
         }
      }

      default Integer fromSliderValue(double d) {
         if (d >= 1.0) {
            d = 0.99999F;
         }

         return Mth.floor(Mth.map(d, 0.0, 1.0, (double)this.minInclusive(), this.maxInclusive() + 1.0));
      }

      default <R> OptionInstance.SliderableValueSet<R> xmap(IntFunction<? extends R> encoder, ToIntFunction<? super R> decoder, boolean allowStepping) {
         return new OptionInstance.SliderableValueSet<R>() {
            @Override
            public Optional<R> validateValue(R value) {
               return IntRangeBase.this.validateValue(decoder.applyAsInt(value)).map(encoder::apply);
            }

            @Override
            public double toSliderValue(R value) {
               return IntRangeBase.this.toSliderValue(decoder.applyAsInt(value));
            }

            @Override
            public Optional<R> next(R current) {
               if (!allowStepping) {
                  return Optional.empty();
               } else {
                  int i = decoder.applyAsInt(current);
                  return (Optional<R>)Optional.of(encoder.apply(IntRangeBase.this.validateValue(i + 1).orElse(i)));
               }
            }

            @Override
            public Optional<R> previous(R current) {
               if (!allowStepping) {
                  return Optional.empty();
               } else {
                  int i = decoder.applyAsInt(current);
                  return (Optional<R>)Optional.of(encoder.apply(IntRangeBase.this.validateValue(i - 1).orElse(i)));
               }
            }

            @Override
            public R fromSliderValue(double value) {
               return (R)encoder.apply(IntRangeBase.this.fromSliderValue(value));
            }

            @Override
            public Codec<R> codec() {
               return IntRangeBase.this.codec().xmap(encoder::apply, decoder::applyAsInt);
            }
         };
      }
   }

   @Environment(EnvType.CLIENT)
   public record LazyEnum<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public Optional<T> validateValue(T value) {
         return this.validateValue.apply(value);
      }

      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.values.get());
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class OptionInstanceSliderButton<N> extends AbstractOptionSliderButton implements ResettableOptionWidget {
      private final OptionInstance<N> instance;
      private final OptionInstance.SliderableValueSet<N> values;
      private final OptionInstance.TooltipSupplier<N> tooltipSupplier;
      private final Consumer<N> onValueChanged;
      @Nullable
      private Long delayedApplyAt;
      private final boolean applyValueImmediately;

      OptionInstanceSliderButton(
         Options options,
         int x,
         int y,
         int width,
         int height,
         OptionInstance<N> instance,
         OptionInstance.SliderableValueSet<N> values,
         OptionInstance.TooltipSupplier<N> tooltipSupplier,
         Consumer<N> onValueChanged,
         boolean applyValueImmediately
      ) {
         super(options, x, y, width, height, values.toSliderValue(instance.get()));
         this.instance = instance;
         this.values = values;
         this.tooltipSupplier = tooltipSupplier;
         this.onValueChanged = onValueChanged;
         this.applyValueImmediately = applyValueImmediately;
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(this.instance.toString.apply(this.values.fromSliderValue(this.value)));
         this.setTooltip(this.tooltipSupplier.apply(this.values.fromSliderValue(this.value)));
      }

      @Override
      protected void applyValue() {
         if (this.applyValueImmediately) {
            this.applyUnsavedValue();
         } else {
            this.delayedApplyAt = Util.getMillis() + 600L;
         }
      }

      public void applyUnsavedValue() {
         N object = this.values.fromSliderValue(this.value);
         if (!Objects.equals(object, this.instance.get())) {
            this.instance.set(object);
            this.onValueChanged.accept(this.instance.get());
         }
      }

      @Override
      public void resetValue() {
         if (this.value != this.values.toSliderValue(this.instance.get())) {
            this.value = this.values.toSliderValue(this.instance.get());
            this.delayedApplyAt = null;
            this.updateMessage();
         }
      }

      @Override
      public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
         if (this.delayedApplyAt != null && Util.getMillis() >= this.delayedApplyAt) {
            this.delayedApplyAt = null;
            this.applyUnsavedValue();
            this.resetValue();
         }
      }

      @Override
      public void onRelease(MouseButtonEvent event) {
         super.onRelease(event);
         if (this.applyValueImmediately) {
            this.resetValue();
         }
      }

      @Override
      public boolean keyPressed(KeyEvent event) {
         if (event.isSelection()) {
            this.canChangeValue = !this.canChangeValue;
            return true;
         } else {
            if (this.canChangeValue) {
               boolean bl = event.isLeft();
               boolean bl2 = event.isRight();
               if (bl) {
                  Optional<N> optional = this.values.previous(this.values.fromSliderValue(this.value));
                  if (optional.isPresent()) {
                     this.setValue(this.values.toSliderValue(optional.get()));
                     return true;
                  }
               }

               if (bl2) {
                  Optional<N> optional = this.values.next(this.values.fromSliderValue(this.value));
                  if (optional.isPresent()) {
                     this.setValue(this.values.toSliderValue(optional.get()));
                     return true;
                  }
               }

               if (bl || bl2) {
                  float f = bl ? -1.0F : 1.0F;
                  this.setValue(this.value + f / (this.width - 8));
                  return true;
               }
            }

            return false;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public record SliderableEnum<T>(List<T> values, Codec<T> codec) implements OptionInstance.SliderableValueSet<T> {
      @Override
      public double toSliderValue(T value) {
         if (value == this.values.getFirst()) {
            return 0.0;
         } else {
            return value == this.values.getLast() ? 1.0 : Mth.map((double)this.values.indexOf(value), 0.0, (double)(this.values.size() - 1), 0.0, 1.0);
         }
      }

      @Override
      public Optional<T> next(T current) {
         int i = this.values.indexOf(current);
         int j = Mth.clamp(i + 1, 0, this.values.size() - 1);
         return Optional.of(this.values.get(j));
      }

      @Override
      public Optional<T> previous(T current) {
         int i = this.values.indexOf(current);
         int j = Mth.clamp(i - 1, 0, this.values.size() - 1);
         return Optional.of(this.values.get(j));
      }

      @Override
      public T fromSliderValue(double value) {
         if (value >= 1.0) {
            value = 0.99999F;
         }

         int i = Mth.floor(Mth.map(value, 0.0, 1.0, 0.0, (double)this.values.size()));
         return this.values.get(Mth.clamp(i, 0, this.values.size() - 1));
      }

      @Override
      public Optional<T> validateValue(T value) {
         int i = this.values.indexOf(value);
         return i > -1 ? Optional.of(value) : Optional.empty();
      }
   }

   @Environment(EnvType.CLIENT)
   interface SliderableOrCyclableValueSet<T> extends OptionInstance.CycleableValueSet<T>, OptionInstance.SliderableValueSet<T> {
      boolean createCycleButton();

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         OptionInstance.TooltipSupplier<T> tooltipSupplier, Options options, int x, int y, int width, Consumer<T> onValueChanged
      ) {
         return this.createCycleButton()
            ? OptionInstance.CycleableValueSet.super.createButton(tooltipSupplier, options, x, y, width, onValueChanged)
            : OptionInstance.SliderableValueSet.super.createButton(tooltipSupplier, options, x, y, width, onValueChanged);
      }
   }

   @Environment(EnvType.CLIENT)
   interface SliderableValueSet<T> extends OptionInstance.ValueSet<T> {
      double toSliderValue(T value);

      default Optional<T> next(T current) {
         return Optional.empty();
      }

      default Optional<T> previous(T current) {
         return Optional.empty();
      }

      T fromSliderValue(double value);

      default boolean applyValueImmediately() {
         return true;
      }

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         OptionInstance.TooltipSupplier<T> tooltipSupplier, Options options, int x, int y, int width, Consumer<T> onValueChanged
      ) {
         return optionInstance -> new OptionInstance.OptionInstanceSliderButton<>(
            options, x, y, width, 20, optionInstance, this, tooltipSupplier, onValueChanged, this.applyValueImmediately()
         );
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface TooltipSupplier<T> {
      @Nullable
      Tooltip apply(T object);
   }

   @Environment(EnvType.CLIENT)
   public static enum UnitDouble implements OptionInstance.SliderableValueSet<Double> {
      INSTANCE;

      public Optional<Double> validateValue(Double double_) {
         return double_ >= 0.0 && double_ <= 1.0 ? Optional.of(double_) : Optional.empty();
      }

      public double toSliderValue(Double double_) {
         return double_;
      }

      public Double fromSliderValue(double d) {
         return d;
      }

      public <R> OptionInstance.SliderableValueSet<R> xmap(DoubleFunction<? extends R> encoder, ToDoubleFunction<? super R> decoder) {
         return new OptionInstance.SliderableValueSet<R>() {
            @Override
            public Optional<R> validateValue(R value) {
               return UnitDouble.this.validateValue(decoder.applyAsDouble(value)).map(encoder::apply);
            }

            @Override
            public double toSliderValue(R value) {
               return UnitDouble.this.toSliderValue(decoder.applyAsDouble(value));
            }

            @Override
            public R fromSliderValue(double value) {
               return (R)encoder.apply(UnitDouble.this.fromSliderValue(value));
            }

            @Override
            public Codec<R> codec() {
               return UnitDouble.this.codec().xmap(encoder::apply, decoder::applyAsDouble);
            }
         };
      }

      @Override
      public Codec<Double> codec() {
         return Codec.withAlternative(Codec.doubleRange(0.0, 1.0), Codec.BOOL, boolean_ -> boolean_ ? 1.0 : 0.0);
      }
   }

   @Environment(EnvType.CLIENT)
   interface ValueSet<T> {
      Function<OptionInstance<T>, AbstractWidget> createButton(
         OptionInstance.TooltipSupplier<T> tooltipSupplier, Options options, int x, int y, int width, Consumer<T> onValueChanged
      );

      Optional<T> validateValue(T value);

      Codec<T> codec();
   }
}
