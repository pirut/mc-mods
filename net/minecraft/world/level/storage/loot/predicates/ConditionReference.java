package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.slf4j.Logger;

public record ConditionReference(ResourceKey<LootItemCondition> name) implements LootItemCondition {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<ConditionReference> CODEC = RecordCodecBuilder.mapCodec(
      p_335367_ -> p_335367_.group(ResourceKey.codec(Registries.PREDICATE).fieldOf("name").forGetter(ConditionReference::name))
         .apply(p_335367_, ConditionReference::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.REFERENCE;
   }

   @Override
   public void validate(ValidationContext p_81560_) {
      if (!p_81560_.allowsReferences()) {
         p_81560_.reportProblem("Uses reference to " + this.name.location() + ", but references are not allowed");
      } else if (p_81560_.hasVisitedElement(this.name)) {
         p_81560_.reportProblem("Condition " + this.name.location() + " is recursively called");
      } else {
         LootItemCondition.super.validate(p_81560_);
         p_81560_.resolver()
            .get(Registries.PREDICATE, this.name)
            .ifPresentOrElse(
               p_339583_ -> p_339583_.value().validate(p_81560_.enterElement(".{" + this.name.location() + "}", this.name)),
               () -> p_81560_.reportProblem("Unknown condition table called " + this.name.location())
            );
      }
   }

   public boolean test(LootContext p_81558_) {
      LootItemCondition $$1 = p_81558_.getResolver().get(Registries.PREDICATE, this.name).map(Holder.Reference::value).orElse(null);
      if ($$1 == null) {
         LOGGER.warn("Tried using unknown condition table called {}", this.name.location());
         return false;
      } else {
         LootContext.VisitedEntry<?> $$2 = LootContext.createVisitedEntry($$1);
         if (p_81558_.pushVisitedElement($$2)) {
            boolean var4;
            try {
               var4 = $$1.test(p_81558_);
            } finally {
               p_81558_.popVisitedElement($$2);
            }

            return var4;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
         }
      }
   }

   public static LootItemCondition.Builder conditionReference(ResourceKey<LootItemCondition> p_335879_) {
      return () -> new ConditionReference(p_335879_);
   }
}
