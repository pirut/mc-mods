package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class HugeMushroomBlock extends Block {
   public static final MapCodec<HugeMushroomBlock> CODEC = simpleCodec(HugeMushroomBlock::new);
   public static final BooleanProperty NORTH = PipeBlock.NORTH;
   public static final BooleanProperty EAST = PipeBlock.EAST;
   public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
   public static final BooleanProperty WEST = PipeBlock.WEST;
   public static final BooleanProperty UP = PipeBlock.UP;
   public static final BooleanProperty DOWN = PipeBlock.DOWN;
   private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

   @Override
   public MapCodec<HugeMushroomBlock> codec() {
      return CODEC;
   }

   public HugeMushroomBlock(BlockBehaviour.Properties p_54136_) {
      super(p_54136_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(NORTH, true)
            .setValue(EAST, true)
            .setValue(SOUTH, true)
            .setValue(WEST, true)
            .setValue(UP, true)
            .setValue(DOWN, true)
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_54138_) {
      BlockGetter $$1 = p_54138_.getLevel();
      BlockPos $$2 = p_54138_.getClickedPos();
      return this.defaultBlockState()
         .setValue(DOWN, !$$1.getBlockState($$2.below()).is(this))
         .setValue(UP, !$$1.getBlockState($$2.above()).is(this))
         .setValue(NORTH, !$$1.getBlockState($$2.north()).is(this))
         .setValue(EAST, !$$1.getBlockState($$2.east()).is(this))
         .setValue(SOUTH, !$$1.getBlockState($$2.south()).is(this))
         .setValue(WEST, !$$1.getBlockState($$2.west()).is(this));
   }

   @Override
   protected BlockState updateShape(BlockState p_54146_, Direction p_54147_, BlockState p_54148_, LevelAccessor p_54149_, BlockPos p_54150_, BlockPos p_54151_) {
      return p_54148_.is(this)
         ? p_54146_.setValue(PROPERTY_BY_DIRECTION.get(p_54147_), false)
         : super.updateShape(p_54146_, p_54147_, p_54148_, p_54149_, p_54150_, p_54151_);
   }

   @Override
   protected BlockState rotate(BlockState p_54143_, Rotation p_54144_) {
      return p_54143_.setValue(PROPERTY_BY_DIRECTION.get(p_54144_.rotate(Direction.NORTH)), p_54143_.getValue(NORTH))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54144_.rotate(Direction.SOUTH)), p_54143_.getValue(SOUTH))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54144_.rotate(Direction.EAST)), p_54143_.getValue(EAST))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54144_.rotate(Direction.WEST)), p_54143_.getValue(WEST))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54144_.rotate(Direction.UP)), p_54143_.getValue(UP))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54144_.rotate(Direction.DOWN)), p_54143_.getValue(DOWN));
   }

   @Override
   protected BlockState mirror(BlockState p_54140_, Mirror p_54141_) {
      return p_54140_.setValue(PROPERTY_BY_DIRECTION.get(p_54141_.mirror(Direction.NORTH)), p_54140_.getValue(NORTH))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54141_.mirror(Direction.SOUTH)), p_54140_.getValue(SOUTH))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54141_.mirror(Direction.EAST)), p_54140_.getValue(EAST))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54141_.mirror(Direction.WEST)), p_54140_.getValue(WEST))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54141_.mirror(Direction.UP)), p_54140_.getValue(UP))
         .setValue(PROPERTY_BY_DIRECTION.get(p_54141_.mirror(Direction.DOWN)), p_54140_.getValue(DOWN));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54153_) {
      p_54153_.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
   }
}
