package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
   @Override
   public Codec<TradeTrigger.TriggerInstance> codec() {
      return TradeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469137_, AbstractVillager p_481118_, ItemStack p_468353_) {
      LootContext lootcontext = EntityPredicate.createContext(p_469137_, p_481118_);
      this.trigger(p_469137_, p_467160_ -> p_467160_.matches(lootcontext, p_468353_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> villager, Optional<ItemPredicate> item)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<TradeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_469897_ -> p_469897_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TradeTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("villager").forGetter(TradeTrigger.TriggerInstance::villager),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TradeTrigger.TriggerInstance::item)
            )
            .apply(p_469897_, TradeTrigger.TriggerInstance::new)
      );

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager() {
         return CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager(EntityPredicate.Builder p_466968_) {
         return CriteriaTriggers.TRADE
            .createCriterion(new TradeTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_466968_)), Optional.empty(), Optional.empty()));
      }

      public boolean matches(LootContext p_467025_, ItemStack p_468505_) {
         return this.villager.isPresent() && !this.villager.get().matches(p_467025_) ? false : !this.item.isPresent() || this.item.get().test(p_468505_);
      }

      @Override
      public void validate(CriterionValidator p_467631_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_467631_);
         p_467631_.validateEntity(this.villager, "villager");
      }
   }
}
