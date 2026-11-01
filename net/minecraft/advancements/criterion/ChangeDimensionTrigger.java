package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class ChangeDimensionTrigger extends SimpleCriterionTrigger<ChangeDimensionTrigger.TriggerInstance> {
   @Override
   public Codec<ChangeDimensionTrigger.TriggerInstance> codec() {
      return ChangeDimensionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468237_, ResourceKey<Level> p_469451_, ResourceKey<Level> p_466992_) {
      this.trigger(p_468237_, p_468972_ -> p_468972_.matches(p_469451_, p_466992_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceKey<Level>> from, Optional<ResourceKey<Level>> to)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ChangeDimensionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467574_ -> p_467574_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ChangeDimensionTrigger.TriggerInstance::player),
               ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("from").forGetter(ChangeDimensionTrigger.TriggerInstance::from),
               ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("to").forGetter(ChangeDimensionTrigger.TriggerInstance::to)
            )
            .apply(p_467574_, ChangeDimensionTrigger.TriggerInstance::new)
      );

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension() {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension(ResourceKey<Level> p_467045_, ResourceKey<Level> p_468286_) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(p_467045_), Optional.of(p_468286_)));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionTo(ResourceKey<Level> p_469437_) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(p_469437_)));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionFrom(ResourceKey<Level> p_469758_) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(p_469758_), Optional.empty()));
      }

      public boolean matches(ResourceKey<Level> p_469274_, ResourceKey<Level> p_469396_) {
         return this.from.isPresent() && this.from.get() != p_469274_ ? false : !this.to.isPresent() || this.to.get() == p_469396_;
      }
   }
}
