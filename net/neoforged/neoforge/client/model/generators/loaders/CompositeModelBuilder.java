package net.neoforged.neoforge.client.model.generators.loaders;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;

public class CompositeModelBuilder extends CustomLoaderBuilder {
   private final Map<String, Either<Identifier, CompositeModelBuilder.InlineChild>> childModels = new LinkedHashMap<>();
   private final List<String> itemRenderOrder = new ArrayList<>();

   public CompositeModelBuilder() {
      super(Identifier.fromNamespaceAndPath("neoforge", "composite"), false);
   }

   public CompositeModelBuilder child(String name, Identifier model) {
      Preconditions.checkNotNull(name, "name must not be null");
      Preconditions.checkNotNull(model, "model must not be null");
      this.childModels.put(name, Either.left(model));
      this.itemRenderOrder.add(name);
      return this;
   }

   public CompositeModelBuilder inlineChild(String name, ModelTemplate template, TextureMapping textures) {
      Preconditions.checkNotNull(name, "name must not be null");
      Preconditions.checkNotNull(template, "model template must not be null");
      Preconditions.checkNotNull(textures, "textures must not be null");
      this.childModels.put(name, Either.right(new CompositeModelBuilder.InlineChild(template, textures)));
      this.itemRenderOrder.add(name);
      return this;
   }

   public CompositeModelBuilder itemRenderOrder(String... names) {
      Preconditions.checkNotNull(names, "names must not be null");
      Preconditions.checkArgument(names.length > 0, "names must contain at least one element");

      for (String name : names) {
         if (!this.childModels.containsKey(name)) {
            throw new IllegalArgumentException("names contains \"" + name + "\", which is not a child of this model");
         }
      }

      this.itemRenderOrder.clear();
      this.itemRenderOrder.addAll(Arrays.asList(names));
      return this;
   }

   @Override
   protected CustomLoaderBuilder copyInternal() {
      CompositeModelBuilder builder = new CompositeModelBuilder();
      builder.childModels.putAll(this.childModels);
      builder.itemRenderOrder.addAll(this.itemRenderOrder);
      return builder;
   }

   @Override
   public JsonObject toJson(JsonObject json) {
      json = super.toJson(json);
      JsonObject children = new JsonObject();

      for (Entry<String, Either<Identifier, CompositeModelBuilder.InlineChild>> entry : this.childModels.entrySet()) {
         entry.getValue()
            .ifLeft(reference -> children.addProperty(entry.getKey(), reference.toString()))
            .ifRight(inline -> serializeNestedTemplate(inline.template, inline.textures, inlineJson -> children.add(entry.getKey(), inlineJson)));
      }

      json.add("children", children);
      JsonArray itemRenderOrder = new JsonArray();

      for (String name : this.itemRenderOrder) {
         itemRenderOrder.add(name);
      }

      json.add("item_render_order", itemRenderOrder);
      return json;
   }

   private record InlineChild(ModelTemplate template, TextureMapping textures) {
   }
}
