package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class UsedEnderEyeTrigger extends SimpleCriterionTrigger<UsedEnderEyeTrigger.TriggerInstance> {
   @Override
   public Codec<UsedEnderEyeTrigger.TriggerInstance> codec() {
      return UsedEnderEyeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468719_, BlockPos p_468779_) {
      double d0 = p_468719_.getX() - p_468779_.getX();
      double d1 = p_468719_.getZ() - p_468779_.getZ();
      double d2 = d0 * d0 + d1 * d1;
      this.trigger(p_468719_, p_467689_ -> p_467689_.matches(d2));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Doubles distance) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsedEnderEyeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467428_ -> p_467428_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(UsedEnderEyeTrigger.TriggerInstance::player),
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("distance", MinMaxBounds.Doubles.ANY).forGetter(UsedEnderEyeTrigger.TriggerInstance::distance)
            )
            .apply(p_467428_, UsedEnderEyeTrigger.TriggerInstance::new)
      );

      public boolean matches(double p_468114_) {
         return this.distance.matchesSqr(p_468114_);
      }
   }
}
