package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ContextAwarePredicate {
   public static final Codec<ContextAwarePredicate> CODEC = LootItemCondition.DIRECT_CODEC
      .listOf()
      .xmap(ContextAwarePredicate::new, p_467727_ -> p_467727_.conditions);
   private final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositePredicates;

   ContextAwarePredicate(List<LootItemCondition> p_469942_) {
      this.conditions = p_469942_;
      this.compositePredicates = Util.allOf(p_469942_);
   }

   public static ContextAwarePredicate create(LootItemCondition... p_467697_) {
      return new ContextAwarePredicate(List.of(p_467697_));
   }

   public boolean matches(LootContext p_469203_) {
      return this.compositePredicates.test(p_469203_);
   }

   public void validate(ValidationContext p_467635_) {
      for (int i = 0; i < this.conditions.size(); i++) {
         LootItemCondition lootitemcondition = this.conditions.get(i);
         lootitemcondition.validate(p_467635_.forChild(new ProblemReporter.IndexedPathElement(i)));
      }
   }
}
