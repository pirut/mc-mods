package net.neoforged.neoforge.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class Capabilities {
   private static Identifier create(String path) {
      return Identifier.fromNamespaceAndPath("neoforge", path);
   }

   private Capabilities() {
   }

   public static final class Energy {
      public static final BlockCapability<EnergyHandler, Direction> BLOCK = BlockCapability.createSided(
         Capabilities.create("energy_handler"), EnergyHandler.class
      );
      public static final EntityCapability<EnergyHandler, Direction> ENTITY = EntityCapability.createSided(
         Capabilities.create("energy_handler"), EnergyHandler.class
      );
      public static final ItemCapability<EnergyHandler, ItemAccess> ITEM = ItemCapability.create(
         Capabilities.create("energy_handler"), EnergyHandler.class, ItemAccess.class
      );

      private Energy() {
      }
   }

   public static final class Fluid {
      public static final BlockCapability<ResourceHandler<FluidResource>, Direction> BLOCK = BlockCapability.createSided(
         Capabilities.create("fluid_handler"), ResourceHandler.asClass()
      );
      public static final EntityCapability<ResourceHandler<FluidResource>, Direction> ENTITY = EntityCapability.createSided(
         Capabilities.create("fluid_handler"), ResourceHandler.asClass()
      );
      public static final ItemCapability<ResourceHandler<FluidResource>, ItemAccess> ITEM = ItemCapability.create(
         Capabilities.create("fluid_handler"), ResourceHandler.asClass(), ItemAccess.class
      );

      private Fluid() {
      }
   }

   public static final class Item {
      public static final BlockCapability<ResourceHandler<ItemResource>, Direction> BLOCK = BlockCapability.createSided(
         Capabilities.create("item_handler"), ResourceHandler.asClass()
      );
      public static final EntityCapability<ResourceHandler<ItemResource>, Void> ENTITY = EntityCapability.createVoid(
         Capabilities.create("item_handler"), ResourceHandler.asClass()
      );
      public static final EntityCapability<ResourceHandler<ItemResource>, Direction> ENTITY_AUTOMATION = EntityCapability.createSided(
         Capabilities.create("item_handler_automation"), ResourceHandler.asClass()
      );
      public static final ItemCapability<ResourceHandler<ItemResource>, ItemAccess> ITEM = ItemCapability.create(
         Capabilities.create("item_handler"), ResourceHandler.asClass(), ItemAccess.class
      );

      private Item() {
      }
   }
}
