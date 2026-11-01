package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerInteractTrigger extends SimpleCriterionTrigger<PlayerInteractTrigger.TriggerInstance> {
   @Override
   public Codec<PlayerInteractTrigger.TriggerInstance> codec() {
      return PlayerInteractTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469667_, ItemStack p_469104_, Entity p_467432_) {
      LootContext lootcontext = EntityPredicate.createContext(p_469667_, p_467432_);
      this.trigger(p_469667_, p_467495_ -> p_467495_.matches(p_469104_, lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PlayerInteractTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_466987_ -> p_466987_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerInteractTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(PlayerInteractTrigger.TriggerInstance::item),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(PlayerInteractTrigger.TriggerInstance::entity)
            )
            .apply(p_466987_, PlayerInteractTrigger.TriggerInstance::new)
      );

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(
         Optional<ContextAwarePredicate> p_468064_, ItemPredicate.Builder p_466867_, Optional<ContextAwarePredicate> p_467855_
      ) {
         return CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(p_468064_, Optional.of(p_466867_.build()), p_467855_));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> equipmentSheared(
         Optional<ContextAwarePredicate> p_469821_, ItemPredicate.Builder p_469988_, Optional<ContextAwarePredicate> p_468618_
      ) {
         return CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(p_469821_, Optional.of(p_469988_.build()), p_468618_));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> equipmentSheared(
         ItemPredicate.Builder p_469544_, Optional<ContextAwarePredicate> p_469063_
      ) {
         return CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(Optional.empty(), Optional.of(p_469544_.build()), p_469063_));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(
         ItemPredicate.Builder p_469962_, Optional<ContextAwarePredicate> p_469839_
      ) {
         return itemUsedOnEntity(Optional.empty(), p_469962_, p_469839_);
      }

      public boolean matches(ItemStack p_468735_, LootContext p_466974_) {
         return this.item.isPresent() && !this.item.get().test(p_468735_) ? false : this.entity.isEmpty() || this.entity.get().matches(p_466974_);
      }

      @Override
      public void validate(CriterionValidator p_468592_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_468592_);
         p_468592_.validateEntity(this.entity, "entity");
      }
   }
}
