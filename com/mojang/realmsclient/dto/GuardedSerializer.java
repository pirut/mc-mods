package com.mojang.realmsclient.dto;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GuardedSerializer {
   ExclusionStrategy strategy = new ExclusionStrategy() {
      public boolean shouldSkipClass(Class<?> class_) {
         return false;
      }

      public boolean shouldSkipField(FieldAttributes fieldAttributes) {
         return fieldAttributes.getAnnotation(Exclude.class) != null;
      }
   };
   private final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(this.strategy).addDeserializationExclusionStrategy(this.strategy).create();

   public String toJson(ReflectionBasedSerialization reflectionBasedSerialization) {
      return this.gson.toJson(reflectionBasedSerialization);
   }

   public String toJson(JsonElement json) {
      return this.gson.toJson(json);
   }

   @Nullable
   public <T extends ReflectionBasedSerialization> T fromJson(String json, Class<T> classOfT) {
      return (T)this.gson.fromJson(json, classOfT);
   }
}
