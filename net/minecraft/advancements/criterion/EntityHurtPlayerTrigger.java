package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class EntityHurtPlayerTrigger extends SimpleCriterionTrigger<EntityHurtPlayerTrigger.TriggerInstance> {
   @Override
   public Codec<EntityHurtPlayerTrigger.TriggerInstance> codec() {
      return EntityHurtPlayerTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468804_, DamageSource p_466880_, float p_468379_, float p_469223_, boolean p_468748_) {
      this.trigger(p_468804_, p_469721_ -> p_469721_.matches(p_468804_, p_466880_, p_468379_, p_469223_, p_468748_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DamagePredicate> damage) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EntityHurtPlayerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467028_ -> p_467028_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EntityHurtPlayerTrigger.TriggerInstance::player),
               DamagePredicate.CODEC.optionalFieldOf("damage").forGetter(EntityHurtPlayerTrigger.TriggerInstance::damage)
            )
            .apply(p_467028_, EntityHurtPlayerTrigger.TriggerInstance::new)
      );

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer() {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(DamagePredicate p_467037_) {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(p_467037_)));
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(DamagePredicate.Builder p_467819_) {
         return CriteriaTriggers.ENTITY_HURT_PLAYER
            .createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(p_467819_.build())));
      }

      public boolean matches(ServerPlayer p_468807_, DamageSource p_468141_, float p_469936_, float p_467293_, boolean p_467369_) {
         return !this.damage.isPresent() || this.damage.get().matches(p_468807_, p_468141_, p_469936_, p_467293_, p_467369_);
      }
   }
}
