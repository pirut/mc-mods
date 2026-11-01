package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

public interface IBoatExtension {
   private Boat self() {
      return (Boat)this;
   }

   default boolean canBoatInFluid(FluidState state) {
      return state.supportsBoating(this.self());
   }

   default boolean canBoatInFluid(FluidType type) {
      return type.supportsBoating(this.self());
   }
}
