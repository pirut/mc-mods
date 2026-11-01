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

public class EnterBlockTrigger extends SimpleCriterionTrigger<EnterBlockTrigger.TriggerInstance> {
   @Override
   public Codec<EnterBlockTrigger.TriggerInstance> codec() {
      return EnterBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469923_, BlockState p_468407_) {
      this.trigger(p_469923_, p_467240_ -> p_467240_.matches(p_468407_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<StatePropertiesPredicate> state)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EnterBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_469089_ -> p_469089_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EnterBlockTrigger.TriggerInstance::player),
                  BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(EnterBlockTrigger.TriggerInstance::block),
                  StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(EnterBlockTrigger.TriggerInstance::state)
               )
               .apply(p_469089_, EnterBlockTrigger.TriggerInstance::new)
         )
         .validate(EnterBlockTrigger.TriggerInstance::validate);

      private static DataResult<EnterBlockTrigger.TriggerInstance> validate(EnterBlockTrigger.TriggerInstance p_469607_) {
         return p_469607_.block
            .<DataResult<EnterBlockTrigger.TriggerInstance>>flatMap(
               p_469588_ -> p_469607_.state
                  .<String>flatMap(p_467282_ -> p_467282_.checkState(((Block)p_469588_.value()).getStateDefinition()))
                  .map(p_469578_ -> DataResult.error(() -> "Block" + p_469588_ + " has no property " + p_469578_))
            )
            .orElseGet(() -> DataResult.success(p_469607_));
      }

      public static Criterion<EnterBlockTrigger.TriggerInstance> entersBlock(Block p_468345_) {
         return CriteriaTriggers.ENTER_BLOCK
            .createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(p_468345_.builtInRegistryHolder()), Optional.empty()));
      }

      public boolean matches(BlockState p_467539_) {
         return this.block.isPresent() && !p_467539_.is(this.block.get()) ? false : !this.state.isPresent() || this.state.get().matches(p_467539_);
      }
   }
}
