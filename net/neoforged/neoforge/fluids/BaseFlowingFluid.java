package net.neoforged.neoforge.fluids;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.SoundActions;
import org.jspecify.annotations.Nullable;

public abstract class BaseFlowingFluid extends FlowingFluid {
   private final Supplier<? extends FluidType> fluidType;
   private final Supplier<? extends Fluid> flowing;
   private final Supplier<? extends Fluid> still;
   @Nullable
   private final Supplier<? extends Item> bucket;
   @Nullable
   private final Supplier<? extends LiquidBlock> block;
   private final int slopeFindDistance;
   private final int levelDecreasePerBlock;
   private final float explosionResistance;
   private final int tickRate;

   protected BaseFlowingFluid(BaseFlowingFluid.Properties properties) {
      this.fluidType = properties.fluidType;
      this.flowing = properties.flowing;
      this.still = properties.still;
      this.bucket = properties.bucket;
      this.block = properties.block;
      this.slopeFindDistance = properties.slopeFindDistance;
      this.levelDecreasePerBlock = properties.levelDecreasePerBlock;
      this.explosionResistance = properties.explosionResistance;
      this.tickRate = properties.tickRate;
   }

   @Override
   public FluidType getFluidType() {
      return this.fluidType.get();
   }

   @Override
   public Fluid getFlowing() {
      return this.flowing.get();
   }

   @Override
   public Fluid getSource() {
      return this.still.get();
   }

   @Override
   protected boolean canConvertToSource(ServerLevel level) {
      return false;
   }

   @Override
   public boolean canConvertToSource(FluidState state, ServerLevel level, BlockPos pos) {
      return this.getFluidType().canConvertToSource(state, level, pos);
   }

   @Override
   protected void beforeDestroyingBlock(LevelAccessor worldIn, BlockPos pos, BlockState state) {
      BlockEntity blockEntity = state.hasBlockEntity() ? worldIn.getBlockEntity(pos) : null;
      Block.dropResources(state, worldIn, pos, blockEntity);
   }

   @Override
   protected int getSlopeFindDistance(LevelReader worldIn) {
      return this.slopeFindDistance;
   }

   @Override
   protected int getDropOff(LevelReader worldIn) {
      return this.levelDecreasePerBlock;
   }

   @Override
   public Item getBucket() {
      return this.bucket != null ? this.bucket.get() : Items.AIR;
   }

   @Override
   protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluidIn, Direction direction) {
      return direction == Direction.DOWN && !this.isSame(fluidIn);
   }

   @Override
   public int getTickDelay(LevelReader level) {
      return this.tickRate;
   }

   @Override
   protected float getExplosionResistance() {
      return this.explosionResistance;
   }

   @Override
   protected BlockState createLegacyBlock(FluidState state) {
      return this.block != null ? this.block.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state)) : Blocks.AIR.defaultBlockState();
   }

   @Override
   public boolean isSame(Fluid fluidIn) {
      return fluidIn == this.still.get() || fluidIn == this.flowing.get();
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.ofNullable(this.getFluidType().getSound(SoundActions.BUCKET_FILL));
   }

   public static class Flowing extends BaseFlowingFluid {
      public Flowing(BaseFlowingFluid.Properties properties) {
         super(properties);
         this.registerDefaultState(this.getStateDefinition().any().setValue(LEVEL, 7));
      }

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

   public static class Properties {
      private Supplier<? extends FluidType> fluidType;
      private Supplier<? extends Fluid> still;
      private Supplier<? extends Fluid> flowing;
      private Supplier<? extends Item> bucket;
      private Supplier<? extends LiquidBlock> block;
      private int slopeFindDistance = 4;
      private int levelDecreasePerBlock = 1;
      private float explosionResistance = 1.0F;
      private int tickRate = 5;

      public Properties(Supplier<? extends FluidType> fluidType, Supplier<? extends Fluid> still, Supplier<? extends Fluid> flowing) {
         this.fluidType = fluidType;
         this.still = still;
         this.flowing = flowing;
      }

      public BaseFlowingFluid.Properties bucket(Supplier<? extends Item> bucket) {
         this.bucket = bucket;
         return this;
      }

      public BaseFlowingFluid.Properties block(Supplier<? extends LiquidBlock> block) {
         this.block = block;
         return this;
      }

      public BaseFlowingFluid.Properties slopeFindDistance(int slopeFindDistance) {
         this.slopeFindDistance = slopeFindDistance;
         return this;
      }

      public BaseFlowingFluid.Properties levelDecreasePerBlock(int levelDecreasePerBlock) {
         this.levelDecreasePerBlock = levelDecreasePerBlock;
         return this;
      }

      public BaseFlowingFluid.Properties explosionResistance(float explosionResistance) {
         this.explosionResistance = explosionResistance;
         return this;
      }

      public BaseFlowingFluid.Properties tickRate(int tickRate) {
         this.tickRate = tickRate;
         return this;
      }
   }

   public static class Source extends BaseFlowingFluid {
      public Source(BaseFlowingFluid.Properties properties) {
         super(properties);
      }

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
