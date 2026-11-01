package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public class BredAnimalsTrigger extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
   @Override
   public Codec<BredAnimalsTrigger.TriggerInstance> codec() {
      return BredAnimalsTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_468335_, Animal p_467783_, Animal p_468968_, @Nullable AgeableMob p_467366_) {
      LootContext lootcontext = EntityPredicate.createContext(p_468335_, p_467783_);
      LootContext lootcontext1 = EntityPredicate.createContext(p_468335_, p_468968_);
      LootContext lootcontext2 = p_467366_ != null ? EntityPredicate.createContext(p_468335_, p_467366_) : null;
      this.trigger(p_468335_, p_469218_ -> p_469218_.matches(lootcontext, lootcontext1, lootcontext2));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player,
      Optional<ContextAwarePredicate> parent,
      Optional<ContextAwarePredicate> partner,
      Optional<ContextAwarePredicate> child
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<BredAnimalsTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467133_ -> p_467133_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(BredAnimalsTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("parent").forGetter(BredAnimalsTrigger.TriggerInstance::parent),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("partner").forGetter(BredAnimalsTrigger.TriggerInstance::partner),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("child").forGetter(BredAnimalsTrigger.TriggerInstance::child)
            )
            .apply(p_467133_, BredAnimalsTrigger.TriggerInstance::new)
      );

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals() {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(EntityPredicate.Builder p_469635_) {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(
               new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(p_469635_)))
            );
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(
         Optional<EntityPredicate> p_469787_, Optional<EntityPredicate> p_468729_, Optional<EntityPredicate> p_468824_
      ) {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(
               new BredAnimalsTrigger.TriggerInstance(
                  Optional.empty(), EntityPredicate.wrap(p_469787_), EntityPredicate.wrap(p_468729_), EntityPredicate.wrap(p_468824_)
               )
            );
      }

      public boolean matches(LootContext p_468320_, LootContext p_469547_, @Nullable LootContext p_467039_) {
         return !this.child.isPresent() || p_467039_ != null && this.child.get().matches(p_467039_)
            ? matches(this.parent, p_468320_) && matches(this.partner, p_469547_) || matches(this.parent, p_469547_) && matches(this.partner, p_468320_)
            : false;
      }

      private static boolean matches(Optional<ContextAwarePredicate> p_468736_, LootContext p_468763_) {
         return p_468736_.isEmpty() || p_468736_.get().matches(p_468763_);
      }

      @Override
      public void validate(CriterionValidator p_469226_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_469226_);
         p_469226_.validateEntity(this.parent, "parent");
         p_469226_.validateEntity(this.partner, "partner");
         p_469226_.validateEntity(this.child, "child");
      }
   }
}
