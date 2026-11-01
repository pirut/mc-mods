package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FenceBlock extends CrossCollisionBlock {
   public static final MapCodec<FenceBlock> CODEC = simpleCodec(FenceBlock::new);
   private final VoxelShape[] occlusionByIndex;

   @Override
   public MapCodec<FenceBlock> codec() {
      return CODEC;
   }

   public FenceBlock(BlockBehaviour.Properties p_53302_) {
      super(2.0F, 2.0F, 16.0F, 16.0F, 24.0F, p_53302_);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
      );
      this.occlusionByIndex = this.makeShapes(2.0F, 1.0F, 16.0F, 6.0F, 15.0F);
   }

   @Override
   protected VoxelShape getOcclusionShape(BlockState p_53338_, BlockGetter p_53339_, BlockPos p_53340_) {
      return this.occlusionByIndex[this.getAABBIndex(p_53338_)];
   }

   @Override
   protected VoxelShape getVisualShape(BlockState p_53311_, BlockGetter p_53312_, BlockPos p_53313_, CollisionContext p_53314_) {
      return this.getShape(p_53311_, p_53312_, p_53313_, p_53314_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_53306_, PathComputationType p_53309_) {
      return false;
   }

   public boolean connectsTo(BlockState p_53330_, boolean p_53331_, Direction p_53332_) {
      Block $$3 = p_53330_.getBlock();
      boolean $$4 = this.isSameFence(p_53330_);
      boolean $$5 = $$3 instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(p_53330_, p_53332_);
      return !isExceptionForConnection(p_53330_) && p_53331_ || $$4 || $$5;
   }

   private boolean isSameFence(BlockState p_153255_) {
      return p_153255_.is(BlockTags.FENCES) && p_153255_.is(BlockTags.WOODEN_FENCES) == this.defaultBlockState().is(BlockTags.WOODEN_FENCES);
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack p_316871_, BlockState p_316305_, Level p_316763_, BlockPos p_316504_, Player p_316364_, InteractionHand p_316507_, BlockHitResult p_316760_
   ) {
      if (p_316763_.isClientSide) {
         return p_316871_.is(Items.LEAD) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      } else {
         return super.useItemOn(p_316871_, p_316305_, p_316763_, p_316504_, p_316364_, p_316507_, p_316760_);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_316111_, Level p_316725_, BlockPos p_316646_, Player p_316453_, BlockHitResult p_316769_) {
      return !p_316725_.isClientSide() ? LeadItem.bindPlayerMobs(p_316453_, p_316725_, p_316646_) : InteractionResult.PASS;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_53304_) {
      BlockGetter $$1 = p_53304_.getLevel();
      BlockPos $$2 = p_53304_.getClickedPos();
      FluidState $$3 = p_53304_.getLevel().getFluidState(p_53304_.getClickedPos());
      BlockPos $$4 = $$2.north();
      BlockPos $$5 = $$2.east();
      BlockPos $$6 = $$2.south();
      BlockPos $$7 = $$2.west();
      BlockState $$8 = $$1.getBlockState($$4);
      BlockState $$9 = $$1.getBlockState($$5);
      BlockState $$10 = $$1.getBlockState($$6);
      BlockState $$11 = $$1.getBlockState($$7);
      return super.getStateForPlacement(p_53304_)
         .setValue(NORTH, this.connectsTo($$8, $$8.isFaceSturdy($$1, $$4, Direction.SOUTH), Direction.SOUTH))
         .setValue(EAST, this.connectsTo($$9, $$9.isFaceSturdy($$1, $$5, Direction.WEST), Direction.WEST))
         .setValue(SOUTH, this.connectsTo($$10, $$10.isFaceSturdy($$1, $$6, Direction.NORTH), Direction.NORTH))
         .setValue(WEST, this.connectsTo($$11, $$11.isFaceSturdy($$1, $$7, Direction.EAST), Direction.EAST))
         .setValue(WATERLOGGED, $$3.getType() == Fluids.WATER);
   }

   @Override
   protected BlockState updateShape(BlockState p_53323_, Direction p_53324_, BlockState p_53325_, LevelAccessor p_53326_, BlockPos p_53327_, BlockPos p_53328_) {
      if (p_53323_.getValue(WATERLOGGED)) {
         p_53326_.scheduleTick(p_53327_, Fluids.WATER, Fluids.WATER.getTickDelay(p_53326_));
      }

      return p_53324_.getAxis().getPlane() == Direction.Plane.HORIZONTAL
         ? p_53323_.setValue(
            PROPERTY_BY_DIRECTION.get(p_53324_),
            this.connectsTo(p_53325_, p_53325_.isFaceSturdy(p_53326_, p_53328_, p_53324_.getOpposite()), p_53324_.getOpposite())
         )
         : super.updateShape(p_53323_, p_53324_, p_53325_, p_53326_, p_53327_, p_53328_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_53334_) {
      p_53334_.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}
