package net.neoforged.neoforge.transfer.fluid;

import com.google.common.collect.MapMaker;
import com.google.common.math.IntMath;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class CauldronWrapper extends SnapshotJournal<BlockState> implements ResourceHandler<FluidResource> {
   private static final Map<CauldronWrapper.WrapperLocation, CauldronWrapper> wrappers = new MapMaker().concurrencyLevel(1).weakKeys().weakValues().makeMap();
   private final CauldronWrapper.WrapperLocation location;

   public static CauldronWrapper get(Level level, BlockPos pos) {
      CauldronWrapper.WrapperLocation location = new CauldronWrapper.WrapperLocation(level, pos.immutable());
      return wrappers.computeIfAbsent(location, CauldronWrapper::new);
   }

   private CauldronWrapper(CauldronWrapper.WrapperLocation location) {
      this.location = location;
   }

   private CauldronFluidContent getContent(BlockState state) {
      CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());
      if (content == null) {
         throw new IllegalStateException(
            "Unexpected error: no cauldron at location " + this.location.pos + " in " + this.location.level.dimension().identifier()
         );
      } else {
         return content;
      }
   }

   @Override
   public int size() {
      return 1;
   }

   public FluidResource getResource(int index) {
      Objects.checkIndex(index, this.size());
      BlockState state = this.location.getBlockState();
      return FluidResource.of(this.getContent(state).fluid);
   }

   @Override
   public long getAmountAsLong(int index) {
      Objects.checkIndex(index, this.size());
      BlockState state = this.location.getBlockState();
      CauldronFluidContent content = this.getContent(state);
      return (long)content.totalAmount * content.currentLevel(state) / content.maxLevel;
   }

   public long getCapacityAsLong(int index, FluidResource resource) {
      Objects.checkIndex(index, this.size());
      CauldronFluidContent fluidContent = CauldronFluidContent.getForFluid(resource.getFluid());
      return fluidContent == null ? 0L : fluidContent.totalAmount;
   }

   public boolean isValid(int index, FluidResource resource) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmpty(resource);
      return resource.isComponentsPatchEmpty() && CauldronFluidContent.getForFluid(resource.getFluid()) != null;
   }

   private void setLevel(CauldronFluidContent newContent, int fluidLevel, TransactionContext transaction) {
      this.updateSnapshots(transaction);
      if (fluidLevel == 0) {
         this.location.level.setBlock(this.location.pos, Blocks.CAULDRON.defaultBlockState(), 0);
      } else {
         BlockState newState = newContent.block.defaultBlockState();
         if (newContent.levelProperty != null) {
            newState = newState.setValue(newContent.levelProperty, fluidLevel);
         }

         this.location.level.setBlock(this.location.pos, newState, 0);
      }
   }

   public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      if (!resource.isComponentsPatchEmpty()) {
         return 0;
      } else {
         CauldronFluidContent insertContent = CauldronFluidContent.getForFluid(resource.getFluid());
         if (insertContent == null) {
            return 0;
         } else {
            BlockState state = this.location.getBlockState();
            CauldronFluidContent currentContent = this.getContent(state);
            if (currentContent.fluid != Fluids.EMPTY && !resource.is(currentContent.fluid)) {
               return 0;
            } else {
               int d = IntMath.gcd(insertContent.maxLevel, insertContent.totalAmount);
               int amountIncrements = insertContent.totalAmount / d;
               int levelIncrements = insertContent.maxLevel / d;
               int currentLevel = currentContent.currentLevel(state);
               int insertedIncrements = Math.min(amount / amountIncrements, (insertContent.maxLevel - currentLevel) / levelIncrements);
               if (insertedIncrements > 0) {
                  this.setLevel(insertContent, currentLevel + insertedIncrements * levelIncrements, transaction);
               }

               return insertedIncrements * amountIncrements;
            }
         }
      }
   }

   public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      BlockState state = this.location.getBlockState();
      CauldronFluidContent currentContent = this.getContent(state);
      if (resource.is(currentContent.fluid) && resource.isComponentsPatchEmpty()) {
         int d = IntMath.gcd(currentContent.maxLevel, currentContent.totalAmount);
         int levelIncrements = currentContent.maxLevel / d;
         int amountIncrements = currentContent.totalAmount / d;
         int currentLevel = currentContent.currentLevel(state);
         int extractedIncrements = Math.min(amount / amountIncrements, currentLevel / levelIncrements);
         if (extractedIncrements > 0) {
            this.setLevel(currentContent, currentLevel - extractedIncrements * levelIncrements, transaction);
         }

         return extractedIncrements * amountIncrements;
      } else {
         return 0;
      }
   }

   protected BlockState createSnapshot() {
      return this.location.getBlockState();
   }

   protected void revertToSnapshot(BlockState snapshot) {
      this.location.level.setBlock(this.location.pos, snapshot, 0);
   }

   protected void onRootCommit(BlockState originalState) {
      BlockState state = this.location.getBlockState();
      if (originalState != state && CauldronFluidContent.getForBlock(state.getBlock()) != null) {
         this.location.level.setBlock(this.location.pos, originalState, 0);
         this.location.level.setBlockAndUpdate(this.location.pos, state);
      }
   }

   private record WrapperLocation(Level level, BlockPos pos) {
      public BlockState getBlockState() {
         return this.level.getBlockState(this.pos);
      }
   }
}
