package net.neoforged.neoforge.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;

public final class SizedIngredient {
   public static final Codec<SizedIngredient> NESTED_CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
            NeoForgeExtraCodecs.optionalFieldAlwaysWrite(ExtraCodecs.POSITIVE_INT, "count", 1).forGetter(SizedIngredient::count)
         )
         .apply(instance, SizedIngredient::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient, ByteBufCodecs.VAR_INT, SizedIngredient::count, SizedIngredient::new
   );
   private final Ingredient ingredient;
   private final int count;

   public static SizedIngredient of(ItemLike item, int count) {
      return new SizedIngredient(Ingredient.of(item), count);
   }

   public SizedIngredient(Ingredient ingredient, int count) {
      if (count <= 0) {
         throw new IllegalArgumentException("Size must be positive");
      } else {
         this.ingredient = ingredient;
         this.count = count;
      }
   }

   public Ingredient ingredient() {
      return this.ingredient;
   }

   public int count() {
      return this.count;
   }

   public boolean test(ItemStack stack) {
      return this.ingredient.test(stack) && stack.getCount() >= this.count;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof SizedIngredient other) ? false : this.count == other.count && this.ingredient.equals(other.ingredient);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.ingredient, this.count);
   }

   @Override
   public String toString() {
      return this.count + "x " + this.ingredient;
   }
}
