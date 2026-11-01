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

public class KilledTrigger extends SimpleCriterionTrigger<KilledTrigger.TriggerInstance> {
   @Override
   public Codec<KilledTrigger.TriggerInstance> codec() {
      return KilledTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469272_, Entity p_466950_, DamageSource p_469752_) {
      LootContext lootcontext = EntityPredicate.createContext(p_469272_, p_466950_);
      this.trigger(p_469272_, p_469322_ -> p_469322_.matches(p_469272_, lootcontext, p_469752_));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entityPredicate, Optional<DamageSourcePredicate> killingBlow
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<KilledTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468935_ -> p_468935_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(KilledTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(KilledTrigger.TriggerInstance::entityPredicate),
               DamageSourcePredicate.CODEC.optionalFieldOf("killing_blow").forGetter(KilledTrigger.TriggerInstance::killingBlow)
            )
            .apply(p_468935_, KilledTrigger.TriggerInstance::new)
      );

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> p_467468_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_467468_), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder p_469529_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_469529_)), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity() {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> p_468755_, Optional<DamageSourcePredicate> p_469870_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_468755_), p_469870_));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder p_467806_, Optional<DamageSourcePredicate> p_467276_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_467806_)), p_467276_));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(Optional<EntityPredicate> p_468795_, DamageSourcePredicate.Builder p_467318_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_468795_), Optional.of(p_467318_.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntity(EntityPredicate.Builder p_467532_, DamageSourcePredicate.Builder p_467443_) {
         return CriteriaTriggers.PLAYER_KILLED_ENTITY
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_467532_)), Optional.of(p_467443_.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> playerKilledEntityNearSculkCatalyst() {
         return CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> p_469352_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_469352_), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder p_469216_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_469216_)), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer() {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER.createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> p_468034_, Optional<DamageSourcePredicate> p_468242_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_468034_), p_468242_));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder p_467035_, Optional<DamageSourcePredicate> p_468601_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_467035_)), p_468601_));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(Optional<EntityPredicate> p_468485_, DamageSourcePredicate.Builder p_469796_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_468485_), Optional.of(p_469796_.build())));
      }

      public static Criterion<KilledTrigger.TriggerInstance> entityKilledPlayer(EntityPredicate.Builder p_468107_, DamageSourcePredicate.Builder p_468503_) {
         return CriteriaTriggers.ENTITY_KILLED_PLAYER
            .createCriterion(new KilledTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_468107_)), Optional.of(p_468503_.build())));
      }

      public boolean matches(ServerPlayer p_469685_, LootContext p_467912_, DamageSource p_468241_) {
         return this.killingBlow.isPresent() && !this.killingBlow.get().matches(p_469685_, p_468241_)
            ? false
            : this.entityPredicate.isEmpty() || this.entityPredicate.get().matches(p_467912_);
      }

      @Override
      public void validate(CriterionValidator p_469689_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_469689_);
         p_469689_.validateEntity(this.entityPredicate, "entity");
      }
   }
}
