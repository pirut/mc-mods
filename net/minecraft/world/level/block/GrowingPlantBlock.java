package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantBlock extends Block {
   protected final Direction growthDirection;
   protected final boolean scheduleFluidTicks;
   protected final VoxelShape shape;

   protected GrowingPlantBlock(BlockBehaviour.Properties p_53863_, Direction p_53864_, VoxelShape p_53865_, boolean p_53866_) {
      super(p_53863_);
      this.growthDirection = p_53864_;
      this.shape = p_53865_;
      this.scheduleFluidTicks = p_53866_;
   }

   @Override
   protected abstract MapCodec<? extends GrowingPlantBlock> codec();

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_53868_) {
      BlockState $$1 = p_53868_.getLevel().getBlockState(p_53868_.getClickedPos().relative(this.growthDirection));
      return !$$1.is(this.getHeadBlock()) && !$$1.is(this.getBodyBlock())
         ? this.getStateForPlacement(p_53868_.getLevel())
         : this.getBodyBlock().defaultBlockState();
   }

   public BlockState getStateForPlacement(LevelAccessor p_53869_) {
      return this.defaultBlockState();
   }

   @Override
   protected boolean canSurvive(BlockState p_53876_, LevelReader p_53877_, BlockPos p_53878_) {
      BlockPos $$3 = p_53878_.relative(this.growthDirection.getOpposite());
      BlockState $$4 = p_53877_.getBlockState($$3);
      return !this.canAttachTo($$4)
         ? false
         : $$4.is(this.getHeadBlock()) || $$4.is(this.getBodyBlock()) || $$4.isFaceSturdy(p_53877_, $$3, this.growthDirection);
   }

   @Override
   protected void tick(BlockState p_221280_, ServerLevel p_221281_, BlockPos p_221282_, RandomSource p_221283_) {
      if (!p_221280_.canSurvive(p_221281_, p_221282_)) {
         p_221281_.destroyBlock(p_221282_, true);
      }
   }

   protected boolean canAttachTo(BlockState p_153321_) {
      return true;
   }

   @Override
   protected VoxelShape getShape(BlockState p_53880_, BlockGetter p_53881_, BlockPos p_53882_, CollisionContext p_53883_) {
      return this.shape;
   }

   protected abstract GrowingPlantHeadBlock getHeadBlock();

   protected abstract Block getBodyBlock();
}
