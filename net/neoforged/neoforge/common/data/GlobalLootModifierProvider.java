package net.neoforged.neoforge.common.data;

import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

public abstract class GlobalLootModifierProvider implements DataProvider {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registriesLookup;
   protected HolderLookup.Provider registries;
   private final String modid;
   private final Map<String, WithConditions<IGlobalLootModifier>> toSerialize = new LinkedHashMap<>();
   private boolean replace = false;

   public GlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String modid) {
      this.output = output;
      this.registriesLookup = registries;
      this.modid = modid;
   }

   protected void replacing() {
      this.replace = true;
   }

   protected abstract void start();

   @Override
   public final CompletableFuture<?> run(CachedOutput cache) {
      return this.registriesLookup.thenCompose(registries -> this.run(cache, registries));
   }

   protected CompletableFuture<?> run(CachedOutput cache, HolderLookup.Provider registries) {
      this.registries = registries;
      this.start();
      Path forgePath = this.output
         .getOutputFolder(PackOutput.Target.DATA_PACK)
         .resolve("neoforge")
         .resolve("loot_modifiers")
         .resolve("global_loot_modifiers.json");
      Path modifierFolderPath = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(this.modid).resolve("loot_modifiers");
      List<ResourceLocation> entries = new ArrayList<>();
      Builder<CompletableFuture<?>> futuresBuilder = new Builder();

      for (Entry<String, WithConditions<IGlobalLootModifier>> entry : this.toSerialize.entrySet()) {
         String name = entry.getKey();
         WithConditions<IGlobalLootModifier> lootModifier = entry.getValue();
         entries.add(ResourceLocation.fromNamespaceAndPath(this.modid, name));
         Path modifierPath = modifierFolderPath.resolve(name + ".json");
         futuresBuilder.add(DataProvider.saveStable(cache, registries, IGlobalLootModifier.CONDITIONAL_CODEC, Optional.of(lootModifier), modifierPath));
      }

      JsonObject forgeJson = new JsonObject();
      forgeJson.addProperty("replace", this.replace);
      forgeJson.add("entries", GSON.toJsonTree(entries.stream().map(ResourceLocation::toString).collect(Collectors.toList())));
      futuresBuilder.add(DataProvider.saveStable(cache, forgeJson, forgePath));
      return CompletableFuture.allOf((CompletableFuture<?>[])futuresBuilder.build().toArray(CompletableFuture[]::new));
   }

   public <T extends IGlobalLootModifier> void add(String modifier, T instance, List<ICondition> conditions) {
      this.toSerialize.put(modifier, new WithConditions<>(conditions, instance));
   }

   public <T extends IGlobalLootModifier> void add(String modifier, T instance, ICondition... conditions) {
      this.add(modifier, instance, Arrays.asList(conditions));
   }

   @Override
   public String getName() {
      return "Global Loot Modifiers : " + this.modid;
   }
}
