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

public class UsedTotemTrigger extends SimpleCriterionTrigger<UsedTotemTrigger.TriggerInstance> {
   @Override
   public Codec<UsedTotemTrigger.TriggerInstance> codec() {
      return UsedTotemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469202_, ItemStack p_468791_) {
      this.trigger(p_469202_, p_468435_ -> p_468435_.matches(p_468791_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsedTotemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467051_ -> p_467051_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(UsedTotemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(UsedTotemTrigger.TriggerInstance::item)
            )
            .apply(p_467051_, UsedTotemTrigger.TriggerInstance::new)
      );

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(ItemPredicate p_468494_) {
         return CriteriaTriggers.USED_TOTEM.createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(p_468494_)));
      }

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(HolderGetter<Item> p_467167_, ItemLike p_469931_) {
         return CriteriaTriggers.USED_TOTEM
            .createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(p_467167_, p_469931_).build())));
      }

      public boolean matches(ItemStack p_469390_) {
         return this.item.isEmpty() || this.item.get().test(p_469390_);
      }
   }
}
