package net.neoforged.neoforge.fluids;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.util.DataComponentUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class FluidStack implements MutableDataComponentHolder {
   public static final Codec<Holder<Fluid>> FLUID_NON_EMPTY_CODEC = BuiltInRegistries.FLUID
      .holderByNameCodec()
      .validate(
         holder -> holder.is(Fluids.EMPTY.builtInRegistryHolder()) ? DataResult.error(() -> "Fluid must not be minecraft:empty") : DataResult.success(holder)
      );
   public static final Codec<FluidStack> CODEC = Codec.lazyInitialized(
      () -> RecordCodecBuilder.create(
         instance -> instance.group(
               FLUID_NON_EMPTY_CODEC.fieldOf("id").forGetter(FluidStack::getFluidHolder),
               ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidStack::getAmount),
               DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(stack -> stack.components.asPatch())
            )
            .apply(instance, FluidStack::new)
      )
   );
   public static final Codec<FluidStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
      .xmap(optional -> optional.orElse(FluidStack.EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));
   public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, FluidStack>() {
      private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Fluid>> FLUID_STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.FLUID);

      public FluidStack decode(RegistryFriendlyByteBuf buf) {
         int amount = buf.readVarInt();
         if (amount <= 0) {
            return FluidStack.EMPTY;
         } else {
            Holder<Fluid> holder = FLUID_STREAM_CODEC.decode(buf);
            DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(buf);
            return new FluidStack(holder, amount, patch);
         }
      }

      public void encode(RegistryFriendlyByteBuf buf, FluidStack stack) {
         if (stack.isEmpty()) {
            buf.writeVarInt(0);
         } else {
            buf.writeVarInt(stack.getAmount());
            FLUID_STREAM_CODEC.encode(buf, stack.getFluidHolder());
            DataComponentPatch.STREAM_CODEC.encode(buf, stack.components.asPatch());
         }
      }
   };
   public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, FluidStack>() {
      public FluidStack decode(RegistryFriendlyByteBuf buf) {
         FluidStack stack = FluidStack.OPTIONAL_STREAM_CODEC.decode(buf);
         if (stack.isEmpty()) {
            throw new DecoderException("Empty FluidStack not allowed");
         } else {
            return stack;
         }
      }

      public void encode(RegistryFriendlyByteBuf buf, FluidStack stack) {
         if (stack.isEmpty()) {
            throw new EncoderException("Empty FluidStack not allowed");
         } else {
            FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
         }
      }
   };
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final FluidStack EMPTY = new FluidStack(null);
   private int amount;
   private final Fluid fluid;
   private final PatchedDataComponentMap components;

   public static Codec<FluidStack> fixedAmountCodec(int amount) {
      return Codec.lazyInitialized(
         () -> RecordCodecBuilder.create(
            instance -> instance.group(
                  FLUID_NON_EMPTY_CODEC.fieldOf("id").forGetter(FluidStack::getFluidHolder),
                  DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(stack -> stack.components.asPatch())
               )
               .apply(instance, (holder, patch) -> new FluidStack(holder, amount, patch))
         )
      );
   }

   public PatchedDataComponentMap getComponents() {
      return this.components;
   }

   public DataComponentPatch getComponentsPatch() {
      return !this.isEmpty() ? this.components.asPatch() : DataComponentPatch.EMPTY;
   }

   public boolean isComponentsPatchEmpty() {
      return !this.isEmpty() ? this.components.isPatchEmpty() : true;
   }

   public FluidStack(Holder<Fluid> fluid, int amount, DataComponentPatch patch) {
      this(fluid.value(), amount, PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch));
   }

   public FluidStack(Holder<Fluid> fluid, int amount) {
      this(fluid.value(), amount);
   }

   public FluidStack(Fluid fluid, int amount) {
      this(fluid, amount, new PatchedDataComponentMap(DataComponentMap.EMPTY));
   }

   private FluidStack(Fluid fluid, int amount, PatchedDataComponentMap components) {
      this.fluid = fluid;
      this.amount = amount;
      this.components = components;
   }

   private FluidStack(@Nullable Void unused) {
      this.fluid = null;
      this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
   }

   public static Optional<FluidStack> parse(HolderLookup.Provider lookupProvider, Tag tag) {
      return CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag)
         .resultOrPartial(error -> LOGGER.error("Tried to load invalid fluid: '{}'", error));
   }

   public static FluidStack parseOptional(HolderLookup.Provider lookupProvider, CompoundTag tag) {
      return tag.isEmpty() ? EMPTY : parse(lookupProvider, tag).orElse(EMPTY);
   }

   public boolean isEmpty() {
      return this == EMPTY || this.fluid == Fluids.EMPTY || this.amount <= 0;
   }

   public FluidStack split(int amount) {
      int i = Math.min(amount, this.amount);
      FluidStack fluidStack = this.copyWithAmount(i);
      this.shrink(i);
      return fluidStack;
   }

   public FluidStack copyAndClear() {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         FluidStack fluidStack = this.copy();
         this.setAmount(0);
         return fluidStack;
      }
   }

   public Fluid getFluid() {
      return this.isEmpty() ? Fluids.EMPTY : this.fluid;
   }

   public Holder<Fluid> getFluidHolder() {
      return this.getFluid().builtInRegistryHolder();
   }

   public boolean is(TagKey<Fluid> tag) {
      return this.getFluid().builtInRegistryHolder().is(tag);
   }

   public boolean is(Fluid fluid) {
      return this.getFluid() == fluid;
   }

   public boolean is(Predicate<Holder<Fluid>> holderPredicate) {
      return holderPredicate.test(this.getFluidHolder());
   }

   public boolean is(Holder<Fluid> holder) {
      return this.is(holder.value());
   }

   public boolean is(HolderSet<Fluid> holderSet) {
      return holderSet.contains(this.getFluidHolder());
   }

   public Stream<TagKey<Fluid>> getTags() {
      return this.getFluid().builtInRegistryHolder().tags();
   }

   public Tag save(HolderLookup.Provider lookupProvider, Tag prefix) {
      if (this.isEmpty()) {
         throw new IllegalStateException("Cannot encode empty FluidStack");
      } else {
         return DataComponentUtil.wrapEncodingExceptions(this, CODEC, lookupProvider, prefix);
      }
   }

   public Tag save(HolderLookup.Provider lookupProvider) {
      if (this.isEmpty()) {
         throw new IllegalStateException("Cannot encode empty FluidStack");
      } else {
         return DataComponentUtil.wrapEncodingExceptions(this, CODEC, lookupProvider);
      }
   }

   public Tag saveOptional(HolderLookup.Provider lookupProvider) {
      return (Tag)(this.isEmpty() ? new CompoundTag() : this.save(lookupProvider, new CompoundTag()));
   }

   public FluidStack copy() {
      return this.isEmpty() ? EMPTY : new FluidStack(this.fluid, this.amount, this.components.copy());
   }

   public FluidStack copyWithAmount(int amount) {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         FluidStack fluidStack = this.copy();
         fluidStack.setAmount(amount);
         return fluidStack;
      }
   }

   public static boolean matches(FluidStack first, FluidStack second) {
      if (first == second) {
         return true;
      } else {
         return first.getAmount() != second.getAmount() ? false : isSameFluidSameComponents(first, second);
      }
   }

   public static boolean isSameFluid(FluidStack first, FluidStack second) {
      return first.is(second.getFluid());
   }

   public static boolean isSameFluidSameComponents(FluidStack first, FluidStack second) {
      if (!first.is(second.getFluid())) {
         return false;
      } else {
         return first.isEmpty() && second.isEmpty() ? true : Objects.equals(first.components, second.components);
      }
   }

   public static MapCodec<FluidStack> lenientOtionalFieldOf(String fieldName) {
      return CODEC.lenientOptionalFieldOf(fieldName).xmap(optional -> optional.orElse(EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));
   }

   public static int hashFluidAndComponents(@Nullable FluidStack stack) {
      if (stack != null) {
         int i = 31 + stack.getFluid().hashCode();
         return 31 * i + stack.getComponents().hashCode();
      } else {
         return 0;
      }
   }

   public String getDescriptionId() {
      return this.getFluidType().getDescriptionId(this);
   }

   @Override
   public String toString() {
      return this.getAmount() + " " + this.getFluid();
   }

   @Nullable
   @Override
   public <T> T set(DataComponentType<? super T> type, @Nullable T component) {
      return this.components.set(type, component);
   }

   @Nullable
   @Override
   public <T> T remove(DataComponentType<? extends T> type) {
      return this.components.remove(type);
   }

   @Override
   public void applyComponents(DataComponentPatch patch) {
      this.components.applyPatch(patch);
   }

   @Override
   public void applyComponents(DataComponentMap components) {
      this.components.setAll(components);
   }

   public Component getHoverName() {
      return this.getFluidType().getDescription(this);
   }

   public int getAmount() {
      return this.isEmpty() ? 0 : this.amount;
   }

   public void setAmount(int amount) {
      this.amount = amount;
   }

   public void limitSize(int amount) {
      if (!this.isEmpty() && this.getAmount() > amount) {
         this.setAmount(amount);
      }
   }

   public void grow(int addedAmount) {
      this.setAmount(this.getAmount() + addedAmount);
   }

   public void shrink(int removedAmount) {
      this.grow(-removedAmount);
   }

   public FluidType getFluidType() {
      return this.getFluid().getFluidType();
   }

   public boolean is(FluidType fluidType) {
      return this.getFluidType() == fluidType;
   }

   @Deprecated(
      forRemoval = true,
      since = "1.20.5"
   )
   public Component getDisplayName() {
      return this.getHoverName();
   }

   @Deprecated(
      forRemoval = true,
      since = "1.20.5"
   )
   public String getTranslationKey() {
      return this.getFluidType().getDescriptionId(this);
   }

   @Deprecated(
      forRemoval = true,
      since = "1.20.5"
   )
   public boolean isFluidEqual(FluidStack other) {
      return isSameFluidSameComponents(this, other);
   }

   @Deprecated(
      forRemoval = true,
      since = "1.20.5"
   )
   public static boolean areFluidStackTagsEqual(FluidStack first, FluidStack second) {
      return Objects.equals(first.components, second.components);
   }

   @Deprecated(
      forRemoval = true,
      since = "1.20.5"
   )
   public boolean containsFluid(FluidStack other) {
      return this.isFluidEqual(other) && this.amount >= other.amount;
   }

   @Deprecated(
      forRemoval = true,
      since = "1.20.5"
   )
   public boolean isFluidStackIdentical(FluidStack other) {
      return matches(this, other);
   }

   @Deprecated(
      forRemoval = true,
      since = "1.20.5"
   )
   public boolean isFluidEqual(ItemStack other) {
      return FluidUtil.getFluidContained(other).map(this::isFluidEqual).orElse(false);
   }
}
