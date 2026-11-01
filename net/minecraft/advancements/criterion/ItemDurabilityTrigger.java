package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilityTrigger extends SimpleCriterionTrigger<ItemDurabilityTrigger.TriggerInstance> {
   @Override
   public Codec<ItemDurabilityTrigger.TriggerInstance> codec() {
      return ItemDurabilityTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467358_, ItemStack p_467702_, int p_469686_) {
      this.trigger(p_467358_, p_466854_ -> p_466854_.matches(p_467702_, p_469686_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, MinMaxBounds.Ints durability, MinMaxBounds.Ints delta)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ItemDurabilityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468626_ -> p_468626_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemDurabilityTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ItemDurabilityTrigger.TriggerInstance::item),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("durability", MinMaxBounds.Ints.ANY).forGetter(ItemDurabilityTrigger.TriggerInstance::durability),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("delta", MinMaxBounds.Ints.ANY).forGetter(ItemDurabilityTrigger.TriggerInstance::delta)
            )
            .apply(p_468626_, ItemDurabilityTrigger.TriggerInstance::new)
      );

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(Optional<ItemPredicate> p_469412_, MinMaxBounds.Ints p_469814_) {
         return changedDurability(Optional.empty(), p_469412_, p_469814_);
      }

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(
         Optional<ContextAwarePredicate> p_468362_, Optional<ItemPredicate> p_467653_, MinMaxBounds.Ints p_467261_
      ) {
         return CriteriaTriggers.ITEM_DURABILITY_CHANGED
            .createCriterion(new ItemDurabilityTrigger.TriggerInstance(p_468362_, p_467653_, p_467261_, MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ItemStack p_468522_, int p_468453_) {
         if (this.item.isPresent() && !this.item.get().test(p_468522_)) {
            return false;
         } else {
            return !this.durability.matches(p_468522_.getMaxDamage() - p_468453_) ? false : this.delta.matches(p_468522_.getDamageValue() - p_468453_);
         }
      }
   }
}
