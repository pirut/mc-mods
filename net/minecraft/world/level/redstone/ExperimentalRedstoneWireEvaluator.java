package net.minecraft.world.level.redstone;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jspecify.annotations.Nullable;

public class ExperimentalRedstoneWireEvaluator extends RedstoneWireEvaluator {
   private final Deque<BlockPos> wiresToTurnOff = new ArrayDeque<>();
   private final Deque<BlockPos> wiresToTurnOn = new ArrayDeque<>();
   private final Object2IntMap<BlockPos> updatedWires = new Object2IntLinkedOpenHashMap();

   public ExperimentalRedstoneWireEvaluator(RedStoneWireBlock redStoneWireBlock) {
      super(redStoneWireBlock);
   }

   @Override
   public void updatePowerStrength(Level level, BlockPos pos, BlockState state, @Nullable Orientation orientation, boolean updateShape) {
      Orientation orientation2 = getInitialOrientation(level, orientation);
      this.calculateCurrentChanges(level, pos, orientation2);
      ObjectIterator<Entry<BlockPos>> objectIterator = this.updatedWires.object2IntEntrySet().iterator();

      for (boolean bl = true; objectIterator.hasNext(); bl = false) {
         Entry<BlockPos> entry = (Entry<BlockPos>)objectIterator.next();
         BlockPos blockPos = (BlockPos)entry.getKey();
         int i = entry.getIntValue();
         int j = unpackPower(i);
         BlockState blockState = level.getBlockState(blockPos);
         if (blockState.is(this.wireBlock) && !blockState.getValue(RedStoneWireBlock.POWER).equals(j)) {
            int k = 2;
            if (!updateShape || !bl) {
               k |= 128;
            }

            level.setBlock(blockPos, blockState.setValue(RedStoneWireBlock.POWER, j), k);
         } else {
            objectIterator.remove();
         }
      }

      this.causeNeighborUpdates(level);
   }

   private void causeNeighborUpdates(Level level) {
      this.updatedWires.forEach((blockPos, i) -> {
         Orientation orientation = unpackOrientation(i);
         BlockState blockState = level.getBlockState(blockPos);

         for (Direction direction : orientation.getDirections()) {
            if (isConnected(blockState, direction)) {
               BlockPos blockPos2 = blockPos.relative(direction);
               BlockState blockState2 = level.getBlockState(blockPos2);
               Orientation orientation2 = orientation.withFrontPreserveUp(direction);
               level.neighborChanged(blockState2, blockPos2, this.wireBlock, orientation2, false);
               if (blockState2.isRedstoneConductor(level, blockPos2)) {
                  for (Direction direction2 : orientation2.getDirections()) {
                     if (direction2 != direction.getOpposite()) {
                        level.neighborChanged(blockPos2.relative(direction2), this.wireBlock, orientation2.withFrontPreserveUp(direction2));
                     }
                  }
               }
            }
         }
      });
      if (level instanceof ServerLevel serverLevel && serverLevel.debugSynchronizers().hasAnySubscriberFor(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS)) {
         this.updatedWires
            .forEach(
               (blockPos, i) -> serverLevel.debugSynchronizers().sendBlockValue(blockPos, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, unpackOrientation(i))
            );
      }
   }

