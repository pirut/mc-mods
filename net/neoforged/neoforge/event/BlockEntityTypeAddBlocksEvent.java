package net.neoforged.neoforge.event;

import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.mixins.BlockEntityTypeAccessor;
import org.jspecify.annotations.Nullable;

public class BlockEntityTypeAddBlocksEvent extends Event implements IModBusEvent {
   private final Function<BlockEntityType<?>, ? extends Class<?>> memoizedCommonSuperClass = Util.memoize(
      blockEntityType -> this.getCommonSuperClassForExistingValidBlocks(blockEntityType.getValidBlocks())
   );

   public void modify(BlockEntityType<?> blockEntityType, Block... blocksToAdd) {
      if (blocksToAdd.length != 0) {
         Set<Block> currentValidBlocks = new HashSet<>(blockEntityType.getValidBlocks());

         for (Block block : blocksToAdd) {
            this.addValidBlock(block, (Class<?>)this.memoizedCommonSuperClass.apply(blockEntityType), currentValidBlocks);
         }

         ((BlockEntityTypeAccessor)blockEntityType).neoforge$setValidBlocks(currentValidBlocks);
      }
   }

   public void modify(ResourceKey<BlockEntityType<?>> blockEntityTypeKey, Block... blocksToAdd) {
      BuiltInRegistries.BLOCK_ENTITY_TYPE
         .getOptional(blockEntityTypeKey)
         .ifPresent(blockEntityType -> this.modify((BlockEntityType<?>)blockEntityType, blocksToAdd));
   }

   public void modify(BiPredicate<ResourceKey<BlockEntityType<?>>, BlockEntityType<?>> blockEntityTypeToMatch, Block... blocksToAdd) {
      for (Entry<ResourceKey<BlockEntityType<?>>, BlockEntityType<?>> blockEntityTypeEntry : BuiltInRegistries.BLOCK_ENTITY_TYPE.entrySet()) {
         if (blockEntityTypeToMatch.test(blockEntityTypeEntry.getKey(), blockEntityTypeEntry.getValue())) {
            this.modify(blockEntityTypeEntry.getValue(), blocksToAdd);
         }
      }
   }

   private void addValidBlock(Block block, @Nullable Class<?> baseClass, Set<Block> currentValidBlocks) {
      if (baseClass != null && !baseClass.isAssignableFrom(block.getClass())) {
         throw new IllegalArgumentException("Given block " + block + " does not derive from existing valid block's common superclass of " + baseClass);
      } else {
         currentValidBlocks.add(block);
      }
   }

   @Nullable
   private Class<?> getCommonSuperClassForExistingValidBlocks(Set<Block> validBlocks) {
      Class<?> calculatedBaseClass = null;

      for (Block existingBlock : validBlocks) {
         if (calculatedBaseClass != null) {
            calculatedBaseClass = findClosestCommonSuper(calculatedBaseClass, existingBlock.getClass());
         } else {
            calculatedBaseClass = existingBlock.getClass();
         }
      }

      return calculatedBaseClass;
   }

   private static Class<?> findClosestCommonSuper(Class<?> superClass, Class<?> childClass) {
      while (!superClass.isAssignableFrom(childClass)) {
         superClass = superClass.getSuperclass();
      }

      return superClass;
   }
}
