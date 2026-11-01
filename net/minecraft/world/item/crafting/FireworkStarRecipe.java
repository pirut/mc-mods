package net.minecraft.world.item.crafting;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;

public class FireworkStarRecipe extends CustomRecipe {
   private static final Ingredient SHAPE_INGREDIENT = Ingredient.of(
      Items.FIRE_CHARGE,
      Items.FEATHER,
      Items.GOLD_NUGGET,
      Items.SKELETON_SKULL,
      Items.WITHER_SKELETON_SKULL,
      Items.CREEPER_HEAD,
      Items.PLAYER_HEAD,
      Items.DRAGON_HEAD,
      Items.ZOMBIE_HEAD,
      Items.PIGLIN_HEAD
   );
   private static final Ingredient TRAIL_INGREDIENT = Ingredient.of(Items.DIAMOND);
   private static final Ingredient TWINKLE_INGREDIENT = Ingredient.of(Items.GLOWSTONE_DUST);
   private static final Map<Item, FireworkExplosion.Shape> SHAPE_BY_ITEM = Util.make(Maps.newHashMap(), p_261449_ -> {
      p_261449_.put(Items.FIRE_CHARGE, FireworkExplosion.Shape.LARGE_BALL);
      p_261449_.put(Items.FEATHER, FireworkExplosion.Shape.BURST);
      p_261449_.put(Items.GOLD_NUGGET, FireworkExplosion.Shape.STAR);
      p_261449_.put(Items.SKELETON_SKULL, FireworkExplosion.Shape.CREEPER);
      p_261449_.put(Items.WITHER_SKELETON_SKULL, FireworkExplosion.Shape.CREEPER);
      p_261449_.put(Items.CREEPER_HEAD, FireworkExplosion.Shape.CREEPER);
      p_261449_.put(Items.PLAYER_HEAD, FireworkExplosion.Shape.CREEPER);
      p_261449_.put(Items.DRAGON_HEAD, FireworkExplosion.Shape.CREEPER);
      p_261449_.put(Items.ZOMBIE_HEAD, FireworkExplosion.Shape.CREEPER);
      p_261449_.put(Items.PIGLIN_HEAD, FireworkExplosion.Shape.CREEPER);
   });
   private static final Ingredient GUNPOWDER_INGREDIENT = Ingredient.of(Items.GUNPOWDER);

   public FireworkStarRecipe(CraftingBookCategory p_251577_) {
      super(p_251577_);
   }

   public boolean matches(CraftingInput p_344993_, Level p_43896_) {
      boolean $$2 = false;
      boolean $$3 = false;
      boolean $$4 = false;
      boolean $$5 = false;
      boolean $$6 = false;

      for (int $$7 = 0; $$7 < p_344993_.size(); $$7++) {
         ItemStack $$8 = p_344993_.getItem($$7);
         if (!$$8.isEmpty()) {
            if (SHAPE_INGREDIENT.test($$8)) {
               if ($$4) {
                  return false;
               }

               $$4 = true;
            } else if (TWINKLE_INGREDIENT.test($$8)) {
               if ($$6) {
                  return false;
               }

               $$6 = true;
            } else if (TRAIL_INGREDIENT.test($$8)) {
               if ($$5) {
                  return false;
               }

               $$5 = true;
            } else if (GUNPOWDER_INGREDIENT.test($$8)) {
               if ($$2) {
                  return false;
               }

               $$2 = true;
            } else {
               if (!($$8.getItem() instanceof DyeItem)) {
                  return false;
               }

               $$3 = true;
            }
         }
      }

      return $$2 && $$3;
   }

   public ItemStack assemble(CraftingInput p_346206_, HolderLookup.Provider p_335498_) {
      FireworkExplosion.Shape $$2 = FireworkExplosion.Shape.SMALL_BALL;
      boolean $$3 = false;
      boolean $$4 = false;
      IntList $$5 = new IntArrayList();

      for (int $$6 = 0; $$6 < p_346206_.size(); $$6++) {
         ItemStack $$7 = p_346206_.getItem($$6);
         if (!$$7.isEmpty()) {
            if (SHAPE_INGREDIENT.test($$7)) {
               $$2 = SHAPE_BY_ITEM.get($$7.getItem());
            } else if (TWINKLE_INGREDIENT.test($$7)) {
               $$3 = true;
            } else if (TRAIL_INGREDIENT.test($$7)) {
               $$4 = true;
            } else if ($$7.getItem() instanceof DyeItem) {
               $$5.add(((DyeItem)$$7.getItem()).getDyeColor().getFireworkColor());
            }
         }
      }

      ItemStack $$8 = new ItemStack(Items.FIREWORK_STAR);
      $$8.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion($$2, $$5, IntList.of(), $$4, $$3));
      return $$8;
   }

   @Override
   public boolean canCraftInDimensions(int p_43885_, int p_43886_) {
      return p_43885_ * p_43886_ >= 2;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider p_335423_) {
      return new ItemStack(Items.FIREWORK_STAR);
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.FIREWORK_STAR;
   }
}
