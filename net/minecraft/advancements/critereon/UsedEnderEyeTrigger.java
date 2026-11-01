package net.minecraft.advancements.critereon;

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

   public void trigger(ServerPlayer p_73936_, BlockPos p_73937_) {
      double $$2 = p_73936_.getX() - p_73937_.getX();
      double $$3 = p_73936_.getZ() - p_73937_.getZ();
      double $$4 = $$2 * $$2 + $$3 * $$3;
      this.trigger(p_73936_, p_73934_ -> p_73934_.matches($$4));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Doubles distance) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsedEnderEyeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_337402_ -> p_337402_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(UsedEnderEyeTrigger.TriggerInstance::player),
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("distance", MinMaxBounds.Doubles.ANY).forGetter(UsedEnderEyeTrigger.TriggerInstance::distance)
            )
            .apply(p_337402_, UsedEnderEyeTrigger.TriggerInstance::new)
      );

      public boolean matches(double p_73952_) {
         return this.distance.matchesSqr(p_73952_);
      }
   }
}
