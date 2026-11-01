package net.minecraft.world.level.block;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SideChainPart;

public interface SideChainPartBlock {
   SideChainPart getSideChainPart(BlockState var1);

   BlockState setSideChainPart(BlockState var1, SideChainPart var2);

   Direction getFacing(BlockState var1);

   boolean isConnectable(BlockState var1);

   int getMaxChainLength();

   default List<BlockPos> getAllBlocksConnectedTo(LevelAccessor p_435499_, BlockPos p_433515_) {
      BlockState blockstate = p_435499_.getBlockState(p_433515_);
      if (!this.isConnectable(blockstate)) {
         return List.of();
      } else {
         SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(p_435499_, p_433515_, this.getFacing(blockstate));
         List<BlockPos> list = new LinkedList<>();
         list.add(p_433515_);
         this.addBlocksConnectingTowards(sidechainpartblock$neighbors::left, SideChainPart.LEFT, list::addFirst);
         this.addBlocksConnectingTowards(sidechainpartblock$neighbors::right, SideChainPart.RIGHT, list::addLast);
         return list;
      }
   }

   private void addBlocksConnectingTowards(IntFunction<SideChainPartBlock.Neighbor> p_434541_, SideChainPart p_432965_, Consumer<BlockPos> p_435458_) {
      for (int i = 1; i < this.getMaxChainLength(); i++) {
         SideChainPartBlock.Neighbor sidechainpartblock$neighbor = p_434541_.apply(i);
         if (sidechainpartblock$neighbor.connectsTowards(p_432965_)) {
            p_435458_.accept(sidechainpartblock$neighbor.pos());
         }

         if (sidechainpartblock$neighbor.isUnconnectableOrChainEnd()) {
            break;
         }
      }
   }

   default void updateNeighborsAfterPoweringDown(LevelAccessor p_435537_, BlockPos p_433559_, BlockState p_435709_) {
      SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(p_435537_, p_433559_, this.getFacing(p_435709_));
      sidechainpartblock$neighbors.left().disconnectFromRight();
      sidechainpartblock$neighbors.right().disconnectFromLeft();
   }

   default void updateSelfAndNeighborsOnPoweringUp(LevelAccessor p_433035_, BlockPos p_435882_, BlockState p_433411_, BlockState p_432995_) {
      if (this.isConnectable(p_433411_) && !this.isBeingUpdatedByNeighbor(p_433411_, p_432995_)) {
         SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(p_433035_, p_435882_, this.getFacing(p_433411_));
         SideChainPart sidechainpart = SideChainPart.UNCONNECTED;
         int i = sidechainpartblock$neighbors.left().isConnectable()
            ? this.getAllBlocksConnectedTo(p_433035_, sidechainpartblock$neighbors.left().pos()).size()
            : 0;
         int j = sidechainpartblock$neighbors.right().isConnectable()
            ? this.getAllBlocksConnectedTo(p_433035_, sidechainpartblock$neighbors.right().pos()).size()
            : 0;
         int k = 1;
         if (this.canConnect(i, k)) {
            sidechainpart = sidechainpart.whenConnectedToTheLeft();
            sidechainpartblock$neighbors.left().connectToTheRight();
            k += i;
         }

         if (this.canConnect(j, k)) {
            sidechainpart = sidechainpart.whenConnectedToTheRight();
            sidechainpartblock$neighbors.right().connectToTheLeft();
         }

         this.setPart(p_433035_, p_435882_, sidechainpart);
      }
   }

   private boolean canConnect(int p_434798_, int p_432790_) {
      return p_434798_ > 0 && p_432790_ + p_434798_ <= this.getMaxChainLength();
   }

   private boolean isBeingUpdatedByNeighbor(BlockState p_432818_, BlockState p_434637_) {
      boolean flag = this.getSideChainPart(p_432818_).isConnected();
      boolean flag1 = this.isConnectable(p_434637_) && this.getSideChainPart(p_434637_).isConnected();
      return flag || flag1;
   }

   private SideChainPartBlock.Neighbors getNeighbors(LevelAccessor p_435632_, BlockPos p_435119_, Direction p_434051_) {
      return new SideChainPartBlock.Neighbors(this, p_435632_, p_434051_, p_435119_, new HashMap<>());
   }

