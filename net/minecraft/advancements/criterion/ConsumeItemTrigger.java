package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ConsumeItemTrigger extends SimpleCriterionTrigger<ConsumeItemTrigger.TriggerInstance> {
   @Override
   public Codec<ConsumeItemTrigger.TriggerInstance> codec() {
      return ConsumeItemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468823_, ItemStack p_469441_) {
      this.trigger(p_468823_, p_468137_ -> p_468137_.matches(p_469441_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ConsumeItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468661_ -> p_468661_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ConsumeItemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ConsumeItemTrigger.TriggerInstance::item)
            )
            .apply(p_468661_, ConsumeItemTrigger.TriggerInstance::new)
      );

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem() {
         return CriteriaTriggers.CONSUME_ITEM.createCriterion(new ConsumeItemTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem(HolderGetter<Item> p_467424_, ItemLike p_467397_) {
         return usedItem(ItemPredicate.Builder.item().of(p_467424_, p_467397_.asItem()));
      }

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem(ItemPredicate.Builder p_467450_) {
         return CriteriaTriggers.CONSUME_ITEM.createCriterion(new ConsumeItemTrigger.TriggerInstance(Optional.empty(), Optional.of(p_467450_.build())));
      }

      public boolean matches(ItemStack p_468709_) {
         return this.item.isEmpty() || this.item.get().test(p_468709_);
      }
   }
}
