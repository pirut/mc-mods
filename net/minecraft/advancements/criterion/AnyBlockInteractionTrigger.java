package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class AnyBlockInteractionTrigger extends SimpleCriterionTrigger<AnyBlockInteractionTrigger.TriggerInstance> {
   @Override
   public Codec<AnyBlockInteractionTrigger.TriggerInstance> codec() {
      return AnyBlockInteractionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467750_, BlockPos p_467907_, ItemStack p_469475_) {
      ServerLevel serverlevel = p_467750_.level();
      BlockState blockstate = serverlevel.getBlockState(p_467907_);
      LootParams lootparams = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, p_467907_.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, p_467750_)
         .withParameter(LootContextParams.BLOCK_STATE, blockstate)
         .withParameter(LootContextParams.TOOL, p_469475_)
         .create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
      this.trigger(p_467750_, p_468118_ -> p_468118_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<AnyBlockInteractionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_469168_ -> p_469168_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(AnyBlockInteractionTrigger.TriggerInstance::player),
               ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(AnyBlockInteractionTrigger.TriggerInstance::location)
            )
            .apply(p_469168_, AnyBlockInteractionTrigger.TriggerInstance::new)
      );

      public boolean matches(LootContext p_469157_) {
         return this.location.isEmpty() || this.location.get().matches(p_469157_);
      }

      @Override
      public void validate(CriterionValidator p_467082_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_467082_);
         this.location.ifPresent(p_468604_ -> p_467082_.validate(p_468604_, LootContextParamSets.ADVANCEMENT_LOCATION, "location"));
      }
   }
}
