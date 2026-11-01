package net.neoforged.neoforge.fluids.capability.wrappers;

import com.google.common.math.IntMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class CauldronWrapper implements IFluidHandler {
   private final Level level;
   private final BlockPos pos;

   public CauldronWrapper(Level level, BlockPos pos) {
      this.level = level;
      this.pos = pos;
   }

   @Override
   public int getTanks() {
      return 1;
   }

   private CauldronFluidContent getContent(BlockState state) {
      CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());
      if (content == null) {
         throw new IllegalStateException("Unexpected error: no cauldron at location " + this.pos);
      } else {
         return content;
      }
   }

   @Override
   public FluidStack getFluidInTank(int tank) {
      BlockState state = this.level.getBlockState(this.pos);
      CauldronFluidContent contents = this.getContent(state);
      return new FluidStack(contents.fluid, contents.totalAmount * contents.currentLevel(state) / contents.maxLevel);
   }

   @Override
   public int getTankCapacity(int tank) {
      BlockState state = this.level.getBlockState(this.pos);
      CauldronFluidContent contents = this.getContent(state);
      return contents.totalAmount;
   }

   @Override
   public boolean isFluidValid(int tank, FluidStack stack) {
      return CauldronFluidContent.getForFluid(stack.getFluid()) != null;
   }

   private void updateLevel(CauldronFluidContent newContent, int level, IFluidHandler.FluidAction action) {
      if (action.execute()) {
         BlockState newState = newContent.block.defaultBlockState();
         if (newContent.levelProperty != null) {
            newState = newState.setValue(newContent.levelProperty, level);
         }

         this.level.setBlockAndUpdate(this.pos, newState);
      }
   }

   @Override
   public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      if (resource.isEmpty()) {
         return 0;
      } else {
         CauldronFluidContent insertContent = CauldronFluidContent.getForFluid(resource.getFluid());
         if (insertContent == null) {
            return 0;
         } else {
            BlockState state = this.level.getBlockState(this.pos);
            CauldronFluidContent currentContent = this.getContent(state);
            if (currentContent.fluid != Fluids.EMPTY && !resource.is(currentContent.fluid)) {
               return 0;
            } else {
               int d = IntMath.gcd(insertContent.maxLevel, insertContent.totalAmount);
               int amountIncrements = insertContent.totalAmount / d;
               int levelIncrements = insertContent.maxLevel / d;
               int currentLevel = currentContent.currentLevel(state);
               int insertedIncrements = Math.min(resource.getAmount() / amountIncrements, (insertContent.maxLevel - currentLevel) / levelIncrements);
               if (insertedIncrements > 0) {
                  this.updateLevel(insertContent, currentLevel + insertedIncrements * levelIncrements, action);
               }

               return insertedIncrements * amountIncrements;
            }
         }
      }
   }

   @Override
   public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      if (resource.isEmpty()) {
         return FluidStack.EMPTY;
      } else {
         BlockState state = this.level.getBlockState(this.pos);
         return resource.is(this.getContent(state).fluid) && resource.getComponents().isEmpty()
            ? this.drain(state, resource.getAmount(), action)
            : FluidStack.EMPTY;
      }
   }

   @Override
   public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      return maxDrain <= 0 ? FluidStack.EMPTY : this.drain(this.level.getBlockState(this.pos), maxDrain, action);
   }

   private FluidStack drain(BlockState state, int maxDrain, IFluidHandler.FluidAction action) {
      CauldronFluidContent content = this.getContent(state);
      int d = IntMath.gcd(content.maxLevel, content.totalAmount);
      int amountIncrements = content.totalAmount / d;
      int levelIncrements = content.maxLevel / d;
      int currentLevel = content.currentLevel(state);
      int extractedIncrements = Math.min(maxDrain / amountIncrements, currentLevel / levelIncrements);
      if (extractedIncrements > 0) {
         int newLevel = currentLevel - extractedIncrements * levelIncrements;
         if (newLevel == 0) {
            if (action.execute()) {
               this.level.setBlockAndUpdate(this.pos, Blocks.CAULDRON.defaultBlockState());
            }
         } else {
            this.updateLevel(content, newLevel, action);
         }
      }

      return new FluidStack(content.fluid, extractedIncrements * amountIncrements);
   }
}
