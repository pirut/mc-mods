package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public enum ServiceQuality {
   GREAT(1, "icon/ping_5"),
   GOOD(2, "icon/ping_4"),
   OKAY(3, "icon/ping_3"),
   POOR(4, "icon/ping_2"),
   UNKNOWN(5, "icon/ping_unknown");

   final int value;
   private final Identifier icon;

   private ServiceQuality(int p_419890_, String p_420001_) {
      this.value = p_419890_;
      this.icon = Identifier.withDefaultNamespace(p_420001_);
   }

   @Nullable
   public static ServiceQuality byValue(int p_419561_) {
      for (ServiceQuality servicequality : values()) {
         if (servicequality.getValue() == p_419561_) {
            return servicequality;
         }
      }

      return null;
   }

   public int getValue() {
      return this.value;
   }

   public Identifier getIcon() {
      return this.icon;
   }

   @OnlyIn(Dist.CLIENT)
   public static class RealmsServiceQualityJsonAdapter extends TypeAdapter<ServiceQuality> {
      private static final Logger LOGGER = LogUtils.getLogger();

      public void write(JsonWriter p_419523_, ServiceQuality p_419821_) throws IOException {
         p_419523_.value(p_419821_.value);
      }

      public ServiceQuality read(JsonReader p_419604_) throws IOException {
         int i = p_419604_.nextInt();
         ServiceQuality servicequality = ServiceQuality.byValue(i);
         if (servicequality == null) {
            LOGGER.warn("Unsupported ServiceQuality {}", i);
            return ServiceQuality.UNKNOWN;
         } else {
            return servicequality;
         }
      }
   }
}
