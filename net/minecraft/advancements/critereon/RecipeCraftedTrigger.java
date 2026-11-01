package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
   @Override
   public Codec<RecipeCraftedTrigger.TriggerInstance> codec() {
      return RecipeCraftedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_281468_, ResourceLocation p_282903_, List<ItemStack> p_282070_) {
      this.trigger(p_281468_, p_282798_ -> p_282798_.matches(p_282903_, p_282070_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceLocation recipeId, List<ItemPredicate> ingredients)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<RecipeCraftedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_337391_ -> p_337391_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(RecipeCraftedTrigger.TriggerInstance::player),
               ResourceLocation.CODEC.fieldOf("recipe_id").forGetter(RecipeCraftedTrigger.TriggerInstance::recipeId),
               ItemPredicate.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(RecipeCraftedTrigger.TriggerInstance::ingredients)
            )
            .apply(p_337391_, RecipeCraftedTrigger.TriggerInstance::new)
      );

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceLocation p_282794_, List<ItemPredicate.Builder> p_281369_) {
         return CriteriaTriggers.RECIPE_CRAFTED
            .createCriterion(
               new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_282794_, p_281369_.stream().map(ItemPredicate.Builder::build).toList())
            );
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceLocation p_283538_) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_283538_, List.of()));
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> crafterCraftedItem(ResourceLocation p_335404_) {
         return CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_335404_, List.of()));
      }

      boolean matches(ResourceLocation p_283528_, List<ItemStack> p_283698_) {
         if (!p_283528_.equals(this.recipeId)) {
            return false;
         } else {
            List<ItemStack> $$2 = new ArrayList<>(p_283698_);

            for (ItemPredicate $$3 : this.ingredients) {
               boolean $$4 = false;
               Iterator<ItemStack> $$5 = $$2.iterator();

               while ($$5.hasNext()) {
                  if ($$3.test($$5.next())) {
                     $$5.remove();
                     $$4 = true;
                     break;
                  }
               }

               if (!$$4) {
                  return false;
               }
            }

            return true;
         }
      }
   }
}
