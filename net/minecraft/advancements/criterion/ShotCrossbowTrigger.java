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

public class ShotCrossbowTrigger extends SimpleCriterionTrigger<ShotCrossbowTrigger.TriggerInstance> {
   @Override
   public Codec<ShotCrossbowTrigger.TriggerInstance> codec() {
      return ShotCrossbowTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469017_, ItemStack p_469903_) {
      this.trigger(p_469017_, p_468761_ -> p_468761_.matches(p_469903_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ShotCrossbowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467579_ -> p_467579_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ShotCrossbowTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ShotCrossbowTrigger.TriggerInstance::item)
            )
            .apply(p_467579_, ShotCrossbowTrigger.TriggerInstance::new)
      );

      public static Criterion<ShotCrossbowTrigger.TriggerInstance> shotCrossbow(Optional<ItemPredicate> p_469799_) {
         return CriteriaTriggers.SHOT_CROSSBOW.createCriterion(new ShotCrossbowTrigger.TriggerInstance(Optional.empty(), p_469799_));
      }

      public static Criterion<ShotCrossbowTrigger.TriggerInstance> shotCrossbow(HolderGetter<Item> p_467222_, ItemLike p_468801_) {
         return CriteriaTriggers.SHOT_CROSSBOW
            .createCriterion(
               new ShotCrossbowTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(p_467222_, p_468801_).build()))
            );
      }

      public boolean matches(ItemStack p_467715_) {
         return this.item.isEmpty() || this.item.get().test(p_467715_);
      }
   }
}
