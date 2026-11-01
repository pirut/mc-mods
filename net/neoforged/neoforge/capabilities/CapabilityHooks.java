package net.neoforged.neoforge.capabilities;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.BucketResourceHandler;
import net.neoforged.neoforge.transfer.item.BundleItemHandler;
import net.neoforged.neoforge.transfer.item.ComposterWrapper;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.LivingEntityEquipmentWrapper;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class CapabilityHooks {
   private static boolean initialized = false;
   static boolean initFinished = false;
   private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, ResourceHandler<ItemResource>> CHEST_COMBINER_HANDLER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, ResourceHandler<ItemResource>>() {
      public ResourceHandler<ItemResource> acceptDouble(ChestBlockEntity chest1, ChestBlockEntity chest2) {
         return new CombinedResourceHandler<>(VanillaContainerWrapper.of(chest1), VanillaContainerWrapper.of(chest2));
      }

      public ResourceHandler<ItemResource> acceptSingle(ChestBlockEntity chest) {
         return VanillaContainerWrapper.of(chest);
      }

      public ResourceHandler<ItemResource> acceptNone() {
         return null;
      }
   };

   public static void init() {
      if (initialized) {
         throw new IllegalArgumentException("CapabilityHooks.init() called twice");
      } else {
         initialized = true;
         RegisterCapabilitiesEvent event = new RegisterCapabilitiesEvent();
         ModLoader.postEventWrapContainerInModOrder(event);
         initFinished = true;
      }
   }

   public static void markProxyableCapabilities(RegisterCapabilitiesEvent event) {
      event.setProxyable(Capabilities.Energy.BLOCK);
      event.setProxyable(Capabilities.Fluid.BLOCK);
      event.setProxyable(Capabilities.Item.BLOCK);
   }

   public static void registerVanillaProviders(RegisterCapabilitiesEvent event) {
      event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, blockEntity, side) -> ComposterWrapper.get(level, pos, side), Blocks.COMPOSTER);
      event.registerBlock(
         Capabilities.Item.BLOCK,
         (level, pos, state, blockEntity, side) -> ((ChestBlock)state.getBlock()).combine(state, level, pos, true).apply(CHEST_COMBINER_HANDLER),
         Blocks.CHEST,
         Blocks.TRAPPED_CHEST,
         Blocks.COPPER_CHEST,
         Blocks.EXPOSED_COPPER_CHEST,
         Blocks.WEATHERED_COPPER_CHEST,
         Blocks.OXIDIZED_COPPER_CHEST,
         Blocks.WAXED_COPPER_CHEST,
         Blocks.WAXED_EXPOSED_COPPER_CHEST,
         Blocks.WAXED_WEATHERED_COPPER_CHEST,
         Blocks.WAXED_OXIDIZED_COPPER_CHEST
      );

      for (BlockEntityType type : List.of(
         BlockEntityType.BLAST_FURNACE, BlockEntityType.BREWING_STAND, BlockEntityType.FURNACE, BlockEntityType.SMOKER, BlockEntityType.SHULKER_BOX
      )) {
         event.registerBlockEntity(Capabilities.Item.BLOCK, type, (x$0, x$1) -> new WorldlyContainerWrapper((WorldlyContainer)x$0, x$1));
      }

      for (BlockEntityType type : List.of(
         BlockEntityType.BARREL,
         BlockEntityType.CHISELED_BOOKSHELF,
         BlockEntityType.DISPENSER,
         BlockEntityType.DROPPER,
         BlockEntityType.HOPPER,
         BlockEntityType.JUKEBOX,
         BlockEntityType.CRAFTER,
         BlockEntityType.DECORATED_POT,
         BlockEntityType.SHELF
      )) {
         event.registerBlockEntity(Capabilities.Item.BLOCK, type, (container, side) -> VanillaContainerWrapper.of((Container)container));
      }

      for (EntityType entityType : List.of(
         EntityType.ACACIA_CHEST_BOAT,
         EntityType.BIRCH_CHEST_BOAT,
         EntityType.CHERRY_CHEST_BOAT,
         EntityType.DARK_OAK_CHEST_BOAT,
         EntityType.JUNGLE_CHEST_BOAT,
         EntityType.MANGROVE_CHEST_BOAT,
         EntityType.OAK_CHEST_BOAT,
         EntityType.SPRUCE_CHEST_BOAT,
         EntityType.BAMBOO_CHEST_RAFT,
         EntityType.PALE_OAK_CHEST_BOAT,
         EntityType.CHEST_MINECART,
         EntityType.HOPPER_MINECART
      )) {
         event.registerEntity(Capabilities.Item.ENTITY, entityType, (entity, ctx) -> VanillaContainerWrapper.of((Container)entity));
         event.registerEntity(Capabilities.Item.ENTITY_AUTOMATION, entityType, (entity, ctx) -> VanillaContainerWrapper.of((Container)entity));
      }

      event.registerEntity(Capabilities.Item.ENTITY, EntityType.PLAYER, (player, ctx) -> PlayerInventoryWrapper.of(player.getInventory()));
      event.registerItem(
         Capabilities.Item.ITEM,
         (stack, access) -> new ItemAccessItemHandler(access, DataComponents.CONTAINER, 27),
         Items.SHULKER_BOX,
         Items.BLACK_SHULKER_BOX,
         Items.BLUE_SHULKER_BOX,
         Items.BROWN_SHULKER_BOX,
         Items.CYAN_SHULKER_BOX,
         Items.GRAY_SHULKER_BOX,
         Items.GREEN_SHULKER_BOX,
         Items.LIGHT_BLUE_SHULKER_BOX,
         Items.LIGHT_GRAY_SHULKER_BOX,
         Items.LIME_SHULKER_BOX,
         Items.MAGENTA_SHULKER_BOX,
         Items.ORANGE_SHULKER_BOX,
         Items.PINK_SHULKER_BOX,
         Items.PURPLE_SHULKER_BOX,
         Items.RED_SHULKER_BOX,
         Items.WHITE_SHULKER_BOX,
         Items.YELLOW_SHULKER_BOX
      );
      event.registerItem(
         Capabilities.Item.ITEM,
         (stack, access) -> new BundleItemHandler(access, DataComponents.BUNDLE_CONTENTS),
         Items.BUNDLE,
         Items.BLACK_BUNDLE,
         Items.BLUE_BUNDLE,
         Items.BROWN_BUNDLE,
         Items.CYAN_BUNDLE,
         Items.GRAY_BUNDLE,
         Items.GREEN_BUNDLE,
         Items.LIGHT_BLUE_BUNDLE,
         Items.LIGHT_GRAY_BUNDLE,
         Items.LIME_BUNDLE,
         Items.MAGENTA_BUNDLE,
         Items.ORANGE_BUNDLE,
         Items.PINK_BUNDLE,
         Items.PURPLE_BUNDLE,
         Items.RED_BUNDLE,
         Items.WHITE_BUNDLE,
         Items.YELLOW_BUNDLE
      );
   }

   public static void registerFallbackVanillaProviders(RegisterCapabilitiesEvent event) {
      for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
         event.registerEntity(Capabilities.Item.ENTITY, entityType, (entity, ctx) -> {
            if (entity instanceof AbstractHorse horse) {
               return VanillaContainerWrapper.of(horse.getInventory());
            } else if (entity instanceof LivingEntity livingEntity) {
               ResourceHandler<ItemResource> handsWrapper = LivingEntityEquipmentWrapper.of(livingEntity, EquipmentSlot.Type.HAND);
               ResourceHandler<ItemResource> armorWrapper = LivingEntityEquipmentWrapper.of(livingEntity, EquipmentSlot.Type.HUMANOID_ARMOR);
               return new CombinedResourceHandler(handsWrapper, armorWrapper);
            } else {
               return null;
            }
         });
      }

      for (Item item : BuiltInRegistries.ITEM) {
         if (item.getClass() == BucketItem.class) {
            event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new BucketResourceHandler(access), item);
         }
      }

      if (NeoForgeMod.MILK.isBound()) {
         event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new BucketResourceHandler(access), Items.MILK_BUCKET);
      }
   }

   public static void invalidateCapsOnChunkLoad(ChunkEvent.Load event) {
      if (event.getLevel() instanceof ServerLevel sl) {
         sl.invalidateCapabilities(((LevelChunk)event.getChunk()).getPos());
      }
   }

   public static void invalidateCapsOnChunkUnload(ChunkEvent.Unload event) {
      if (event.getLevel() instanceof ServerLevel sl) {
         sl.invalidateCapabilities(((LevelChunk)event.getChunk()).getPos());
      }
   }

   public static void cleanCapabilityListenerReferencesOnTick(LevelTickEvent.Post event) {
      if (event.getLevel() instanceof ServerLevel sl) {
         sl.cleanCapabilityListenerReferences();
      }
   }
}
