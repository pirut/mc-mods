package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

public class SpearMobsTrigger extends SimpleCriterionTrigger<SpearMobsTrigger.TriggerInstance> {
   @Override
   public Codec<SpearMobsTrigger.TriggerInstance> codec() {
      return SpearMobsTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469702_, int p_468199_) {
      this.trigger(p_469702_, p_467902_ -> p_467902_.matches(p_468199_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Integer> count) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<SpearMobsTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467924_ -> p_467924_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SpearMobsTrigger.TriggerInstance::player),
               ExtraCodecs.POSITIVE_INT.optionalFieldOf("count").forGetter(SpearMobsTrigger.TriggerInstance::count)
            )
            .apply(p_467924_, SpearMobsTrigger.TriggerInstance::new)
      );

      public static Criterion<SpearMobsTrigger.TriggerInstance> spearMobs(int p_469901_) {
         return CriteriaTriggers.SPEAR_MOBS_TRIGGER.createCriterion(new SpearMobsTrigger.TriggerInstance(Optional.empty(), Optional.of(p_469901_)));
      }

      public boolean matches(int p_469908_) {
         return this.count.isEmpty() || p_469908_ >= this.count.get();
      }
   }
}
