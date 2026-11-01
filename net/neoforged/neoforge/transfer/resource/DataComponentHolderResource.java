package net.neoforged.neoforge.transfer.resource;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;

public interface DataComponentHolderResource<T> extends RegisteredResource<T>, DataComponentHolder {
   boolean isComponentsPatchEmpty();

   DataComponentHolderResource<T> withMergedPatch(DataComponentPatch var1);

   <D> DataComponentHolderResource<T> with(DataComponentType<D> var1, D var2);

   DataComponentHolderResource<T> without(DataComponentType<?> var1);

   DataComponentPatch getComponentsPatch();

   default <D> DataComponentHolderResource<T> with(Supplier<? extends DataComponentType<D>> type, D data) {
      return this.with((DataComponentType<D>)type.get(), data);
   }

   default DataComponentHolderResource<T> without(Supplier<? extends DataComponentType<?>> type) {
      return this.without((DataComponentType<?>)type.get());
   }
}
