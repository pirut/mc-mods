package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SugarCaneBlock extends Block {
   public static final MapCodec<SugarCaneBlock> CODEC = simpleCodec(SugarCaneBlock::new);
   public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 16.0);

   @Override
   public MapCodec<SugarCaneBlock> codec() {
      return CODEC;
   }

   protected SugarCaneBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!state.canSurvive(level, pos)) {
         level.destroyBlock(pos, true);
      }
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (level.isEmptyBlock(pos.above())) {
         int i = 1;

         while (level.getBlockState(pos.below(i)).is(this)) {
            i++;
         }

         if (i < 3) {
            int j = state.getValue(AGE);
            if (j == 15) {
               level.setBlockAndUpdate(pos.above(), this.defaultBlockState());
               level.setBlock(pos, state.setValue(AGE, 0), 260);
            } else {
               level.setBlock(pos, state.setValue(AGE, j + 1), 260);
            }
         }
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess scheduledTickAccess,
      BlockPos pos,
      Direction direction,
      BlockPos neighborPos,
      BlockState neighborState,
      RandomSource random
   ) {
      if (!state.canSurvive(level, pos)) {
         scheduledTickAccess.scheduleTick(pos, this, 1);
      }

      return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      BlockState blockState = level.getBlockState(pos.below());
      if (blockState.is(this)) {
         return true;
      } else {
         if (blockState.is(BlockTags.DIRT) || blockState.is(BlockTags.SAND)) {
            BlockPos blockPos = pos.below();

            for (Direction direction : Direction.Plane.HORIZONTAL) {
               BlockState blockState2 = level.getBlockState(blockPos.relative(direction));
               FluidState fluidState = level.getFluidState(blockPos.relative(direction));
               if (fluidState.is(FluidTags.WATER) || blockState2.is(Blocks.FROSTED_ICE)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AGE);
   }
}
