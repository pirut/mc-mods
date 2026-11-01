package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class SummonedEntityTrigger extends SimpleCriterionTrigger<SummonedEntityTrigger.TriggerInstance> {
   @Override
   public Codec<SummonedEntityTrigger.TriggerInstance> codec() {
      return SummonedEntityTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468864_, Entity p_468722_) {
      LootContext lootcontext = EntityPredicate.createContext(p_468864_, p_468722_);
      this.trigger(p_468864_, p_467615_ -> p_467615_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<SummonedEntityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467224_ -> p_467224_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SummonedEntityTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(SummonedEntityTrigger.TriggerInstance::entity)
            )
            .apply(p_467224_, SummonedEntityTrigger.TriggerInstance::new)
      );

      public static Criterion<SummonedEntityTrigger.TriggerInstance> summonedEntity(EntityPredicate.Builder p_467496_) {
         return CriteriaTriggers.SUMMONED_ENTITY
            .createCriterion(new SummonedEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_467496_))));
      }

      public boolean matches(LootContext p_466825_) {
         return this.entity.isEmpty() || this.entity.get().matches(p_466825_);
      }

      @Override
      public void validate(CriterionValidator p_469152_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_469152_);
         p_469152_.validateEntity(this.entity, "entity");
      }
   }
}
