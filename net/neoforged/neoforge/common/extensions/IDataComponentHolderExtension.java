package net.neoforged.neoforge.common.extensions;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;

public interface IDataComponentHolderExtension {
   private DataComponentHolder self() {
      return (DataComponentHolder)this;
   }

   default <T extends TooltipProvider> void addToTooltip(
      DataComponentType<T> type, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag
   ) {
      T value = (T)this.self().get(type);
      if (value != null && display.shows(type)) {
         value.addToTooltip(context, adder, flag, this.self());
      }
   }

   default <T extends TooltipProvider> void addToTooltip(
      Supplier<? extends DataComponentType<T>> type, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag
   ) {
      this.addToTooltip((DataComponentType<T>)type.get(), context, display, adder, flag);
   }
}
