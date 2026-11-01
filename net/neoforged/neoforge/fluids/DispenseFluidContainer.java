package net.neoforged.neoforge.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public class DispenseFluidContainer extends DefaultDispenseItemBehavior {
   private static final DispenseFluidContainer INSTANCE = new DispenseFluidContainer();
   private final DefaultDispenseItemBehavior dispenseBehavior = new DefaultDispenseItemBehavior();

   public static DispenseFluidContainer getInstance() {
      return INSTANCE;
   }

   private DispenseFluidContainer() {
   }

   @Override
   public ItemStack execute(BlockSource source, ItemStack stack) {
      return FluidUtil.getFluidContained(stack).isPresent() ? this.dumpContainer(source, stack) : this.fillContainer(source, stack);
   }

   private ItemStack fillContainer(BlockSource source, ItemStack stack) {
      Level level = source.level();
      Direction dispenserFacing = source.state().getValue(DispenserBlock.FACING);
      BlockPos blockpos = source.pos().relative(dispenserFacing);
      FluidActionResult actionResult = FluidUtil.tryPickUpFluid(stack, null, level, blockpos, dispenserFacing.getOpposite());
      ItemStack resultStack = actionResult.getResult();
      return actionResult.isSuccess() && !resultStack.isEmpty() ? this.consumeWithRemainder(source, stack, resultStack) : super.execute(source, stack);
   }

   private ItemStack dumpContainer(BlockSource source, ItemStack stack) {
      ItemStack singleStack = stack.copy();
      singleStack.setCount(1);
      IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(singleStack).orElse(null);
      if (fluidHandler == null) {
         return super.execute(source, stack);
      } else {
         FluidStack fluidStack = fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
         Direction dispenserFacing = source.state().getValue(DispenserBlock.FACING);
         BlockPos blockpos = source.pos().relative(dispenserFacing);
         FluidActionResult result = FluidUtil.tryPlaceFluid(null, source.level(), InteractionHand.MAIN_HAND, blockpos, stack, fluidStack);
         if (result.isSuccess()) {
            ItemStack drainedStack = result.getResult();
            return this.consumeWithRemainder(source, stack, drainedStack);
         } else {
            return this.dispenseBehavior.dispense(source, stack);
         }
      }
   }
}
