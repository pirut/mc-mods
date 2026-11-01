package net.minecraft.client.data.models.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@Environment(EnvType.CLIENT)
public class ModelTemplate {
   private final Optional<Identifier> model;
   private final Set<TextureSlot> requiredSlots;
   private final Optional<String> suffix;

   public ModelTemplate(Optional<Identifier> model, Optional<String> suffix, TextureSlot... requiredSlots) {
      this.model = model;
      this.suffix = suffix;
      this.requiredSlots = ImmutableSet.copyOf(requiredSlots);
   }

   public Identifier getDefaultModelLocation(Block block) {
      return ModelLocationUtils.getModelLocation(block, this.suffix.orElse(""));
   }

   public Identifier create(Block block, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> output) {
      return this.create(ModelLocationUtils.getModelLocation(block, this.suffix.orElse("")), textureMapping, output);
   }

   public Identifier createWithSuffix(Block block, String suffix, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> output) {
      return this.create(ModelLocationUtils.getModelLocation(block, suffix + this.suffix.orElse("")), textureMapping, output);
   }

   public Identifier createWithOverride(Block block, String suffix, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> output) {
      return this.create(ModelLocationUtils.getModelLocation(block, suffix), textureMapping, output);
   }

   public Identifier create(Item item, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> output) {
      return this.create(ModelLocationUtils.getModelLocation(item, this.suffix.orElse("")), textureMapping, output);
   }

   public Identifier create(Identifier modelLocation, TextureMapping textureMapping, BiConsumer<Identifier, ModelInstance> output) {
      Map<TextureSlot, Identifier> map = this.createMap(textureMapping);
      output.accept(modelLocation, () -> {
         JsonObject jsonObject = new JsonObject();
         this.model.ifPresent(identifier -> jsonObject.addProperty("parent", identifier.toString()));
         if (!map.isEmpty()) {
            JsonObject jsonObject2 = new JsonObject();
            map.forEach((textureSlot, identifier) -> jsonObject2.addProperty(textureSlot.getId(), identifier.toString()));
            jsonObject.add("textures", jsonObject2);
         }

         return jsonObject;
      });
      return modelLocation;
   }

   private Map<TextureSlot, Identifier> createMap(TextureMapping textureMapping) {
      return Streams.concat(new Stream[]{this.requiredSlots.stream(), textureMapping.getForced()})
         .collect(ImmutableMap.toImmutableMap(Function.identity(), textureMapping::get));
   }
}
