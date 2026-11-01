package net.neoforged.neoforge.event.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterSpawnPlacementsEvent extends Event implements IModBusEvent {
   private final Map<EntityType<?>, RegisterSpawnPlacementsEvent.MergedSpawnPredicate<?>> map;

   @Internal
   public RegisterSpawnPlacementsEvent(Map<EntityType<?>, RegisterSpawnPlacementsEvent.MergedSpawnPredicate<?>> map) {
      this.map = map;
   }

   public <T extends Entity> void register(EntityType<T> entityType, SpawnPlacements.SpawnPredicate<T> predicate) {
      this.register(entityType, null, null, predicate, RegisterSpawnPlacementsEvent.Operation.OR);
   }

   public <T extends Entity> void register(
      EntityType<T> entityType, SpawnPlacements.SpawnPredicate<T> predicate, RegisterSpawnPlacementsEvent.Operation operation
   ) {
      this.register(entityType, null, null, predicate, operation);
   }

   public <T extends Entity> void register(
      EntityType<T> entityType,
      @Nullable SpawnPlacementType placementType,
      @Nullable Heightmap.Types heightmap,
      SpawnPlacements.SpawnPredicate<T> predicate,
      RegisterSpawnPlacementsEvent.Operation operation
   ) {
      if (!this.map.containsKey(entityType)) {
         if (placementType == null) {
            throw new NullPointerException(
               "Registering a new Spawn Predicate requires a nonnull placement type! Entity Type: " + BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
            );
         }

         if (heightmap == null) {
            throw new NullPointerException(
               "Registering a new Spawn Predicate requires a nonnull heightmap type! Entity Type: " + BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
            );
         }

         this.map.put(entityType, new RegisterSpawnPlacementsEvent.MergedSpawnPredicate<>(predicate, placementType, heightmap));
      } else {
         if (operation != RegisterSpawnPlacementsEvent.Operation.REPLACE && (heightmap != null || placementType != null)) {
            throw new IllegalStateException(
               "Nonnull heightmap types or spawn placement types may only be used with the REPLACE operation. Entity Type: "
                  + BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
            );
         }

         this.map.get(entityType).merge(operation, predicate, placementType, heightmap);
      }
   }

   public static class MergedSpawnPredicate<T extends Entity> {
      private final SpawnPlacements.SpawnPredicate<T> originalPredicate;
      private final List<SpawnPlacements.SpawnPredicate<T>> orPredicates;
      private final List<SpawnPlacements.SpawnPredicate<T>> andPredicates;
      @Nullable
      private SpawnPlacements.SpawnPredicate<T> replacementPredicate;
      private SpawnPlacementType spawnType;
      private Heightmap.Types heightmapType;

      public MergedSpawnPredicate(SpawnPlacements.SpawnPredicate<T> originalPredicate, SpawnPlacementType spawnType, Heightmap.Types heightmapType) {
         this.originalPredicate = originalPredicate;
         this.orPredicates = new ArrayList<>();
         this.andPredicates = new ArrayList<>();
         this.replacementPredicate = null;
         this.spawnType = spawnType;
         this.heightmapType = heightmapType;
      }

      public SpawnPlacementType getSpawnType() {
         return this.spawnType;
      }

      public Heightmap.Types getHeightmapType() {
         return this.heightmapType;
      }

      private void merge(
         RegisterSpawnPlacementsEvent.Operation operation,
         SpawnPlacements.SpawnPredicate<T> predicate,
         @Nullable SpawnPlacementType spawnType,
         @Nullable Heightmap.Types heightmapType
      ) {
         if (operation == RegisterSpawnPlacementsEvent.Operation.AND) {
            this.andPredicates.add(predicate);
         } else if (operation == RegisterSpawnPlacementsEvent.Operation.OR) {
            this.orPredicates.add(predicate);
         } else if (operation == RegisterSpawnPlacementsEvent.Operation.REPLACE) {
            this.replacementPredicate = predicate;
            if (spawnType != null) {
               this.spawnType = spawnType;
            }

            if (heightmapType != null) {
               this.heightmapType = heightmapType;
            }
         }
      }

      @Internal
      public SpawnPlacements.SpawnPredicate<T> build() {
         if (this.replacementPredicate != null) {
            return this.replacementPredicate;
         } else {
            SpawnPlacements.SpawnPredicate<T> original = this.originalPredicate;
            SpawnPlacements.SpawnPredicate<T> compiledOrPredicate = (entityType, level, spawnType, pos, random) -> {
               if (original.test(entityType, level, spawnType, pos, random)) {
                  return true;
               } else {
                  for (SpawnPlacements.SpawnPredicate<T> predicate : this.orPredicates) {
                     if (predicate.test(entityType, level, spawnType, pos, random)) {
                        return true;
                     }
                  }

                  return false;
               }
            };
            return (entityType, level, spawnType, pos, random) -> {
               if (!compiledOrPredicate.test(entityType, level, spawnType, pos, random)) {
                  return false;
               } else {
                  for (SpawnPlacements.SpawnPredicate<T> predicate : this.andPredicates) {
                     if (!predicate.test(entityType, level, spawnType, pos, random)) {
                        return false;
                     }
                  }

                  return true;
               }
            };
         }
      }
   }

   public static enum Operation {
      AND,
      OR,
      REPLACE;
   }
}
