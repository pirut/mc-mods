package net.neoforged.neoforge.common.data;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.conditions.ICondition;

public class DatapackBuiltinEntriesProvider extends RegistriesDatapackGenerator {
   private final CompletableFuture<HolderLookup.Provider> fullRegistries;

   public DatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries, Set<String> modIds) {
      this(output, registries, b -> {}, modIds);
   }

   public DatapackBuiltinEntriesProvider(
      PackOutput output,
      CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries,
      Consumer<BiConsumer<ResourceKey<?>, ICondition>> conditionsBuilder,
      Set<String> modIds
   ) {
      this(output, registries, buildConditionsMap(conditionsBuilder), modIds);
   }

   public DatapackBuiltinEntriesProvider(
      PackOutput output,
      CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries,
      Map<ResourceKey<?>, List<ICondition>> conditions,
      Set<String> modIds
   ) {
      super(output, registries.thenApply(RegistrySetBuilder.PatchedRegistries::patches), modIds, conditions);
      this.fullRegistries = registries.thenApply(RegistrySetBuilder.PatchedRegistries::full);
   }

   public DatapackBuiltinEntriesProvider(
      PackOutput output, CompletableFuture<HolderLookup.Provider> registries, RegistrySetBuilder datapackEntriesBuilder, Set<String> modIds
   ) {
      this(output, RegistryPatchGenerator.createLookup(registries, datapackEntriesBuilder), modIds);
   }

   public DatapackBuiltinEntriesProvider(
      PackOutput output,
      CompletableFuture<HolderLookup.Provider> registries,
      RegistrySetBuilder datapackEntriesBuilder,
      Map<ResourceKey<?>, List<ICondition>> conditions,
      Set<String> modIds
   ) {
      this(output, RegistryPatchGenerator.createLookup(registries, datapackEntriesBuilder), conditions, modIds);
   }

   public DatapackBuiltinEntriesProvider(
      PackOutput output,
      CompletableFuture<HolderLookup.Provider> registries,
      RegistrySetBuilder datapackEntriesBuilder,
      Consumer<BiConsumer<ResourceKey<?>, ICondition>> conditionsBuilder,
      Set<String> modIds
   ) {
      this(output, RegistryPatchGenerator.createLookup(registries, datapackEntriesBuilder), conditionsBuilder, modIds);
   }

   public CompletableFuture<HolderLookup.Provider> getRegistryProvider() {
      return this.fullRegistries;
   }

   private static Map<ResourceKey<?>, List<ICondition>> buildConditionsMap(Consumer<BiConsumer<ResourceKey<?>, ICondition>> conditionBuilder) {
      Map<ResourceKey<?>, List<ICondition>> conditions = new IdentityHashMap<>();
      conditionBuilder.accept((key, condition) -> conditions.computeIfAbsent(key, k -> new ArrayList<>()).add(condition));
      return conditions;
   }
}
