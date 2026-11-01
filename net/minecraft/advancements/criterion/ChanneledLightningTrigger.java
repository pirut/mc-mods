package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance> {
   @Override
   public Codec<ChanneledLightningTrigger.TriggerInstance> codec() {
      return ChanneledLightningTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467705_, Collection<? extends Entity> p_467712_) {
      List<LootContext> list = p_467712_.stream().map(p_468596_ -> EntityPredicate.createContext(p_467705_, p_468596_)).collect(Collectors.toList());
      this.trigger(p_467705_, p_468918_ -> p_468918_.matches(list));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ChanneledLightningTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_468656_ -> p_468656_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ChanneledLightningTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.listOf().optionalFieldOf("victims", List.of()).forGetter(ChanneledLightningTrigger.TriggerInstance::victims)
            )
            .apply(p_468656_, ChanneledLightningTrigger.TriggerInstance::new)
      );

      public static Criterion<ChanneledLightningTrigger.TriggerInstance> channeledLightning(EntityPredicate.Builder... p_467022_) {
         return CriteriaTriggers.CHANNELED_LIGHTNING
            .createCriterion(new ChanneledLightningTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_467022_)));
      }

      public boolean matches(Collection<? extends LootContext> p_467204_) {
         for (ContextAwarePredicate contextawarepredicate : this.victims) {
            boolean flag = false;

            for (LootContext lootcontext : p_467204_) {
               if (contextawarepredicate.matches(lootcontext)) {
                  flag = true;
                  break;
               }
            }

            if (!flag) {
               return false;
            }
         }

         return true;
      }

      @Override
      public void validate(CriterionValidator p_467628_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_467628_);
         p_467628_.validateEntities(this.victims, "victims");
      }
   }
}
