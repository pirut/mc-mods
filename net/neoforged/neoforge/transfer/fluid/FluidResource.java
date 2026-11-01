package net.neoforged.neoforge.transfer.fluid;

import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.resource.DataComponentHolderResource;

public final class FluidResource implements DataComponentHolderResource<Fluid> {
   public static final FluidResource EMPTY = new FluidResource(FluidStack.EMPTY);
   public static final Codec<FluidResource> CODEC = FluidStack.fixedAmountCodec(1000).xmap(FluidResource::of, resource -> resource.toStack(1000));
   public static final Codec<FluidResource> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
      .xmap(optional -> optional.orElse(EMPTY), resource -> resource.isEmpty() ? Optional.empty() : Optional.of(resource));
   public static final StreamCodec<RegistryFriendlyByteBuf, FluidResource> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.holderRegistry(Registries.FLUID),
      FluidResource::getHolder,
      DataComponentPatch.STREAM_CODEC,
      FluidResource::getComponentsPatch,
      FluidResource::of
   );
   private final FluidStack innerStack;

   public static FluidResource of(FluidStack stack) {
      return !stack.isEmpty() && !stack.isComponentsPatchEmpty() ? new FluidResource(stack.copyWithAmount(1000)) : of(stack.getFluid());
   }

   public static FluidResource of(Fluid fluid) {
      return fluid == Fluids.EMPTY ? EMPTY : fluid.computeDefaultResource(f -> new FluidResource(new FluidStack(f, 1000)));
   }

   public static FluidResource of(Fluid fluid, DataComponentPatch patch) {
      return of(fluid.builtInRegistryHolder(), patch);
   }

   public static FluidResource of(Holder<Fluid> fluid) {
      return of(fluid.value());
   }

   public static FluidResource of(Holder<Fluid> holder, DataComponentPatch patch) {
      return holder.value() != Fluids.EMPTY && !patch.isEmpty() ? new FluidResource(new FluidStack(holder, 1000, patch)) : of(holder.value());
   }

   private FluidResource(FluidStack stack) {
      this.innerStack = stack;
   }

   public Fluid value() {
      return this.innerStack.getFluid();
   }

   public Fluid getFluid() {
      return this.value();
   }

   @Override
   public Holder<Fluid> getHolder() {
      return this.innerStack.getFluidHolder();
   }

   public FluidType getFluidType() {
      return this.innerStack.getFluidType();
   }

   @Override
   public boolean isEmpty() {
      return this.innerStack.isEmpty();
   }

   public FluidResource withMergedPatch(DataComponentPatch patch) {
      if (!this.isEmpty() && !patch.isEmpty()) {
         FluidStack stack = this.innerStack.copy();
         stack.applyComponents(patch);
         return of(stack);
      } else {
         return this;
      }
   }

   public <D> FluidResource with(DataComponentType<D> type, D data) {
      if (this.isEmpty()) {
         return EMPTY;
      } else if (Objects.equals(this.get(type), data)) {
         return this;
      } else {
         FluidStack stack = this.innerStack.copy();
         stack.set(type, data);
         return of(stack);
      }
   }

   public <D> FluidResource with(Supplier<? extends DataComponentType<D>> type, D data) {
      return this.with((DataComponentType<D>)type.get(), data);
   }

   public FluidResource without(DataComponentType<?> type) {
      if (this.isEmpty()) {
         return EMPTY;
      } else if (this.get(type) == null) {
         return this;
      } else {
         FluidStack stack = this.innerStack.copy();
         stack.remove(type);
         return of(stack);
      }
   }

   public FluidResource without(Supplier<? extends DataComponentType<?>> type) {
      return this.without((DataComponentType<?>)type.get());
   }

   @Override
   public DataComponentMap getComponents() {
      return this.innerStack.immutableComponents();
   }

   @Override
   public DataComponentPatch getComponentsPatch() {
      return this.innerStack.getComponentsPatch();
   }

   public FluidStack toStack(int amount) {
      TransferPreconditions.checkNonNegative(amount);
      return amount == 0 ? FluidStack.EMPTY : this.innerStack.copyWithAmount(amount);
   }

   @Override
   public boolean isComponentsPatchEmpty() {
      return this.innerStack.isComponentsPatchEmpty();
   }

   public boolean is(FluidType fluidType) {
      return this.innerStack.is(fluidType);
   }

   public boolean matches(FluidStack stack) {
      return FluidStack.isSameFluidSameComponents(stack, this.innerStack);
   }

   public boolean test(Predicate<FluidStack> predicate) {
      return predicate.test(this.innerStack);
   }

   public Component getHoverName() {
      return this.innerStack.getHoverName();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj != null && this.getClass() == obj.getClass()) {
         FluidResource other = (FluidResource)obj;
         return FluidStack.isSameFluidSameComponents(this.innerStack, other.innerStack);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return FluidStack.hashFluidAndComponents(this.innerStack);
   }

   @Override
   public String toString() {
      return this.getFluidType() + " [" + this.getComponentsPatch().size() + "]";
   }
}
