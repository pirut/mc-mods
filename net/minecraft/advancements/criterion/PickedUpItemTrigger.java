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
import org.jspecify.annotations.Nullable;

public class PickedUpItemTrigger extends SimpleCriterionTrigger<PickedUpItemTrigger.TriggerInstance> {
   @Override
   public Codec<PickedUpItemTrigger.TriggerInstance> codec() {
      return PickedUpItemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467173_, ItemStack p_469527_, @Nullable Entity p_467262_) {
      LootContext lootcontext = EntityPredicate.createContext(p_467173_, p_467262_);
      this.trigger(p_467173_, p_467283_ -> p_467283_.matches(p_467173_, p_469527_, lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PickedUpItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_469946_ -> p_469946_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PickedUpItemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(PickedUpItemTrigger.TriggerInstance::item),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(PickedUpItemTrigger.TriggerInstance::entity)
            )
            .apply(p_469946_, PickedUpItemTrigger.TriggerInstance::new)
      );

      public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByEntity(
         ContextAwarePredicate p_469926_, Optional<ItemPredicate> p_468272_, Optional<ContextAwarePredicate> p_468403_
      ) {
         return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY
            .createCriterion(new PickedUpItemTrigger.TriggerInstance(Optional.of(p_469926_), p_468272_, p_468403_));
      }

      public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByPlayer(
         Optional<ContextAwarePredicate> p_468179_, Optional<ItemPredicate> p_467422_, Optional<ContextAwarePredicate> p_467126_
      ) {
         return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.createCriterion(new PickedUpItemTrigger.TriggerInstance(p_468179_, p_467422_, p_467126_));
      }

      public boolean matches(ServerPlayer p_467298_, ItemStack p_469967_, LootContext p_469971_) {
         return this.item.isPresent() && !this.item.get().test(p_469967_) ? false : !this.entity.isPresent() || this.entity.get().matches(p_469971_);
      }

      @Override
      public void validate(CriterionValidator p_469538_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_469538_);
         p_469538_.validateEntity(this.entity, "entity");
      }
   }
}
