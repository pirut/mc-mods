package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.loot.LootContext;

public class LightningStrikeTrigger extends SimpleCriterionTrigger<LightningStrikeTrigger.TriggerInstance> {
   @Override
   public Codec<LightningStrikeTrigger.TriggerInstance> codec() {
      return LightningStrikeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468346_, LightningBolt p_469025_, List<Entity> p_466866_) {
      List<LootContext> list = p_466866_.stream().map(p_468903_ -> EntityPredicate.createContext(p_468346_, p_468903_)).collect(Collectors.toList());
      LootContext lootcontext = EntityPredicate.createContext(p_468346_, p_469025_);
      this.trigger(p_468346_, p_469122_ -> p_469122_.matches(lootcontext, list));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> lightning, Optional<ContextAwarePredicate> bystander)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<LightningStrikeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_469123_ -> p_469123_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(LightningStrikeTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("lightning").forGetter(LightningStrikeTrigger.TriggerInstance::lightning),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("bystander").forGetter(LightningStrikeTrigger.TriggerInstance::bystander)
            )
            .apply(p_469123_, LightningStrikeTrigger.TriggerInstance::new)
      );

      public static Criterion<LightningStrikeTrigger.TriggerInstance> lightningStrike(Optional<EntityPredicate> p_468921_, Optional<EntityPredicate> p_469254_) {
         return CriteriaTriggers.LIGHTNING_STRIKE
            .createCriterion(new LightningStrikeTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_468921_), EntityPredicate.wrap(p_469254_)));
      }

      public boolean matches(LootContext p_467634_, List<LootContext> p_467162_) {
         return this.lightning.isPresent() && !this.lightning.get().matches(p_467634_)
            ? false
            : !this.bystander.isPresent() || !p_467162_.stream().noneMatch(this.bystander.get()::matches);
      }

      @Override
      public void validate(CriterionValidator p_468144_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_468144_);
         p_468144_.validateEntity(this.lightning, "lightning");
         p_468144_.validateEntity(this.bystander, "bystander");
      }
   }
}
