package com.mojang.realmsclient.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JsonUtils {
   public static <T> T getRequired(String p_275573_, JsonObject p_275650_, Function<JsonObject, T> p_275655_) {
      JsonElement $$3 = p_275650_.get(p_275573_);
      if ($$3 == null || $$3.isJsonNull()) {
         throw new IllegalStateException("Missing required property: " + p_275573_);
      } else if (!$$3.isJsonObject()) {
         throw new IllegalStateException("Required property " + p_275573_ + " was not a JsonObject as espected");
      } else {
         return p_275655_.apply($$3.getAsJsonObject());
      }
   }

   @Nullable
   public static <T> T getOptional(String p_304642_, JsonObject p_304910_, Function<JsonObject, T> p_304507_) {
      JsonElement $$3 = p_304910_.get(p_304642_);
      if ($$3 == null || $$3.isJsonNull()) {
         return null;
      } else if (!$$3.isJsonObject()) {
         throw new IllegalStateException("Required property " + p_304642_ + " was not a JsonObject as espected");
      } else {
         return p_304507_.apply($$3.getAsJsonObject());
      }
   }

   public static String getRequiredString(String p_275692_, JsonObject p_275706_) {
      String $$2 = getStringOr(p_275692_, p_275706_, null);
      if ($$2 == null) {
         throw new IllegalStateException("Missing required property: " + p_275692_);
      } else {
         return $$2;
      }
   }

   public static String getRequiredStringOr(String p_307555_, JsonObject p_307369_, String p_307525_) {
      JsonElement $$3 = p_307369_.get(p_307555_);
      if ($$3 != null) {
         return $$3.isJsonNull() ? p_307525_ : $$3.getAsString();
      } else {
         return p_307525_;
      }
   }

   @Nullable
   public static String getStringOr(String p_90162_, JsonObject p_90163_, @Nullable String p_90164_) {
      JsonElement $$3 = p_90163_.get(p_90162_);
      if ($$3 != null) {
         return $$3.isJsonNull() ? p_90164_ : $$3.getAsString();
      } else {
         return p_90164_;
      }
   }

   @Nullable
   public static UUID getUuidOr(String p_275342_, JsonObject p_275515_, @Nullable UUID p_275232_) {
      String $$3 = getStringOr(p_275342_, p_275515_, null);
      return $$3 == null ? p_275232_ : UndashedUuid.fromStringLenient($$3);
   }

   public static int getIntOr(String p_90154_, JsonObject p_90155_, int p_90156_) {
      JsonElement $$3 = p_90155_.get(p_90154_);
      if ($$3 != null) {
         return $$3.isJsonNull() ? p_90156_ : $$3.getAsInt();
      } else {
         return p_90156_;
      }
   }

   public static long getLongOr(String p_90158_, JsonObject p_90159_, long p_90160_) {
      JsonElement $$3 = p_90159_.get(p_90158_);
      if ($$3 != null) {
         return $$3.isJsonNull() ? p_90160_ : $$3.getAsLong();
      } else {
         return p_90160_;
      }
   }

   public static boolean getBooleanOr(String p_90166_, JsonObject p_90167_, boolean p_90168_) {
      JsonElement $$3 = p_90167_.get(p_90166_);
      if ($$3 != null) {
         return $$3.isJsonNull() ? p_90168_ : $$3.getAsBoolean();
      } else {
         return p_90168_;
      }
   }

   public static Date getDateOr(String p_90151_, JsonObject p_90152_) {
      JsonElement $$2 = p_90152_.get(p_90151_);
      return $$2 != null ? new Date(Long.parseLong($$2.getAsString())) : new Date();
   }
}