   default void setPart(LevelAccessor p_434157_, BlockPos p_435354_, SideChainPart p_434477_) {
      BlockState blockstate = p_434157_.getBlockState(p_435354_);
      if (this.getSideChainPart(blockstate) != p_434477_) {
         p_434157_.setBlock(p_435354_, this.setSideChainPart(blockstate, p_434477_), 3);
      }
   }

   public record EmptyNeighbor(BlockPos pos) implements SideChainPartBlock.Neighbor {
      @Override
      public boolean isConnectable() {
         return false;
      }

      @Override
      public boolean isUnconnectableOrChainEnd() {
         return true;
      }

      @Override
      public boolean connectsTowards(SideChainPart p_433702_) {
         return false;
      }
   }

   public sealed interface Neighbor permits SideChainPartBlock.EmptyNeighbor, SideChainPartBlock.SideChainNeighbor {
      BlockPos pos();

      boolean isConnectable();

      boolean isUnconnectableOrChainEnd();

      boolean connectsTowards(SideChainPart var1);

      default void connectToTheRight() {
      }

      default void connectToTheLeft() {
      }

      default void disconnectFromRight() {
      }

      default void disconnectFromLeft() {
      }
   }

   public record Neighbors(SideChainPartBlock block, LevelAccessor level, Direction facing, BlockPos center, Map<BlockPos, SideChainPartBlock.Neighbor> cache) {
      private boolean isConnectableToThisBlock(BlockState p_435544_) {
         return this.block.isConnectable(p_435544_) && this.block.getFacing(p_435544_) == this.facing;
      }

      private SideChainPartBlock.Neighbor createNewNeighbor(BlockPos p_433108_) {
         BlockState blockstate = this.level.getBlockState(p_433108_);
         SideChainPart sidechainpart = this.isConnectableToThisBlock(blockstate) ? this.block.getSideChainPart(blockstate) : null;
         return (SideChainPartBlock.Neighbor)(
            sidechainpart == null
               ? new SideChainPartBlock.EmptyNeighbor(p_433108_)
               : new SideChainPartBlock.SideChainNeighbor(this.level, this.block, p_433108_, sidechainpart)
         );
      }

      private SideChainPartBlock.Neighbor getOrCreateNeighbor(Direction p_433287_, Integer p_433172_) {
         return this.cache.computeIfAbsent(this.center.relative(p_433287_, p_433172_), this::createNewNeighbor);
      }

      public SideChainPartBlock.Neighbor left(int p_433481_) {
         return this.getOrCreateNeighbor(this.facing.getClockWise(), p_433481_);
      }

      public SideChainPartBlock.Neighbor right(int p_432981_) {
         return this.getOrCreateNeighbor(this.facing.getCounterClockWise(), p_432981_);
      }

      public SideChainPartBlock.Neighbor left() {
         return this.left(1);
      }

      public SideChainPartBlock.Neighbor right() {
         return this.right(1);
      }
   }

   public record SideChainNeighbor(LevelAccessor level, SideChainPartBlock block, BlockPos pos, SideChainPart part) implements SideChainPartBlock.Neighbor {
      @Override
      public boolean isConnectable() {
         return true;
      }

      @Override
      public boolean isUnconnectableOrChainEnd() {
         return this.part.isChainEnd();
      }

      @Override
      public boolean connectsTowards(SideChainPart p_434316_) {
         return this.part.isConnectionTowards(p_434316_);
      }

      @Override
      public void connectToTheRight() {
         this.block.setPart(this.level, this.pos, this.part.whenConnectedToTheRight());
      }

      @Override
      public void connectToTheLeft() {
         this.block.setPart(this.level, this.pos, this.part.whenConnectedToTheLeft());
      }

      @Override
      public void disconnectFromRight() {
         this.block.setPart(this.level, this.pos, this.part.whenDisconnectedFromTheRight());
      }

      @Override
      public void disconnectFromLeft() {
         this.block.setPart(this.level, this.pos, this.part.whenDisconnectedFromTheLeft());
      }
   }
}
