package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.display.FluidSlotDisplay;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jspecify.annotations.Nullable;

public abstract class FluidIngredient implements Predicate<FluidStack> {
   public static final Codec<FluidIngredient> CODEC = FluidIngredientCodecs.codec();
   public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = FluidIngredientCodecs.streamCodec();
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<FluidIngredient>> OPTIONAL_STREAM_CODEC = ByteBufCodecs.optional(STREAM_CODEC);
   @Nullable
   private List<Holder<Fluid>> fluids;

   public final List<Holder<Fluid>> fluids() {
      if (this.fluids == null) {
         this.fluids = this.generateFluids().toList();
      }

      return this.fluids;
   }

   public abstract boolean test(FluidStack var1);

   @OverrideOnly
   protected abstract Stream<Holder<Fluid>> generateFluids();

   public SlotDisplay display() {
      return new SlotDisplay.Composite(this.fluids().stream().map(FluidIngredient::displayForSingleFluid).toList());
   }

   public abstract boolean isSimple();

   public abstract FluidIngredientType<?> getType();

   @Override
   public abstract int hashCode();

   @Override
   public abstract boolean equals(Object var1);

   public static SlotDisplay displayForSingleFluid(Holder<Fluid> holder) {
      return new FluidSlotDisplay(holder);
   }

   public static FluidIngredient of(FluidStack... fluids) {
      return of(Arrays.stream(fluids).map(FluidStack::getFluid));
   }

   public static FluidIngredient of(Fluid... fluids) {
      return of(Arrays.stream(fluids));
   }

   public static FluidIngredient of(Stream<Fluid> fluids) {
      return of(HolderSet.direct(fluids.map(Fluid::builtInRegistryHolder).toList()));
   }

   public static FluidIngredient of(HolderSet<Fluid> fluids) {
      return new SimpleFluidIngredient(fluids);
   }
}
