package net.neoforged.neoforge.internal;

import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.capabilities.CapabilityHooks;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.common.world.poi.PoiTypeExtender;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.registries.RegistryManager;

public class RegistrationEvents {
   private static boolean canModifyComponents;

   static void init() {
      CauldronFluidContent.init();
      CapabilityHooks.init();
      ForcedChunkManager.init();
      RegistryManager.initDataMaps();
      modifyComponents();
      PoiTypeExtender.extendPoiTypes();
   }

   public static void modifyComponents() {
      canModifyComponents = true;
      ModLoader.postEvent(new ModifyDefaultComponentsEvent());
      canModifyComponents = false;
   }

   public static boolean canModifyComponents() {
      return canModifyComponents;
   }
}
