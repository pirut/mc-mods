package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.fluids.FluidType;

public interface ILivingEntityExtension extends IEntityExtension {
   default LivingEntity self() {
      return (LivingEntity)this;
   }

   @Override
   default boolean canSwimInFluidType(FluidType type) {
      return type == NeoForgeMod.WATER_TYPE.value() ? !this.self().isSensitiveToWater() : IEntityExtension.super.canSwimInFluidType(type);
   }

   default void jumpInFluid(FluidType type) {
      double multiplier = type == NeoForgeMod.WATER_TYPE.value() ? this.self().getAttributeValue(NeoForgeMod.SWIM_SPEED) : 1.0;
      this.self().setDeltaMovement(this.self().getDeltaMovement().add(0.0, 0.04F * multiplier, 0.0));
   }

   default void sinkInFluid(FluidType type) {
      double multiplier = type == NeoForgeMod.WATER_TYPE.value() ? this.self().getAttributeValue(NeoForgeMod.SWIM_SPEED) : 1.0;
      this.self().setDeltaMovement(this.self().getDeltaMovement().add(0.0, -0.04F * multiplier, 0.0));
   }

   default boolean canDrownInFluidType(FluidType type) {
      return type == NeoForgeMod.WATER_TYPE.value() ? !this.self().canBreatheUnderwater() : type.canDrownIn(this.self());
   }

   default boolean moveInFluid(FluidState state, Vec3 movementVector, double gravity) {
      return state.move(this.self(), movementVector, gravity);
   }

   default void onDamageTaken(DamageContainer damageContainer) {
   }
}
