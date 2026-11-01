package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;

public class FireworkRocketRecipe extends CustomRecipe {
   private static final Ingredient PAPER_INGREDIENT = Ingredient.of(Items.PAPER);
   private static final Ingredient GUNPOWDER_INGREDIENT = Ingredient.of(Items.GUNPOWDER);
   private static final Ingredient STAR_INGREDIENT = Ingredient.of(Items.FIREWORK_STAR);

   public FireworkRocketRecipe(CraftingBookCategory p_250134_) {
      super(p_250134_);
   }

   public boolean matches(CraftingInput p_345559_, Level p_43855_) {
      boolean $$2 = false;
      int $$3 = 0;

      for (int $$4 = 0; $$4 < p_345559_.size(); $$4++) {
         ItemStack $$5 = p_345559_.getItem($$4);
         if (!$$5.isEmpty()) {
            if (PAPER_INGREDIENT.test($$5)) {
               if ($$2) {
                  return false;
               }

               $$2 = true;
            } else if (GUNPOWDER_INGREDIENT.test($$5)) {
               if (++$$3 > 3) {
                  return false;
               }
            } else if (!STAR_INGREDIENT.test($$5)) {
               return false;
            }
         }
      }

      return $$2 && $$3 >= 1;
   }

   public ItemStack assemble(CraftingInput p_345921_, HolderLookup.Provider p_335560_) {
      List<FireworkExplosion> $$2 = new ArrayList<>();
      int $$3 = 0;

      for (int $$4 = 0; $$4 < p_345921_.size(); $$4++) {
         ItemStack $$5 = p_345921_.getItem($$4);
         if (!$$5.isEmpty()) {
            if (GUNPOWDER_INGREDIENT.test($$5)) {
               $$3++;
            } else if (STAR_INGREDIENT.test($$5)) {
               FireworkExplosion $$6 = $$5.get(DataComponents.FIREWORK_EXPLOSION);
               if ($$6 != null) {
                  $$2.add($$6);
               }
            }
         }
      }

      ItemStack $$7 = new ItemStack(Items.FIREWORK_ROCKET, 3);
      $$7.set(DataComponents.FIREWORKS, new Fireworks($$3, $$2));
      return $$7;
   }

   @Override
   public boolean canCraftInDimensions(int p_43844_, int p_43845_) {
      return p_43844_ * p_43845_ >= 2;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider p_335481_) {
      return new ItemStack(Items.FIREWORK_ROCKET);
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.FIREWORK_ROCKET;
   }
}
