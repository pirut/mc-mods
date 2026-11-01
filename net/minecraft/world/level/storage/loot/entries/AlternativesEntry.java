package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class AlternativesEntry extends CompositeEntryBase {
   public static final MapCodec<AlternativesEntry> CODEC = createCodec(AlternativesEntry::new);

   AlternativesEntry(List<LootPoolEntryContainer> p_299062_, List<LootItemCondition> p_298668_) {
      super(p_299062_, p_298668_);
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.ALTERNATIVES;
   }

   @Override
   protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> p_298256_) {
      return switch (p_298256_.size()) {
         case 0 -> ALWAYS_FALSE;
         case 1 -> (ComposableEntryContainer)p_298256_.get(0);
         case 2 -> p_298256_.get(0).or(p_298256_.get(1));
         default -> (p_298004_, p_298005_) -> {
            for (ComposableEntryContainer $$3 : p_298256_) {
               if ($$3.expand(p_298004_, p_298005_)) {
                  return true;
               }
            }

            return false;
         };
      };
   }

   @Override
   public void validate(ValidationContext p_79388_) {
      super.validate(p_79388_);

      for (int $$1 = 0; $$1 < this.children.size() - 1; $$1++) {
         if (this.children.get($$1).conditions.isEmpty()) {
            p_79388_.reportProblem("Unreachable entry!");
         }
      }
   }

   public static AlternativesEntry.Builder alternatives(LootPoolEntryContainer.Builder<?>... p_79396_) {
      return new AlternativesEntry.Builder(p_79396_);
   }

   public static <E> AlternativesEntry.Builder alternatives(Collection<E> p_230934_, Function<E, LootPoolEntryContainer.Builder<?>> p_230935_) {
      return new AlternativesEntry.Builder(p_230934_.stream().map(p_230935_::apply).toArray(LootPoolEntryContainer.Builder[]::new));
   }

   public static class Builder extends LootPoolEntryContainer.Builder<AlternativesEntry.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

      public Builder(LootPoolEntryContainer.Builder<?>... p_79399_) {
         for (LootPoolEntryContainer.Builder<?> $$1 : p_79399_) {
            this.entries.add($$1.build());
         }
      }

      protected AlternativesEntry.Builder getThis() {
         return this;
      }

      @Override
      public AlternativesEntry.Builder otherwise(LootPoolEntryContainer.Builder<?> p_79402_) {
         this.entries.add(p_79402_.build());
         return this;
      }

      @Override
      public LootPoolEntryContainer build() {
         return new AlternativesEntry(this.entries.build(), this.getConditions());
      }
   }
}
