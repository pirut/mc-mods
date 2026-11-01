package com.mojang.realmsclient.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsHttpException;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public interface RealmsError {
   Component NO_MESSAGE = Component.translatable("mco.errorMessage.noDetails");
   Logger LOGGER = LogUtils.getLogger();

   int errorCode();

   Component errorMessage();

   String logMessage();

   static RealmsError parse(int p_294532_, String p_87304_) {
      if (p_294532_ == 429) {
         return RealmsError.CustomError.SERVICE_BUSY;
      } else if (Strings.isNullOrEmpty(p_87304_)) {
         return RealmsError.CustomError.noPayload(p_294532_);
      } else {
         try {
            JsonObject $$2 = JsonParser.parseString(p_87304_).getAsJsonObject();
            String $$3 = GsonHelper.getAsString($$2, "reason", null);
            String $$4 = GsonHelper.getAsString($$2, "errorMsg", null);
            int $$5 = GsonHelper.getAsInt($$2, "errorCode", -1);
            if ($$4 != null || $$3 != null || $$5 != -1) {
               return new RealmsError.ErrorWithJsonPayload(p_294532_, $$5 != -1 ? $$5 : p_294532_, $$3, $$4);
            }
         } catch (Exception var6) {
            LOGGER.error("Could not parse RealmsError", var6);
         }

         return new RealmsError.ErrorWithRawPayload(p_294532_, p_87304_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record AuthenticationError(String message) implements RealmsError {
      public static final int ERROR_CODE = 401;

      @Override
      public int errorCode() {
         return 401;
      }

      @Override
      public Component errorMessage() {
         return Component.literal(this.message);
      }

      @Override
      public String logMessage() {
         return String.format(Locale.ROOT, "Realms authentication error with message '%s'", this.message);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record CustomError(int httpCode, @Nullable Component payload) implements RealmsError {
      public static final RealmsError.CustomError SERVICE_BUSY = new RealmsError.CustomError(429, Component.translatable("mco.errorMessage.serviceBusy"));
      public static final Component RETRY_MESSAGE = Component.translatable("mco.errorMessage.retry");

      public static RealmsError.CustomError unknownCompatibilityResponse(String p_294662_) {
         return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.unknownCompatibility", p_294662_));
      }

      public static RealmsError.CustomError connectivityError(RealmsHttpException p_294863_) {
         return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.connectivity", p_294863_.getMessage()));
      }

      public static RealmsError.CustomError retry(int p_295020_) {
         return new RealmsError.CustomError(p_295020_, RETRY_MESSAGE);
      }

      public static RealmsError.CustomError noPayload(int p_295243_) {
         return new RealmsError.CustomError(p_295243_, null);
      }

      @Override
      public int errorCode() {
         return this.httpCode;
      }

      @Override
      public Component errorMessage() {
         return this.payload != null ? this.payload : NO_MESSAGE;
      }

      @Override
      public String logMessage() {
         return this.payload != null
            ? String.format(Locale.ROOT, "Realms service error (%d) with message '%s'", this.httpCode, this.payload.getString())
            : String.format(Locale.ROOT, "Realms service error (%d) with no payload", this.httpCode);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record ErrorWithJsonPayload(int httpCode, int code, @Nullable String reason, @Nullable String message) implements RealmsError {
      @Override
      public int errorCode() {
         return this.code;
      }

      @Override
      public Component errorMessage() {
         String $$0 = "mco.errorMessage." + this.code;
         if (I18n.exists($$0)) {
            return Component.translatable($$0);
         } else {
            if (this.reason != null) {
               String $$1 = "mco.errorReason." + this.reason;
               if (I18n.exists($$1)) {
                  return Component.translatable($$1);
               }
            }

            return (Component)(this.message != null ? Component.literal(this.message) : NO_MESSAGE);
         }
      }

      @Override
      public String logMessage() {
         return String.format(Locale.ROOT, "Realms service error (%d/%d/%s) with message '%s'", this.httpCode, this.code, this.reason, this.message);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record ErrorWithRawPayload(int httpCode, String payload) implements RealmsError {
      @Override
      public int errorCode() {
         return this.httpCode;
      }

      @Override
      public Component errorMessage() {
         return Component.literal(this.payload);
      }

      @Override
      public String logMessage() {
         return String.format(Locale.ROOT, "Realms service error (%d) with raw payload '%s'", this.httpCode, this.payload);
      }
   }
}