   private static boolean isConnected(BlockState state, Direction direction) {
      EnumProperty<RedstoneSide> enumProperty = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction);
      return enumProperty == null ? direction == Direction.DOWN : state.getValue(enumProperty).isConnected();
   }

   private static Orientation getInitialOrientation(Level level, @Nullable Orientation orientation) {
      Orientation orientation2;
      if (orientation != null) {
         orientation2 = orientation;
      } else {
         orientation2 = Orientation.random(level.random);
      }

      return orientation2.withUp(Direction.UP).withSideBias(Orientation.SideBias.LEFT);
   }

   private void calculateCurrentChanges(Level level, BlockPos pos, Orientation orientation) {
      BlockState blockState = level.getBlockState(pos);
      if (blockState.is(this.wireBlock)) {
         this.setPower(pos, blockState.getValue(RedStoneWireBlock.POWER), orientation);
         this.wiresToTurnOff.add(pos);
      } else {
         this.propagateChangeToNeighbors(level, pos, 0, orientation, true);
      }

      while (!this.wiresToTurnOff.isEmpty()) {
         BlockPos blockPos = this.wiresToTurnOff.removeFirst();
         int i = this.updatedWires.getInt(blockPos);
         Orientation orientation2 = unpackOrientation(i);
         int j = unpackPower(i);
         int k = this.getBlockSignal(level, blockPos);
         int l = this.getIncomingWireSignal(level, blockPos);
         int m = Math.max(k, l);
         int n;
         if (m < j) {
            if (k > 0 && !this.wiresToTurnOn.contains(blockPos)) {
               this.wiresToTurnOn.add(blockPos);
            }

            n = 0;
         } else {
            n = m;
         }

         if (n != j) {
            this.setPower(blockPos, n, orientation2);
         }

         this.propagateChangeToNeighbors(level, blockPos, n, orientation2, j > m);
      }

      while (!this.wiresToTurnOn.isEmpty()) {
         BlockPos blockPosx = this.wiresToTurnOn.removeFirst();
         int ix = this.updatedWires.getInt(blockPosx);
         int o = unpackPower(ix);
         int jx = this.getBlockSignal(level, blockPosx);
         int kx = this.getIncomingWireSignal(level, blockPosx);
         int lx = Math.max(jx, kx);
         Orientation orientation3 = unpackOrientation(ix);
         if (lx > o) {
            this.setPower(blockPosx, lx, orientation3);
         } else if (lx < o) {
            throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
         }

         this.propagateChangeToNeighbors(level, blockPosx, lx, orientation3, false);
      }
   }

   private static int packOrientationAndPower(Orientation orientation, int power) {
      return orientation.getIndex() << 4 | power;
   }

   private static Orientation unpackOrientation(int data) {
      return Orientation.fromIndex(data >> 4);
   }

   private static int unpackPower(int data) {
      return data & 15;
   }

   private void setPower(BlockPos pos, int power, Orientation orientation) {
      this.updatedWires
         .compute(
            pos,
            (blockPos, integer) -> integer == null ? packOrientationAndPower(orientation, power) : packOrientationAndPower(unpackOrientation(integer), power)
         );
   }

   private void propagateChangeToNeighbors(Level level, BlockPos pos, int power, Orientation orientation, boolean canTurnOff) {
      for (Direction direction : orientation.getHorizontalDirections()) {
         BlockPos blockPos = pos.relative(direction);
         this.enqueueNeighborWire(level, blockPos, power, orientation.withFront(direction), canTurnOff);
      }

      for (Direction direction : orientation.getVerticalDirections()) {
         BlockPos blockPos = pos.relative(direction);
         boolean bl = level.getBlockState(blockPos).isRedstoneConductor(level, blockPos);

         for (Direction direction2 : orientation.getHorizontalDirections()) {
            BlockPos blockPos2 = pos.relative(direction2);
            if (direction == Direction.UP && !bl) {
               BlockPos blockPos3 = blockPos.relative(direction2);
               this.enqueueNeighborWire(level, blockPos3, power, orientation.withFront(direction2), canTurnOff);
            } else if (direction == Direction.DOWN && !level.getBlockState(blockPos2).isRedstoneConductor(level, blockPos2)) {
               BlockPos blockPos3 = blockPos.relative(direction2);
               this.enqueueNeighborWire(level, blockPos3, power, orientation.withFront(direction2), canTurnOff);
            }
         }
      }
   }

   private void enqueueNeighborWire(Level level, BlockPos pos, int power, Orientation orientation, boolean canTurnOff) {
      BlockState blockState = level.getBlockState(pos);
      if (blockState.is(this.wireBlock)) {
         int i = this.getWireSignal(pos, blockState);
         if (i < power - 1 && !this.wiresToTurnOn.contains(pos)) {
            this.wiresToTurnOn.add(pos);
            this.setPower(pos, i, orientation);
         }

         if (canTurnOff && i > power && !this.wiresToTurnOff.contains(pos)) {
            this.wiresToTurnOff.add(pos);
            this.setPower(pos, i, orientation);
         }
      }
   }

   @Override
   protected int getWireSignal(BlockPos pos, BlockState state) {
      int i = this.updatedWires.getOrDefault(pos, -1);
      return i != -1 ? unpackPower(i) : super.getWireSignal(pos, state);
   }
}
