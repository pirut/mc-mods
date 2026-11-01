package net.neoforged.neoforge.common.data;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

public abstract class RecipePrioritiesProvider implements DataProvider {
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registriesLookup;
   protected HolderLookup.Provider registries;
   private final String modid;
   private final Map<Identifier, Integer> toSerialize = new LinkedHashMap<>();
   private boolean replace = false;

   public RecipePrioritiesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String modid) {
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
      Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("neoforge").resolve("recipe_priorities.json");
      JsonObject entries = new JsonObject();
      this.toSerialize.forEach((key, value) -> entries.addProperty(key.toString(), value));
      JsonObject json = new JsonObject();
      if (this.replace) {
         json.addProperty("replace", true);
      }

      json.add("entries", entries);
      return DataProvider.saveStable(cache, json, path);
   }

   public void add(Identifier recipe, int priority) {
      this.toSerialize.put(recipe, priority);
   }

   public void add(ResourceKey<Recipe> recipe, int priority) {
      this.add(recipe.identifier(), priority);
   }

   public void add(String recipe, int priority) {
      this.add(Identifier.fromNamespaceAndPath(this.modid, recipe), priority);
   }

   @Override
   public String getName() {
      return "Recipe Priorities : " + this.modid;
   }
}
