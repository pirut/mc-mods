package net.neoforged.neoforge.common.crafting;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;

public record CompoundIngredient(List<Ingredient> children) implements ICustomIngredient {
   public static final MapCodec<CompoundIngredient> CODEC = NeoForgeExtraCodecs.aliasedFieldOf(
         Ingredient.CODEC.listOf(1, Integer.MAX_VALUE), "children", "ingredients"
      )
      .xmap(CompoundIngredient::new, CompoundIngredient::children);

   public CompoundIngredient(List<Ingredient> children) {
      if (children.isEmpty()) {
         throw new IllegalArgumentException("Compound ingredient must have at least one child.");
      } else {
         this.children = children;
      }
   }

   public static Ingredient of(Ingredient... children) {
      return children.length == 1 ? children[0] : new CompoundIngredient(List.of(children)).toVanilla();
   }

   @Override
   public Stream<Holder<Item>> items() {
      return this.children.stream().flatMap(Ingredient::items);
   }

   @Override
   public boolean test(ItemStack stack) {
      for (Ingredient child : this.children) {
         if (child.test(stack)) {
            return true;
         }
      }

      return false;
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
      return NeoForgeMod.COMPOUND_INGREDIENT_TYPE.get();
   }

   @Override
   public SlotDisplay display() {
      return new SlotDisplay.Composite(this.children.stream().map(Ingredient::display).toList());
   }
}
