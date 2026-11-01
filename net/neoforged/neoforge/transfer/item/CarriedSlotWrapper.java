package net.neoforged.neoforge.transfer.item;

import com.google.common.collect.MapMaker;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;

public final class CarriedSlotWrapper extends ItemStackResourceHandler {
   private static final Map<AbstractContainerMenu, CarriedSlotWrapper> wrappers = new MapMaker().weakKeys().weakValues().makeMap();
   private final AbstractContainerMenu menu;

   public static ResourceHandler<ItemResource> of(AbstractContainerMenu menu) {
      return wrappers.computeIfAbsent(menu, CarriedSlotWrapper::new);
   }

   private CarriedSlotWrapper(AbstractContainerMenu menu) {
      this.menu = menu;
   }

   @Override
   protected ItemStack getStack() {
      return this.menu.getCarried();
   }

   @Override
   protected void setStack(ItemStack stack) {
      this.menu.setCarried(stack);
   }

   @Override
   public String toString() {
      return "CarriedSlotWrapper[" + this.menu + "/" + BuiltInRegistries.MENU.getId(this.menu.getType()) + "]";
   }
}
