package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
   public static final MapCodec<ConcretePowderBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("concrete").forGetter(concretePowderBlock -> concretePowderBlock.concrete), propertiesCodec()
         )
         .apply(instance, ConcretePowderBlock::new)
   );
   private final Block concrete;

   @Override
   public MapCodec<ConcretePowderBlock> codec() {
      return CODEC;
   }

   public ConcretePowderBlock(Block concrete, BlockBehaviour.Properties properties) {
      super(properties);
      this.concrete = concrete;
   }

   @Override
   public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaceableState, FallingBlockEntity fallingBlock) {
      if (shouldSolidify(level, pos, replaceableState)) {
         level.setBlock(pos, this.concrete.defaultBlockState(), 3);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockGetter blockGetter = context.getLevel();
      BlockPos blockPos = context.getClickedPos();
      BlockState blockState = blockGetter.getBlockState(blockPos);
      return shouldSolidify(blockGetter, blockPos, blockState) ? this.concrete.defaultBlockState() : super.getStateForPlacement(context);
   }

   private static boolean shouldSolidify(BlockGetter level, BlockPos pos, BlockState state) {
      return canSolidify(state) || touchesLiquid(level, pos);
   }

   private static boolean touchesLiquid(BlockGetter level, BlockPos pos) {
      boolean bl = false;
      BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

      for (Direction direction : Direction.values()) {
         BlockState blockState = level.getBlockState(mutableBlockPos);
         if (direction != Direction.DOWN || canSolidify(blockState)) {
            mutableBlockPos.setWithOffset(pos, direction);
            blockState = level.getBlockState(mutableBlockPos);
            if (canSolidify(blockState) && !blockState.isFaceSturdy(level, pos, direction.getOpposite())) {
               bl = true;
               break;
            }
         }
      }

      return bl;
   }

   private static boolean canSolidify(BlockState state) {
      return state.getFluidState().is(FluidTags.WATER);
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
      return touchesLiquid(level, pos)
         ? this.concrete.defaultBlockState()
         : super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
   }

   @Override
   public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getMapColor(level, pos).col;
   }
}
