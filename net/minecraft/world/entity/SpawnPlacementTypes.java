package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public interface SpawnPlacementTypes {
   SpawnPlacementType NO_RESTRICTIONS = (p_321554_, p_321832_, p_321540_) -> true;
   SpawnPlacementType IN_WATER = (p_325672_, p_325673_, p_325674_) -> {
      if (p_325674_ != null && p_325672_.getWorldBorder().isWithinBounds(p_325673_)) {
         BlockPos $$3 = p_325673_.above();
         return p_325672_.getFluidState(p_325673_).is(FluidTags.WATER) && !p_325672_.getBlockState($$3).isRedstoneConductor(p_325672_, $$3);
      } else {
         return false;
      }
   };
   SpawnPlacementType IN_LAVA = (p_325669_, p_325670_, p_325671_) -> p_325671_ != null && p_325669_.getWorldBorder().isWithinBounds(p_325670_)
      ? p_325669_.getFluidState(p_325670_).is(FluidTags.LAVA)
      : false;
   SpawnPlacementType ON_GROUND = new SpawnPlacementType() {
      @Override
      public boolean isSpawnPositionOk(LevelReader p_321666_, BlockPos p_321783_, @Nullable EntityType<?> p_321839_) {
         if (p_321839_ != null && p_321666_.getWorldBorder().isWithinBounds(p_321783_)) {
            BlockPos $$3 = p_321783_.above();
            BlockPos $$4 = p_321783_.below();
            BlockState $$5 = p_321666_.getBlockState($$4);
            return !$$5.isValidSpawn(p_321666_, $$4, p_321839_)
               ? false
               : this.isValidEmptySpawnBlock(p_321666_, p_321783_, p_321839_) && this.isValidEmptySpawnBlock(p_321666_, $$3, p_321839_);
         } else {
            return false;
         }
      }

      private boolean isValidEmptySpawnBlock(LevelReader p_321512_, BlockPos p_321822_, EntityType<?> p_321785_) {
         BlockState $$3 = p_321512_.getBlockState(p_321822_);
         return NaturalSpawner.isValidEmptySpawnBlock(p_321512_, p_321822_, $$3, $$3.getFluidState(), p_321785_);
      }

      @Override
      public BlockPos adjustSpawnPosition(LevelReader p_321527_, BlockPos p_321602_) {
         BlockPos $$2 = p_321602_.below();
         return p_321527_.getBlockState($$2).isPathfindable(PathComputationType.LAND) ? $$2 : p_321602_;
      }
   };
}
