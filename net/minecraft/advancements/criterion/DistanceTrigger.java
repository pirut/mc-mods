package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class DistanceTrigger extends SimpleCriterionTrigger<DistanceTrigger.TriggerInstance> {
   @Override
   public Codec<DistanceTrigger.TriggerInstance> codec() {
      return DistanceTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467918_, Vec3 p_467916_) {
      Vec3 vec3 = p_467918_.position();
      this.trigger(p_467918_, p_467709_ -> p_467709_.matches(p_467918_.level(), p_467916_, vec3));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<LocationPredicate> startPosition, Optional<DistancePredicate> distance)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<DistanceTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467106_ -> p_467106_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(DistanceTrigger.TriggerInstance::player),
               LocationPredicate.CODEC.optionalFieldOf("start_position").forGetter(DistanceTrigger.TriggerInstance::startPosition),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(DistanceTrigger.TriggerInstance::distance)
            )
            .apply(p_467106_, DistanceTrigger.TriggerInstance::new)
      );

      public static Criterion<DistanceTrigger.TriggerInstance> fallFromHeight(
         EntityPredicate.Builder p_468019_, DistancePredicate p_467269_, LocationPredicate.Builder p_468508_
      ) {
         return CriteriaTriggers.FALL_FROM_HEIGHT
            .createCriterion(
               new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_468019_)), Optional.of(p_468508_.build()), Optional.of(p_467269_))
            );
      }

      public static Criterion<DistanceTrigger.TriggerInstance> rideEntityInLava(EntityPredicate.Builder p_468251_, DistancePredicate p_469023_) {
         return CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER
            .createCriterion(new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_468251_)), Optional.empty(), Optional.of(p_469023_)));
      }

      public static Criterion<DistanceTrigger.TriggerInstance> travelledThroughNether(DistancePredicate p_467395_) {
         return CriteriaTriggers.NETHER_TRAVEL.createCriterion(new DistanceTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(p_467395_)));
      }

      public boolean matches(ServerLevel p_468997_, Vec3 p_467370_, Vec3 p_467471_) {
         return this.startPosition.isPresent() && !this.startPosition.get().matches(p_468997_, p_467370_.x, p_467370_.y, p_467370_.z)
            ? false
            : !this.distance.isPresent() || this.distance.get().matches(p_467370_.x, p_467370_.y, p_467370_.z, p_467471_.x, p_467471_.y, p_467471_.z);
      }
   }
}
