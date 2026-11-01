package net.neoforged.neoforge.items;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class VanillaInventoryCodeHooks {
   @Nullable
   public static Boolean extractHook(Level level, Hopper dest) {
      return getSourceItemHandler(level, dest)
         .map(
            itemHandlerResult -> {
               IItemHandler handler = (IItemHandler)itemHandlerResult.getKey();

               for (int i = 0; i < handler.getSlots(); i++) {
                  ItemStack extractItem = handler.extractItem(i, 1, true);
                  if (!extractItem.isEmpty()) {
                     for (int j = 0; j < dest.getContainerSize(); j++) {
                        ItemStack destStack = dest.getItem(j);
                        if (dest.canPlaceItem(j, extractItem)
                           && (
                              destStack.isEmpty()
                                 || destStack.getCount() < destStack.getMaxStackSize()
                                    && destStack.getCount() < dest.getMaxStackSize()
                                    && ItemStack.isSameItemSameComponents(extractItem, destStack)
                           )) {
                           extractItem = handler.extractItem(i, 1, false);
                           if (destStack.isEmpty()) {
                              dest.setItem(j, extractItem);
                           } else {
                              destStack.grow(1);
                              dest.setItem(j, destStack);
                           }

                           dest.setChanged();
                           return true;
                        }
                     }
                  }
               }

               return false;
            }
         )
         .orElse(null);
   }

   public static boolean dropperInsertHook(Level level, BlockPos pos, DispenserBlockEntity dropper, int slot, ItemStack stack) {
      Direction facing = level.getBlockState(pos).getValue(DropperBlock.FACING);
      return getAttachedItemHandler(level, pos, facing).map(destinationResult -> {
         IItemHandler itemHandler = (IItemHandler)destinationResult.getKey();
         Object destination = destinationResult.getValue();
         ItemStack dispensedStack = stack.copy().split(1);
         ItemStack remainder = putStackInInventoryAllSlots(dropper, destination, itemHandler, dispensedStack);
         if (remainder.isEmpty()) {
            remainder = stack.copy();
            remainder.shrink(1);
         } else {
            remainder = stack.copy();
         }

         dropper.setItem(slot, remainder);
         return false;
      }).orElse(true);
   }

   public static boolean insertHook(HopperBlockEntity hopper) {
      Direction hopperFacing = hopper.getBlockState().getValue(HopperBlock.FACING);
      return getAttachedItemHandler(hopper.getLevel(), hopper.getBlockPos(), hopperFacing).map(destinationResult -> {
         IItemHandler itemHandler = (IItemHandler)destinationResult.getKey();
         Object destination = destinationResult.getValue();
         if (isFull(itemHandler)) {
            return false;
         } else {
            for (int i = 0; i < hopper.getContainerSize(); i++) {
               if (!hopper.getItem(i).isEmpty()) {
                  ItemStack originalSlotContents = hopper.getItem(i).copy();
                  ItemStack insertStack = hopper.removeItem(i, 1);
                  ItemStack remainder = putStackInInventoryAllSlots(hopper, destination, itemHandler, insertStack);
                  if (remainder.isEmpty()) {
                     return true;
                  }

                  hopper.setItem(i, originalSlotContents);
               }
            }

            return false;
         }
      }).orElse(false);
   }

   public static ItemStack insertCrafterOutput(Level level, BlockPos pos, CrafterBlockEntity crafterBlockEntity, ItemStack stack) {
      FrontAndTop frontAndTop = level.getBlockState(pos).getValue(BlockStateProperties.ORIENTATION);
      return getAttachedItemHandler(level, pos, frontAndTop.front()).map(destinationResult -> {
         IItemHandler itemHandler = (IItemHandler)destinationResult.getKey();
         Object destination = destinationResult.getValue();
         return putStackInInventoryAllSlots(crafterBlockEntity, destination, itemHandler, stack);
      }).orElse(stack);
   }

   private static ItemStack putStackInInventoryAllSlots(BlockEntity source, Object destination, IItemHandler destInventory, ItemStack stack) {
      for (int slot = 0; slot < destInventory.getSlots() && !stack.isEmpty(); slot++) {
         stack = insertStack(source, destination, destInventory, stack, slot);
      }

      return stack;
   }

   private static ItemStack insertStack(BlockEntity source, Object destination, IItemHandler destInventory, ItemStack stack, int slot) {
      ItemStack itemstack = destInventory.getStackInSlot(slot);
      if (destInventory.insertItem(slot, stack, true).isEmpty()) {
         boolean insertedItem = false;
         boolean inventoryWasEmpty = isEmpty(destInventory);
         if (itemstack.isEmpty()) {
            destInventory.insertItem(slot, stack, false);
            stack = ItemStack.EMPTY;
            insertedItem = true;
         } else if (ItemStack.isSameItemSameComponents(itemstack, stack)) {
            int originalSize = stack.getCount();
            stack = destInventory.insertItem(slot, stack, false);
            insertedItem = originalSize < stack.getCount();
         }

         if (insertedItem && inventoryWasEmpty && destination instanceof HopperBlockEntity destinationHopper && !destinationHopper.isOnCustomCooldown()) {
            int k = 0;
            if (source instanceof HopperBlockEntity && destinationHopper.getLastUpdateTime() >= ((HopperBlockEntity)source).getLastUpdateTime()) {
               k = 1;
            }

            destinationHopper.setCooldown(8 - k);
         }
      }

      return stack;
   }

   private static boolean isFull(IItemHandler itemHandler) {
      for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
         ItemStack stackInSlot = itemHandler.getStackInSlot(slot);
         if (stackInSlot.isEmpty() || stackInSlot.getCount() < itemHandler.getSlotLimit(slot)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isEmpty(IItemHandler itemHandler) {
      for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
         ItemStack stackInSlot = itemHandler.getStackInSlot(slot);
         if (stackInSlot.getCount() > 0) {
            return false;
         }
      }

      return true;
   }

   private static Optional<Pair<IItemHandler, Object>> getAttachedItemHandler(Level level, BlockPos pos, Direction direction) {
      return getItemHandlerAt(
         level,
         pos.getX() + direction.getStepX() + 0.5,
         pos.getY() + direction.getStepY() + 0.5,
         pos.getZ() + direction.getStepZ() + 0.5,
         direction.getOpposite()
      );
   }

   private static Optional<Pair<IItemHandler, Object>> getSourceItemHandler(Level level, Hopper hopper) {
      return getItemHandlerAt(level, hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ(), Direction.DOWN);
   }

   private static Optional<Pair<IItemHandler, Object>> getItemHandlerAt(Level worldIn, double x, double y, double z, Direction side) {
      BlockPos blockpos = BlockPos.containing(x, y, z);
      BlockState state = worldIn.getBlockState(blockpos);
      BlockEntity blockEntity = state.hasBlockEntity() ? worldIn.getBlockEntity(blockpos) : null;
      IItemHandler blockCap = worldIn.getCapability(Capabilities.ItemHandler.BLOCK, blockpos, state, blockEntity, side);
      if (blockCap != null) {
         return Optional.of(ImmutablePair.of(blockCap, blockEntity));
      } else {
         List<Entity> list = worldIn.getEntities(
            (Entity)null, new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), EntitySelector.ENTITY_STILL_ALIVE
         );
         if (!list.isEmpty()) {
            Collections.shuffle(list);

            for (Entity entity : list) {
               IItemHandler entityCap = entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side);
               if (entityCap != null) {
                  return Optional.of(ImmutablePair.of(entityCap, entity));
               }
            }
         }

         return Optional.empty();
      }
   }
}
