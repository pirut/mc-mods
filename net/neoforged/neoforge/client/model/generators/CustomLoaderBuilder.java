package net.neoforged.neoforge.client.model.generators;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public abstract class CustomLoaderBuilder<T extends ModelBuilder<T>> {
   protected final ResourceLocation loaderId;
   protected final T parent;
   protected final ExistingFileHelper existingFileHelper;
   protected final Map<String, Boolean> visibility = new LinkedHashMap<>();
   protected final boolean allowInlineElements;
   private boolean optional = false;

   protected CustomLoaderBuilder(ResourceLocation loaderId, T parent, ExistingFileHelper existingFileHelper, boolean allowInlineElements) {
      this.loaderId = loaderId;
      this.parent = parent;
      this.existingFileHelper = existingFileHelper;
      this.allowInlineElements = allowInlineElements;
   }

   public CustomLoaderBuilder<T> visibility(String partName, boolean show) {
      Preconditions.checkNotNull(partName, "partName must not be null");
      this.visibility.put(partName, show);
      return this;
   }

   public CustomLoaderBuilder<T> optional() {
      Preconditions.checkState(this.allowInlineElements, "Only loaders with support for inline elements can be marked as optional");
      this.optional = true;
      return this;
   }

   public T end() {
      return this.parent;
   }

   public JsonObject toJson(JsonObject json) {
      if (this.optional) {
         JsonObject loaderObj = new JsonObject();
         loaderObj.addProperty("id", this.loaderId.toString());
         loaderObj.addProperty("optional", true);
         json.add("loader", loaderObj);
      } else {
         json.addProperty("loader", this.loaderId.toString());
      }

      if (this.visibility.size() > 0) {
         JsonObject visibilityObj = new JsonObject();

         for (Entry<String, Boolean> entry : this.visibility.entrySet()) {
            visibilityObj.addProperty(entry.getKey(), entry.getValue());
         }

         json.add("visibility", visibilityObj);
      }

      return json;
   }
}
