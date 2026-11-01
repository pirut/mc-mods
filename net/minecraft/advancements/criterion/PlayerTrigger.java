package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class PlayerTrigger extends SimpleCriterionTrigger<PlayerTrigger.TriggerInstance> {
   @Override
   public Codec<PlayerTrigger.TriggerInstance> codec() {
      return PlayerTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468412_) {
      this.trigger(p_468412_, p_469880_ -> true);
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PlayerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468572_ -> p_468572_.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerTrigger.TriggerInstance::player))
            .apply(p_468572_, PlayerTrigger.TriggerInstance::new)
      );

      public static Criterion<PlayerTrigger.TriggerInstance> located(LocationPredicate.Builder p_469276_) {
         return CriteriaTriggers.LOCATION
            .createCriterion(new PlayerTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().located(p_469276_)))));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> located(EntityPredicate.Builder p_467920_) {
         return CriteriaTriggers.LOCATION.createCriterion(new PlayerTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_467920_.build()))));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> located(Optional<EntityPredicate> p_469789_) {
         return CriteriaTriggers.LOCATION.createCriterion(new PlayerTrigger.TriggerInstance(EntityPredicate.wrap(p_469789_)));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> sleptInBed() {
         return CriteriaTriggers.SLEPT_IN_BED.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> raidWon() {
         return CriteriaTriggers.RAID_WIN.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> avoidVibration() {
         return CriteriaTriggers.AVOID_VIBRATION.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> tick() {
         return CriteriaTriggers.TICK.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> walkOnBlockWithEquipment(
         HolderGetter<Block> p_467565_, HolderGetter<Item> p_467153_, Block p_468157_, Item p_469117_
      ) {
         return located(
            EntityPredicate.Builder.entity()
               .equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(p_467153_, p_469117_)))
               .steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(p_467565_, p_468157_)))
         );
      }
   }
}
