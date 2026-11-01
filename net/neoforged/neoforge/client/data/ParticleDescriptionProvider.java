package net.neoforged.neoforge.client.data;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.VisibleForTesting;

public abstract class ParticleDescriptionProvider implements DataProvider {
   private final PackOutput.PathProvider particlesPath;
   @VisibleForTesting
   protected final Map<Identifier, List<String>> descriptions;

   protected ParticleDescriptionProvider(PackOutput output) {
      this.particlesPath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "particles");
      this.descriptions = new HashMap<>();
   }

   protected abstract void addDescriptions();

   protected void spriteSet(ParticleType<?> type, Identifier baseName, int numOfTextures, boolean reverse) {
      Preconditions.checkArgument(numOfTextures > 0, "The number of textures to generate must be positive");
      this.spriteSet(type, () -> new Iterator<Identifier>() {
         private int counter = 0;

         @Override
         public boolean hasNext() {
            return this.counter < numOfTextures;
         }

         public Identifier next() {
            Identifier texture = baseName.withSuffix("_" + (reverse ? numOfTextures - this.counter - 1 : this.counter));
            this.counter++;
            return texture;
         }
      });
   }

   protected void spriteSet(ParticleType<?> type, Identifier texture, Identifier... textures) {
      this.spriteSet(type, Stream.concat(Stream.of(texture), Arrays.stream(textures))::iterator);
   }

   protected void spriteSet(ParticleType<?> type, Iterable<Identifier> textures) {
      Identifier particle = (Identifier)Preconditions.checkNotNull(BuiltInRegistries.PARTICLE_TYPE.getKey(type), "The particle type is not registered");
      List<String> desc = new ArrayList<>();

      for (Identifier texture : textures) {
         desc.add(texture.toString());
      }

      Preconditions.checkArgument(desc.size() > 0, "The particle type '%s' must have one texture", particle);
      if (this.descriptions.putIfAbsent(particle, desc) != null) {
         throw new IllegalArgumentException(String.format("The particle type '%s' already has a description associated with it", particle));
      }
   }

   @Override
   public CompletableFuture<?> run(CachedOutput cache) {
      this.addDescriptions();
      return CompletableFuture.allOf(this.descriptions.entrySet().stream().map(entry -> {
         JsonArray textures = new JsonArray();
         entry.getValue().forEach(textures::add);
         return DataProvider.saveStable(cache, Util.make(new JsonObject(), obj -> obj.add("textures", textures)), this.particlesPath.json(entry.getKey()));
      }).toArray(CompletableFuture[]::new));
   }

   @Override
   public String getName() {
      return "Particle Descriptions";
   }
}
