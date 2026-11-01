package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record RealmsSetting(@SerializedName("name") String name, @SerializedName("value") String value) implements ReflectionBasedSerialization {
   public static RealmsSetting hardcoreSetting(boolean hardcore) {
      return new RealmsSetting("hardcore", Boolean.toString(hardcore));
   }

   public static boolean isHardcore(List<RealmsSetting> settings) {
      for (RealmsSetting realmsSetting : settings) {
         if (realmsSetting.name().equals("hardcore")) {
            return Boolean.parseBoolean(realmsSetting.value());
         }
      }

      return false;
   }
}
