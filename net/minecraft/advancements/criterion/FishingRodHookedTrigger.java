package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class FishingRodHookedTrigger extends SimpleCriterionTrigger<FishingRodHookedTrigger.TriggerInstance> {
   @Override
   public Codec<FishingRodHookedTrigger.TriggerInstance> codec() {
      return FishingRodHookedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_467505_, ItemStack p_467187_, FishingHook p_469806_, Collection<ItemStack> p_468611_) {
      LootContext lootcontext = EntityPredicate.createContext(p_467505_, (Entity)(p_469806_.getHookedIn() != null ? p_469806_.getHookedIn() : p_469806_));
      this.trigger(p_467505_, p_469575_ -> p_469575_.matches(p_467187_, lootcontext, p_468611_));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, Optional<ItemPredicate> rod, Optional<ContextAwarePredicate> entity, Optional<ItemPredicate> item
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<FishingRodHookedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_469664_ -> p_469664_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(FishingRodHookedTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("rod").forGetter(FishingRodHookedTrigger.TriggerInstance::rod),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(FishingRodHookedTrigger.TriggerInstance::entity),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(FishingRodHookedTrigger.TriggerInstance::item)
            )
            .apply(p_469664_, FishingRodHookedTrigger.TriggerInstance::new)
      );

      public static Criterion<FishingRodHookedTrigger.TriggerInstance> fishedItem(
         Optional<ItemPredicate> p_466827_, Optional<EntityPredicate> p_468506_, Optional<ItemPredicate> p_467285_
      ) {
         return CriteriaTriggers.FISHING_ROD_HOOKED
            .createCriterion(new FishingRodHookedTrigger.TriggerInstance(Optional.empty(), p_466827_, EntityPredicate.wrap(p_468506_), p_467285_));
      }

      public boolean matches(ItemStack p_468975_, LootContext p_467085_, Collection<ItemStack> p_469987_) {
         if (this.rod.isPresent() && !this.rod.get().test(p_468975_)) {
            return false;
         } else if (this.entity.isPresent() && !this.entity.get().matches(p_467085_)) {
            return false;
         } else {
            if (this.item.isPresent()) {
               boolean flag = false;
               Entity entity = p_467085_.getOptionalParameter(LootContextParams.THIS_ENTITY);
               if (entity instanceof ItemEntity itementity && this.item.get().test(itementity.getItem())) {
                  flag = true;
               }

               for (ItemStack itemstack : p_469987_) {
                  if (this.item.get().test(itemstack)) {
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
      }

      @Override
      public void validate(CriterionValidator p_466947_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_466947_);
         p_466947_.validateEntity(this.entity, "entity");
      }
   }
}
