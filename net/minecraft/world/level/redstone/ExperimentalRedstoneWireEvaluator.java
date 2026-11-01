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

   public ExperimentalRedstoneWireEvaluator(RedStoneWireBlock p_364859_) {
      super(p_364859_);
   }

   @Override
   public void updatePowerStrength(Level p_364203_, BlockPos p_362182_, BlockState p_361454_, @Nullable Orientation p_363673_, boolean p_366757_) {
      Orientation orientation = getInitialOrientation(p_364203_, p_363673_);
      this.calculateCurrentChanges(p_364203_, p_362182_, orientation);
      ObjectIterator<Entry<BlockPos>> objectiterator = this.updatedWires.object2IntEntrySet().iterator();

      for (boolean flag = true; objectiterator.hasNext(); flag = false) {
         Entry<BlockPos> entry = (Entry<BlockPos>)objectiterator.next();
         BlockPos blockpos = (BlockPos)entry.getKey();
         int i = entry.getIntValue();
         int j = unpackPower(i);
         BlockState blockstate = p_364203_.getBlockState(blockpos);
         if (blockstate.is(this.wireBlock) && !blockstate.getValue(RedStoneWireBlock.POWER).equals(j)) {
            int k = 2;
            if (!p_366757_ || !flag) {
               k |= 128;
            }

            p_364203_.setBlock(blockpos, blockstate.setValue(RedStoneWireBlock.POWER, j), k);
         } else {
            objectiterator.remove();
         }
      }

      this.causeNeighborUpdates(p_364203_);
   }

   private void causeNeighborUpdates(Level p_362391_) {
      this.updatedWires.forEach((p_458895_, p_458896_) -> {
         Orientation orientation = unpackOrientation(p_458896_);
         BlockState blockstate = p_362391_.getBlockState(p_458895_);

         for (Direction direction : orientation.getDirections()) {
            if (isConnected(blockstate, direction)) {
               BlockPos blockpos = p_458895_.relative(direction);
               BlockState blockstate1 = p_362391_.getBlockState(blockpos);
               Orientation orientation1 = orientation.withFrontPreserveUp(direction);
               p_362391_.neighborChanged(blockstate1, blockpos, this.wireBlock, orientation1, false);
               if (blockstate1.isRedstoneConductor(p_362391_, blockpos)) {
                  for (Direction direction1 : orientation1.getDirections()) {
                     if (direction1 != direction.getOpposite()) {
                        p_362391_.neighborChanged(blockpos.relative(direction1), this.wireBlock, orientation1.withFrontPreserveUp(direction1));
                     }
                  }
               }
            }
         }
      });
      if (p_362391_ instanceof ServerLevel serverlevel && serverlevel.debugSynchronizers().hasAnySubscriberFor(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS)) {
         this.updatedWires
            .forEach(
               (p_458892_, p_458893_) -> serverlevel.debugSynchronizers()
                  .sendBlockValue(p_458892_, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, unpackOrientation(p_458893_))
            );
      }
   }

   private static boolean isConnected(BlockState p_363855_, Direction p_362257_) {
      EnumProperty<RedstoneSide> enumproperty = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(p_362257_);
      return enumproperty == null ? p_362257_ == Direction.DOWN : p_363855_.getValue(enumproperty).isConnected();
   }

   private static Orientation getInitialOrientation(Level p_363526_, @Nullable Orientation p_363389_) {
      Orientation orientation;
      if (p_363389_ != null) {
         orientation = p_363389_;
      } else {
         orientation = Orientation.random(p_363526_.random);
      }

      return orientation.withUp(Direction.UP).withSideBias(Orientation.SideBias.LEFT);
   }

   private void calculateCurrentChanges(Level p_361794_, BlockPos p_363346_, Orientation p_362683_) {
      BlockState blockstate = p_361794_.getBlockState(p_363346_);
      if (blockstate.is(this.wireBlock)) {
         this.setPower(p_363346_, blockstate.getValue(RedStoneWireBlock.POWER), p_362683_);
         this.wiresToTurnOff.add(p_363346_);
      } else {
         this.propagateChangeToNeighbors(p_361794_, p_363346_, 0, p_362683_, true);
      }

      while (!this.wiresToTurnOff.isEmpty()) {
         BlockPos blockpos = this.wiresToTurnOff.removeFirst();
         int i = this.updatedWires.getInt(blockpos);
         Orientation orientation = unpackOrientation(i);
         int j = unpackPower(i);
         int k = this.getBlockSignal(p_361794_, blockpos);
         int l = this.getIncomingWireSignal(p_361794_, blockpos);
         int i1 = Math.max(k, l);
         int j1;
         if (i1 < j) {
            if (k > 0 && !this.wiresToTurnOn.contains(blockpos)) {
               this.wiresToTurnOn.add(blockpos);
            }

            j1 = 0;
         } else {
            j1 = i1;
         }

         if (j1 != j) {
            this.setPower(blockpos, j1, orientation);
         }

         this.propagateChangeToNeighbors(p_361794_, blockpos, j1, orientation, j > i1);
      }

      while (!this.wiresToTurnOn.isEmpty()) {
         BlockPos blockpos1 = this.wiresToTurnOn.removeFirst();
         int k1 = this.updatedWires.getInt(blockpos1);
         int l1 = unpackPower(k1);
         int i2 = this.getBlockSignal(p_361794_, blockpos1);
         int j2 = this.getIncomingWireSignal(p_361794_, blockpos1);
         int k2 = Math.max(i2, j2);
         Orientation orientation1 = unpackOrientation(k1);
         if (k2 > l1) {
            this.setPower(blockpos1, k2, orientation1);
         } else if (k2 < l1) {
            throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
         }

         this.propagateChangeToNeighbors(p_361794_, blockpos1, k2, orientation1, false);
      }
   }

   private static int packOrientationAndPower(Orientation p_363227_, int p_364958_) {
      return p_363227_.getIndex() << 4 | p_364958_;
   }

   private static Orientation unpackOrientation(int p_361592_) {
      return Orientation.fromIndex(p_361592_ >> 4);
   }

   private static int unpackPower(int p_360709_) {
      return p_360709_ & 15;
   }

   private void setPower(BlockPos p_364897_, int p_364699_, Orientation p_364501_) {
      this.updatedWires
         .compute(
            p_364897_,
            (p_362131_, p_363114_) -> p_363114_ == null
               ? packOrientationAndPower(p_364501_, p_364699_)
               : packOrientationAndPower(unpackOrientation(p_363114_), p_364699_)
         );
   }

   private void propagateChangeToNeighbors(Level p_363529_, BlockPos p_364797_, int p_360356_, Orientation p_361821_, boolean p_363868_) {
      for (Direction direction : p_361821_.getHorizontalDirections()) {
         BlockPos blockpos = p_364797_.relative(direction);
         this.enqueueNeighborWire(p_363529_, blockpos, p_360356_, p_361821_.withFront(direction), p_363868_);
      }

      for (Direction direction2 : p_361821_.getVerticalDirections()) {
         BlockPos blockpos3 = p_364797_.relative(direction2);
         boolean flag = p_363529_.getBlockState(blockpos3).isRedstoneConductor(p_363529_, blockpos3);

         for (Direction direction1 : p_361821_.getHorizontalDirections()) {
            BlockPos blockpos1 = p_364797_.relative(direction1);
            if (direction2 == Direction.UP && !flag) {
               BlockPos blockpos4 = blockpos3.relative(direction1);
               this.enqueueNeighborWire(p_363529_, blockpos4, p_360356_, p_361821_.withFront(direction1), p_363868_);
            } else if (direction2 == Direction.DOWN && !p_363529_.getBlockState(blockpos1).isRedstoneConductor(p_363529_, blockpos1)) {
               BlockPos blockpos2 = blockpos3.relative(direction1);
               this.enqueueNeighborWire(p_363529_, blockpos2, p_360356_, p_361821_.withFront(direction1), p_363868_);
            }
         }
      }
   }

   private void enqueueNeighborWire(Level p_361921_, BlockPos p_361620_, int p_361312_, Orientation p_364781_, boolean p_363545_) {
      BlockState blockstate = p_361921_.getBlockState(p_361620_);
      if (blockstate.is(this.wireBlock)) {
         int i = this.getWireSignal(p_361620_, blockstate);
         if (i < p_361312_ - 1 && !this.wiresToTurnOn.contains(p_361620_)) {
            this.wiresToTurnOn.add(p_361620_);
            this.setPower(p_361620_, i, p_364781_);
         }

         if (p_363545_ && i > p_361312_ && !this.wiresToTurnOff.contains(p_361620_)) {
            this.wiresToTurnOff.add(p_361620_);
            this.setPower(p_361620_, i, p_364781_);
         }
      }
   }

   @Override
   protected int getWireSignal(BlockPos p_360845_, BlockState p_363778_) {
      int i = this.updatedWires.getOrDefault(p_360845_, -1);
      return i != -1 ? unpackPower(i) : super.getWireSignal(p_360845_, p_363778_);
   }
}
