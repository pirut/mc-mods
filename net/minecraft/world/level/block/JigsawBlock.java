package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;

public class JigsawBlock extends Block implements EntityBlock, GameMasterBlock {
   public static final MapCodec<JigsawBlock> CODEC = simpleCodec(JigsawBlock::new);
   public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;

   @Override
   public MapCodec<JigsawBlock> codec() {
      return CODEC;
   }

   public JigsawBlock(BlockBehaviour.Properties p_54225_) {
      super(p_54225_);
      this.registerDefaultState(this.stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54244_) {
      p_54244_.add(ORIENTATION);
   }

   @Override
   protected BlockState rotate(BlockState p_54241_, Rotation p_54242_) {
      return p_54241_.setValue(ORIENTATION, p_54242_.rotation().rotate(p_54241_.getValue(ORIENTATION)));
   }

   @Override
   protected BlockState mirror(BlockState p_54238_, Mirror p_54239_) {
      return p_54238_.setValue(ORIENTATION, p_54239_.rotation().rotate(p_54238_.getValue(ORIENTATION)));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_54227_) {
      Direction $$1 = p_54227_.getClickedFace();
      Direction $$2;
      if ($$1.getAxis() == Direction.Axis.Y) {
         $$2 = p_54227_.getHorizontalDirection().getOpposite();
      } else {
         $$2 = Direction.UP;
      }

      return this.defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop($$1, $$2));
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153448_, BlockState p_153449_) {
      return new JigsawBlockEntity(p_153448_, p_153449_);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_54231_, Level p_54232_, BlockPos p_54233_, Player p_54234_, BlockHitResult p_54236_) {
      BlockEntity $$5 = p_54232_.getBlockEntity(p_54233_);
      if ($$5 instanceof JigsawBlockEntity && p_54234_.canUseGameMasterBlocks()) {
         p_54234_.openJigsawBlock((JigsawBlockEntity)$$5);
         return InteractionResult.sidedSuccess(p_54232_.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public static boolean canAttach(StructureTemplate.StructureBlockInfo p_54246_, StructureTemplate.StructureBlockInfo p_54247_) {
      Direction $$2 = getFrontFacing(p_54246_.state());
      Direction $$3 = getFrontFacing(p_54247_.state());
      Direction $$4 = getTopFacing(p_54246_.state());
      Direction $$5 = getTopFacing(p_54247_.state());
      JigsawBlockEntity.JointType $$6 = JigsawBlockEntity.JointType.byName(p_54246_.nbt().getString("joint"))
         .orElseGet(() -> $$2.getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE);
      boolean $$7 = $$6 == JigsawBlockEntity.JointType.ROLLABLE;
      return $$2 == $$3.getOpposite() && ($$7 || $$4 == $$5) && p_54246_.nbt().getString("target").equals(p_54247_.nbt().getString("name"));
   }

   public static Direction getFrontFacing(BlockState p_54251_) {
      return p_54251_.getValue(ORIENTATION).front();
   }

   public static Direction getTopFacing(BlockState p_54253_) {
      return p_54253_.getValue(ORIENTATION).top();
   }
}
