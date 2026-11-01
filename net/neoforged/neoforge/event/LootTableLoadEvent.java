package net.neoforged.neoforge.event;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class LootTableLoadEvent extends Event implements ICancellableEvent {
   private final HolderLookup.Provider registries;
   private final ResourceLocation name;
   private LootTable table;
   @Nullable
   private ResourceKey<LootTable> key;

   @Deprecated
   public LootTableLoadEvent(ResourceLocation name, LootTable table) {
      this(RegistryAccess.EMPTY, name, table);
   }

   @Internal
   public LootTableLoadEvent(HolderLookup.Provider registries, ResourceLocation name, LootTable table) {
      this.registries = registries;
      this.name = name;
      this.table = table;
   }

   public HolderLookup.Provider getRegistries() {
      return this.registries;
   }

   public ResourceLocation getName() {
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
