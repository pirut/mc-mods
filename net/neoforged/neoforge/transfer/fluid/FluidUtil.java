package net.neoforged.neoforge.transfer.fluid;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class FluidUtil {
   private static final Logger LOGGER = LogUtils.getLogger();

   private FluidUtil() {
   }

   public static FluidStack getStack(ResourceHandler<FluidResource> handler, int index) {
      FluidResource resource = handler.getResource(index);
      return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack(handler.getAmountAsInt(index));
   }

   public static FluidStack getFirstStackContained(ItemStack stack) {
      if (stack.isEmpty()) {
         return FluidStack.EMPTY;
      } else {
         ResourceHandler<FluidResource> handler = ItemAccess.forStack(stack).oneByOne().getCapability(Capabilities.Fluid.ITEM);
         if (handler == null) {
            return FluidStack.EMPTY;
         } else {
            int size = handler.size();

            for (int index = 0; index < size; index++) {
               FluidStack fluidStack = getStack(handler, index);
               if (!fluidStack.isEmpty()) {
                  return fluidStack;
               }
            }

            return FluidStack.EMPTY;
         }
      }
   }

   public static boolean interactWithFluidHandler(Player player, InteractionHand hand, Level level, BlockPos pos, @Nullable Direction side) {
      Preconditions.checkNotNull(level);
      Preconditions.checkNotNull(pos);
      ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
      return fluidHandler != null && interactWithFluidHandler(player, hand, pos, fluidHandler);
   }

   public static boolean interactWithFluidHandler(Player player, InteractionHand hand, @Nullable BlockPos pos, ResourceHandler<FluidResource> handler) {
      ItemAccess itemAccess = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
      ResourceHandler<FluidResource> handHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
      return handHandler == null
         ? false
         : moveWithSound(handler, handHandler, player.level(), pos, player, true) != null
            || moveWithSound(handHandler, handler, player.level(), pos, player, false) != null;
   }

   @Nullable
   private static ResourceStack<FluidResource> moveWithSound(
      ResourceHandler<FluidResource> from, ResourceHandler<FluidResource> to, Level level, @Nullable BlockPos pos, @Nullable Player player, boolean pickup
   ) {
      if (player == null && pos == null) {
         throw new IllegalArgumentException("Either player or pos must be provided.");
      } else {
         ResourceStack<FluidResource> moved = ResourceHandlerUtil.moveFirst(from, to, fr -> true, Integer.MAX_VALUE, null);
         if (moved != null) {
            playSoundAndGameEvent(moved.resource(), level, pos, player, pickup);
         }

         return moved;
      }
   }

   private static void playSoundAndGameEvent(FluidResource resource, Level level, @Nullable BlockPos blockPos, @Nullable Player player, boolean pickup) {
      if (player == null && blockPos == null) {
         throw new IllegalArgumentException("Either player or blockPos must be provided.");
      } else {
         Vec3 position = blockPos != null ? Vec3.atCenterOf(blockPos) : new Vec3(player.getX(), player.getY() + 0.5, player.getZ());
         triggerSoundAndGameEvent(resource, level, position, player, pickup);
      }
   }

   public static void triggerSoundAndGameEvent(FluidResource resource, Level level, Vec3 position, @Nullable Player player, boolean pickup) {
      FluidStack stack = resource.toStack(1000);
      SoundEvent soundEvent = resource.getFluidType().getSound(stack, pickup ? SoundActions.BUCKET_FILL : SoundActions.BUCKET_EMPTY);
      if (soundEvent != null) {
         level.playSound(null, position.x, position.y, position.z, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      level.gameEvent(player, pickup ? GameEvent.FLUID_PICKUP : GameEvent.FLUID_PLACE, position);
   }

   public static FluidStack tryPickupFluid(
      @Nullable ResourceHandler<FluidResource> destination, @Nullable Player player, Level level, BlockPos pos, @Nullable Direction side
   ) {
      if (destination == null) {
         return FluidStack.EMPTY;
      } else {
         BlockState state = level.getBlockState(pos);
         if (!(state.getBlock() instanceof BucketPickup bucketPickup)) {
            ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, pos, state, null, side);
            if (fluidHandler == null) {
               return FluidStack.EMPTY;
            } else {
               ResourceStack<FluidResource> moved = moveWithSound(fluidHandler, destination, level, pos, player, true);
               return moved != null ? moved.resource().toStack(moved.amount()) : FluidStack.EMPTY;
            }
         } else {
            Fluid fluid = level.getFluidState(pos).getType();
            if (fluid == Fluids.EMPTY) {
               return FluidStack.EMPTY;
            } else {
               FluidStack extracted;
               try (Transaction tx = Transaction.openRoot()) {
                  FluidResource resource = FluidResource.of(fluid);
                  int inserted = destination.insert(resource, 1000, tx);
                  if (inserted != 1000) {
                     return FluidStack.EMPTY;
                  }

                  if (level.getFluidState(pos).getType() != fluid) {
                     return FluidStack.EMPTY;
                  }

                  ItemStack pickedUpStack = bucketPickup.pickupBlock(player, level, pos, level.getBlockState(pos));
                  if (pickedUpStack.getItem() instanceof BucketItem bucket) {
                     extracted = new FluidStack(bucket.content, 1000);
                     if (!resource.matches(extracted)) {
                        LOGGER.warn(
                           "Fluid removed without successfully being picked up. Fluid {} at {} in {} matched requested type, but after performing pickup was {}.",
                           new Object[]{
                              BuiltInRegistries.FLUID.getKey(fluid), pos, level.dimension().identifier(), BuiltInRegistries.FLUID.getKey(bucket.content)
                           }
                        );
                        return FluidStack.EMPTY;
                     }

                     tx.commit();
                     playSoundAndGameEvent(resource, level, pos, player, true);
                     return extracted;
                  }

                  if (!pickedUpStack.isEmpty()) {
                     LOGGER.warn(
                        "Picked up stack is not a bucket. Fluid {} at {} in {} picked up as {}.",
                        new Object[]{BuiltInRegistries.FLUID.getKey(fluid), pos, level.dimension().identifier(), pickedUpStack}
                     );
                  }

                  extracted = FluidStack.EMPTY;
               }

               return extracted;
            }
         }
      }
   }

   public static FluidStack tryPlaceFluid(
      @Nullable ResourceHandler<FluidResource> source, @Nullable Player player, Level level, InteractionHand hand, BlockPos pos
   ) {
      if (source == null) {
         return FluidStack.EMPTY;
      } else {
         int size = source.size();

         for (int index = 0; index < size; index++) {
            FluidResource resource = source.getResource(index);
            if (!resource.isEmpty()) {
               try (Transaction tx = Transaction.openRoot()) {
                  int amount = source.extract(index, resource, 1000, tx);
                  if (amount == 1000 && tryPlaceFluid(resource, player, level, hand, pos)) {
                     tx.commit();
                     return resource.toStack(1000);
                  }
               }
            }
         }

         return FluidStack.EMPTY;
      }
   }

   public static boolean tryPlaceFluid(FluidResource resource, @Nullable Player player, Level level, InteractionHand hand, BlockPos pos) {
      FluidStack stack = resource.toStack(1000);
      FluidType fluidType = resource.getFluidType();
      if (!stack.isEmpty() && fluidType.canBePlacedInLevel(level, pos, stack)) {
         ItemStack handItem = player == null ? ItemStack.EMPTY : player.getItemInHand(hand);
         BlockPlaceContext context = new BlockPlaceContext(level, player, hand, handItem, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
         BlockState destBlockState = level.getBlockState(pos);
         boolean isDestReplaceable = destBlockState.canBeReplaced(context);
         boolean canDestContainFluid = destBlockState.getBlock() instanceof LiquidBlockContainer lbc
            && lbc.canPlaceLiquid(player, level, pos, destBlockState, resource.getFluid());
         if (!destBlockState.isAir() && !isDestReplaceable && !canDestContainFluid) {
            return false;
         } else if (fluidType.isVaporizedOnPlacement(level, pos, stack)) {
            fluidType.onVaporize(player, level, pos, stack);
            return true;
         } else {
            if (canDestContainFluid) {
               LiquidBlockContainer lbcx = (LiquidBlockContainer)destBlockState.getBlock();
               lbcx.placeLiquid(level, pos, destBlockState, fluidType.getStateForPlacement(level, pos, stack));
            } else {
               if (!level.isClientSide() && isDestReplaceable && !destBlockState.liquid()) {
                  level.destroyBlock(pos, true);
               }

               BlockState state = fluidType.getBlockForFluidState(level, pos, resource.getFluid().defaultFluidState());
               level.setBlock(pos, state, 11);
            }

            playSoundAndGameEvent(resource, level, pos, player, false);
            return true;
         }
      } else {
         return false;
      }
   }
}
