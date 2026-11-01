package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public class EffectsChangedTrigger extends SimpleCriterionTrigger<EffectsChangedTrigger.TriggerInstance> {
   @Override
   public Codec<EffectsChangedTrigger.TriggerInstance> codec() {
      return EffectsChangedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469831_, @Nullable Entity p_468168_) {
      LootContext lootcontext = p_468168_ != null ? EntityPredicate.createContext(p_469831_, p_468168_) : null;
      this.trigger(p_469831_, p_469603_ -> p_469603_.matches(p_469831_, lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<MobEffectsPredicate> effects, Optional<ContextAwarePredicate> source)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EffectsChangedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_469345_ -> p_469345_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EffectsChangedTrigger.TriggerInstance::player),
               MobEffectsPredicate.CODEC.optionalFieldOf("effects").forGetter(EffectsChangedTrigger.TriggerInstance::effects),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("source").forGetter(EffectsChangedTrigger.TriggerInstance::source)
            )
            .apply(p_469345_, EffectsChangedTrigger.TriggerInstance::new)
      );

      public static Criterion<EffectsChangedTrigger.TriggerInstance> hasEffects(MobEffectsPredicate.Builder p_467763_) {
         return CriteriaTriggers.EFFECTS_CHANGED
            .createCriterion(new EffectsChangedTrigger.TriggerInstance(Optional.empty(), p_467763_.build(), Optional.empty()));
      }

      public static Criterion<EffectsChangedTrigger.TriggerInstance> gotEffectsFrom(EntityPredicate.Builder p_466886_) {
         return CriteriaTriggers.EFFECTS_CHANGED
            .createCriterion(
               new EffectsChangedTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(p_466886_.build())))
            );
      }

      public boolean matches(ServerPlayer p_466845_, @Nullable LootContext p_468638_) {
         return this.effects.isPresent() && !this.effects.get().matches((LivingEntity)p_466845_)
            ? false
            : !this.source.isPresent() || p_468638_ != null && this.source.get().matches(p_468638_);
      }

      @Override
      public void validate(CriterionValidator p_468730_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_468730_);
         p_468730_.validateEntity(this.source, "source");
      }
   }
}
