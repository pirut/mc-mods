package net.minecraft.advancements.criterion;

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
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public class KilledByArrowTrigger extends SimpleCriterionTrigger<KilledByArrowTrigger.TriggerInstance> {
   @Override
   public Codec<KilledByArrowTrigger.TriggerInstance> codec() {
      return KilledByArrowTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468606_, Collection<Entity> p_468099_, @Nullable ItemStack p_469551_) {
      List<LootContext> list = Lists.newArrayList();
      Set<EntityType<?>> set = Sets.newHashSet();

      for (Entity entity : p_468099_) {
         set.add(entity.getType());
         list.add(EntityPredicate.createContext(p_468606_, entity));
      }

      this.trigger(p_468606_, p_469229_ -> p_469229_.matches(list, set.size(), p_469551_));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims, MinMaxBounds.Ints uniqueEntityTypes, Optional<ItemPredicate> firedFromWeapon
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<KilledByArrowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467703_ -> p_467703_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(KilledByArrowTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.listOf().optionalFieldOf("victims", List.of()).forGetter(KilledByArrowTrigger.TriggerInstance::victims),
               MinMaxBounds.Ints.CODEC
                  .optionalFieldOf("unique_entity_types", MinMaxBounds.Ints.ANY)
                  .forGetter(KilledByArrowTrigger.TriggerInstance::uniqueEntityTypes),
               ItemPredicate.CODEC.optionalFieldOf("fired_from_weapon").forGetter(KilledByArrowTrigger.TriggerInstance::firedFromWeapon)
            )
            .apply(p_467703_, KilledByArrowTrigger.TriggerInstance::new)
      );

      public static Criterion<KilledByArrowTrigger.TriggerInstance> crossbowKilled(HolderGetter<Item> p_467101_, EntityPredicate.Builder... p_467973_) {
         return CriteriaTriggers.KILLED_BY_ARROW
            .createCriterion(
               new KilledByArrowTrigger.TriggerInstance(
                  Optional.empty(),
                  EntityPredicate.wrap(p_467973_),
                  MinMaxBounds.Ints.ANY,
                  Optional.of(ItemPredicate.Builder.item().of(p_467101_, Items.CROSSBOW).build())
               )
            );
      }

      public static Criterion<KilledByArrowTrigger.TriggerInstance> crossbowKilled(HolderGetter<Item> p_467799_, MinMaxBounds.Ints p_468442_) {
         return CriteriaTriggers.KILLED_BY_ARROW
            .createCriterion(
               new KilledByArrowTrigger.TriggerInstance(
                  Optional.empty(), List.of(), p_468442_, Optional.of(ItemPredicate.Builder.item().of(p_467799_, Items.CROSSBOW).build())
               )
            );
      }

      public boolean matches(Collection<LootContext> p_469847_, int p_466859_, @Nullable ItemStack p_469676_) {
         if (!this.firedFromWeapon.isPresent() || p_469676_ != null && this.firedFromWeapon.get().test(p_469676_)) {
            if (!this.victims.isEmpty()) {
               List<LootContext> list = Lists.newArrayList(p_469847_);

               for (ContextAwarePredicate contextawarepredicate : this.victims) {
                  boolean flag = false;
                  Iterator<LootContext> iterator = list.iterator();

                  while (iterator.hasNext()) {
                     LootContext lootcontext = iterator.next();
                     if (contextawarepredicate.matches(lootcontext)) {
                        iterator.remove();
                        flag = true;
                        break;
                     }
                  }

                  if (!flag) {
                     return false;
                  }
               }
            }

            return this.uniqueEntityTypes.matches(p_466859_);
         } else {
            return false;
         }
      }

      @Override
      public void validate(CriterionValidator p_469713_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_469713_);
         p_469713_.validateEntities(this.victims, "victims");
      }
   }
}
