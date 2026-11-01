package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FallAfterExplosionTrigger extends SimpleCriterionTrigger<FallAfterExplosionTrigger.TriggerInstance> {
   @Override
   public Codec<FallAfterExplosionTrigger.TriggerInstance> codec() {
      return FallAfterExplosionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468238_, Vec3 p_469567_, @Nullable Entity p_469636_) {
      Vec3 vec3 = p_468238_.position();
      LootContext lootcontext = p_469636_ != null ? EntityPredicate.createContext(p_468238_, p_469636_) : null;
      this.trigger(p_468238_, p_468880_ -> p_468880_.matches(p_468238_.level(), p_469567_, vec3, lootcontext));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player,
      Optional<LocationPredicate> startPosition,
      Optional<DistancePredicate> distance,
      Optional<ContextAwarePredicate> cause
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<FallAfterExplosionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468000_ -> p_468000_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(FallAfterExplosionTrigger.TriggerInstance::player),
               LocationPredicate.CODEC.optionalFieldOf("start_position").forGetter(FallAfterExplosionTrigger.TriggerInstance::startPosition),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(FallAfterExplosionTrigger.TriggerInstance::distance),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("cause").forGetter(FallAfterExplosionTrigger.TriggerInstance::cause)
            )
            .apply(p_468000_, FallAfterExplosionTrigger.TriggerInstance::new)
      );

      public static Criterion<FallAfterExplosionTrigger.TriggerInstance> fallAfterExplosion(DistancePredicate p_469174_, EntityPredicate.Builder p_467603_) {
         return CriteriaTriggers.FALL_AFTER_EXPLOSION
            .createCriterion(
               new FallAfterExplosionTrigger.TriggerInstance(
                  Optional.empty(), Optional.empty(), Optional.of(p_469174_), Optional.of(EntityPredicate.wrap(p_467603_))
               )
            );
      }

      @Override
      public void validate(CriterionValidator p_468672_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_468672_);
         p_468672_.validateEntity(this.cause(), "cause");
      }

      public boolean matches(ServerLevel p_469783_, Vec3 p_467503_, Vec3 p_468970_, @Nullable LootContext p_466897_) {
         if (this.startPosition.isPresent() && !this.startPosition.get().matches(p_469783_, p_467503_.x, p_467503_.y, p_467503_.z)) {
            return false;
         } else {
            return this.distance.isPresent() && !this.distance.get().matches(p_467503_.x, p_467503_.y, p_467503_.z, p_468970_.x, p_468970_.y, p_468970_.z)
               ? false
               : !this.cause.isPresent() || p_466897_ != null && this.cause.get().matches(p_466897_);
         }
      }
   }
}
