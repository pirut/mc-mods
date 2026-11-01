package net.neoforged.neoforge.common;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.Nullable;

public interface MutableDataComponentHolder extends DataComponentHolder {
   @Nullable
   <T> T set(DataComponentType<? super T> var1, @Nullable T var2);

   @Nullable
   default <T> T set(Supplier<? extends DataComponentType<? super T>> componentType, @Nullable T value) {
      return this.set((DataComponentType<? super T>)componentType.get(), value);
   }

   @Nullable
   default <T, U> T update(DataComponentType<T> componentType, T value, U updateContext, BiFunction<T, U, T> updater) {
      return this.set(componentType, updater.apply(this.getOrDefault(componentType, value), updateContext));
   }

   @Nullable
   default <T, U> T update(Supplier<? extends DataComponentType<T>> componentType, T value, U updateContext, BiFunction<T, U, T> updater) {
      return this.update((DataComponentType<T>)componentType.get(), value, updateContext, updater);
   }

   @Nullable
   default <T> T update(DataComponentType<T> componentType, T value, UnaryOperator<T> updater) {
      return this.set(componentType, updater.apply(this.getOrDefault(componentType, value)));
   }

   @Nullable
   default <T> T update(Supplier<? extends DataComponentType<T>> componentType, T value, UnaryOperator<T> updater) {
      return this.update((DataComponentType<T>)componentType.get(), value, updater);
   }

   @Nullable
   <T> T remove(DataComponentType<? extends T> var1);

   @Nullable
   default <T> T remove(Supplier<? extends DataComponentType<? extends T>> componentType) {
      return this.remove((DataComponentType<? extends T>)componentType.get());
   }

   default void copyFrom(DataComponentHolder src, DataComponentType<?>... componentTypes) {
      for (DataComponentType<?> componentType : componentTypes) {
         this.copyFrom(componentType, src);
      }
   }

   default void copyFrom(DataComponentHolder src, Supplier<? extends DataComponentType<?>>... componentTypes) {
      for (Supplier<? extends DataComponentType<?>> componentType : componentTypes) {
         this.copyFrom((DataComponentType<?>)componentType.get(), src);
      }
   }

   void applyComponents(DataComponentPatch var1);

   void applyComponents(DataComponentMap var1);

   private <T> void copyFrom(DataComponentType<T> componentType, DataComponentHolder src) {
      this.set(componentType, src.get(componentType));
   }
}
