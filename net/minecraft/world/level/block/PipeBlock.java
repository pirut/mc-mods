package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class PipeBlock extends Block {
   private static final Direction[] DIRECTIONS = Direction.values();
   public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
   public static final BooleanProperty EAST = BlockStateProperties.EAST;
   public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
   public static final BooleanProperty WEST = BlockStateProperties.WEST;
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
   public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), p_55164_ -> {
      p_55164_.put(Direction.NORTH, NORTH);
      p_55164_.put(Direction.EAST, EAST);
      p_55164_.put(Direction.SOUTH, SOUTH);
      p_55164_.put(Direction.WEST, WEST);
      p_55164_.put(Direction.UP, UP);
      p_55164_.put(Direction.DOWN, DOWN);
   }));
   protected final VoxelShape[] shapeByIndex;

   public PipeBlock(float p_55159_, BlockBehaviour.Properties p_55160_) {
      super(p_55160_);
      this.shapeByIndex = this.makeShapes(p_55159_);
   }

   @Override
   protected abstract MapCodec<? extends PipeBlock> codec();

   private VoxelShape[] makeShapes(float p_55162_) {
      float $$1 = 0.5F - p_55162_;
      float $$2 = 0.5F + p_55162_;
      VoxelShape $$3 = Block.box($$1 * 16.0F, $$1 * 16.0F, $$1 * 16.0F, $$2 * 16.0F, $$2 * 16.0F, $$2 * 16.0F);
      VoxelShape[] $$4 = new VoxelShape[DIRECTIONS.length];

      for (int $$5 = 0; $$5 < DIRECTIONS.length; $$5++) {
         Direction $$6 = DIRECTIONS[$$5];
         $$4[$$5] = Shapes.box(
            0.5 + Math.min((double)(-p_55162_), $$6.getStepX() * 0.5),
            0.5 + Math.min((double)(-p_55162_), $$6.getStepY() * 0.5),
            0.5 + Math.min((double)(-p_55162_), $$6.getStepZ() * 0.5),
            0.5 + Math.max((double)p_55162_, $$6.getStepX() * 0.5),
            0.5 + Math.max((double)p_55162_, $$6.getStepY() * 0.5),
            0.5 + Math.max((double)p_55162_, $$6.getStepZ() * 0.5)
         );
      }

      VoxelShape[] $$7 = new VoxelShape[64];

      for (int $$8 = 0; $$8 < 64; $$8++) {
         VoxelShape $$9 = $$3;

         for (int $$10 = 0; $$10 < DIRECTIONS.length; $$10++) {
            if (($$8 & 1 << $$10) != 0) {
               $$9 = Shapes.or($$9, $$4[$$10]);
            }
         }

         $$7[$$8] = $$9;
      }

      return $$7;
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_55166_, BlockGetter p_55167_, BlockPos p_55168_) {
      return false;
   }

   @Override
   protected VoxelShape getShape(BlockState p_55170_, BlockGetter p_55171_, BlockPos p_55172_, CollisionContext p_55173_) {
      return this.shapeByIndex[this.getAABBIndex(p_55170_)];
   }

   protected int getAABBIndex(BlockState p_55175_) {
      int $$1 = 0;

      for (int $$2 = 0; $$2 < DIRECTIONS.length; $$2++) {
         if (p_55175_.getValue(PROPERTY_BY_DIRECTION.get(DIRECTIONS[$$2]))) {
            $$1 |= 1 << $$2;
         }
      }

      return $$1;
   }
}
