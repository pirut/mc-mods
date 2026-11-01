package net.neoforged.neoforge.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.NamedRenderTypeManager;
import net.neoforged.neoforge.client.RenderTypeGroup;
import org.jspecify.annotations.Nullable;

public final class NeoForgeModelProperties {
   public static final ContextKey<Transformation> TRANSFORM = ContextKey.vanilla("transform");
   public static final ContextKey<RenderTypeGroup> RENDER_TYPE = ContextKey.vanilla("render_type");
   public static final ContextKey<Map<String, Boolean>> PART_VISIBILITY = ContextKey.vanilla("part_visibility");

   private NeoForgeModelProperties() {
   }

   @Nullable
   public static Transformation deserializeRootTransform(JsonObject jsonObject, JsonDeserializationContext context) {
      if (jsonObject.has("transform")) {
         JsonElement transform = jsonObject.get("transform");
         return (Transformation)context.deserialize(transform, Transformation.class);
      } else {
         return null;
      }
   }

   public static RenderTypeGroup deserializeRenderType(JsonObject jsonObject) {
      if (jsonObject.has("render_type")) {
         String renderTypeHintName = GsonHelper.getAsString(jsonObject, "render_type");
         return NamedRenderTypeManager.get(Identifier.parse(renderTypeHintName));
      } else {
         return RenderTypeGroup.EMPTY;
      }
   }

   public static Map<String, Boolean> deserializePartVisibility(JsonObject jsonObject) {
      Map<String, Boolean> partVisibility = new HashMap<>();
      if (jsonObject.has("visibility")) {
         JsonObject visibility = GsonHelper.getAsJsonObject(jsonObject, "visibility");

         for (Entry<String, JsonElement> part : visibility.entrySet()) {
            partVisibility.put(part.getKey(), part.getValue().getAsBoolean());
         }
      }

      return Map.copyOf(partVisibility);
   }

   public static void fillRootTransformProperty(ContextMap.Builder propertiesBuilder, @Nullable Transformation rootTransform) {
      if (rootTransform != null) {
         propertiesBuilder.withParameter(TRANSFORM, rootTransform);
      }
   }

   public static void fillRenderTypeProperty(ContextMap.Builder propertiesBuilder, RenderTypeGroup renderTypeGroup) {
      if (!renderTypeGroup.isEmpty()) {
         propertiesBuilder.withParameter(RENDER_TYPE, renderTypeGroup);
      }
   }

   public static void fillPartVisibilityProperty(ContextMap.Builder propertiesBuilder, Map<String, Boolean> partVisibility) {
      if (!partVisibility.isEmpty()) {
         Map<String, Boolean> visibility = propertiesBuilder.getOptionalParameter(PART_VISIBILITY);
         if (visibility != null) {
            visibility = new HashMap<>(visibility);
            visibility.putAll(partVisibility);
         } else {
            visibility = partVisibility;
         }

         visibility = Map.copyOf(visibility);
         propertiesBuilder.withParameter(PART_VISIBILITY, visibility);
      }
   }
}
