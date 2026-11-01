package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LevitationTrigger extends SimpleCriterionTrigger<LevitationTrigger.TriggerInstance> {
   @Override
   public Codec<LevitationTrigger.TriggerInstance> codec() {
      return LevitationTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468700_, Vec3 p_468049_, int p_467558_) {
      this.trigger(p_468700_, p_467658_ -> p_467658_.matches(p_468700_, p_468049_, p_467558_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DistancePredicate> distance, MinMaxBounds.Ints duration)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<LevitationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_469096_ -> p_469096_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(LevitationTrigger.TriggerInstance::player),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(LevitationTrigger.TriggerInstance::distance),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("duration", MinMaxBounds.Ints.ANY).forGetter(LevitationTrigger.TriggerInstance::duration)
            )
            .apply(p_469096_, LevitationTrigger.TriggerInstance::new)
      );

      public static Criterion<LevitationTrigger.TriggerInstance> levitated(DistancePredicate p_468090_) {
         return CriteriaTriggers.LEVITATION
            .createCriterion(new LevitationTrigger.TriggerInstance(Optional.empty(), Optional.of(p_468090_), MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ServerPlayer p_467881_, Vec3 p_468696_, int p_468274_) {
         return this.distance.isPresent()
               && !this.distance.get().matches(p_468696_.x, p_468696_.y, p_468696_.z, p_467881_.getX(), p_467881_.getY(), p_467881_.getZ())
            ? false
            : this.duration.matches(p_468274_);
      }
   }
}
