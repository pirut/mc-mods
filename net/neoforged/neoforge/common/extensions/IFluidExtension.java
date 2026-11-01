package net.neoforged.neoforge.common.extensions;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

public interface IFluidExtension {
   private Fluid self() {
      return (Fluid)this;
   }

   default float getExplosionResistance(FluidState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      return state.getExplosionResistance();
   }

   FluidType getFluidType();

   default boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
      return this.getFluidType().move(state, entity, movementVector, gravity);
   }

   default boolean canConvertToSource(FluidState state, ServerLevel level, BlockPos pos) {
      return this.getFluidType().canConvertToSource(state, level, pos);
   }

   default boolean supportsBoating(FluidState state, AbstractBoat boat) {
      return this.getFluidType().supportsBoating(state, boat);
   }

   @Nullable
   default PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
      return this.getFluidType().getBlockPathType(state, level, pos, mob, canFluidLog);
   }

   @Nullable
   default PathType getAdjacentBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, PathType originalType) {
      return this.getFluidType().getAdjacentBlockPathType(state, level, pos, mob, originalType);
   }

   default boolean canHydrate(FluidState state, BlockGetter getter, BlockPos pos, BlockState source, BlockPos sourcePos) {
      return this.getFluidType().canHydrate(state, getter, pos, source, sourcePos);
   }

   default boolean canExtinguish(FluidState state, BlockGetter getter, BlockPos pos) {
      return this.getFluidType().canExtinguish(state, getter, pos);
   }

   default void appendHoverText(
      FluidStack fluidStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag
   ) {
   }
}
