package net.neoforged.neoforge.event;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class LootTableLoadEvent extends Event implements ICancellableEvent {
   private final HolderLookup.Provider registries;
   private final Identifier name;
   private LootTable table;
   @Nullable
   private ResourceKey<LootTable> key;

   @Internal
   public LootTableLoadEvent(HolderLookup.Provider registries, Identifier name, LootTable table) {
      this.registries = registries;
      this.name = name;
      this.table = table;
   }

   public HolderLookup.Provider getRegistries() {
      return this.registries;
   }

   public Identifier getName() {
      return this.name;
   }

   public ResourceKey<LootTable> getKey() {
      if (this.key == null) {
         this.key = ResourceKey.create(Registries.LOOT_TABLE, this.name);
      }

      return this.key;
   }

   public LootTable getTable() {
      return this.table;
   }

   public void setTable(LootTable table) {
      Objects.requireNonNull(table);
      this.table = table;
   }
}
