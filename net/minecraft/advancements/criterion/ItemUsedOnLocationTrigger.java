package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class ItemUsedOnLocationTrigger extends SimpleCriterionTrigger<ItemUsedOnLocationTrigger.TriggerInstance> {
   @Override
   public Codec<ItemUsedOnLocationTrigger.TriggerInstance> codec() {
      return ItemUsedOnLocationTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_470005_, BlockPos p_468018_, ItemStack p_466962_) {
      ServerLevel serverlevel = p_470005_.level();
      BlockState blockstate = serverlevel.getBlockState(p_468018_);
      LootParams lootparams = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, p_468018_.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, p_470005_)
         .withParameter(LootContextParams.BLOCK_STATE, blockstate)
         .withParameter(LootContextParams.TOOL, p_466962_)
         .create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
      this.trigger(p_470005_, p_467581_ -> p_467581_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ItemUsedOnLocationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_469543_ -> p_469543_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::player),
               ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::location)
            )
            .apply(p_469543_, ItemUsedOnLocationTrigger.TriggerInstance::new)
      );

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block p_466997_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_466997_).build()
         );
         return CriteriaTriggers.PLACED_BLOCK
            .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(LootItemCondition.Builder... p_468650_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            Arrays.stream(p_468650_).map(LootItemCondition.Builder::build).toArray(LootItemCondition[]::new)
         );
         return CriteriaTriggers.PLACED_BLOCK
            .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      public static <T extends Comparable<T>> Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         Block p_469642_, Property<T> p_469928_, String p_468907_
      ) {
         StatePropertiesPredicate.Builder statepropertiespredicate$builder = StatePropertiesPredicate.Builder.properties().hasProperty(p_469928_, p_468907_);
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_469642_).setProperties(statepropertiespredicate$builder).build()
         );
         return CriteriaTriggers.PLACED_BLOCK
            .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         Block p_468388_, Property<Boolean> p_467275_, boolean p_468564_
      ) {
         return placedBlockWithProperties(p_468388_, p_467275_, String.valueOf(p_468564_));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(Block p_466823_, Property<Integer> p_469179_, int p_466982_) {
         return placedBlockWithProperties(p_466823_, p_469179_, String.valueOf(p_466982_));
      }

      public static <T extends Comparable<T> & StringRepresentable> Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         Block p_469882_, Property<T> p_469581_, T p_467219_
      ) {
         return placedBlockWithProperties(p_469882_, p_469581_, p_467219_.getSerializedName());
      }

      private static ItemUsedOnLocationTrigger.TriggerInstance itemUsedOnLocation(LocationPredicate.Builder p_468835_, ItemPredicate.Builder p_469026_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            LocationCheck.checkLocation(p_468835_).build(), MatchTool.toolMatches(p_469026_).build()
         );
         return new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> itemUsedOnBlock(LocationPredicate.Builder p_467460_, ItemPredicate.Builder p_468053_) {
         return CriteriaTriggers.ITEM_USED_ON_BLOCK.createCriterion(itemUsedOnLocation(p_467460_, p_468053_));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> allayDropItemOnBlock(
         LocationPredicate.Builder p_468912_, ItemPredicate.Builder p_466889_
      ) {
         return CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.createCriterion(itemUsedOnLocation(p_468912_, p_466889_));
      }

      public boolean matches(LootContext p_468805_) {
         return this.location.isEmpty() || this.location.get().matches(p_468805_);
      }

      @Override
      public void validate(CriterionValidator p_467834_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_467834_);
         this.location.ifPresent(p_469336_ -> p_467834_.validate(p_469336_, LootContextParamSets.ADVANCEMENT_LOCATION, "location"));
      }
   }
}
