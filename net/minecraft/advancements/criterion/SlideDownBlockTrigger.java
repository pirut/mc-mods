package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SlideDownBlockTrigger extends SimpleCriterionTrigger<SlideDownBlockTrigger.TriggerInstance> {
   @Override
   public Codec<SlideDownBlockTrigger.TriggerInstance> codec() {
      return SlideDownBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468808_, BlockState p_467962_) {
      this.trigger(p_468808_, p_469428_ -> p_469428_.matches(p_467962_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<StatePropertiesPredicate> state)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<SlideDownBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_468718_ -> p_468718_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SlideDownBlockTrigger.TriggerInstance::player),
                  BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(SlideDownBlockTrigger.TriggerInstance::block),
                  StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(SlideDownBlockTrigger.TriggerInstance::state)
               )
               .apply(p_468718_, SlideDownBlockTrigger.TriggerInstance::new)
         )
         .validate(SlideDownBlockTrigger.TriggerInstance::validate);

      private static DataResult<SlideDownBlockTrigger.TriggerInstance> validate(SlideDownBlockTrigger.TriggerInstance p_469624_) {
         return p_469624_.block
            .<DataResult<SlideDownBlockTrigger.TriggerInstance>>flatMap(
               p_468227_ -> p_469624_.state
                  .<String>flatMap(p_468724_ -> p_468724_.checkState(((Block)p_468227_.value()).getStateDefinition()))
                  .map(p_469304_ -> DataResult.error(() -> "Block" + p_468227_ + " has no property " + p_469304_))
            )
            .orElseGet(() -> DataResult.success(p_469624_));
      }

      public static Criterion<SlideDownBlockTrigger.TriggerInstance> slidesDownBlock(Block p_467514_) {
         return CriteriaTriggers.HONEY_BLOCK_SLIDE
            .createCriterion(new SlideDownBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(p_467514_.builtInRegistryHolder()), Optional.empty()));
      }

      public boolean matches(BlockState p_467781_) {
         return this.block.isPresent() && !p_467781_.is(this.block.get()) ? false : !this.state.isPresent() || this.state.get().matches(p_467781_);
      }
   }
}
