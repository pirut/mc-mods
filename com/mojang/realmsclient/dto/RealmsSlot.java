package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RealmsSlot implements ReflectionBasedSerialization {
   @SerializedName("slotId")
   public int slotId;
   @SerializedName("options")
   @JsonAdapter(RealmsSlot.RealmsWorldOptionsJsonAdapter.class)
   public RealmsWorldOptions options;
   @SerializedName("settings")
   public List<RealmsSetting> settings;

   public RealmsSlot(int p_419471_, RealmsWorldOptions p_420054_, List<RealmsSetting> p_419956_) {
      this.slotId = p_419471_;
      this.options = p_420054_;
      this.settings = p_419956_;
   }

   public static RealmsSlot defaults(int p_419925_) {
      return new RealmsSlot(p_419925_, RealmsWorldOptions.createEmptyDefaults(), List.of(RealmsSetting.hardcoreSetting(false)));
   }

   public RealmsSlot copy() {
      return new RealmsSlot(this.slotId, this.options.copy(), new ArrayList<>(this.settings));
   }

   public boolean isHardcore() {
      return RealmsSetting.isHardcore(this.settings);
   }

   @OnlyIn(Dist.CLIENT)
   static class RealmsWorldOptionsJsonAdapter extends TypeAdapter<RealmsWorldOptions> {
      private RealmsWorldOptionsJsonAdapter() {
      }

      public void write(JsonWriter p_420023_, RealmsWorldOptions p_419932_) throws IOException {
         p_420023_.jsonValue(new GuardedSerializer().toJson(p_419932_));
      }

      public RealmsWorldOptions read(JsonReader p_419978_) throws IOException {
         String s = p_419978_.nextString();
         return RealmsWorldOptions.parse(new GuardedSerializer(), s);
      }
   }
}
