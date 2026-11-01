package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class SculkVeinBlock extends MultifaceBlock implements SculkBehaviour, SimpleWaterloggedBlock {
   public static final MapCodec<SculkVeinBlock> CODEC = simpleCodec(SculkVeinBlock::new);
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.DEFAULT_SPREAD_ORDER));
   private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(
      new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType.SAME_POSITION)
   );

   @Override
   public MapCodec<SculkVeinBlock> codec() {
      return CODEC;
   }

   public SculkVeinBlock(BlockBehaviour.Properties p_222353_) {
      super(p_222353_);
      this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   @Override
   public MultifaceSpreader getSpreader() {
      return this.veinSpreader;
   }

   public MultifaceSpreader getSameSpaceSpreader() {
      return this.sameSpaceSpreader;
   }

   public static boolean regrow(LevelAccessor p_222364_, BlockPos p_222365_, BlockState p_222366_, Collection<Direction> p_222367_) {
      boolean $$4 = false;
      BlockState $$5 = Blocks.SCULK_VEIN.defaultBlockState();

      for (Direction $$6 : p_222367_) {
         BlockPos $$7 = p_222365_.relative($$6);
         if (canAttachTo(p_222364_, $$6, $$7, p_222364_.getBlockState($$7))) {
            $$5 = $$5.setValue(getFaceProperty($$6), true);
            $$4 = true;
         }
      }

      if (!$$4) {
         return false;
      } else {
         if (!p_222366_.getFluidState().isEmpty()) {
            $$5 = $$5.setValue(WATERLOGGED, true);
         }

         p_222364_.setBlock(p_222365_, $$5, 3);
         return true;
      }
   }

   @Override
   public void onDischarged(LevelAccessor p_222359_, BlockState p_222360_, BlockPos p_222361_, RandomSource p_222362_) {
      if (p_222360_.is(this)) {
         for (Direction $$4 : DIRECTIONS) {
            BooleanProperty $$5 = getFaceProperty($$4);
            if (p_222360_.getValue($$5) && p_222359_.getBlockState(p_222361_.relative($$4)).is(Blocks.SCULK)) {
               p_222360_ = p_222360_.setValue($$5, false);
            }
         }

         if (!hasAnyFace(p_222360_)) {
            FluidState $$6 = p_222359_.getFluidState(p_222361_);
            p_222360_ = ($$6.isEmpty() ? Blocks.AIR : Blocks.WATER).defaultBlockState();
         }

         p_222359_.setBlock(p_222361_, p_222360_, 3);
         SculkBehaviour.super.onDischarged(p_222359_, p_222360_, p_222361_, p_222362_);
      }
   }

   @Override
   public int attemptUseCharge(
      SculkSpreader.ChargeCursor p_222369_, LevelAccessor p_222370_, BlockPos p_222371_, RandomSource p_222372_, SculkSpreader p_222373_, boolean p_222374_
   ) {
      if (p_222374_ && this.attemptPlaceSculk(p_222373_, p_222370_, p_222369_.getPos(), p_222372_)) {
         return p_222369_.getCharge() - 1;
      } else {
         return p_222372_.nextInt(p_222373_.chargeDecayRate()) == 0 ? Mth.floor(p_222369_.getCharge() * 0.5F) : p_222369_.getCharge();
      }
   }

   private boolean attemptPlaceSculk(SculkSpreader p_222376_, LevelAccessor p_222377_, BlockPos p_222378_, RandomSource p_222379_) {
      BlockState $$4 = p_222377_.getBlockState(p_222378_);
      TagKey<Block> $$5 = p_222376_.replaceableBlocks();

      for (Direction $$6 : Direction.allShuffled(p_222379_)) {
         if (hasFace($$4, $$6)) {
            BlockPos $$7 = p_222378_.relative($$6);
            BlockState $$8 = p_222377_.getBlockState($$7);
            if ($$8.is($$5)) {
               BlockState $$9 = Blocks.SCULK.defaultBlockState();
               p_222377_.setBlock($$7, $$9, 3);
               Block.pushEntitiesUp($$8, $$9, p_222377_, $$7);
               p_222377_.playSound(null, $$7, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
               this.veinSpreader.spreadAll($$9, p_222377_, $$7, p_222376_.isWorldGeneration());
               Direction $$10 = $$6.getOpposite();

               for (Direction $$11 : DIRECTIONS) {
                  if ($$11 != $$10) {
                     BlockPos $$12 = $$7.relative($$11);
                     BlockState $$13 = p_222377_.getBlockState($$12);
                     if ($$13.is(this)) {
                        this.onDischarged(p_222377_, $$13, $$12, p_222379_);
                     }
                  }
               }

               return true;
            }
         }
      }

      return false;
   }

   public static boolean hasSubstrateAccess(LevelAccessor p_222355_, BlockState p_222356_, BlockPos p_222357_) {
      if (!p_222356_.is(Blocks.SCULK_VEIN)) {
         return false;
      } else {
         for (Direction $$3 : DIRECTIONS) {
            if (hasFace(p_222356_, $$3) && p_222355_.getBlockState(p_222357_.relative($$3)).is(BlockTags.SCULK_REPLACEABLE)) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_222384_, Direction p_222385_, BlockState p_222386_, LevelAccessor p_222387_, BlockPos p_222388_, BlockPos p_222389_
   ) {
      if (p_222384_.getValue(WATERLOGGED)) {
         p_222387_.scheduleTick(p_222388_, Fluids.WATER, Fluids.WATER.getTickDelay(p_222387_));
      }

      return super.updateShape(p_222384_, p_222385_, p_222386_, p_222387_, p_222388_, p_222389_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_222391_) {
      super.createBlockStateDefinition(p_222391_);
      p_222391_.add(WATERLOGGED);
   }

   @Override
   protected boolean canBeReplaced(BlockState p_222381_, BlockPlaceContext p_222382_) {
      return !p_222382_.getItemInHand().is(Items.SCULK_VEIN) || super.canBeReplaced(p_222381_, p_222382_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_222394_) {
      return p_222394_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_222394_);
   }

   class SculkVeinSpreaderConfig extends MultifaceSpreader.DefaultSpreaderConfig {
      private final MultifaceSpreader.SpreadType[] spreadTypes;

      public SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType... p_222402_) {
         super(SculkVeinBlock.this);
         this.spreadTypes = p_222402_;
      }

      @Override
      public boolean stateCanBeReplaced(BlockGetter p_222405_, BlockPos p_222406_, BlockPos p_222407_, Direction p_222408_, BlockState p_222409_) {
         BlockState $$5 = p_222405_.getBlockState(p_222407_.relative(p_222408_));
         if (!$$5.is(Blocks.SCULK) && !$$5.is(Blocks.SCULK_CATALYST) && !$$5.is(Blocks.MOVING_PISTON)) {
            if (p_222406_.distManhattan(p_222407_) == 2) {
               BlockPos $$6 = p_222406_.relative(p_222408_.getOpposite());
               if (p_222405_.getBlockState($$6).isFaceSturdy(p_222405_, $$6, p_222408_)) {
                  return false;
               }
            }

            FluidState $$7 = p_222409_.getFluidState();
            if (!$$7.isEmpty() && !$$7.is(Fluids.WATER)) {
               return false;
            } else {
               return p_222409_.is(BlockTags.FIRE)
                  ? false
                  : p_222409_.canBeReplaced() || super.stateCanBeReplaced(p_222405_, p_222406_, p_222407_, p_222408_, p_222409_);
            }
         } else {
            return false;
         }
      }

      @Override
      public MultifaceSpreader.SpreadType[] getSpreadTypes() {
         return this.spreadTypes;
      }

      @Override
      public boolean isOtherBlockValidAsSource(BlockState p_222411_) {
         return !p_222411_.is(Blocks.SCULK_VEIN);
      }
   }
}
