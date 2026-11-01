package net.minecraft.advancements.critereon;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public interface SingleComponentItemPredicate<T> extends ItemSubPredicate {
   @Override
   default boolean matches(ItemStack p_333958_) {
      T $$1 = p_333958_.get(this.componentType());
      return $$1 != null && this.matches(p_333958_, $$1);
   }

   DataComponentType<T> componentType();

   boolean matches(ItemStack var1, T var2);
}
