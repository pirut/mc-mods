package net.neoforged.neoforge.common.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.NeoForgeMod;

public record IntersectionIngredient(List<Ingredient> children) implements ICustomIngredient {
   public static final MapCodec<IntersectionIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(Ingredient.LIST_CODEC_NONEMPTY.fieldOf("children").forGetter(IntersectionIngredient::children))
         .apply(builder, IntersectionIngredient::new)
   );

   public IntersectionIngredient(List<Ingredient> children) {
      if (children.isEmpty()) {
         throw new IllegalArgumentException("Cannot create an IntersectionIngredient with no children, use Ingredient.of() to create an empty ingredient");
      } else {
         this.children = children;
      }
   }

   public static Ingredient of(Ingredient... ingredients) {
      if (ingredients.length == 0) {
         throw new IllegalArgumentException("Cannot create an IntersectionIngredient with no children, use Ingredient.of() to create an empty ingredient");
      } else {
         return ingredients.length == 1 ? ingredients[0] : new IntersectionIngredient(Arrays.asList(ingredients)).toVanilla();
      }
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
   public Stream<ItemStack> getItems() {
      return this.children.stream().flatMap(child -> Arrays.stream(child.getItems())).filter(this::test);
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
