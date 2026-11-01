package net.neoforged.neoforge.common.extensions;

import java.util.List;
import net.minecraft.data.loot.LootTableSubProvider;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.ConditionalLootTableSubProvider;

public interface LootTableSubProviderExtension {
   default LootTableSubProvider withConditions(List<ICondition> conditions) {
      return (LootTableSubProvider)(conditions.isEmpty() ? this.self() : new ConditionalLootTableSubProvider(this.self(), conditions));
   }

   private LootTableSubProvider self() {
      return (LootTableSubProvider)this;
   }
}
