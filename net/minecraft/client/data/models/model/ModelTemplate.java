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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

@OnlyIn(Dist.CLIENT)
public class ModelTemplate {
   public final Optional<Identifier> model;
   public final Set<TextureSlot> requiredSlots;
   public final Optional<String> suffix;

   public ModelTemplate(Optional<Identifier> p_388104_, Optional<String> p_386919_, TextureSlot... p_388141_) {
      this.model = p_388104_;
      this.suffix = p_386919_;
      this.requiredSlots = ImmutableSet.copyOf(p_388141_);
   }

   public Identifier getDefaultModelLocation(Block p_386647_) {
      return ModelLocationUtils.getModelLocation(p_386647_, this.suffix.orElse(""));
   }

   public Identifier create(Block p_387250_, TextureMapping p_386985_, BiConsumer<Identifier, ModelInstance> p_388339_) {
      return this.create(ModelLocationUtils.getModelLocation(p_387250_, this.suffix.orElse("")), p_386985_, p_388339_);
   }

   public Identifier createWithSuffix(Block p_388627_, String p_388661_, TextureMapping p_386633_, BiConsumer<Identifier, ModelInstance> p_388050_) {
      return this.create(ModelLocationUtils.getModelLocation(p_388627_, p_388661_ + this.suffix.orElse("")), p_386633_, p_388050_);
   }

   public Identifier createWithOverride(Block p_388829_, String p_388013_, TextureMapping p_386709_, BiConsumer<Identifier, ModelInstance> p_386791_) {
      return this.create(ModelLocationUtils.getModelLocation(p_388829_, p_388013_), p_386709_, p_386791_);
   }

   public Identifier create(Item p_386721_, TextureMapping p_387847_, BiConsumer<Identifier, ModelInstance> p_386603_) {
      return this.create(ModelLocationUtils.getModelLocation(p_386721_, this.suffix.orElse("")), p_387847_, p_386603_);
   }

   public Identifier create(Identifier p_467879_, TextureMapping p_387099_, BiConsumer<Identifier, ModelInstance> p_387748_) {
      Map<TextureSlot, Identifier> map = this.createMap(p_387099_);
      p_387748_.accept(p_467879_, () -> this.createBaseTemplate(p_467879_, map));
      return p_467879_;
   }

   public JsonObject createBaseTemplate(Identifier p_388380_, Map<TextureSlot, Identifier> map) {
      JsonObject jsonobject = new JsonObject();
      this.model.ifPresent(p_465448_ -> jsonobject.addProperty("parent", p_465448_.toString()));
      if (!map.isEmpty()) {
         JsonObject jsonobject1 = new JsonObject();
         map.forEach((p_465445_, p_465446_) -> jsonobject1.addProperty(p_465445_.getId(), p_465446_.toString()));
         jsonobject.add("textures", jsonobject1);
      }

      return jsonobject;
   }

   private Map<TextureSlot, Identifier> createMap(TextureMapping p_387972_) {
      return Streams.concat(new Stream[]{this.requiredSlots.stream(), p_387972_.getForced()})
         .collect(ImmutableMap.toImmutableMap(Function.identity(), p_387972_::get));
   }

   public ExtendedModelTemplateBuilder extend() {
      return ExtendedModelTemplateBuilder.of(this);
   }
}
