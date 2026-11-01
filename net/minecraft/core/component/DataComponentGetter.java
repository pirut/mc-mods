package net.minecraft.core.component;

import org.jspecify.annotations.Nullable;

public interface DataComponentGetter {
   @Nullable
   <T> T get(DataComponentType<? extends T> component);

   default <T> T getOrDefault(DataComponentType<? extends T> component, T defaultValue) {
      T object = this.get(component);
      return object != null ? object : defaultValue;
   }

   @Nullable
   default <T> TypedDataComponent<T> getTyped(DataComponentType<T> component) {
      T object = this.get(component);
      return object != null ? new TypedDataComponent<>(component, object) : null;
   }
}
