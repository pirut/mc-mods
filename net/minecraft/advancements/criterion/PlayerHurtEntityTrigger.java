package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerHurtEntityTrigger extends SimpleCriterionTrigger<PlayerHurtEntityTrigger.TriggerInstance> {
   @Override
   public Codec<PlayerHurtEntityTrigger.TriggerInstance> codec() {
      return PlayerHurtEntityTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469000_, Entity p_467019_, DamageSource p_468402_, float p_467478_, float p_468085_, boolean p_468662_) {
      LootContext lootcontext = EntityPredicate.createContext(p_469000_, p_467019_);
      this.trigger(p_469000_, p_467617_ -> p_467617_.matches(p_469000_, lootcontext, p_468402_, p_467478_, p_468085_, p_468662_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DamagePredicate> damage, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PlayerHurtEntityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467828_ -> p_467828_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerHurtEntityTrigger.TriggerInstance::player),
               DamagePredicate.CODEC.optionalFieldOf("damage").forGetter(PlayerHurtEntityTrigger.TriggerInstance::damage),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(PlayerHurtEntityTrigger.TriggerInstance::entity)
            )
            .apply(p_467828_, PlayerHurtEntityTrigger.TriggerInstance::new)
      );

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity() {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(Optional<DamagePredicate> p_467488_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), p_467488_, Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(DamagePredicate.Builder p_469819_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(p_469819_.build()), Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(Optional<EntityPredicate> p_469016_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), EntityPredicate.wrap(p_469016_)));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(
         Optional<DamagePredicate> p_468325_, Optional<EntityPredicate> p_469474_
      ) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), p_468325_, EntityPredicate.wrap(p_469474_)));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(DamagePredicate.Builder p_469484_, Optional<EntityPredicate> p_469169_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(p_469484_.build()), EntityPredicate.wrap(p_469169_)));
      }

      public boolean matches(ServerPlayer p_467645_, LootContext p_468941_, DamageSource p_467525_, float p_469100_, float p_467036_, boolean p_468870_) {
         return this.damage.isPresent() && !this.damage.get().matches(p_467645_, p_467525_, p_469100_, p_467036_, p_468870_)
            ? false
            : !this.entity.isPresent() || this.entity.get().matches(p_468941_);
      }

      @Override
      public void validate(CriterionValidator p_469050_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_469050_);
         p_469050_.validateEntity(this.entity, "entity");
      }
   }
}
