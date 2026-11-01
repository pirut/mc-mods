package net.neoforged.neoforge.common.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

public abstract class LanguageProvider implements DataProvider {
   private static final Codec<Map<String, Component>> CODEC = Codec.unboundedMap(Codec.STRING, ComponentSerialization.CODEC);
   private final Map<String, Component> data = new TreeMap<>();
   private final PackOutput output;
   private final String modid;
   private final String locale;

   public LanguageProvider(PackOutput output, String modid, String locale) {
      this.output = output;
      this.modid = modid;
      this.locale = locale;
   }

   protected abstract void addTranslations();

   @Override
   public CompletableFuture<?> run(CachedOutput cache) {
      this.addTranslations();
      return !this.data.isEmpty()
         ? this.save(cache, this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(this.modid).resolve("lang").resolve(this.locale + ".json"))
         : CompletableFuture.allOf();
   }

   @Override
   public String getName() {
      return "Languages: " + this.locale + " for mod: " + this.modid;
   }

   private CompletableFuture<?> save(CachedOutput cache, Path target) {
      JsonElement json = (JsonElement)CODEC.encode(this.data, JsonOps.INSTANCE, new JsonObject()).getOrThrow();
      return DataProvider.saveStable(cache, json, target);
   }

   public void addBlock(Supplier<? extends Block> key, String name) {
      this.add(key.get(), name);
   }

   public void add(Block key, String name) {
      this.add(key.getDescriptionId(), name);
   }

   public void addItem(Supplier<? extends Item> key, String name) {
      this.add(key.get(), name);
   }

   public void add(Item key, String name) {
      this.add(key.getDescriptionId(), name);
   }

   public void addItemStack(Supplier<ItemStack> key, String name) {
      this.add(key.get(), name);
   }

   public void add(ItemStack key, String name) {
      this.add(key.getItem().getDescriptionId(), name);
   }

   public void addEffect(Supplier<? extends MobEffect> key, String name) {
      this.add(key.get(), name);
   }

   public void add(MobEffect key, String name) {
      this.add(key.getDescriptionId(), name);
   }

   public void addEntityType(Supplier<? extends EntityType<?>> key, String name) {
      this.add((EntityType<?>)key.get(), name);
   }

   public void add(EntityType<?> key, String name) {
      this.add(key.getDescriptionId(), name);
   }

   public void addTag(Supplier<? extends TagKey<?>> key, String name) {
      this.add((TagKey<?>)key.get(), name);
   }

   public void add(TagKey<?> tagKey, String name) {
      this.add(Tags.getTagTranslationKey(tagKey), name);
   }

   public void add(String key, String value) {
      this.add(key, Component.literal(value));
   }

   public void add(String key, Component value) {
      if (this.data.put(key, value) != null) {
         throw new IllegalStateException("Duplicate translation key " + key);
      }
   }

   public void addDimension(ResourceKey<Level> dimension, String value) {
      this.add(dimension.identifier().toLanguageKey("dimension"), value);
   }

   public void addBiome(ResourceKey<Biome> biome, String value) {
      this.add(biome.identifier().toLanguageKey("biome"), value);
   }
}
