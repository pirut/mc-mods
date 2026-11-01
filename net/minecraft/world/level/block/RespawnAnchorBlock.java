package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RespawnAnchorBlock extends Block {
   public static final MapCodec<RespawnAnchorBlock> CODEC = simpleCodec(RespawnAnchorBlock::new);
   public static final int MIN_CHARGES = 0;
   public static final int MAX_CHARGES = 4;
   public static final IntegerProperty CHARGE = BlockStateProperties.RESPAWN_ANCHOR_CHARGES;
   private static final ImmutableList<Vec3i> RESPAWN_HORIZONTAL_OFFSETS = ImmutableList.of(
      new Vec3i(0, 0, -1),
      new Vec3i(-1, 0, 0),
      new Vec3i(0, 0, 1),
      new Vec3i(1, 0, 0),
      new Vec3i(-1, 0, -1),
      new Vec3i(1, 0, -1),
      new Vec3i(-1, 0, 1),
      new Vec3i(1, 0, 1)
   );
   private static final ImmutableList<Vec3i> RESPAWN_OFFSETS = new Builder()
      .addAll(RESPAWN_HORIZONTAL_OFFSETS)
      .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator())
      .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator())
      .add(new Vec3i(0, 1, 0))
      .build();

   @Override
   public MapCodec<RespawnAnchorBlock> codec() {
      return CODEC;
   }

   public RespawnAnchorBlock(BlockBehaviour.Properties p_55838_) {
      super(p_55838_);
      this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack p_316531_, BlockState p_316665_, Level p_316165_, BlockPos p_316402_, Player p_316556_, InteractionHand p_316586_, BlockHitResult p_316326_
   ) {
      if (isRespawnFuel(p_316531_) && canBeCharged(p_316665_)) {
         charge(p_316556_, p_316165_, p_316402_, p_316665_);
         p_316531_.consume(1, p_316556_);
         return ItemInteractionResult.sidedSuccess(p_316165_.isClientSide);
      } else {
         return p_316586_ == InteractionHand.MAIN_HAND && isRespawnFuel(p_316556_.getItemInHand(InteractionHand.OFF_HAND)) && canBeCharged(p_316665_)
            ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_316377_, Level p_316150_, BlockPos p_316161_, Player p_316889_, BlockHitResult p_316358_) {
      if (p_316377_.getValue(CHARGE) == 0) {
         return InteractionResult.PASS;
      } else if (!canSetSpawn(p_316150_)) {
         if (!p_316150_.isClientSide) {
            this.explode(p_316377_, p_316150_, p_316161_);
         }

         return InteractionResult.sidedSuccess(p_316150_.isClientSide);
      } else {
         if (!p_316150_.isClientSide) {
            ServerPlayer $$5 = (ServerPlayer)p_316889_;
            if ($$5.getRespawnDimension() != p_316150_.dimension() || !p_316161_.equals($$5.getRespawnPosition())) {
               $$5.setRespawnPosition(p_316150_.dimension(), p_316161_, 0.0F, false, true);
               p_316150_.playSound(
                  null,
                  p_316161_.getX() + 0.5,
                  p_316161_.getY() + 0.5,
                  p_316161_.getZ() + 0.5,
                  SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
                  SoundSource.BLOCKS,
                  1.0F,
                  1.0F
               );
               return InteractionResult.SUCCESS;
            }
         }

         return InteractionResult.CONSUME;
      }
   }

   private static boolean isRespawnFuel(ItemStack p_55849_) {
      return p_55849_.is(Items.GLOWSTONE);
   }

   private static boolean canBeCharged(BlockState p_55895_) {
      return p_55895_.getValue(CHARGE) < 4;
   }

   private static boolean isWaterThatWouldFlow(BlockPos p_55888_, Level p_55889_) {
      FluidState $$2 = p_55889_.getFluidState(p_55888_);
      if (!$$2.is(FluidTags.WATER)) {
         return false;
      } else if ($$2.isSource()) {
         return true;
      } else {
         float $$3 = $$2.getAmount();
         if ($$3 < 2.0F) {
            return false;
         } else {
            FluidState $$4 = p_55889_.getFluidState(p_55888_.below());
            return !$$4.is(FluidTags.WATER);
         }
      }
   }

   private void explode(BlockState p_55891_, Level p_55892_, final BlockPos p_55893_) {
      p_55892_.removeBlock(p_55893_, false);
      boolean $$3 = Direction.Plane.HORIZONTAL.stream().map(p_55893_::relative).anyMatch(p_55854_ -> isWaterThatWouldFlow(p_55854_, p_55892_));
      final boolean $$4 = $$3 || p_55892_.getFluidState(p_55893_.above()).is(FluidTags.WATER);
      ExplosionDamageCalculator $$5 = new ExplosionDamageCalculator() {
         @Override
         public Optional<Float> getBlockExplosionResistance(
            Explosion p_55904_, BlockGetter p_55905_, BlockPos p_55906_, BlockState p_55907_, FluidState p_55908_
         ) {
            return p_55906_.equals(p_55893_) && $$4
               ? Optional.of(Blocks.WATER.getExplosionResistance())
               : super.getBlockExplosionResistance(p_55904_, p_55905_, p_55906_, p_55907_, p_55908_);
         }
      };
      Vec3 $$6 = p_55893_.getCenter();
      p_55892_.explode(null, p_55892_.damageSources().badRespawnPointExplosion($$6), $$5, $$6, 5.0F, true, Level.ExplosionInteraction.BLOCK);
   }

   public static boolean canSetSpawn(Level p_55851_) {
      return p_55851_.dimensionType().respawnAnchorWorks();
   }

   public static void charge(@Nullable Entity p_270997_, Level p_270172_, BlockPos p_270534_, BlockState p_270661_) {
      BlockState $$4 = p_270661_.setValue(CHARGE, p_270661_.getValue(CHARGE) + 1);
      p_270172_.setBlock(p_270534_, $$4, 3);
      p_270172_.gameEvent(GameEvent.BLOCK_CHANGE, p_270534_, GameEvent.Context.of(p_270997_, $$4));
      p_270172_.playSound(
         null, p_270534_.getX() + 0.5, p_270534_.getY() + 0.5, p_270534_.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F
      );
   }

   @Override
   public void animateTick(BlockState p_221969_, Level p_221970_, BlockPos p_221971_, RandomSource p_221972_) {
      if (p_221969_.getValue(CHARGE) != 0) {
         if (p_221972_.nextInt(100) == 0) {
            p_221970_.playLocalSound(p_221971_, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         double $$4 = p_221971_.getX() + 0.5 + (0.5 - p_221972_.nextDouble());
         double $$5 = p_221971_.getY() + 1.0;
         double $$6 = p_221971_.getZ() + 0.5 + (0.5 - p_221972_.nextDouble());
         double $$7 = p_221972_.nextFloat() * 0.04;
         p_221970_.addParticle(ParticleTypes.REVERSE_PORTAL, $$4, $$5, $$6, 0.0, $$7, 0.0);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55886_) {
      p_55886_.add(CHARGE);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_55860_) {
      return true;
   }

   public static int getScaledChargeLevel(BlockState p_55862_, int p_55863_) {
      return Mth.floor((p_55862_.getValue(CHARGE) - 0) / 4.0F * p_55863_);
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_55870_, Level p_55871_, BlockPos p_55872_) {
      return getScaledChargeLevel(p_55870_, 15);
   }

   public static Optional<Vec3> findStandUpPosition(EntityType<?> p_55840_, CollisionGetter p_55841_, BlockPos p_55842_) {
      Optional<Vec3> $$3 = findStandUpPosition(p_55840_, p_55841_, p_55842_, true);
      return $$3.isPresent() ? $$3 : findStandUpPosition(p_55840_, p_55841_, p_55842_, false);
   }

   private static Optional<Vec3> findStandUpPosition(EntityType<?> p_55844_, CollisionGetter p_55845_, BlockPos p_55846_, boolean p_55847_) {
      BlockPos.MutableBlockPos $$4 = new BlockPos.MutableBlockPos();
      UnmodifiableIterator var5 = RESPAWN_OFFSETS.iterator();

      while (var5.hasNext()) {
         Vec3i $$5 = (Vec3i)var5.next();
         $$4.set(p_55846_).move($$5);
         Vec3 $$6 = DismountHelper.findSafeDismountLocation(p_55844_, p_55845_, $$4, p_55847_);
         if ($$6 != null) {
            return Optional.of($$6);
         }
      }

      return Optional.empty();
   }

   @Override
   protected boolean isPathfindable(BlockState p_55865_, PathComputationType p_55868_) {
      return false;
   }
}
