package net.minecraft.client.multiplayer;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientRecipeContainer implements RecipeAccess {
   private final Map<ResourceKey<RecipePropertySet>, RecipePropertySet> itemSets;
   private final SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes;

   public ClientRecipeContainer(Map<ResourceKey<RecipePropertySet>, RecipePropertySet> p_380386_, SelectableRecipe.SingleInputSet<StonecutterRecipe> p_380139_) {
      this.itemSets = p_380386_;
      this.stonecutterRecipes = p_380139_;
   }

   @Override
   public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> p_379878_) {
      return this.itemSets.getOrDefault(p_379878_, RecipePropertySet.EMPTY);
   }

   @Override
   public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
      return this.stonecutterRecipes;
   }
}
