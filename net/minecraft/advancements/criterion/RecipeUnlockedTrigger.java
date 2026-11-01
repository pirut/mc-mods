package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeUnlockedTrigger extends SimpleCriterionTrigger<RecipeUnlockedTrigger.TriggerInstance> {
   @Override
   public Codec<RecipeUnlockedTrigger.TriggerInstance> codec() {
      return RecipeUnlockedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469825_, RecipeHolder<?> p_469173_) {
      this.trigger(p_469825_, p_468219_ -> p_468219_.matches(p_469173_));
   }

   public static Criterion<RecipeUnlockedTrigger.TriggerInstance> unlocked(ResourceKey<Recipe<?>> p_468460_) {
      return CriteriaTriggers.RECIPE_UNLOCKED.createCriterion(new RecipeUnlockedTrigger.TriggerInstance(Optional.empty(), p_468460_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceKey<Recipe<?>> recipe) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<RecipeUnlockedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468559_ -> p_468559_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(RecipeUnlockedTrigger.TriggerInstance::player),
               Recipe.KEY_CODEC.fieldOf("recipe").forGetter(RecipeUnlockedTrigger.TriggerInstance::recipe)
            )
            .apply(p_468559_, RecipeUnlockedTrigger.TriggerInstance::new)
      );

      public boolean matches(RecipeHolder<?> p_467910_) {
         return this.recipe == p_467910_.id();
      }
   }
}
