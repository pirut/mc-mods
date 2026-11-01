package net.neoforged.neoforge.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;

public final class Capabilities {
   private static ResourceLocation create(String path) {
      return ResourceLocation.fromNamespaceAndPath("neoforge", path);
   }

   private Capabilities() {
   }

   public static final class EnergyStorage {
      public static final BlockCapability<IEnergyStorage, Direction> BLOCK = BlockCapability.createSided(Capabilities.create("energy"), IEnergyStorage.class);
      public static final EntityCapability<IEnergyStorage, Direction> ENTITY = EntityCapability.createSided(Capabilities.create("energy"), IEnergyStorage.class);
      public static final ItemCapability<IEnergyStorage, Void> ITEM = ItemCapability.createVoid(Capabilities.create("energy"), IEnergyStorage.class);

      private EnergyStorage() {
      }
   }

   public static final class FluidHandler {
      public static final BlockCapability<IFluidHandler, Direction> BLOCK = BlockCapability.createSided(
         Capabilities.create("fluid_handler"), IFluidHandler.class
      );
      public static final EntityCapability<IFluidHandler, Direction> ENTITY = EntityCapability.createSided(
         Capabilities.create("fluid_handler"), IFluidHandler.class
      );
      public static final ItemCapability<IFluidHandlerItem, Void> ITEM = ItemCapability.createVoid(
         Capabilities.create("fluid_handler"), IFluidHandlerItem.class
      );

      private FluidHandler() {
      }
   }

   public static final class ItemHandler {
      public static final BlockCapability<IItemHandler, Direction> BLOCK = BlockCapability.createSided(Capabilities.create("item_handler"), IItemHandler.class);
      public static final EntityCapability<IItemHandler, Void> ENTITY = EntityCapability.createVoid(Capabilities.create("item_handler"), IItemHandler.class);
      public static final EntityCapability<IItemHandler, Direction> ENTITY_AUTOMATION = EntityCapability.createSided(
         Capabilities.create("item_handler_automation"), IItemHandler.class
      );
      public static final ItemCapability<IItemHandler, Void> ITEM = ItemCapability.createVoid(Capabilities.create("item_handler"), IItemHandler.class);

      private ItemHandler() {
      }
   }
}
