package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class TameAnimalTrigger extends SimpleCriterionTrigger<TameAnimalTrigger.TriggerInstance> {
   @Override
   public Codec<TameAnimalTrigger.TriggerInstance> codec() {
      return TameAnimalTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469068_, Animal p_467535_) {
      LootContext lootcontext = EntityPredicate.createContext(p_469068_, p_467535_);
      this.trigger(p_469068_, p_467031_ -> p_467031_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<TameAnimalTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468851_ -> p_468851_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TameAnimalTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TameAnimalTrigger.TriggerInstance::entity)
            )
            .apply(p_468851_, TameAnimalTrigger.TriggerInstance::new)
      );

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal() {
         return CriteriaTriggers.TAME_ANIMAL.createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal(EntityPredicate.Builder p_468470_) {
         return CriteriaTriggers.TAME_ANIMAL
            .createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_468470_))));
      }

      public boolean matches(LootContext p_467790_) {
         return this.entity.isEmpty() || this.entity.get().matches(p_467790_);
      }

      @Override
      public void validate(CriterionValidator p_468561_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_468561_);
         p_468561_.validateEntity(this.entity, "entity");
      }
   }
}
