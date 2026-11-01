package net.minecraft.advancements.criterion;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class CriterionValidator {
   private final ProblemReporter reporter;
   private final HolderGetter.Provider lootData;

   public CriterionValidator(ProblemReporter p_469930_, HolderGetter.Provider p_469221_) {
      this.reporter = p_469930_;
      this.lootData = p_469221_;
   }

   public void validateEntity(Optional<ContextAwarePredicate> p_469506_, String p_468862_) {
      p_469506_.ifPresent(p_468892_ -> this.validateEntity(p_468892_, p_468862_));
   }

   public void validateEntities(List<ContextAwarePredicate> p_469184_, String p_469213_) {
      this.validate(p_469184_, LootContextParamSets.ADVANCEMENT_ENTITY, p_469213_);
   }

   public void validateEntity(ContextAwarePredicate p_467921_, String p_468575_) {
      this.validate(p_467921_, LootContextParamSets.ADVANCEMENT_ENTITY, p_468575_);
   }

   public void validate(ContextAwarePredicate p_469549_, ContextKeySet p_469952_, String p_468521_) {
      p_469549_.validate(new ValidationContext(this.reporter.forChild(new ProblemReporter.FieldPathElement(p_468521_)), p_469952_, this.lootData));
   }

   public void validate(List<ContextAwarePredicate> p_468846_, ContextKeySet p_469299_, String p_469976_) {
      for (int i = 0; i < p_468846_.size(); i++) {
         ContextAwarePredicate contextawarepredicate = p_468846_.get(i);
         contextawarepredicate.validate(
            new ValidationContext(this.reporter.forChild(new ProblemReporter.IndexedFieldPathElement(p_469976_, i)), p_469299_, this.lootData)
         );
      }
   }
}
