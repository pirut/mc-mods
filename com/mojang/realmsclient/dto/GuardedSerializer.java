package com.mojang.realmsclient.dto;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GuardedSerializer {
   ExclusionStrategy strategy = new ExclusionStrategy() {
      public boolean shouldSkipClass(Class<?> p_419572_) {
         return false;
      }

      public boolean shouldSkipField(FieldAttributes p_419598_) {
         return p_419598_.getAnnotation(Exclude.class) != null;
      }
   };
   private final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(this.strategy).addDeserializationExclusionStrategy(this.strategy).create();

   public String toJson(ReflectionBasedSerialization p_87414_) {
      return this.gson.toJson(p_87414_);
   }

   public String toJson(JsonElement p_275638_) {
      return this.gson.toJson(p_275638_);
   }

   @Nullable
   public <T extends ReflectionBasedSerialization> T fromJson(String p_87416_, Class<T> p_87417_) {
      return (T)this.gson.fromJson(p_87416_, p_87417_);
   }
}
