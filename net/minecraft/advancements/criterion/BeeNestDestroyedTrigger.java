package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BeeNestDestroyedTrigger extends SimpleCriterionTrigger<BeeNestDestroyedTrigger.TriggerInstance> {
   @Override
   public Codec<BeeNestDestroyedTrigger.TriggerInstance> codec() {
      return BeeNestDestroyedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467047_, BlockState p_467484_, ItemStack p_469392_, int p_468853_) {
      this.trigger(p_467047_, p_469327_ -> p_469327_.matches(p_467484_, p_469392_, p_468853_));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<ItemPredicate> item, MinMaxBounds.Ints beesInside
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<BeeNestDestroyedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467812_ -> p_467812_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(BeeNestDestroyedTrigger.TriggerInstance::player),
               BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(BeeNestDestroyedTrigger.TriggerInstance::block),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(BeeNestDestroyedTrigger.TriggerInstance::item),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("num_bees_inside", MinMaxBounds.Ints.ANY).forGetter(BeeNestDestroyedTrigger.TriggerInstance::beesInside)
            )
            .apply(p_467812_, BeeNestDestroyedTrigger.TriggerInstance::new)
      );

      public static Criterion<BeeNestDestroyedTrigger.TriggerInstance> destroyedBeeNest(
         Block p_466896_, ItemPredicate.Builder p_467302_, MinMaxBounds.Ints p_469572_
      ) {
         return CriteriaTriggers.BEE_NEST_DESTROYED
            .createCriterion(
               new BeeNestDestroyedTrigger.TriggerInstance(
                  Optional.empty(), Optional.of(p_466896_.builtInRegistryHolder()), Optional.of(p_467302_.build()), p_469572_
               )
            );
      }

      public boolean matches(BlockState p_466928_, ItemStack p_468427_, int p_467093_) {
         if (this.block.isPresent() && !p_466928_.is(this.block.get())) {
            return false;
         } else {
            return this.item.isPresent() && !this.item.get().test(p_468427_) ? false : this.beesInside.matches(p_467093_);
         }
      }
   }
}
