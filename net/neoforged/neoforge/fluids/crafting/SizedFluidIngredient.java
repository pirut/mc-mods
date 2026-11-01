package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.fluids.FluidStack;

public final class SizedFluidIngredient {
   public static final Codec<SizedFluidIngredient> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            FluidIngredient.CODEC.fieldOf("ingredient").forGetter(SizedFluidIngredient::ingredient),
            NeoForgeExtraCodecs.optionalFieldAlwaysWrite(ExtraCodecs.POSITIVE_INT, "amount", 1000).forGetter(SizedFluidIngredient::amount)
         )
         .apply(instance, SizedFluidIngredient::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SizedFluidIngredient> STREAM_CODEC = StreamCodec.composite(
      FluidIngredient.STREAM_CODEC, SizedFluidIngredient::ingredient, ByteBufCodecs.VAR_INT, SizedFluidIngredient::amount, SizedFluidIngredient::new
   );
   private final FluidIngredient ingredient;
   private final int amount;

   public static SizedFluidIngredient of(Fluid fluid, int amount) {
      return new SizedFluidIngredient(FluidIngredient.of(fluid), amount);
   }

   public SizedFluidIngredient(FluidIngredient ingredient, int amount) {
      if (amount <= 0) {
         throw new IllegalArgumentException("Size must be positive");
      } else {
         this.ingredient = ingredient;
         this.amount = amount;
      }
   }

   public FluidIngredient ingredient() {
      return this.ingredient;
   }

   public int amount() {
      return this.amount;
   }

   public boolean test(FluidStack stack) {
      return this.ingredient.test(stack) && stack.getAmount() >= this.amount;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof SizedFluidIngredient other) ? false : this.amount == other.amount && this.ingredient.equals(other.ingredient);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.ingredient, this.amount);
   }

   @Override
   public String toString() {
      return this.amount + "x " + this.ingredient;
   }
}
