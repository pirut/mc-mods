package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
   @Override
   public Codec<RecipeCraftedTrigger.TriggerInstance> codec() {
      return RecipeCraftedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467334_, ResourceKey<Recipe<?>> p_466878_, List<ItemStack> p_468945_) {
      this.trigger(p_467334_, p_466970_ -> p_466970_.matches(p_466878_, p_468945_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceKey<Recipe<?>> recipeId, List<ItemPredicate> ingredients)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<RecipeCraftedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468843_ -> p_468843_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(RecipeCraftedTrigger.TriggerInstance::player),
               Recipe.KEY_CODEC.fieldOf("recipe_id").forGetter(RecipeCraftedTrigger.TriggerInstance::recipeId),
               ItemPredicate.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(RecipeCraftedTrigger.TriggerInstance::ingredients)
            )
            .apply(p_468843_, RecipeCraftedTrigger.TriggerInstance::new)
      );

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceKey<Recipe<?>> p_468201_, List<ItemPredicate.Builder> p_468654_) {
         return CriteriaTriggers.RECIPE_CRAFTED
            .createCriterion(
               new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_468201_, p_468654_.stream().map(ItemPredicate.Builder::build).toList())
            );
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceKey<Recipe<?>> p_469872_) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_469872_, List.of()));
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> crafterCraftedItem(ResourceKey<Recipe<?>> p_467467_) {
         return CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_467467_, List.of()));
      }

      boolean matches(ResourceKey<Recipe<?>> p_467905_, List<ItemStack> p_469970_) {
         if (p_467905_ != this.recipeId) {
            return false;
         } else {
            List<ItemStack> list = new ArrayList<>(p_469970_);

            for (ItemPredicate itempredicate : this.ingredients) {
               boolean flag = false;
               Iterator<ItemStack> iterator = list.iterator();

               while (iterator.hasNext()) {
                  if (itempredicate.test(iterator.next())) {
                     iterator.remove();
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  return false;
               }
            }

            return true;
         }
      }
   }
}
