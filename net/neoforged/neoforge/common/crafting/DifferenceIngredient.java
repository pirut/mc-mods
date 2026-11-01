package net.neoforged.neoforge.common.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.NeoForgeMod;

public record DifferenceIngredient(Ingredient base, Ingredient subtracted) implements ICustomIngredient {
   public static final MapCodec<DifferenceIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(
            Ingredient.CODEC.fieldOf("base").forGetter(DifferenceIngredient::base),
            Ingredient.CODEC.fieldOf("subtracted").forGetter(DifferenceIngredient::subtracted)
         )
         .apply(builder, DifferenceIngredient::new)
   );

   @Override
   public Stream<ItemStack> getItems() {
      return Stream.of(this.base.getItems()).filter(this.subtracted.negate());
   }

   @Override
   public boolean test(ItemStack stack) {
      return this.base.test(stack) && !this.subtracted.test(stack);
   }

   @Override
   public boolean isSimple() {
      return this.base.isSimple() && this.subtracted.isSimple();
   }

   @Override
   public IngredientType<?> getType() {
      return NeoForgeMod.DIFFERENCE_INGREDIENT_TYPE.get();
   }

   public static Ingredient of(Ingredient base, Ingredient subtracted) {
      return new DifferenceIngredient(base, subtracted).toVanilla();
   }
}
