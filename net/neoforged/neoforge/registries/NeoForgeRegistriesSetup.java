package net.neoforged.neoforge.registries;

import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentSync;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class NeoForgeRegistriesSetup {
   private static boolean setup = false;
   private static final Set<Registry<?>> VANILLA_SYNC_REGISTRIES = Set.of(
      BuiltInRegistries.SOUND_EVENT,
      BuiltInRegistries.MOB_EFFECT,
      BuiltInRegistries.BLOCK,
      BuiltInRegistries.ENTITY_TYPE,
      BuiltInRegistries.ITEM,
      BuiltInRegistries.FLUID,
      BuiltInRegistries.PARTICLE_TYPE,
      BuiltInRegistries.BLOCK_ENTITY_TYPE,
      BuiltInRegistries.MENU,
      BuiltInRegistries.COMMAND_ARGUMENT_TYPE,
      BuiltInRegistries.STAT_TYPE,
      BuiltInRegistries.VILLAGER_TYPE,
      BuiltInRegistries.VILLAGER_PROFESSION,
      BuiltInRegistries.CAT_VARIANT,
      BuiltInRegistries.FROG_VARIANT,
      BuiltInRegistries.DATA_COMPONENT_TYPE,
      BuiltInRegistries.RECIPE_SERIALIZER,
      BuiltInRegistries.ATTRIBUTE,
      BuiltInRegistries.POTION,
      BuiltInRegistries.NUMBER_FORMAT_TYPE,
      BuiltInRegistries.CUSTOM_STAT,
      BuiltInRegistries.POSITION_SOURCE_TYPE,
      BuiltInRegistries.ARMOR_MATERIAL,
      BuiltInRegistries.MAP_DECORATION_TYPE
   );

   public static synchronized void setup(IEventBus modEventBus) {
      if (setup) {
         throw new IllegalStateException("Setup has already been called!");
      } else {
         setup = true;
         modEventBus.addListener(NeoForgeRegistriesSetup::registerRegistries);
         modEventBus.addListener(NeoForgeRegistriesSetup::modifyRegistries);
      }
   }

   private static void registerRegistries(NewRegistryEvent event) {
      event.register(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS);
      event.register(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS);
      event.register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS);
      event.register(NeoForgeRegistries.STRUCTURE_MODIFIER_SERIALIZERS);
      event.register(NeoForgeRegistries.FLUID_TYPES);
      event.register(NeoForgeRegistries.HOLDER_SET_TYPES);
      event.register(NeoForgeRegistries.INGREDIENT_TYPES);
      event.register(NeoForgeRegistries.FLUID_INGREDIENT_TYPES);
      event.register(NeoForgeRegistries.CONDITION_SERIALIZERS);
      event.register(NeoForgeRegistries.ATTACHMENT_TYPES);
      event.register(AttachmentSync.SYNCED_ATTACHMENT_TYPES);
   }

   private static void modifyRegistries(ModifyRegistriesEvent event) {
      for (Registry<?> registry : VANILLA_SYNC_REGISTRIES) {
         ((BaseMappedRegistry)registry).setSync(true);
      }

      BuiltInRegistries.BLOCK.addCallback(NeoForgeRegistryCallbacks.BlockCallbacks.INSTANCE);
      BuiltInRegistries.ITEM.addCallback(NeoForgeRegistryCallbacks.ItemCallbacks.INSTANCE);
      BuiltInRegistries.ATTRIBUTE.addCallback(NeoForgeRegistryCallbacks.AttributeCallbacks.INSTANCE);
      BuiltInRegistries.POINT_OF_INTEREST_TYPE.addCallback(NeoForgeRegistryCallbacks.PoiTypeCallbacks.INSTANCE);
      NeoForgeRegistries.ATTACHMENT_TYPES.addCallback(AttachmentSync.ATTACHMENT_TYPE_ADD_CALLBACK);
   }
}
