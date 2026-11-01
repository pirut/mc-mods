package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public record DamagePredicate(
   MinMaxBounds.Doubles dealtDamage,
   MinMaxBounds.Doubles takenDamage,
   Optional<EntityPredicate> sourceEntity,
   Optional<Boolean> blocked,
   Optional<DamageSourcePredicate> type
) {
   public static final Codec<DamagePredicate> CODEC = RecordCodecBuilder.create(
      p_467861_ -> p_467861_.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("dealt", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::dealtDamage),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("taken", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::takenDamage),
            EntityPredicate.CODEC.optionalFieldOf("source_entity").forGetter(DamagePredicate::sourceEntity),
            Codec.BOOL.optionalFieldOf("blocked").forGetter(DamagePredicate::blocked),
            DamageSourcePredicate.CODEC.optionalFieldOf("type").forGetter(DamagePredicate::type)
         )
         .apply(p_467861_, DamagePredicate::new)
   );

   public boolean matches(ServerPlayer p_469397_, DamageSource p_468721_, float p_469630_, float p_469596_, boolean p_468302_) {
      if (!this.dealtDamage.matches(p_469630_)) {
         return false;
      } else if (!this.takenDamage.matches(p_469596_)) {
         return false;
      } else if (this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(p_469397_, p_468721_.getEntity())) {
         return false;
      } else {
         return this.blocked.isPresent() && this.blocked.get() != p_468302_ ? false : !this.type.isPresent() || this.type.get().matches(p_469397_, p_468721_);
      }
   }

   public static class Builder {
      private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
      private Optional<EntityPredicate> sourceEntity = Optional.empty();
      private Optional<Boolean> blocked = Optional.empty();
      private Optional<DamageSourcePredicate> type = Optional.empty();

      public static DamagePredicate.Builder damageInstance() {
         return new DamagePredicate.Builder();
      }

      public DamagePredicate.Builder dealtDamage(MinMaxBounds.Doubles p_468923_) {
         this.dealtDamage = p_468923_;
         return this;
      }

      public DamagePredicate.Builder takenDamage(MinMaxBounds.Doubles p_469587_) {
         this.takenDamage = p_469587_;
         return this;
      }

      public DamagePredicate.Builder sourceEntity(EntityPredicate p_468321_) {
         this.sourceEntity = Optional.of(p_468321_);
         return this;
      }

      public DamagePredicate.Builder blocked(Boolean p_469094_) {
         this.blocked = Optional.of(p_469094_);
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate p_469399_) {
         this.type = Optional.of(p_469399_);
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate.Builder p_468944_) {
         this.type = Optional.of(p_468944_.build());
         return this;
      }

      public DamagePredicate build() {
         return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
      }
   }
}
