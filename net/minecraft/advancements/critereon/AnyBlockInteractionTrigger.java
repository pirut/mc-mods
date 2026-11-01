package net.minecraft.advancements.critereon;

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

   public void trigger(ServerPlayer p_319979_, BlockPos p_320093_, ItemStack p_320553_) {
      ServerLevel $$3 = p_319979_.serverLevel();
      BlockState $$4 = $$3.getBlockState(p_320093_);
      LootParams $$5 = new LootParams.Builder($$3)
         .withParameter(LootContextParams.ORIGIN, p_320093_.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, p_319979_)
         .withParameter(LootContextParams.BLOCK_STATE, $$4)
         .withParameter(LootContextParams.TOOL, p_320553_)
         .create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext $$6 = new LootContext.Builder($$5).create(Optional.empty());
      this.trigger(p_319979_, p_320901_ -> p_320901_.matches($$6));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<AnyBlockInteractionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_337340_ -> p_337340_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(AnyBlockInteractionTrigger.TriggerInstance::player),
               ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(AnyBlockInteractionTrigger.TriggerInstance::location)
            )
            .apply(p_337340_, AnyBlockInteractionTrigger.TriggerInstance::new)
      );

      public boolean matches(LootContext p_320453_) {
         return this.location.isEmpty() || this.location.get().matches(p_320453_);
      }

      @Override
      public void validate(CriterionValidator p_319820_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_319820_);
         this.location.ifPresent(p_320455_ -> p_319820_.validate(p_320455_, LootContextParamSets.ADVANCEMENT_LOCATION, ".location"));
      }
   }
}
