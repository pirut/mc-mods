package net.neoforged.neoforge.common.extensions;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jetbrains.annotations.Nullable;

public interface IDataComponentHolderExtension {
   private DataComponentHolder self() {
      return (DataComponentHolder)this;
   }

   @Nullable
   default <T> T get(Supplier<? extends DataComponentType<? extends T>> type) {
      return this.self().get((DataComponentType<? extends T>)type.get());
   }

   default <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> type, T defaultValue) {
      return this.self().getOrDefault((DataComponentType<? extends T>)type.get(), defaultValue);
   }

   default boolean has(Supplier<? extends DataComponentType<?>> type) {
      return this.self().has((DataComponentType<?>)type.get());
   }

   default <T extends TooltipProvider> void addToTooltip(DataComponentType<T> type, Item.TooltipContext context, Consumer<Component> adder, TooltipFlag flag) {
      T value = (T)this.self().get(type);
      if (value != null) {
         value.addToTooltip(context, adder, flag);
      }
   }

   default <T extends TooltipProvider> void addToTooltip(
      Supplier<? extends DataComponentType<T>> type, Item.TooltipContext context, Consumer<Component> adder, TooltipFlag flag
   ) {
      this.addToTooltip((DataComponentType<T>)type.get(), context, adder, flag);
   }
}
