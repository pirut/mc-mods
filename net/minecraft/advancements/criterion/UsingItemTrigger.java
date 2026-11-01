package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class UsingItemTrigger extends SimpleCriterionTrigger<UsingItemTrigger.TriggerInstance> {
   @Override
   public Codec<UsingItemTrigger.TriggerInstance> codec() {
      return UsingItemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467200_, ItemStack p_469433_) {
      this.trigger(p_467200_, p_469857_ -> p_469857_.matches(p_469433_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsingItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468239_ -> p_468239_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(UsingItemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(UsingItemTrigger.TriggerInstance::item)
            )
            .apply(p_468239_, UsingItemTrigger.TriggerInstance::new)
      );

      public static Criterion<UsingItemTrigger.TriggerInstance> lookingAt(EntityPredicate.Builder p_468327_, ItemPredicate.Builder p_468973_) {
         return CriteriaTriggers.USING_ITEM
            .createCriterion(new UsingItemTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_468327_)), Optional.of(p_468973_.build())));
      }

      public boolean matches(ItemStack p_467490_) {
         return !this.item.isPresent() || this.item.get().test(p_467490_);
      }
   }
}
