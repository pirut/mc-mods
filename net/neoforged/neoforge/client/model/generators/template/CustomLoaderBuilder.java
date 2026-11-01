package net.neoforged.neoforge.client.model.generators.template;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.resources.Identifier;

public abstract class CustomLoaderBuilder {
   private static final Identifier DUMMY = Identifier.fromNamespaceAndPath("dummy", "dummy");
   protected final Identifier loaderId;
   protected final Map<String, Boolean> visibility = new LinkedHashMap<>();
   protected final boolean allowInlineElements;
   private boolean optional = false;

   protected CustomLoaderBuilder(Identifier loaderId, boolean allowInlineElements) {
      this.loaderId = loaderId;
      this.allowInlineElements = allowInlineElements;
   }

   public CustomLoaderBuilder visibility(String partName, boolean show) {
      Preconditions.checkNotNull(partName, "partName must not be null");
      this.visibility.put(partName, show);
      return this;
   }

   public CustomLoaderBuilder optional() {
      Preconditions.checkState(this.allowInlineElements, "Only loaders with support for inline elements can be marked as optional");
      this.optional = true;
      return this;
   }

   final CustomLoaderBuilder copy() {
      CustomLoaderBuilder builder = this.copyInternal();
      builder.visibility.putAll(this.visibility);
      builder.optional = this.optional;
      return builder;
   }

   protected abstract CustomLoaderBuilder copyInternal();

   public JsonObject toJson(JsonObject json) {
      if (this.optional) {
         JsonObject loaderObj = new JsonObject();
         loaderObj.addProperty("id", this.loaderId.toString());
         loaderObj.addProperty("optional", true);
         json.add("loader", loaderObj);
      } else {
         json.addProperty("loader", this.loaderId.toString());
      }

      if (!this.visibility.isEmpty()) {
         JsonObject visibilityObj = new JsonObject();

         for (Entry<String, Boolean> entry : this.visibility.entrySet()) {
            visibilityObj.addProperty(entry.getKey(), entry.getValue());
         }

         json.add("visibility", visibilityObj);
      }

      return json;
   }

   protected static void serializeNestedTemplate(ModelTemplate template, TextureMapping textures, Consumer<JsonElement> consumer) {
      template.create(DUMMY, textures, (id, jsonSup) -> consumer.accept(jsonSup.get()));
   }
}
