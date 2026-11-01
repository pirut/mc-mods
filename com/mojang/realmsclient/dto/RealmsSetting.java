package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RealmsSetting(@SerializedName("name") String name, @SerializedName("value") String value) implements ReflectionBasedSerialization {
   public static RealmsSetting hardcoreSetting(boolean p_419889_) {
      return new RealmsSetting("hardcore", Boolean.toString(p_419889_));
   }

   public static boolean isHardcore(List<RealmsSetting> p_419685_) {
      for (RealmsSetting realmssetting : p_419685_) {
         if (realmssetting.name().equals("hardcore")) {
            return Boolean.parseBoolean(realmssetting.value());
         }
      }

      return false;
   }
}
