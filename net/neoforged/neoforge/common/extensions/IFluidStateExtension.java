package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

public interface IFluidStateExtension {
   private FluidState self() {
      return (FluidState)this;
   }

   default float getExplosionResistance(BlockGetter level, BlockPos pos, Explosion explosion) {
      return this.self().getType().getExplosionResistance(this.self(), level, pos, explosion);
   }

   default FluidType getFluidType() {
      return this.self().getType().getFluidType();
   }

   default boolean move(LivingEntity entity, Vec3 movementVector, double gravity) {
      return this.self().getType().move(this.self(), entity, movementVector, gravity);
   }

   default boolean canConvertToSource(ServerLevel level, BlockPos pos) {
      return this.self().getType().canConvertToSource(this.self(), level, pos);
   }

   default boolean supportsBoating(AbstractBoat boat) {
      return this.self().getType().supportsBoating(this.self(), boat);
   }

   @Nullable
   default PathType getBlockPathType(BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
      return this.self().getType().getBlockPathType(this.self(), level, pos, mob, canFluidLog);
   }

   @Nullable
   default PathType getAdjacentBlockPathType(BlockGetter level, BlockPos pos, @Nullable Mob mob, PathType originalType) {
      return this.self().getType().getAdjacentBlockPathType(this.self(), level, pos, mob, originalType);
   }

   default boolean canHydrate(BlockGetter getter, BlockPos pos, BlockState source, BlockPos sourcePos) {
      return this.self().getType().canHydrate(this.self(), getter, pos, source, sourcePos);
   }

   default boolean canExtinguish(BlockGetter getter, BlockPos pos) {
      return this.self().getType().canExtinguish(this.self(), getter, pos);
   }
}
