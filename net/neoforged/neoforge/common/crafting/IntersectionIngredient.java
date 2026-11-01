package net.neoforged.neoforge.common.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.NeoForgeMod;

public record IntersectionIngredient(List<Ingredient> children) implements ICustomIngredient {
   public static final MapCodec<IntersectionIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(Ingredient.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("children").forGetter(IntersectionIngredient::children))
         .apply(builder, IntersectionIngredient::new)
   );

   public IntersectionIngredient(List<Ingredient> children) {
      if (children.isEmpty()) {
         throw new IllegalArgumentException("Intersection ingredient must have at least one child.");
      } else {
         this.children = children;
      }
   }

   public static Ingredient of(Ingredient... ingredients) {
      return ingredients.length == 1 ? ingredients[0] : new IntersectionIngredient(Arrays.asList(ingredients)).toVanilla();
   }

   @Override
   public boolean test(ItemStack stack) {
      for (Ingredient child : this.children) {
         if (!child.test(stack)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public Stream<Holder<Item>> items() {
      return this.children.stream().flatMap(child -> child.items()).filter(i -> this.test(i.value().getDefaultInstance()));
   }

   @Override
   public boolean isSimple() {
      for (Ingredient child : this.children) {
         if (!child.isSimple()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public IngredientType<?> getType() {
      return NeoForgeMod.INTERSECTION_INGREDIENT_TYPE.get();
   }
}
