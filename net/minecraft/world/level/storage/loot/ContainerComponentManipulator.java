package net.minecraft.world.level.storage.loot;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public interface ContainerComponentManipulator<T> {
   DataComponentType<T> type();

   T empty();

   T setContents(T var1, Stream<ItemStack> var2);

   Stream<ItemStack> getContents(T var1);

   default void setContents(ItemStack p_340824_, T p_341046_, Stream<ItemStack> p_341372_) {
      T $$3 = p_340824_.getOrDefault(this.type(), p_341046_);
      T $$4 = this.setContents($$3, p_341372_);
      p_340824_.set(this.type(), $$4);
   }

   default void setContents(ItemStack p_340943_, Stream<ItemStack> p_340843_) {
      this.setContents(p_340943_, this.empty(), p_340843_);
   }

   default void modifyItems(ItemStack p_340919_, UnaryOperator<ItemStack> p_341245_) {
      T $$2 = p_340919_.get(this.type());
      if ($$2 != null) {
         UnaryOperator<ItemStack> $$3 = p_344668_ -> {
            if (p_344668_.isEmpty()) {
               return p_344668_;
            } else {
               ItemStack $$2x = p_341245_.apply(p_344668_);
               $$2x.limitSize($$2x.getMaxStackSize());
               return $$2x;
            }
         };
         this.setContents(p_340919_, this.getContents($$2).map($$3));
      }
   }
}
