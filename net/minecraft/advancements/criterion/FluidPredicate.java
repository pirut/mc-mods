package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public record FluidPredicate(Optional<HolderSet<Fluid>> fluids, Optional<StatePropertiesPredicate> properties) {
   public static final Codec<FluidPredicate> CODEC = RecordCodecBuilder.create(
      p_468577_ -> p_468577_.group(
            RegistryCodecs.homogeneousList(Registries.FLUID).optionalFieldOf("fluids").forGetter(FluidPredicate::fluids),
            StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(FluidPredicate::properties)
         )
         .apply(p_468577_, FluidPredicate::new)
   );

   public boolean matches(ServerLevel p_467553_, BlockPos p_467566_) {
      if (!p_467553_.isLoaded(p_467566_)) {
         return false;
      } else {
         FluidState fluidstate = p_467553_.getFluidState(p_467566_);
         return this.fluids.isPresent() && !fluidstate.is(this.fluids.get())
            ? false
            : !this.properties.isPresent() || this.properties.get().matches(fluidstate);
      }
   }

   public static class Builder {
      private Optional<HolderSet<Fluid>> fluids = Optional.empty();
      private Optional<StatePropertiesPredicate> properties = Optional.empty();

      private Builder() {
      }

      public static FluidPredicate.Builder fluid() {
         return new FluidPredicate.Builder();
      }

      public FluidPredicate.Builder of(Fluid p_467001_) {
         this.fluids = Optional.of(HolderSet.direct(p_467001_.builtInRegistryHolder()));
         return this;
      }

      public FluidPredicate.Builder of(HolderSet<Fluid> p_468839_) {
         this.fluids = Optional.of(p_468839_);
         return this;
      }

      public FluidPredicate.Builder setProperties(StatePropertiesPredicate p_468459_) {
         this.properties = Optional.of(p_468459_);
         return this;
      }

      public FluidPredicate build() {
         return new FluidPredicate(this.fluids, this.properties);
      }
   }
}
