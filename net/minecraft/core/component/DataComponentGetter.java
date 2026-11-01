package net.minecraft.core.component;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface DataComponentGetter {
   @Nullable
   <T> T get(DataComponentType<? extends T> var1);

   default <T> T getOrDefault(DataComponentType<? extends T> p_397089_, T p_397491_) {
      T t = this.get(p_397089_);
      return t != null ? t : p_397491_;
   }

   @Nullable
   default <T> TypedDataComponent<T> getTyped(DataComponentType<T> p_397753_) {
      T t = this.get(p_397753_);
      return t != null ? new TypedDataComponent<>(p_397753_, t) : null;
   }

   @Nullable
   default <T> T get(Supplier<? extends DataComponentType<? extends T>> componentType) {
      return this.get((DataComponentType<? extends T>)componentType.get());
   }

   default <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> componentType, T value) {
      return this.getOrDefault((DataComponentType<? extends T>)componentType.get(), value);
   }

   default <T> boolean has(Supplier<? extends DataComponentType<? extends T>> componentType) {
      return this.<T>get(componentType) != null;
   }

   default boolean has(DataComponentType<?> componentType) {
      return this.get(componentType) != null;
   }
}
