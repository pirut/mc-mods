package net.neoforged.neoforge.common.world;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.NeoForgeMod;

public final class StructureModifiers {
   private StructureModifiers() {
   }

   public record AddSpawnsStructureModifier(HolderSet<Structure> structures, List<MobSpawnSettings.SpawnerData> spawners) implements StructureModifier {
      public static StructureModifiers.AddSpawnsStructureModifier singleSpawn(HolderSet<Structure> structures, MobSpawnSettings.SpawnerData spawner) {
         return new StructureModifiers.AddSpawnsStructureModifier(structures, List.of(spawner));
      }

      @Override
      public void modify(Holder<Structure> structure, StructureModifier.Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
         if (phase == StructureModifier.Phase.ADD && this.structures.contains(structure)) {
            StructureSettingsBuilder settingsBuilder = builder.getStructureSettings();

            for (MobSpawnSettings.SpawnerData spawner : this.spawners) {
               EntityType<?> type = spawner.type;
               settingsBuilder.getOrAddSpawnOverrides(type.getCategory()).addSpawn(spawner);
            }
         }
      }

      @Override
      public MapCodec<? extends StructureModifier> codec() {
         return NeoForgeMod.ADD_SPAWNS_STRUCTURE_MODIFIER_TYPE.get();
      }
   }

   public record ClearSpawnsStructureModifier(HolderSet<Structure> structures, Set<MobCategory> categories) implements StructureModifier {
      @Override
      public void modify(Holder<Structure> structure, StructureModifier.Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
         if (phase == StructureModifier.Phase.REMOVE && this.structures.contains(structure)) {
            StructureSettingsBuilder settingsBuilder = builder.getStructureSettings();

            for (MobCategory category : this.categories) {
               settingsBuilder.removeSpawnOverrides(category);
            }
         }
      }

      @Override
      public MapCodec<? extends StructureModifier> codec() {
         return NeoForgeMod.CLEAR_SPAWNS_STRUCTURE_MODIFIER_TYPE.get();
      }
   }

   public record RemoveSpawnsStructureModifier(HolderSet<Structure> structures, HolderSet<EntityType<?>> entityTypes) implements StructureModifier {
      @Override
      public void modify(Holder<Structure> structure, StructureModifier.Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
         if (phase == StructureModifier.Phase.REMOVE && this.structures.contains(structure)) {
            StructureSettingsBuilder settingsBuilder = builder.getStructureSettings();

            for (MobCategory category : MobCategory.values()) {
               StructureSettingsBuilder.StructureSpawnOverrideBuilder overrides = settingsBuilder.getSpawnOverrides(category);
               if (overrides != null && !overrides.getSpawns().isEmpty()) {
                  overrides.removeSpawns(spawnerData -> {
                     ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(spawnerData.type);
                     return this.entityTypes.contains(BuiltInRegistries.ENTITY_TYPE.getHolderOrThrow(ResourceKey.create(Registries.ENTITY_TYPE, key)));
                  });
               }
            }
         }
      }

      @Override
      public MapCodec<? extends StructureModifier> codec() {
         return NeoForgeMod.REMOVE_SPAWNS_STRUCTURE_MODIFIER_TYPE.get();
      }
   }
}
