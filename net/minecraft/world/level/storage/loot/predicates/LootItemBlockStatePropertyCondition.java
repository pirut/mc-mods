package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record LootItemBlockStatePropertyCondition(Holder<Block> block, Optional<StatePropertiesPredicate> properties) implements LootItemCondition {
   public static final MapCodec<LootItemBlockStatePropertyCondition> CODEC = RecordCodecBuilder.mapCodec(
         p_466802_ -> p_466802_.group(
               BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(LootItemBlockStatePropertyCondition::block),
               StatePropertiesPredicate.CODEC.optionalFieldOf("properties").forGetter(LootItemBlockStatePropertyCondition::properties)
            )
            .apply(p_466802_, LootItemBlockStatePropertyCondition::new)
      )
      .validate(LootItemBlockStatePropertyCondition::validate);

   private static DataResult<LootItemBlockStatePropertyCondition> validate(LootItemBlockStatePropertyCondition p_298471_) {
      return p_298471_.properties()
         .flatMap(p_466801_ -> p_466801_.checkState(p_298471_.block().value().getStateDefinition()))
         .map(p_299129_ -> DataResult.error(() -> "Block " + p_298471_.block() + " has no property" + p_299129_))
         .orElse(DataResult.success(p_298471_));
   }

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.BLOCK_STATE_PROPERTY;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.BLOCK_STATE);
   }

   public boolean test(LootContext p_81772_) {
      BlockState blockstate = p_81772_.getOptionalParameter(LootContextParams.BLOCK_STATE);
      return blockstate != null && blockstate.is(this.block) && (this.properties.isEmpty() || this.properties.get().matches(blockstate));
   }

   public static LootItemBlockStatePropertyCondition.Builder hasBlockStateProperties(Block p_81770_) {
      return new LootItemBlockStatePropertyCondition.Builder(p_81770_);
   }

   public static class Builder implements LootItemCondition.Builder {
      private final Holder<Block> block;
      private Optional<StatePropertiesPredicate> properties = Optional.empty();

      public Builder(Block p_81783_) {
         this.block = p_81783_.builtInRegistryHolder();
      }

      public LootItemBlockStatePropertyCondition.Builder setProperties(StatePropertiesPredicate.Builder p_467007_) {
         this.properties = p_467007_.build();
         return this;
      }

      @Override
      public LootItemCondition build() {
         return new LootItemBlockStatePropertyCondition(this.block, this.properties);
      }
   }
}
