package net.minecraft.world.level.material;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public abstract class LavaFluid extends FlowingFluid {
   public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

   @Override
   public Fluid getFlowing() {
      return Fluids.FLOWING_LAVA;
   }

   @Override
   public Fluid getSource() {
      return Fluids.LAVA;
   }

   @Override
   public Item getBucket() {
      return Items.LAVA_BUCKET;
   }

   @Override
   public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
      BlockPos blockPos = pos.above();
      if (level.getBlockState(blockPos).isAir() && !level.getBlockState(blockPos).isSolidRender()) {
         if (random.nextInt(100) == 0) {
            double d = pos.getX() + random.nextDouble();
            double e = pos.getY() + 1.0;
            double f = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.LAVA, d, e, f, 0.0, 0.0, 0.0);
            level.playLocalSound(d, e, f, SoundEvents.LAVA_POP, SoundSource.AMBIENT, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
         }

         if (random.nextInt(200) == 0) {
            level.playLocalSound(
               pos.getX(),
               pos.getY(),
               pos.getZ(),
               SoundEvents.LAVA_AMBIENT,
               SoundSource.AMBIENT,
               0.2F + random.nextFloat() * 0.2F,
               0.9F + random.nextFloat() * 0.15F,
               false
            );
         }
      }
   }

   @Override
   public void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
      if (level.canSpreadFireAround(pos)) {
         int i = random.nextInt(3);
         if (i > 0) {
            BlockPos blockPos = pos;

            for (int j = 0; j < i; j++) {
               blockPos = blockPos.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
               if (!level.isLoaded(blockPos)) {
                  return;
               }

               BlockState blockState = level.getBlockState(blockPos);
               if (blockState.isAir()) {
                  if (this.hasFlammableNeighbours(level, blockPos)) {
                     level.setBlockAndUpdate(blockPos, BaseFireBlock.getState(level, blockPos));
                     return;
                  }
               } else if (blockState.blocksMotion()) {
                  return;
               }
            }
         } else {
            for (int k = 0; k < 3; k++) {
               BlockPos blockPos2 = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
               if (!level.isLoaded(blockPos2)) {
                  return;
               }

               if (level.isEmptyBlock(blockPos2.above()) && this.isFlammable(level, blockPos2)) {
                  level.setBlockAndUpdate(blockPos2.above(), BaseFireBlock.getState(level, blockPos2));
               }
            }
         }
      }
   }

   @Override
   protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
      effectApplier.apply(InsideBlockEffectType.CLEAR_FREEZE);
      effectApplier.apply(InsideBlockEffectType.LAVA_IGNITE);
      effectApplier.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
   }

   private boolean hasFlammableNeighbours(LevelReader level, BlockPos pos) {
      for (Direction direction : Direction.values()) {
         if (this.isFlammable(level, pos.relative(direction))) {
            return true;
         }
      }

      return false;
   }

   private boolean isFlammable(LevelReader level, BlockPos pos) {
      return level.isInsideBuildHeight(pos.getY()) && !level.hasChunkAt(pos) ? false : level.getBlockState(pos).ignitedByLava();
   }

   @Nullable
   @Override
   public ParticleOptions getDripParticle() {
      return ParticleTypes.DRIPPING_LAVA;
   }

   @Override
   protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
      this.fizz(level, pos);
   }

   @Override
   public int getSlopeFindDistance(LevelReader level) {
      return isFastLava(level) ? 4 : 2;
   }

   @Override
   public BlockState createLegacyBlock(FluidState state) {
      return Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
   }

   @Override
   public boolean isSame(Fluid fluid) {
      return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
   }

   @Override
   public int getDropOff(LevelReader level) {
      return isFastLava(level) ? 1 : 2;
   }

   @Override
   public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
      return state.getHeight(level, pos) >= 0.44444445F && fluid.is(FluidTags.WATER);
   }

   @Override
   public int getTickDelay(LevelReader level) {
      return isFastLava(level) ? 10 : 30;
   }

   @Override
   public int getSpreadDelay(Level level, BlockPos pos, FluidState currentState, FluidState newState) {
      int i = this.getTickDelay(level);
      if (!currentState.isEmpty()
         && !newState.isEmpty()
         && !currentState.getValue(FALLING)
         && !newState.getValue(FALLING)
         && newState.getHeight(level, pos) > currentState.getHeight(level, pos)
         && level.getRandom().nextInt(4) != 0) {
         i *= 4;
      }

      return i;
   }

   private void fizz(LevelAccessor level, BlockPos pos) {
      level.levelEvent(1501, pos, 0);
   }

   @Override
   protected boolean canConvertToSource(ServerLevel level) {
      return level.getGameRules().get(GameRules.LAVA_SOURCE_CONVERSION);
   }

   @Override
   protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
      if (direction == Direction.DOWN) {
         FluidState fluidState2 = level.getFluidState(pos);
         if (this.is(FluidTags.LAVA) && fluidState2.is(FluidTags.WATER)) {
            if (blockState.getBlock() instanceof LiquidBlock) {
               level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            }

            this.fizz(level, pos);
            return;
         }
      }

      super.spreadTo(level, pos, blockState, direction, fluidState);
   }

   @Override
   protected boolean isRandomlyTicking() {
      return true;
   }

   @Override
   protected float getExplosionResistance() {
      return 100.0F;
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
   }

   private static boolean isFastLava(LevelReader level) {
      return level.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA);
   }

   public static class Flowing extends LavaFluid {
      @Override
      protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
         super.createFluidStateDefinition(builder);
         builder.add(LEVEL);
      }

      @Override
      public int getAmount(FluidState state) {
         return state.getValue(LEVEL);
      }

      @Override
      public boolean isSource(FluidState state) {
         return false;
      }
   }

   public static class Source extends LavaFluid {
      @Override
      public int getAmount(FluidState state) {
         return 8;
      }

      @Override
      public boolean isSource(FluidState state) {
         return true;
      }
   }
}
