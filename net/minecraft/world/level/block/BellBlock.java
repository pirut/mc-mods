package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BellBlock extends BaseEntityBlock {
   public static final MapCodec<BellBlock> CODEC = simpleCodec(BellBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<BellAttachType> ATTACHMENT = BlockStateProperties.BELL_ATTACHMENT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final VoxelShape NORTH_SOUTH_FLOOR_SHAPE = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
   private static final VoxelShape EAST_WEST_FLOOR_SHAPE = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);
   private static final VoxelShape BELL_TOP_SHAPE = Block.box(5.0, 6.0, 5.0, 11.0, 13.0, 11.0);
   private static final VoxelShape BELL_BOTTOM_SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 6.0, 12.0);
   private static final VoxelShape BELL_SHAPE = Shapes.or(BELL_BOTTOM_SHAPE, BELL_TOP_SHAPE);
   private static final VoxelShape NORTH_SOUTH_BETWEEN = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 0.0, 9.0, 15.0, 16.0));
   private static final VoxelShape EAST_WEST_BETWEEN = Shapes.or(BELL_SHAPE, Block.box(0.0, 13.0, 7.0, 16.0, 15.0, 9.0));
   private static final VoxelShape TO_WEST = Shapes.or(BELL_SHAPE, Block.box(0.0, 13.0, 7.0, 13.0, 15.0, 9.0));
   private static final VoxelShape TO_EAST = Shapes.or(BELL_SHAPE, Block.box(3.0, 13.0, 7.0, 16.0, 15.0, 9.0));
   private static final VoxelShape TO_NORTH = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 0.0, 9.0, 15.0, 13.0));
   private static final VoxelShape TO_SOUTH = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 3.0, 9.0, 15.0, 16.0));
   private static final VoxelShape CEILING_SHAPE = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 7.0, 9.0, 16.0, 9.0));
   public static final int EVENT_BELL_RING = 1;

   @Override
   public MapCodec<BellBlock> codec() {
      return CODEC;
   }

   public BellBlock(BlockBehaviour.Properties p_49696_) {
      super(p_49696_);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHMENT, BellAttachType.FLOOR).setValue(POWERED, false)
      );
   }

   @Override
   protected void neighborChanged(BlockState p_49729_, Level p_49730_, BlockPos p_49731_, Block p_49732_, BlockPos p_49733_, boolean p_49734_) {
      boolean $$6 = p_49730_.hasNeighborSignal(p_49731_);
      if ($$6 != p_49729_.getValue(POWERED)) {
         if ($$6) {
            this.attemptToRing(p_49730_, p_49731_, null);
         }

         p_49730_.setBlock(p_49731_, p_49729_.setValue(POWERED, $$6), 3);
      }
   }

   @Override
   protected void onProjectileHit(Level p_49708_, BlockState p_49709_, BlockHitResult p_49710_, Projectile p_49711_) {
      Entity $$4 = p_49711_.getOwner();
      Player $$5 = $$4 instanceof Player ? (Player)$$4 : null;
      this.onHit(p_49708_, p_49709_, p_49710_, $$5, true);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_49722_, Level p_49723_, BlockPos p_49724_, Player p_49725_, BlockHitResult p_49727_) {
      return this.onHit(p_49723_, p_49722_, p_49727_, p_49725_, true) ? InteractionResult.sidedSuccess(p_49723_.isClientSide) : InteractionResult.PASS;
   }

   public boolean onHit(Level p_49702_, BlockState p_49703_, BlockHitResult p_49704_, @Nullable Player p_49705_, boolean p_49706_) {
      Direction $$5 = p_49704_.getDirection();
      BlockPos $$6 = p_49704_.getBlockPos();
      boolean $$7 = !p_49706_ || this.isProperHit(p_49703_, $$5, p_49704_.getLocation().y - $$6.getY());
      if ($$7) {
         boolean $$8 = this.attemptToRing(p_49705_, p_49702_, $$6, $$5);
         if ($$8 && p_49705_ != null) {
            p_49705_.awardStat(Stats.BELL_RING);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isProperHit(BlockState p_49740_, Direction p_49741_, double p_49742_) {
      if (p_49741_.getAxis() != Direction.Axis.Y && !(p_49742_ > 0.8124F)) {
         Direction $$3 = p_49740_.getValue(FACING);
         BellAttachType $$4 = p_49740_.getValue(ATTACHMENT);
         switch ($$4) {
            case FLOOR:
               return $$3.getAxis() == p_49741_.getAxis();
            case SINGLE_WALL:
            case DOUBLE_WALL:
               return $$3.getAxis() != p_49741_.getAxis();
            case CEILING:
               return true;
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   public boolean attemptToRing(Level p_49713_, BlockPos p_49714_, @Nullable Direction p_49715_) {
      return this.attemptToRing(null, p_49713_, p_49714_, p_49715_);
   }

   public boolean attemptToRing(@Nullable Entity p_152189_, Level p_152190_, BlockPos p_152191_, @Nullable Direction p_152192_) {
      BlockEntity $$4 = p_152190_.getBlockEntity(p_152191_);
      if (!p_152190_.isClientSide && $$4 instanceof BellBlockEntity) {
         if (p_152192_ == null) {
            p_152192_ = p_152190_.getBlockState(p_152191_).getValue(FACING);
         }

         ((BellBlockEntity)$$4).onHit(p_152192_);
         p_152190_.playSound(null, p_152191_, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
         p_152190_.gameEvent(p_152189_, GameEvent.BLOCK_CHANGE, p_152191_);
         return true;
      } else {
         return false;
      }
   }

   private VoxelShape getVoxelShape(BlockState p_49767_) {
      Direction $$1 = p_49767_.getValue(FACING);
      BellAttachType $$2 = p_49767_.getValue(ATTACHMENT);
      if ($$2 == BellAttachType.FLOOR) {
         return $$1 != Direction.NORTH && $$1 != Direction.SOUTH ? EAST_WEST_FLOOR_SHAPE : NORTH_SOUTH_FLOOR_SHAPE;
      } else if ($$2 == BellAttachType.CEILING) {
         return CEILING_SHAPE;
      } else if ($$2 == BellAttachType.DOUBLE_WALL) {
         return $$1 != Direction.NORTH && $$1 != Direction.SOUTH ? EAST_WEST_BETWEEN : NORTH_SOUTH_BETWEEN;
      } else if ($$1 == Direction.NORTH) {
         return TO_NORTH;
      } else if ($$1 == Direction.SOUTH) {
         return TO_SOUTH;
      } else {
         return $$1 == Direction.EAST ? TO_EAST : TO_WEST;
      }
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_49760_, BlockGetter p_49761_, BlockPos p_49762_, CollisionContext p_49763_) {
      return this.getVoxelShape(p_49760_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_49755_, BlockGetter p_49756_, BlockPos p_49757_, CollisionContext p_49758_) {
      return this.getVoxelShape(p_49755_);
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_49753_) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_49698_) {
      Direction $$1 = p_49698_.getClickedFace();
      BlockPos $$2 = p_49698_.getClickedPos();
      Level $$3 = p_49698_.getLevel();
      Direction.Axis $$4 = $$1.getAxis();
      if ($$4 == Direction.Axis.Y) {
         BlockState $$5 = this.defaultBlockState()
            .setValue(ATTACHMENT, $$1 == Direction.DOWN ? BellAttachType.CEILING : BellAttachType.FLOOR)
            .setValue(FACING, p_49698_.getHorizontalDirection());
         if ($$5.canSurvive(p_49698_.getLevel(), $$2)) {
            return $$5;
         }
      } else {
         boolean $$6 = $$4 == Direction.Axis.X
               && $$3.getBlockState($$2.west()).isFaceSturdy($$3, $$2.west(), Direction.EAST)
               && $$3.getBlockState($$2.east()).isFaceSturdy($$3, $$2.east(), Direction.WEST)
            || $$4 == Direction.Axis.Z
               && $$3.getBlockState($$2.north()).isFaceSturdy($$3, $$2.north(), Direction.SOUTH)
               && $$3.getBlockState($$2.south()).isFaceSturdy($$3, $$2.south(), Direction.NORTH);
         BlockState $$7 = this.defaultBlockState()
            .setValue(FACING, $$1.getOpposite())
            .setValue(ATTACHMENT, $$6 ? BellAttachType.DOUBLE_WALL : BellAttachType.SINGLE_WALL);
         if ($$7.canSurvive(p_49698_.getLevel(), p_49698_.getClickedPos())) {
            return $$7;
         }

         boolean $$8 = $$3.getBlockState($$2.below()).isFaceSturdy($$3, $$2.below(), Direction.UP);
         $$7 = $$7.setValue(ATTACHMENT, $$8 ? BellAttachType.FLOOR : BellAttachType.CEILING);
         if ($$7.canSurvive(p_49698_.getLevel(), p_49698_.getClickedPos())) {
            return $$7;
         }
      }

      return null;
   }

   @Override
   protected void onExplosionHit(BlockState p_311884_, Level p_311798_, BlockPos p_312717_, Explosion p_312800_, BiConsumer<ItemStack, BlockPos> p_312339_) {
      if (p_312800_.canTriggerBlocks()) {
         this.attemptToRing(p_311798_, p_312717_, null);
      }

      super.onExplosionHit(p_311884_, p_311798_, p_312717_, p_312800_, p_312339_);
   }

   @Override
   protected BlockState updateShape(BlockState p_49744_, Direction p_49745_, BlockState p_49746_, LevelAccessor p_49747_, BlockPos p_49748_, BlockPos p_49749_) {
      BellAttachType $$6 = p_49744_.getValue(ATTACHMENT);
      Direction $$7 = getConnectedDirection(p_49744_).getOpposite();
      if ($$7 == p_49745_ && !p_49744_.canSurvive(p_49747_, p_49748_) && $$6 != BellAttachType.DOUBLE_WALL) {
         return Blocks.AIR.defaultBlockState();
      } else {
         if (p_49745_.getAxis() == p_49744_.getValue(FACING).getAxis()) {
            if ($$6 == BellAttachType.DOUBLE_WALL && !p_49746_.isFaceSturdy(p_49747_, p_49749_, p_49745_)) {
               return p_49744_.setValue(ATTACHMENT, BellAttachType.SINGLE_WALL).setValue(FACING, p_49745_.getOpposite());
            }

            if ($$6 == BellAttachType.SINGLE_WALL && $$7.getOpposite() == p_49745_ && p_49746_.isFaceSturdy(p_49747_, p_49749_, p_49744_.getValue(FACING))) {
               return p_49744_.setValue(ATTACHMENT, BellAttachType.DOUBLE_WALL);
            }
         }

         return super.updateShape(p_49744_, p_49745_, p_49746_, p_49747_, p_49748_, p_49749_);
      }
   }

   @Override
   protected boolean canSurvive(BlockState p_49736_, LevelReader p_49737_, BlockPos p_49738_) {
      Direction $$3 = getConnectedDirection(p_49736_).getOpposite();
      return $$3 == Direction.UP
         ? Block.canSupportCenter(p_49737_, p_49738_.above(), Direction.DOWN)
         : FaceAttachedHorizontalDirectionalBlock.canAttach(p_49737_, p_49738_, $$3);
   }

   private static Direction getConnectedDirection(BlockState p_49769_) {
      switch ((BellAttachType)p_49769_.getValue(ATTACHMENT)) {
         case FLOOR:
            return Direction.UP;
         case CEILING:
            return Direction.DOWN;
         default:
            return p_49769_.getValue(FACING).getOpposite();
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49751_) {
      p_49751_.add(FACING, ATTACHMENT, POWERED);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos p_152198_, BlockState p_152199_) {
      return new BellBlockEntity(p_152198_, p_152199_);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_152194_, BlockState p_152195_, BlockEntityType<T> p_152196_) {
      return createTickerHelper(p_152196_, BlockEntityType.BELL, p_152194_.isClientSide ? BellBlockEntity::clientTick : BellBlockEntity::serverTick);
   }

   @Override
   protected boolean isPathfindable(BlockState p_49717_, PathComputationType p_49720_) {
      return false;
   }

   @Override
   public BlockState rotate(BlockState p_304966_, Rotation p_304463_) {
      return p_304966_.setValue(FACING, p_304463_.rotate(p_304966_.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState p_304560_, Mirror p_304709_) {
      return p_304560_.rotate(p_304709_.getRotation(p_304560_.getValue(FACING)));
   }
}
