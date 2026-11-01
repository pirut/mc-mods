package net.minecraft.world.level.storage.loot.entries;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.level.storage.loot.LootContext;

@FunctionalInterface
interface ComposableEntryContainer {
   ComposableEntryContainer ALWAYS_FALSE = (lootContext, consumer) -> false;
   ComposableEntryContainer ALWAYS_TRUE = (lootContext, consumer) -> true;

   boolean expand(LootContext lootContext, Consumer<LootPoolEntry> consumer);

   default ComposableEntryContainer and(ComposableEntryContainer entry) {
      Objects.requireNonNull(entry);
      return (lootContext, consumer) -> this.expand(lootContext, consumer) && entry.expand(lootContext, consumer);
   }

   default ComposableEntryContainer or(ComposableEntryContainer entry) {
      Objects.requireNonNull(entry);
      return (lootContext, consumer) -> this.expand(lootContext, consumer) || entry.expand(lootContext, consumer);
   }
}
