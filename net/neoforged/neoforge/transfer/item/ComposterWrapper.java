package net.neoforged.neoforge.transfer.item;

import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public class ComposterWrapper extends SnapshotJournal<BlockState> {
   private static final Map<ComposterWrapper.WrapperLocation, ComposterWrapper> wrappers = new MapMaker().concurrencyLevel(1).weakKeys().weakValues().makeMap();
   private static final ItemResource BONE_MEAL = ItemResource.of(Items.BONE_MEAL);
   private final ComposterWrapper.WrapperLocation location;
   private final TransactionalRandom transactionalRandom = new TransactionalRandom();
   private final ResourceHandler<ItemResource> topHandler = new ComposterWrapper.Top();
   private final ResourceHandler<ItemResource> bottomHandler = new ComposterWrapper.Bottom();

   @Nullable
   public static ResourceHandler<ItemResource> get(Level level, BlockPos pos, @Nullable Direction direction) {
      if (direction != null && direction.getAxis().isVertical()) {
         ComposterWrapper.WrapperLocation location = new ComposterWrapper.WrapperLocation(level, pos.immutable());
         ComposterWrapper wrapper = wrappers.computeIfAbsent(location, ComposterWrapper::new);
         return direction == Direction.UP ? wrapper.topHandler : wrapper.bottomHandler;
      } else {
         return null;
      }
   }

   private ComposterWrapper(ComposterWrapper.WrapperLocation location) {
      this.location = location;
   }

   protected BlockState createSnapshot() {
      return this.location.getBlockState();
   }

   protected void revertToSnapshot(BlockState snapshot) {
      this.location.level.setBlock(this.location.pos, snapshot, 0);
   }

   protected void onRootCommit(BlockState originalState) {
      BlockState currentState = this.location.getBlockState();
      if (currentState.is(Blocks.COMPOSTER)) {
         if (originalState != currentState) {
            this.location.level.setBlock(this.location.pos, originalState, 0);
            this.location.level.setBlockAndUpdate(this.location.pos, currentState);
            this.location.level.gameEvent(GameEvent.BLOCK_CHANGE, this.location.pos, GameEvent.Context.of(currentState));
         }

         int originalLevel = originalState.getValue(ComposterBlock.LEVEL);
         int currentLevel = currentState.getValue(ComposterBlock.LEVEL);
         if (originalLevel < 7) {
            if (currentLevel == 7) {
               this.location.level.scheduleTick(this.location.pos, currentState.getBlock(), 20);
            }

            this.location.level.levelEvent(1500, this.location.pos, currentLevel > originalLevel ? 1 : 0);
         }
      }
   }

   private static float getComposterValue(ItemResource resource) {
      return ComposterBlock.getValue(resource.toStack());
   }

   private void setLevel(BlockState state, int newLevel) {
      BlockState newState = state.setValue(ComposterBlock.LEVEL, newLevel);
      this.location.level.setBlock(this.location.pos, newState, 0);
   }

   private class Bottom implements ResourceHandler<ItemResource> {
      @Override
      public int size() {
         return 1;
      }

      public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
         return 0;
      }

      public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
         Objects.checkIndex(index, this.size());
         TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
         if (amount < 1) {
            return 0;
         } else if (!ComposterWrapper.BONE_MEAL.equals(resource)) {
            return 0;
         } else {
            BlockState state = ComposterWrapper.this.location.getBlockState();
            if (state.getValue(ComposterBlock.LEVEL) != 8) {
               return 0;
            } else {
               ComposterWrapper.this.updateSnapshots(transaction);
               ComposterWrapper.this.setLevel(state, 0);
               return 1;
            }
         }
      }

      public ItemResource getResource(int index) {
         Objects.checkIndex(index, this.size());
         return ComposterWrapper.this.location.getBlockState().getValue(ComposterBlock.LEVEL) == 8 ? ComposterWrapper.BONE_MEAL : ItemResource.EMPTY;
      }

      @Override
      public long getAmountAsLong(int index) {
         return this.getResource(index).equals(ComposterWrapper.BONE_MEAL) ? 1L : 0L;
      }

      public long getCapacityAsLong(int index, ItemResource resource) {
         Objects.checkIndex(index, this.size());
         return !resource.isEmpty() && !ComposterWrapper.BONE_MEAL.equals(resource) ? 0L : 1L;
      }

      public boolean isValid(int index, ItemResource resource) {
         Objects.checkIndex(index, this.size());
         return ComposterWrapper.BONE_MEAL.equals(resource);
      }

      @Override
      public String toString() {
         return "ComposterWrapper[" + ComposterWrapper.this.location + "/bottom]";
      }
   }

   private class Top implements ResourceHandler<ItemResource> {
      @Override
      public int size() {
         return 1;
      }

      public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
         Objects.checkIndex(index, this.size());
         TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
         if (amount < 1) {
            return 0;
         } else {
            BlockState state = ComposterWrapper.this.location.getBlockState();
            int currentLevel = state.getValue(ComposterBlock.LEVEL);
            if (currentLevel >= 7) {
               return 0;
            } else {
               float value = ComposterWrapper.getComposterValue(resource);
               if (value <= 0.0F) {
                  return 0;
               } else {
                  ComposterWrapper.this.updateSnapshots(transaction);
                  if (currentLevel == 0 || ComposterWrapper.this.transactionalRandom.nextDouble(transaction) < value) {
                     ComposterWrapper.this.setLevel(state, currentLevel + 1);
                  }

                  return 1;
               }
            }
         }
      }

      public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
         return 0;
      }

      public ItemResource getResource(int index) {
         return ItemResource.EMPTY;
      }

      @Override
      public long getAmountAsLong(int index) {
         return 0L;
      }

      public long getCapacityAsLong(int index, ItemResource resource) {
         Objects.checkIndex(index, this.size());
         return !resource.isEmpty() && !(ComposterWrapper.getComposterValue(resource) > 0.0F) ? 0L : 1L;
      }

      public boolean isValid(int index, ItemResource resource) {
         Objects.checkIndex(index, this.size());
         return ComposterWrapper.getComposterValue(resource) > 0.0F;
      }

      @Override
      public String toString() {
         return "ComposterWrapper[" + ComposterWrapper.this.location + "/top]";
      }
   }

   private record WrapperLocation(Level level, BlockPos pos) {
      public BlockState getBlockState() {
         return this.level.getBlockState(this.pos);
      }
   }
}
