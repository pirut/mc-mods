package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledByCrossbowTrigger extends SimpleCriterionTrigger<KilledByCrossbowTrigger.TriggerInstance> {
   @Override
   public Codec<KilledByCrossbowTrigger.TriggerInstance> codec() {
      return KilledByCrossbowTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_46872_, Collection<Entity> p_46873_) {
      List<LootContext> $$2 = Lists.newArrayList();
      Set<EntityType<?>> $$3 = Sets.newHashSet();

      for (Entity $$4 : p_46873_) {
         $$3.add($$4.getType());
         $$2.add(EntityPredicate.createContext(p_46872_, $$4));
      }

      this.trigger(p_46872_, p_46881_ -> p_46881_.matches($$2, $$3.size()));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims, MinMaxBounds.Ints uniqueEntityTypes)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<KilledByCrossbowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_337373_ -> p_337373_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(KilledByCrossbowTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.listOf().optionalFieldOf("victims", List.of()).forGetter(KilledByCrossbowTrigger.TriggerInstance::victims),
               MinMaxBounds.Ints.CODEC
                  .optionalFieldOf("unique_entity_types", MinMaxBounds.Ints.ANY)
                  .forGetter(KilledByCrossbowTrigger.TriggerInstance::uniqueEntityTypes)
            )
            .apply(p_337373_, KilledByCrossbowTrigger.TriggerInstance::new)
      );

      public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(EntityPredicate.Builder... p_301077_) {
         return CriteriaTriggers.KILLED_BY_CROSSBOW
            .createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_301077_), MinMaxBounds.Ints.ANY));
      }

      public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(MinMaxBounds.Ints p_301148_) {
         return CriteriaTriggers.KILLED_BY_CROSSBOW.createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), List.of(), p_301148_));
      }

      public boolean matches(Collection<LootContext> p_46898_, int p_46899_) {
         if (!this.victims.isEmpty()) {
            List<LootContext> $$2 = Lists.newArrayList(p_46898_);

            for (ContextAwarePredicate $$3 : this.victims) {
               boolean $$4 = false;
               Iterator<LootContext> $$5 = $$2.iterator();

               while ($$5.hasNext()) {
                  LootContext $$6 = $$5.next();
                  if ($$3.matches($$6)) {
                     $$5.remove();
                     $$4 = true;
                     break;
                  }
               }

               if (!$$4) {
                  return false;
               }
            }
         }

         return this.uniqueEntityTypes.matches(p_46899_);
      }

      @Override
      public void validate(CriterionValidator p_312236_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_312236_);
         p_312236_.validateEntities(this.victims, ".victims");
      }
   }
}
