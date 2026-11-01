package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

public interface IAbstractBoatExtension {
   private AbstractBoat self() {
      return (AbstractBoat)this;
   }

   default boolean canBoatInFluid(FluidState state) {
      return state.supportsBoating(this.self());
   }

   default boolean canBoatInFluid(FluidType type) {
      return type.supportsBoating(this.self());
   }
}
