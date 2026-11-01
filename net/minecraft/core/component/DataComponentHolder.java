package net.minecraft.core.component;

import java.util.stream.Stream;
import net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension;
import org.jspecify.annotations.Nullable;

public interface DataComponentHolder extends DataComponentGetter, IDataComponentHolderExtension {
   DataComponentMap getComponents();

   @Nullable
   @Override
   default <T> T get(DataComponentType<? extends T> p_331625_) {
      return this.getComponents().get(p_331625_);
   }

   default <T> Stream<T> getAllOfType(Class<? extends T> p_366401_) {
      return this.getComponents()
         .stream()
         .map(TypedDataComponent::value)
         .filter(p_366823_ -> p_366401_.isAssignableFrom(p_366823_.getClass()))
         .map(p_366685_ -> (T)p_366685_);
   }

   @Override
   default <T> T getOrDefault(DataComponentType<? extends T> p_331643_, T p_330718_) {
      return this.getComponents().getOrDefault(p_331643_, p_330718_);
   }

   @Override
   default boolean has(DataComponentType<?> p_330779_) {
      return this.getComponents().has(p_330779_);
   }
}
