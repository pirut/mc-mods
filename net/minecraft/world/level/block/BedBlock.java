package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;

public class BedBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final MapCodec<BedBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_308801_ -> p_308801_.group(DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor), propertiesCodec()).apply(p_308801_, BedBlock::new)
   );
   public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
   public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
   protected static final int HEIGHT = 9;
   protected static final VoxelShape BASE = Block.box(0.0, 3.0, 0.0, 16.0, 9.0, 16.0);
   private static final int LEG_WIDTH = 3;
   protected static final VoxelShape LEG_NORTH_WEST = Block.box(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
   protected static final VoxelShape LEG_SOUTH_WEST = Block.box(0.0, 0.0, 13.0, 3.0, 3.0, 16.0);
   protected static final VoxelShape LEG_NORTH_EAST = Block.box(13.0, 0.0, 0.0, 16.0, 3.0, 3.0);
   protected static final VoxelShape LEG_SOUTH_EAST = Block.box(13.0, 0.0, 13.0, 16.0, 3.0, 16.0);
   protected static final VoxelShape NORTH_SHAPE = Shapes.or(BASE, LEG_NORTH_WEST, LEG_NORTH_EAST);
   protected static final VoxelShape SOUTH_SHAPE = Shapes.or(BASE, LEG_SOUTH_WEST, LEG_SOUTH_EAST);
   protected static final VoxelShape WEST_SHAPE = Shapes.or(BASE, LEG_NORTH_WEST, LEG_SOUTH_WEST);
   protected static final VoxelShape EAST_SHAPE = Shapes.or(BASE, LEG_NORTH_EAST, LEG_SOUTH_EAST);
   private final DyeColor color;

   @Override
   public MapCodec<BedBlock> codec() {
      return CODEC;
   }

   public BedBlock(DyeColor p_49454_, BlockBehaviour.Properties p_49455_) {
      super(p_49455_);
      this.color = p_49454_;
      this.registerDefaultState(this.stateDefinition.any().setValue(PART, BedPart.FOOT).setValue(OCCUPIED, false));
   }

   @Nullable
   public static Direction getBedOrientation(BlockGetter p_49486_, BlockPos p_49487_) {
      BlockState $$2 = p_49486_.getBlockState(p_49487_);
      return $$2.getBlock() instanceof BedBlock ? $$2.getValue(FACING) : null;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_49515_, Level p_49516_, BlockPos p_49517_, Player p_49518_, BlockHitResult p_49520_) {
      if (p_49516_.isClientSide) {
         return InteractionResult.CONSUME;
      } else {
         if (p_49515_.getValue(PART) != BedPart.HEAD) {
            p_49517_ = p_49517_.relative(p_49515_.getValue(FACING));
            p_49515_ = p_49516_.getBlockState(p_49517_);
            if (!p_49515_.is(this)) {
               return InteractionResult.CONSUME;
            }
         }

         if (!canSetSpawn(p_49516_)) {
            p_49516_.removeBlock(p_49517_, false);
            BlockPos $$5 = p_49517_.relative(p_49515_.getValue(FACING).getOpposite());
            if (p_49516_.getBlockState($$5).is(this)) {
               p_49516_.removeBlock($$5, false);
            }

            Vec3 $$6 = p_49517_.getCenter();
            p_49516_.explode(null, p_49516_.damageSources().badRespawnPointExplosion($$6), null, $$6, 5.0F, true, Level.ExplosionInteraction.BLOCK);
            return InteractionResult.SUCCESS;
         } else if (p_49515_.getValue(OCCUPIED)) {
            if (!this.kickVillagerOutOfBed(p_49516_, p_49517_)) {
               p_49518_.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            }

            return InteractionResult.SUCCESS;
         } else {
            p_49518_.startSleepInBed(p_49517_).ifLeft(p_49477_ -> {
               if (p_49477_.getMessage() != null) {
                  p_49518_.displayClientMessage(p_49477_.getMessage(), true);
               }
            });
            return InteractionResult.SUCCESS;
         }
      }
   }

   public static boolean canSetSpawn(Level p_49489_) {
      return p_49489_.dimensionType().bedWorks();
   }

   private boolean kickVillagerOutOfBed(Level p_49491_, BlockPos p_49492_) {
      List<Villager> $$2 = p_49491_.getEntitiesOfClass(Villager.class, new AABB(p_49492_), LivingEntity::isSleeping);
      if ($$2.isEmpty()) {
         return false;
      } else {
         $$2.get(0).stopSleeping();
         return true;
      }
   }

   @Override
   public void fallOn(Level p_152169_, BlockState p_152170_, BlockPos p_152171_, Entity p_152172_, float p_152173_) {
      super.fallOn(p_152169_, p_152170_, p_152171_, p_152172_, p_152173_ * 0.5F);
   }

   @Override
   public void updateEntityAfterFallOn(BlockGetter p_49483_, Entity p_49484_) {
      if (p_49484_.isSuppressingBounce()) {
         super.updateEntityAfterFallOn(p_49483_, p_49484_);
      } else {
         this.bounceUp(p_49484_);
      }
   }

   private void bounceUp(Entity p_49457_) {
      Vec3 $$1 = p_49457_.getDeltaMovement();
      if ($$1.y < 0.0) {
         double $$2 = p_49457_ instanceof LivingEntity ? 1.0 : 0.8;
         p_49457_.setDeltaMovement($$1.x, -$$1.y * 0.66F * $$2, $$1.z);
      }
   }

   @Override
   protected BlockState updateShape(BlockState p_49525_, Direction p_49526_, BlockState p_49527_, LevelAccessor p_49528_, BlockPos p_49529_, BlockPos p_49530_) {
      if (p_49526_ == getNeighbourDirection(p_49525_.getValue(PART), p_49525_.getValue(FACING))) {
         return p_49527_.is(this) && p_49527_.getValue(PART) != p_49525_.getValue(PART)
            ? p_49525_.setValue(OCCUPIED, p_49527_.getValue(OCCUPIED))
            : Blocks.AIR.defaultBlockState();
      } else {
         return super.updateShape(p_49525_, p_49526_, p_49527_, p_49528_, p_49529_, p_49530_);
      }
   }

   private static Direction getNeighbourDirection(BedPart p_49534_, Direction p_49535_) {
      return p_49534_ == BedPart.FOOT ? p_49535_ : p_49535_.getOpposite();
   }

   @Override
   public BlockState playerWillDestroy(Level p_49505_, BlockPos p_49506_, BlockState p_49507_, Player p_49508_) {
      if (!p_49505_.isClientSide && p_49508_.isCreative()) {
         BedPart $$4 = p_49507_.getValue(PART);
         if ($$4 == BedPart.FOOT) {
            BlockPos $$5 = p_49506_.relative(getNeighbourDirection($$4, p_49507_.getValue(FACING)));
            BlockState $$6 = p_49505_.getBlockState($$5);
            if ($$6.is(this) && $$6.getValue(PART) == BedPart.HEAD) {
               p_49505_.setBlock($$5, Blocks.AIR.defaultBlockState(), 35);
               p_49505_.levelEvent(p_49508_, 2001, $$5, Block.getId($$6));
            }
         }
      }

      return super.playerWillDestroy(p_49505_, p_49506_, p_49507_, p_49508_);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_49479_) {
      Direction $$1 = p_49479_.getHorizontalDirection();
      BlockPos $$2 = p_49479_.getClickedPos();
      BlockPos $$3 = $$2.relative($$1);
      Level $$4 = p_49479_.getLevel();
      return $$4.getBlockState($$3).canBeReplaced(p_49479_) && $$4.getWorldBorder().isWithinBounds($$3) ? this.defaultBlockState().setValue(FACING, $$1) : null;
   }

   @Override
   protected VoxelShape getShape(BlockState p_49547_, BlockGetter p_49548_, BlockPos p_49549_, CollisionContext p_49550_) {
      Direction $$4 = getConnectedDirection(p_49547_).getOpposite();
      switch ($$4) {
         case NORTH:
            return NORTH_SHAPE;
         case SOUTH:
            return SOUTH_SHAPE;
         case WEST:
            return WEST_SHAPE;
         default:
            return EAST_SHAPE;
      }
   }

   public static Direction getConnectedDirection(BlockState p_49558_) {
      Direction $$1 = p_49558_.getValue(FACING);
      return p_49558_.getValue(PART) == BedPart.HEAD ? $$1.getOpposite() : $$1;
   }

   public static DoubleBlockCombiner.BlockType getBlockType(BlockState p_49560_) {
      BedPart $$1 = p_49560_.getValue(PART);
      return $$1 == BedPart.HEAD ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
   }

   private static boolean isBunkBed(BlockGetter p_49542_, BlockPos p_49543_) {
      return p_49542_.getBlockState(p_49543_.below()).getBlock() instanceof BedBlock;
   }

   public static Optional<Vec3> findStandUpPosition(
      EntityType<?> p_261547_, CollisionGetter p_261946_, BlockPos p_261614_, Direction p_261648_, float p_261680_
   ) {
      Direction $$5 = p_261648_.getClockWise();
      Direction $$6 = $$5.isFacingAngle(p_261680_) ? $$5.getOpposite() : $$5;
      if (isBunkBed(p_261946_, p_261614_)) {
         return findBunkBedStandUpPosition(p_261547_, p_261946_, p_261614_, p_261648_, $$6);
      } else {
         int[][] $$7 = bedStandUpOffsets(p_261648_, $$6);
         Optional<Vec3> $$8 = findStandUpPositionAtOffset(p_261547_, p_261946_, p_261614_, $$7, true);
         return $$8.isPresent() ? $$8 : findStandUpPositionAtOffset(p_261547_, p_261946_, p_261614_, $$7, false);
      }
   }

   private static Optional<Vec3> findBunkBedStandUpPosition(
      EntityType<?> p_49464_, CollisionGetter p_49465_, BlockPos p_49466_, Direction p_49467_, Direction p_49468_
   ) {
      int[][] $$5 = bedSurroundStandUpOffsets(p_49467_, p_49468_);
      Optional<Vec3> $$6 = findStandUpPositionAtOffset(p_49464_, p_49465_, p_49466_, $$5, true);
      if ($$6.isPresent()) {
         return $$6;
      } else {
         BlockPos $$7 = p_49466_.below();
         Optional<Vec3> $$8 = findStandUpPositionAtOffset(p_49464_, p_49465_, $$7, $$5, true);
         if ($$8.isPresent()) {
            return $$8;
         } else {
            int[][] $$9 = bedAboveStandUpOffsets(p_49467_);
            Optional<Vec3> $$10 = findStandUpPositionAtOffset(p_49464_, p_49465_, p_49466_, $$9, true);
            if ($$10.isPresent()) {
               return $$10;
            } else {
               Optional<Vec3> $$11 = findStandUpPositionAtOffset(p_49464_, p_49465_, p_49466_, $$5, false);
               if ($$11.isPresent()) {
                  return $$11;
               } else {
                  Optional<Vec3> $$12 = findStandUpPositionAtOffset(p_49464_, p_49465_, $$7, $$5, false);
                  return $$12.isPresent() ? $$12 : findStandUpPositionAtOffset(p_49464_, p_49465_, p_49466_, $$9, false);
               }
            }
         }
      }
   }

   private static Optional<Vec3> findStandUpPositionAtOffset(
      EntityType<?> p_49470_, CollisionGetter p_49471_, BlockPos p_49472_, int[][] p_49473_, boolean p_49474_
   ) {
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

      for (int[] $$6 : p_49473_) {
         $$5.set(p_49472_.getX() + $$6[0], p_49472_.getY(), p_49472_.getZ() + $$6[1]);
         Vec3 $$7 = DismountHelper.findSafeDismountLocation(p_49470_, p_49471_, $$5, p_49474_);
         if ($$7 != null) {
            return Optional.of($$7);
         }
      }

      return Optional.empty();
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_49545_) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49532_) {
      p_49532_.add(FACING, PART, OCCUPIED);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_152175_, BlockState p_152176_) {
      return new BedBlockEntity(p_152175_, p_152176_, this.color);
   }

   @Override
   public void setPlacedBy(Level p_49499_, BlockPos p_49500_, BlockState p_49501_, @Nullable LivingEntity p_49502_, ItemStack p_49503_) {
      super.setPlacedBy(p_49499_, p_49500_, p_49501_, p_49502_, p_49503_);
      if (!p_49499_.isClientSide) {
         BlockPos $$5 = p_49500_.relative(p_49501_.getValue(FACING));
         p_49499_.setBlock($$5, p_49501_.setValue(PART, BedPart.HEAD), 3);
         p_49499_.blockUpdated(p_49500_, Blocks.AIR);
         p_49501_.updateNeighbourShapes(p_49499_, p_49500_, 3);
      }
   }

   public DyeColor getColor() {
      return this.color;
   }

   @Override
   protected long getSeed(BlockState p_49522_, BlockPos p_49523_) {
      BlockPos $$2 = p_49523_.relative(p_49522_.getValue(FACING), p_49522_.getValue(PART) == BedPart.HEAD ? 0 : 1);
      return Mth.getSeed($$2.getX(), p_49523_.getY(), $$2.getZ());
   }

   @Override
   protected boolean isPathfindable(BlockState p_49510_, PathComputationType p_49513_) {
      return false;
   }

   private static int[][] bedStandUpOffsets(Direction p_49539_, Direction p_49540_) {
      return (int[][])ArrayUtils.addAll(bedSurroundStandUpOffsets(p_49539_, p_49540_), bedAboveStandUpOffsets(p_49539_));
   }

   private static int[][] bedSurroundStandUpOffsets(Direction p_49552_, Direction p_49553_) {
      return new int[][]{
         {p_49553_.getStepX(), p_49553_.getStepZ()},
         {p_49553_.getStepX() - p_49552_.getStepX(), p_49553_.getStepZ() - p_49552_.getStepZ()},
         {p_49553_.getStepX() - p_49552_.getStepX() * 2, p_49553_.getStepZ() - p_49552_.getStepZ() * 2},
         {-p_49552_.getStepX() * 2, -p_49552_.getStepZ() * 2},
         {-p_49553_.getStepX() - p_49552_.getStepX() * 2, -p_49553_.getStepZ() - p_49552_.getStepZ() * 2},
         {-p_49553_.getStepX() - p_49552_.getStepX(), -p_49553_.getStepZ() - p_49552_.getStepZ()},
         {-p_49553_.getStepX(), -p_49553_.getStepZ()},
         {-p_49553_.getStepX() + p_49552_.getStepX(), -p_49553_.getStepZ() + p_49552_.getStepZ()},
         {p_49552_.getStepX(), p_49552_.getStepZ()},
         {p_49553_.getStepX() + p_49552_.getStepX(), p_49553_.getStepZ() + p_49552_.getStepZ()}
      };
   }

   private static int[][] bedAboveStandUpOffsets(Direction p_49537_) {
      return new int[][]{{0, 0}, {-p_49537_.getStepX(), -p_49537_.getStepZ()}};
   }
}
