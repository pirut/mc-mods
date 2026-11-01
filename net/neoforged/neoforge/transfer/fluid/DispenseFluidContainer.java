package net.neoforged.neoforge.transfer.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;

public final class DispenseFluidContainer extends DefaultDispenseItemBehavior {
   private static final DispenseFluidContainer INSTANCE = new DispenseFluidContainer();

   public static DispenseFluidContainer getInstance() {
      return INSTANCE;
   }

   private DispenseFluidContainer() {
   }

   @Override
   public ItemStack execute(BlockSource source, ItemStack stack) {
      ItemStacksResourceHandler containingHandler = new ItemStacksResourceHandler(2);
      containingHandler.set(0, ItemResource.of(stack), stack.getCount());
      ItemAccess itemAccess = ItemAccess.forHandlerIndex(containingHandler, 0).oneByOne();
      ResourceHandler<FluidResource> resourceHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
      if (resourceHandler == null) {
         return super.execute(source, stack);
      } else {
         Direction dispenserFacing = source.state().getValue(DispenserBlock.FACING);
         BlockPos targetPos = source.pos().relative(dispenserFacing);
         if (FluidUtil.tryPickupFluid(resourceHandler, null, source.level(), targetPos, dispenserFacing.getOpposite()).isEmpty()
            && FluidUtil.tryPlaceFluid(resourceHandler, null, source.level(), InteractionHand.MAIN_HAND, targetPos).isEmpty()) {
            return super.execute(source, stack);
         } else {
            ItemStack stack0 = ItemUtil.getStack(containingHandler, 0);
            ItemStack stack1 = ItemUtil.getStack(containingHandler, 1);
            stack0.grow(1);
            return this.consumeWithRemainder(source, stack, stack1);
         }
      }
   }
}
