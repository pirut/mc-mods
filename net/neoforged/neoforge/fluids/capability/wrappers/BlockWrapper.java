package net.neoforged.neoforge.fluids.capability.wrappers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.VoidFluidHandler;

public class BlockWrapper extends VoidFluidHandler {
   protected final BlockState state;
   protected final Level world;
   protected final BlockPos blockPos;

   public BlockWrapper(BlockState state, Level world, BlockPos blockPos) {
      this.state = state;
      this.world = world;
      this.blockPos = blockPos;
   }

   @Override
   public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      if (resource.getAmount() < 1000) {
         return 0;
      } else {
         if (action.execute()) {
            FluidUtil.destroyBlockOnFluidPlacement(this.world, this.blockPos);
            this.world.setBlock(this.blockPos, this.state, 11);
         }

         return 1000;
      }
   }

   public static class LiquidContainerBlockWrapper extends VoidFluidHandler {
      protected final LiquidBlockContainer liquidContainer;
      protected final Level world;
      protected final BlockPos blockPos;

      public LiquidContainerBlockWrapper(LiquidBlockContainer liquidContainer, Level world, BlockPos blockPos) {
         this.liquidContainer = liquidContainer;
         this.world = world;
         this.blockPos = blockPos;
      }

      @Override
      public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
         if (resource.getAmount() >= 1000) {
            BlockState state = this.world.getBlockState(this.blockPos);
            if (this.liquidContainer.canPlaceLiquid(null, this.world, this.blockPos, state, resource.getFluid())) {
               if (action.execute()) {
                  this.liquidContainer
                     .placeLiquid(this.world, this.blockPos, state, resource.getFluidType().getStateForPlacement(this.world, this.blockPos, resource));
               }

               return 1000;
            }
         }

         return 0;
      }
   }
}
