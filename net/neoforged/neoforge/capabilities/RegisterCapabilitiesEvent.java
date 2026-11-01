package net.neoforged.neoforge.capabilities;

import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class RegisterCapabilitiesEvent extends Event implements IModBusEvent {
   RegisterCapabilitiesEvent() {
   }

   public <T, C> void registerBlock(BlockCapability<T, C> capability, IBlockCapabilityProvider<T, C> provider, Block... blocks) {
      Objects.requireNonNull(provider);
      if (blocks.length == 0) {
         throw new IllegalArgumentException("Must register at least one block");
      } else {
         for (Block block : blocks) {
            Objects.requireNonNull(block);
            capability.providers.computeIfAbsent(block, b -> new ArrayList<>()).add(provider);
         }
      }
   }

   public <T, C, BE extends BlockEntity> void registerBlockEntity(
      BlockCapability<T, C> capability, BlockEntityType<BE> blockEntityType, ICapabilityProvider<? super BE, C, T> provider
   ) {
      Objects.requireNonNull(provider);
      IBlockCapabilityProvider<T, C> adaptedProvider = (level, pos, state, blockEntity, context) -> blockEntity != null
            && blockEntity.getType() == blockEntityType
         ? provider.getCapability((BE)blockEntity, context)
         : null;

      for (Block block : blockEntityType.getValidBlocks()) {
         Objects.requireNonNull(block);
         capability.providers.computeIfAbsent(block, b -> new ArrayList<>()).add(adaptedProvider);
      }
   }

   public boolean isBlockRegistered(BlockCapability<?, ?> capability, Block block) {
      Objects.requireNonNull(block);
      return capability.providers.containsKey(block);
   }

   public void setProxyable(BlockCapability<?, ?> capability) {
      capability.setProxyable(true);
   }

   public void setNonProxyable(BlockCapability<?, ?> capability) {
      capability.setProxyable(false);
   }

   public <T, C, E extends Entity> void registerEntity(
      EntityCapability<T, C> capability, EntityType<E> entityType, ICapabilityProvider<? super E, C, T> provider
   ) {
      Objects.requireNonNull(provider);
      capability.providers.computeIfAbsent(entityType, et -> new ArrayList<>()).add(provider);
   }

   public boolean isEntityRegistered(EntityCapability<?, ?> capability, EntityType<?> entityType) {
      Objects.requireNonNull(entityType);
      return capability.providers.containsKey(entityType);
   }

   public <T, C> void registerItem(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider, ItemLike... items) {
      Objects.requireNonNull(provider);
      if (items.length == 0) {
         throw new IllegalArgumentException("Must register at least one item");
      } else {
         for (ItemLike itemLike : items) {
            Item item = Objects.requireNonNull(itemLike.asItem());
            capability.providers.computeIfAbsent(item, i -> new ArrayList<>()).add(provider);
         }
      }
   }

   public boolean isItemRegistered(ItemCapability<?, ?> capability, Item item) {
      Objects.requireNonNull(item);
      return capability.providers.containsKey(item);
   }
}
