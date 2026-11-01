package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LecternBlock extends BaseEntityBlock {
   public static final MapCodec<LecternBlock> CODEC = simpleCodec(LecternBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
   public static final VoxelShape SHAPE_BASE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
   public static final VoxelShape SHAPE_POST = Block.box(4.0, 2.0, 4.0, 12.0, 14.0, 12.0);
   public static final VoxelShape SHAPE_COMMON = Shapes.or(SHAPE_BASE, SHAPE_POST);
   public static final VoxelShape SHAPE_TOP_PLATE = Block.box(0.0, 15.0, 0.0, 16.0, 15.0, 16.0);
   public static final VoxelShape SHAPE_COLLISION = Shapes.or(SHAPE_COMMON, SHAPE_TOP_PLATE);
   public static final VoxelShape SHAPE_WEST = Shapes.or(
      Block.box(1.0, 10.0, 0.0, 5.333333, 14.0, 16.0),
      Block.box(5.333333, 12.0, 0.0, 9.666667, 16.0, 16.0),
      Block.box(9.666667, 14.0, 0.0, 14.0, 18.0, 16.0),
      SHAPE_COMMON
   );
   public static final VoxelShape SHAPE_NORTH = Shapes.or(
      Block.box(0.0, 10.0, 1.0, 16.0, 14.0, 5.333333),
      Block.box(0.0, 12.0, 5.333333, 16.0, 16.0, 9.666667),
      Block.box(0.0, 14.0, 9.666667, 16.0, 18.0, 14.0),
      SHAPE_COMMON
   );
   public static final VoxelShape SHAPE_EAST = Shapes.or(
      Block.box(10.666667, 10.0, 0.0, 15.0, 14.0, 16.0),
      Block.box(6.333333, 12.0, 0.0, 10.666667, 16.0, 16.0),
      Block.box(2.0, 14.0, 0.0, 6.333333, 18.0, 16.0),
      SHAPE_COMMON
   );
   public static final VoxelShape SHAPE_SOUTH = Shapes.or(
      Block.box(0.0, 10.0, 10.666667, 16.0, 14.0, 15.0),
      Block.box(0.0, 12.0, 6.333333, 16.0, 16.0, 10.666667),
      Block.box(0.0, 14.0, 2.0, 16.0, 18.0, 6.333333),
      SHAPE_COMMON
   );
   private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

   @Override
   public MapCodec<LecternBlock> codec() {
      return CODEC;
   }

   public LecternBlock(BlockBehaviour.Properties p_54479_) {
      super(p_54479_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(HAS_BOOK, false));
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_54559_) {
      return RenderShape.MODEL;
   }

   @Override
   protected VoxelShape getOcclusionShape(BlockState p_54584_, BlockGetter p_54585_, BlockPos p_54586_) {
      return SHAPE_COMMON;
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_54582_) {
      return true;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_54481_) {
      Level $$1 = p_54481_.getLevel();
      ItemStack $$2 = p_54481_.getItemInHand();
      Player $$3 = p_54481_.getPlayer();
      boolean $$4 = false;
      if (!$$1.isClientSide && $$3 != null && $$3.canUseGameMasterBlocks()) {
         CustomData $$5 = $$2.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
         if ($$5.contains("Book")) {
            $$4 = true;
         }
      }

      return this.defaultBlockState().setValue(FACING, p_54481_.getHorizontalDirection().getOpposite()).setValue(HAS_BOOK, $$4);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_54577_, BlockGetter p_54578_, BlockPos p_54579_, CollisionContext p_54580_) {
      return SHAPE_COLLISION;
   }

   @Override
   protected VoxelShape getShape(BlockState p_54561_, BlockGetter p_54562_, BlockPos p_54563_, CollisionContext p_54564_) {
      switch ((Direction)p_54561_.getValue(FACING)) {
         case NORTH:
            return SHAPE_NORTH;
         case SOUTH:
            return SHAPE_SOUTH;
         case EAST:
            return SHAPE_EAST;
         case WEST:
            return SHAPE_WEST;
         default:
            return SHAPE_COMMON;
      }
   }

   @Override
   protected BlockState rotate(BlockState p_54540_, Rotation p_54541_) {
      return p_54540_.setValue(FACING, p_54541_.rotate(p_54540_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_54537_, Mirror p_54538_) {
      return p_54537_.rotate(p_54538_.getRotation(p_54537_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54543_) {
      p_54543_.add(FACING, POWERED, HAS_BOOK);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153573_, BlockState p_153574_) {
      return new LecternBlockEntity(p_153573_, p_153574_);
   }

   public static boolean tryPlaceBook(@Nullable LivingEntity p_347473_, Level p_270604_, BlockPos p_270276_, BlockState p_270445_, ItemStack p_270458_) {
      if (!p_270445_.getValue(HAS_BOOK)) {
         if (!p_270604_.isClientSide) {
            placeBook(p_347473_, p_270604_, p_270276_, p_270445_, p_270458_);
         }

         return true;
      } else {
         return false;
      }
   }

   private static void placeBook(@Nullable LivingEntity p_347484_, Level p_270065_, BlockPos p_270155_, BlockState p_270753_, ItemStack p_270173_) {
      if (p_270065_.getBlockEntity(p_270155_) instanceof LecternBlockEntity $$6) {
         $$6.setBook(p_270173_.consumeAndReturn(1, p_347484_));
         resetBookState(p_347484_, p_270065_, p_270155_, p_270753_, true);
         p_270065_.playSound(null, p_270155_, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   public static void resetBookState(@Nullable Entity p_270231_, Level p_270114_, BlockPos p_270251_, BlockState p_270758_, boolean p_270452_) {
      BlockState $$5 = p_270758_.setValue(POWERED, false).setValue(HAS_BOOK, p_270452_);
      p_270114_.setBlock(p_270251_, $$5, 3);
      p_270114_.gameEvent(GameEvent.BLOCK_CHANGE, p_270251_, GameEvent.Context.of(p_270231_, $$5));
      updateBelow(p_270114_, p_270251_, p_270758_);
   }

   public static void signalPageChange(Level p_54489_, BlockPos p_54490_, BlockState p_54491_) {
      changePowered(p_54489_, p_54490_, p_54491_, true);
      p_54489_.scheduleTick(p_54490_, p_54491_.getBlock(), 2);
      p_54489_.levelEvent(1043, p_54490_, 0);
   }

   private static void changePowered(Level p_54554_, BlockPos p_54555_, BlockState p_54556_, boolean p_54557_) {
      p_54554_.setBlock(p_54555_, p_54556_.setValue(POWERED, p_54557_), 3);
      updateBelow(p_54554_, p_54555_, p_54556_);
   }

   private static void updateBelow(Level p_54545_, BlockPos p_54546_, BlockState p_54547_) {
      p_54545_.updateNeighborsAt(p_54546_.below(), p_54547_.getBlock());
   }

   @Override
   protected void tick(BlockState p_221388_, ServerLevel p_221389_, BlockPos p_221390_, RandomSource p_221391_) {
      changePowered(p_221389_, p_221390_, p_221388_, false);
   }

   @Override
   protected void onRemove(BlockState p_54531_, Level p_54532_, BlockPos p_54533_, BlockState p_54534_, boolean p_54535_) {
      if (!p_54531_.is(p_54534_.getBlock())) {
         if (p_54531_.getValue(HAS_BOOK)) {
            this.popBook(p_54531_, p_54532_, p_54533_);
         }

         super.onRemove(p_54531_, p_54532_, p_54533_, p_54534_, p_54535_);
         if (p_54531_.getValue(POWERED)) {
            p_54532_.updateNeighborsAt(p_54533_.below(), this);
         }
      }
   }

   private void popBook(BlockState p_54588_, Level p_54589_, BlockPos p_54590_) {
      if (p_54589_.getBlockEntity(p_54590_) instanceof LecternBlockEntity $$4) {
         Direction $$5 = p_54588_.getValue(FACING);
         ItemStack $$6 = $$4.getBook().copy();
         float $$7 = 0.25F * $$5.getStepX();
         float $$8 = 0.25F * $$5.getStepZ();
         ItemEntity $$9 = new ItemEntity(p_54589_, p_54590_.getX() + 0.5 + $$7, p_54590_.getY() + 1, p_54590_.getZ() + 0.5 + $$8, $$6);
         $$9.setDefaultPickUpDelay();
         p_54589_.addFreshEntity($$9);
         $$4.clearContent();
      }
   }

   @Override
   protected boolean isSignalSource(BlockState p_54575_) {
      return true;
   }

   @Override
   protected int getSignal(BlockState p_54515_, BlockGetter p_54516_, BlockPos p_54517_, Direction p_54518_) {
      return p_54515_.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected int getDirectSignal(BlockState p_54566_, BlockGetter p_54567_, BlockPos p_54568_, Direction p_54569_) {
      return p_54569_ == Direction.UP && p_54566_.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_54503_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_54520_, Level p_54521_, BlockPos p_54522_) {
      if (p_54520_.getValue(HAS_BOOK)) {
         BlockEntity $$3 = p_54521_.getBlockEntity(p_54522_);
         if ($$3 instanceof LecternBlockEntity) {
            return ((LecternBlockEntity)$$3).getRedstoneSignal();
         }
      }

      return 0;
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack p_316392_, BlockState p_316600_, Level p_316640_, BlockPos p_316673_, Player p_316670_, InteractionHand p_316384_, BlockHitResult p_316419_
   ) {
      if (p_316600_.getValue(HAS_BOOK)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (p_316392_.is(ItemTags.LECTERN_BOOKS)) {
         return tryPlaceBook(p_316670_, p_316640_, p_316673_, p_316600_, p_316392_)
            ? ItemInteractionResult.sidedSuccess(p_316640_.isClientSide)
            : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      } else {
         return p_316392_.isEmpty() && p_316384_ == InteractionHand.MAIN_HAND
            ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_316164_, Level p_316515_, BlockPos p_316598_, Player p_316584_, BlockHitResult p_316197_) {
      if (p_316164_.getValue(HAS_BOOK)) {
         if (!p_316515_.isClientSide) {
            this.openScreen(p_316515_, p_316598_, p_316584_);
         }

         return InteractionResult.sidedSuccess(p_316515_.isClientSide);
      } else {
         return InteractionResult.CONSUME;
      }
   }

   @Nullable
   @Override
   protected MenuProvider getMenuProvider(BlockState p_54571_, Level p_54572_, BlockPos p_54573_) {
      return !p_54571_.getValue(HAS_BOOK) ? null : super.getMenuProvider(p_54571_, p_54572_, p_54573_);
   }

   private void openScreen(Level p_54485_, BlockPos p_54486_, Player p_54487_) {
      BlockEntity $$3 = p_54485_.getBlockEntity(p_54486_);
      if ($$3 instanceof LecternBlockEntity) {
         p_54487_.openMenu((LecternBlockEntity)$$3);
         p_54487_.awardStat(Stats.INTERACT_WITH_LECTERN);
      }
   }

   @Override
   protected boolean isPathfindable(BlockState p_54510_, PathComputationType p_54513_) {
      return false;
   }
}
