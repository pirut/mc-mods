package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class TargetBlockTrigger extends SimpleCriterionTrigger<TargetBlockTrigger.TriggerInstance> {
   @Override
   public Codec<TargetBlockTrigger.TriggerInstance> codec() {
      return TargetBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469187_, Entity p_467661_, Vec3 p_469893_, int p_467740_) {
      LootContext lootcontext = EntityPredicate.createContext(p_469187_, p_467661_);
      this.trigger(p_469187_, p_469404_ -> p_469404_.matches(lootcontext, p_469893_, p_467740_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints signalStrength, Optional<ContextAwarePredicate> projectile)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<TargetBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467752_ -> p_467752_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TargetBlockTrigger.TriggerInstance::player),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("signal_strength", MinMaxBounds.Ints.ANY).forGetter(TargetBlockTrigger.TriggerInstance::signalStrength),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("projectile").forGetter(TargetBlockTrigger.TriggerInstance::projectile)
            )
            .apply(p_467752_, TargetBlockTrigger.TriggerInstance::new)
      );

      public static Criterion<TargetBlockTrigger.TriggerInstance> targetHit(MinMaxBounds.Ints p_468770_, Optional<ContextAwarePredicate> p_468922_) {
         return CriteriaTriggers.TARGET_BLOCK_HIT.createCriterion(new TargetBlockTrigger.TriggerInstance(Optional.empty(), p_468770_, p_468922_));
      }

      public boolean matches(LootContext p_467254_, Vec3 p_467410_, int p_468431_) {
         return !this.signalStrength.matches(p_468431_) ? false : !this.projectile.isPresent() || this.projectile.get().matches(p_467254_);
      }

      @Override
      public void validate(CriterionValidator p_469641_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_469641_);
         p_469641_.validateEntity(this.projectile, "projectile");
      }
   }
}
