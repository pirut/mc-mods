package net.neoforged.neoforge.common.world;

import java.util.Collections;
import java.util.Set;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.jspecify.annotations.Nullable;

public class MobSpawnSettingsBuilder extends MobSpawnSettings.Builder {
   private final Set<MobCategory> typesView = Collections.unmodifiableSet(this.spawners.keySet());
   private final Set<EntityType<?>> costView = Collections.unmodifiableSet(this.mobSpawnCosts.keySet());

   public MobSpawnSettingsBuilder(MobSpawnSettings orig) {
      orig.getSpawnerTypes().forEach(k -> this.spawners.get(k).addAll(orig.getMobs(k)));
      orig.getEntityTypes().forEach(k -> this.mobSpawnCosts.put((EntityType<?>)k, orig.getMobSpawnCost((EntityType<?>)k)));
      this.creatureGenerationProbability = orig.getCreatureProbability();
   }

   public Set<MobCategory> getSpawnerTypes() {
      return this.typesView;
   }

   public WeightedList.Builder<MobSpawnSettings.SpawnerData> getSpawner(MobCategory type) {
      return this.spawners.get(type);
   }

   public Set<EntityType<?>> getEntityTypes() {
      return this.costView;
   }

   @Nullable
   public MobSpawnSettings.MobSpawnCost getCost(EntityType<?> type) {
      return this.mobSpawnCosts.get(type);
   }

   public float getProbability() {
      return this.creatureGenerationProbability;
   }

   public MobSpawnSettingsBuilder disablePlayerSpawn() {
      return this;
   }

   public MobSpawnSettingsBuilder removeSpawnCost(EntityType<?>... entityTypes) {
      for (EntityType<?> entityType : entityTypes) {
         this.mobSpawnCosts.remove(entityType);
      }

      return this;
   }
}
