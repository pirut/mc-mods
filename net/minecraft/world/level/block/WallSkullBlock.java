package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallSkullBlock extends AbstractSkullBlock {
   public static final MapCodec<WallSkullBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_308848_ -> p_308848_.group(SkullBlock.Type.CODEC.fieldOf("kind").forGetter(AbstractSkullBlock::getType), propertiesCodec())
         .apply(p_308848_, WallSkullBlock::new)
   );
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final Map<Direction, VoxelShape> AABBS = Maps.newEnumMap(
      ImmutableMap.of(
         Direction.NORTH,
         Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0),
         Direction.SOUTH,
         Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0),
         Direction.EAST,
         Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0),
         Direction.WEST,
         Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0)
      )
   );

   @Override
   public MapCodec<? extends WallSkullBlock> codec() {
      return CODEC;
   }

   public WallSkullBlock(SkullBlock.Type p_58101_, BlockBehaviour.Properties p_58102_) {
      super(p_58101_, p_58102_);
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   @Override
   public String getDescriptionId() {
      return this.asItem().getDescriptionId();
   }

   @Override
   protected VoxelShape getShape(BlockState p_58114_, BlockGetter p_58115_, BlockPos p_58116_, CollisionContext p_58117_) {
      return AABBS.get(p_58114_.getValue(FACING));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_58104_) {
      BlockState $$1 = super.getStateForPlacement(p_58104_);
      BlockGetter $$2 = p_58104_.getLevel();
      BlockPos $$3 = p_58104_.getClickedPos();
      Direction[] $$4 = p_58104_.getNearestLookingDirections();

      for (Direction $$5 : $$4) {
         if ($$5.getAxis().isHorizontal()) {
            Direction $$6 = $$5.getOpposite();
            $$1 = $$1.setValue(FACING, $$6);
            if (!$$2.getBlockState($$3.relative($$5)).canBeReplaced(p_58104_)) {
               return $$1;
            }
         }
      }

      return null;
   }

   @Override
   protected BlockState rotate(BlockState p_58109_, Rotation p_58110_) {
      return p_58109_.setValue(FACING, p_58110_.rotate(p_58109_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_58106_, Mirror p_58107_) {
      return p_58106_.rotate(p_58107_.getRotation(p_58106_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_58112_) {
      super.createBlockStateDefinition(p_58112_);
      p_58112_.add(FACING);
   }
}
