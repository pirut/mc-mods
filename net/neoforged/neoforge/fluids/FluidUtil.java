package net.neoforged.neoforge.fluids;

import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.BlockWrapper;
import net.neoforged.neoforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public class FluidUtil {
   private FluidUtil() {
   }

   public static boolean interactWithFluidHandler(Player player, InteractionHand hand, Level level, BlockPos pos, @Nullable Direction side) {
      Preconditions.checkNotNull(level);
      Preconditions.checkNotNull(pos);
      return getFluidHandler(level, pos, side).map(handler -> interactWithFluidHandler(player, hand, handler)).orElse(false);
   }

   public static boolean interactWithFluidHandler(Player player, InteractionHand hand, IFluidHandler handler) {
      Preconditions.checkNotNull(player);
      Preconditions.checkNotNull(hand);
      Preconditions.checkNotNull(handler);
      ItemStack heldItem = player.getItemInHand(hand);
      if (heldItem.isEmpty()) {
         return false;
      } else {
         IItemHandler playerInventory = player.getCapability(Capabilities.ItemHandler.ENTITY);
         Objects.requireNonNull(playerInventory, "Player item handler is null");
         FluidActionResult fluidActionResult = tryFillContainerAndStow(heldItem, handler, playerInventory, Integer.MAX_VALUE, player, true);
         if (!fluidActionResult.isSuccess()) {
            fluidActionResult = tryEmptyContainerAndStow(heldItem, handler, playerInventory, Integer.MAX_VALUE, player, true);
         }

         if (fluidActionResult.isSuccess()) {
            player.setItemInHand(hand, fluidActionResult.getResult());
            return true;
         } else {
            return false;
         }
      }
   }

   public static FluidActionResult tryFillContainer(ItemStack container, IFluidHandler fluidSource, int maxAmount, @Nullable Player player, boolean doFill) {
      ItemStack containerCopy = container.copyWithCount(1);
      return getFluidHandler(containerCopy).map(containerFluidHandler -> {
         FluidStack simulatedTransfer = tryFluidTransfer(containerFluidHandler, fluidSource, maxAmount, false);
         if (!simulatedTransfer.isEmpty()) {
            if (doFill) {
               tryFluidTransfer(containerFluidHandler, fluidSource, maxAmount, true);
               if (player != null) {
                  SoundEvent soundevent = simulatedTransfer.getFluidType().getSound(simulatedTransfer, SoundActions.BUCKET_FILL);
                  if (soundevent != null) {
                     player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                  }
               }
            } else {
               containerFluidHandler.fill(simulatedTransfer, IFluidHandler.FluidAction.EXECUTE);
            }

            ItemStack resultContainer = containerFluidHandler.getContainer();
            return new FluidActionResult(resultContainer);
         } else {
            return FluidActionResult.FAILURE;
         }
      }).orElse(FluidActionResult.FAILURE);
   }

   public static FluidActionResult tryEmptyContainer(
      ItemStack container, IFluidHandler fluidDestination, int maxAmount, @Nullable Player player, boolean doDrain
   ) {
      ItemStack containerCopy = container.copyWithCount(1);
      return getFluidHandler(containerCopy).map(containerFluidHandler -> {
         FluidStack transfer = tryFluidTransfer(fluidDestination, containerFluidHandler, maxAmount, doDrain);
         if (transfer.isEmpty()) {
            return FluidActionResult.FAILURE;
         } else {
            if (!doDrain) {
               containerFluidHandler.drain(transfer, IFluidHandler.FluidAction.EXECUTE);
            }

            if (doDrain && player != null) {
               SoundEvent soundevent = transfer.getFluidType().getSound(transfer, SoundActions.BUCKET_EMPTY);
               if (soundevent != null) {
                  player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
               }
            }

            ItemStack resultContainer = containerFluidHandler.getContainer();
            return new FluidActionResult(resultContainer);
         }
      }).orElse(FluidActionResult.FAILURE);
   }

   public static FluidActionResult tryFillContainerAndStow(
      ItemStack container, IFluidHandler fluidSource, IItemHandler inventory, int maxAmount, @Nullable Player player, boolean doFill
   ) {
      if (container.isEmpty()) {
         return FluidActionResult.FAILURE;
      } else {
         if (player != null && player.getAbilities().instabuild) {
            FluidActionResult filledReal = tryFillContainer(container, fluidSource, maxAmount, player, doFill);
            if (filledReal.isSuccess()) {
               return new FluidActionResult(container);
            }
         } else if (container.getCount() == 1) {
            FluidActionResult filledReal = tryFillContainer(container, fluidSource, maxAmount, player, doFill);
            if (filledReal.isSuccess()) {
               return filledReal;
            }
         } else {
            FluidActionResult filledSimulated = tryFillContainer(container, fluidSource, maxAmount, player, false);
            if (filledSimulated.isSuccess()) {
               ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, filledSimulated.getResult(), true);
               if (remainder.isEmpty() || player != null) {
                  FluidActionResult filledReal = tryFillContainer(container, fluidSource, maxAmount, player, doFill);
                  remainder = ItemHandlerHelper.insertItemStacked(inventory, filledReal.getResult(), !doFill);
                  if (!remainder.isEmpty() && player != null && doFill) {
                     ItemHandlerHelper.giveItemToPlayer(player, remainder);
                  }

                  ItemStack containerCopy = container.copy();
                  containerCopy.shrink(1);
                  return new FluidActionResult(containerCopy);
               }
            }
         }

         return FluidActionResult.FAILURE;
      }
   }

   public static FluidActionResult tryEmptyContainerAndStow(
      ItemStack container, IFluidHandler fluidDestination, IItemHandler inventory, int maxAmount, @Nullable Player player, boolean doDrain
   ) {
      if (container.isEmpty()) {
         return FluidActionResult.FAILURE;
      } else {
         if (player != null && player.getAbilities().instabuild) {
            FluidActionResult emptiedReal = tryEmptyContainer(container, fluidDestination, maxAmount, player, doDrain);
            if (emptiedReal.isSuccess()) {
               return new FluidActionResult(container);
            }
         } else if (container.getCount() == 1) {
            FluidActionResult emptiedReal = tryEmptyContainer(container, fluidDestination, maxAmount, player, doDrain);
            if (emptiedReal.isSuccess()) {
               return emptiedReal;
            }
         } else {
            FluidActionResult emptiedSimulated = tryEmptyContainer(container, fluidDestination, maxAmount, player, false);
            if (emptiedSimulated.isSuccess()) {
               ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, emptiedSimulated.getResult(), true);
               if (remainder.isEmpty() || player != null) {
                  FluidActionResult emptiedReal = tryEmptyContainer(container, fluidDestination, maxAmount, player, doDrain);
                  remainder = ItemHandlerHelper.insertItemStacked(inventory, emptiedReal.getResult(), !doDrain);
                  if (!remainder.isEmpty() && player != null && doDrain) {
                     ItemHandlerHelper.giveItemToPlayer(player, remainder);
                  }

                  ItemStack containerCopy = container.copy();
                  containerCopy.shrink(1);
                  return new FluidActionResult(containerCopy);
               }
            }
         }

         return FluidActionResult.FAILURE;
      }
   }

   public static FluidStack tryFluidTransfer(IFluidHandler fluidDestination, IFluidHandler fluidSource, int maxAmount, boolean doTransfer) {
      FluidStack drainable = fluidSource.drain(maxAmount, IFluidHandler.FluidAction.SIMULATE);
      return !drainable.isEmpty() ? tryFluidTransfer_Internal(fluidDestination, fluidSource, drainable, doTransfer) : FluidStack.EMPTY;
   }

   public static FluidStack tryFluidTransfer(IFluidHandler fluidDestination, IFluidHandler fluidSource, FluidStack resource, boolean doTransfer) {
      FluidStack drainable = fluidSource.drain(resource, IFluidHandler.FluidAction.SIMULATE);
      return !drainable.isEmpty() && FluidStack.isSameFluidSameComponents(resource, drainable)
         ? tryFluidTransfer_Internal(fluidDestination, fluidSource, drainable, doTransfer)
         : FluidStack.EMPTY;
   }

   private static FluidStack tryFluidTransfer_Internal(IFluidHandler fluidDestination, IFluidHandler fluidSource, FluidStack drainable, boolean doTransfer) {
      int fillableAmount = fluidDestination.fill(drainable, IFluidHandler.FluidAction.SIMULATE);
      if (fillableAmount > 0) {
         drainable.setAmount(fillableAmount);
         if (!doTransfer) {
            return drainable;
         }

         FluidStack drained = fluidSource.drain(drainable, IFluidHandler.FluidAction.EXECUTE);
         if (!drained.isEmpty()) {
            drained.setAmount(fluidDestination.fill(drained, IFluidHandler.FluidAction.EXECUTE));
            return drained;
         }
      }

      return FluidStack.EMPTY;
   }

   public static Optional<IFluidHandlerItem> getFluidHandler(ItemStack itemStack) {
      return Optional.ofNullable(itemStack.getCapability(Capabilities.FluidHandler.ITEM));
   }

   public static Optional<FluidStack> getFluidContained(ItemStack container) {
      if (!container.isEmpty()) {
         container = container.copyWithCount(1);
         Optional<FluidStack> fluidContained = getFluidHandler(container).map(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE));
         if (fluidContained.isPresent() && !fluidContained.get().isEmpty()) {
            return fluidContained;
         }
      }

      return Optional.empty();
   }

   public static Optional<IFluidHandler> getFluidHandler(Level level, BlockPos blockPos, @Nullable Direction side) {
      return Optional.ofNullable(level.getCapability(Capabilities.FluidHandler.BLOCK, blockPos, side));
   }

   public static FluidActionResult tryPickUpFluid(ItemStack emptyContainer, @Nullable Player playerIn, Level level, BlockPos pos, Direction side) {
      if (!emptyContainer.isEmpty() && level != null && pos != null) {
         BlockState state = level.getBlockState(pos);
         Block block = state.getBlock();
         IFluidHandler targetFluidHandler;
         if (block instanceof BucketPickup) {
            targetFluidHandler = new BucketPickupHandlerWrapper(playerIn, (BucketPickup)block, level, pos);
         } else {
            Optional<IFluidHandler> fluidHandler = getFluidHandler(level, pos, side);
            if (!fluidHandler.isPresent()) {
               return FluidActionResult.FAILURE;
            }

            targetFluidHandler = fluidHandler.get();
         }

         return tryFillContainer(emptyContainer, targetFluidHandler, Integer.MAX_VALUE, playerIn, true);
      } else {
         return FluidActionResult.FAILURE;
      }
   }

   public static FluidActionResult tryPlaceFluid(
      @Nullable Player player, Level level, InteractionHand hand, BlockPos pos, ItemStack container, FluidStack resource
   ) {
      ItemStack containerCopy = container.copyWithCount(1);
      return getFluidHandler(containerCopy)
         .filter(handler -> tryPlaceFluid(player, level, hand, pos, handler, resource))
         .map(IFluidHandlerItem::getContainer)
         .map(FluidActionResult::new)
         .orElse(FluidActionResult.FAILURE);
   }

   public static boolean tryPlaceFluid(@Nullable Player player, Level level, InteractionHand hand, BlockPos pos, IFluidHandler fluidSource, FluidStack resource) {
      if (level != null && pos != null) {
         Fluid fluid = resource.getFluid();
         if (fluid == Fluids.EMPTY || !fluid.getFluidType().canBePlacedInLevel(level, pos, resource)) {
            return false;
         } else if (fluidSource.drain(resource, IFluidHandler.FluidAction.SIMULATE).isEmpty()) {
            return false;
         } else {
            BlockPlaceContext context = new BlockPlaceContext(
               level, player, hand, player == null ? ItemStack.EMPTY : player.getItemInHand(hand), new BlockHitResult(Vec3.ZERO, Direction.UP, pos, false)
            );
            BlockState destBlockState = level.getBlockState(pos);
            boolean isDestNonSolid = !destBlockState.isSolid();
            boolean isDestReplaceable = destBlockState.canBeReplaced(context);
            boolean canDestContainFluid = destBlockState.getBlock() instanceof LiquidBlockContainer
               && ((LiquidBlockContainer)destBlockState.getBlock()).canPlaceLiquid(player, level, pos, destBlockState, fluid);
            if (!level.isEmptyBlock(pos) && !isDestNonSolid && !isDestReplaceable && !canDestContainFluid) {
               return false;
            } else {
               if (fluid.getFluidType().isVaporizedOnPlacement(level, pos, resource)) {
                  FluidStack result = fluidSource.drain(resource, IFluidHandler.FluidAction.EXECUTE);
                  if (!result.isEmpty()) {
                     result.getFluidType().onVaporize(player, level, pos, result);
                     return true;
                  }
               } else {
                  IFluidHandler handler;
                  if (canDestContainFluid) {
                     handler = new BlockWrapper.LiquidContainerBlockWrapper((LiquidBlockContainer)destBlockState.getBlock(), level, pos);
                  } else {
                     handler = getFluidBlockHandler(fluid, level, pos);
                  }

                  FluidStack result = tryFluidTransfer(handler, fluidSource, resource, true);
                  if (!result.isEmpty()) {
                     SoundEvent soundevent = resource.getFluidType().getSound(resource, SoundActions.BUCKET_EMPTY);
                     if (soundevent != null) {
                        level.playSound(player, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                     }

                     return true;
                  }
               }

               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static IFluidHandler getFluidBlockHandler(Fluid fluid, Level level, BlockPos pos) {
      BlockState state = fluid.getFluidType().getBlockForFluidState(level, pos, fluid.defaultFluidState());
      return new BlockWrapper(state, level, pos);
   }

   public static void destroyBlockOnFluidPlacement(Level level, BlockPos pos) {
      if (!level.isClientSide) {
         BlockState destBlockState = level.getBlockState(pos);
         boolean isDestNonSolid = !destBlockState.isSolid();
         boolean isDestReplaceable = false;
         if ((isDestNonSolid || isDestReplaceable) && !destBlockState.liquid()) {
            level.destroyBlock(pos, true);
         }
      }
   }

   public static ItemStack getFilledBucket(FluidStack fluidStack) {
      if (fluidStack.getComponents().isEmpty()) {
         if (fluidStack.is(Fluids.WATER)) {
            return new ItemStack(Items.WATER_BUCKET);
         }

         if (fluidStack.is(Fluids.LAVA)) {
            return new ItemStack(Items.LAVA_BUCKET);
         }
      }

      return fluidStack.getFluidType().getBucket(fluidStack);
   }
}
