package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.util.UUIDTypeAdapter;
import java.util.UUID;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerInfo extends ValueObject implements ReflectionBasedSerialization {
   @SerializedName("name")
   public final String name;
   @SerializedName("uuid")
   @JsonAdapter(UUIDTypeAdapter.class)
   public final UUID uuid;
   @SerializedName("operator")
   public boolean operator;
   @SerializedName("accepted")
   public final boolean accepted;
   @SerializedName("online")
   public final boolean online;

   public PlayerInfo(String p_455009_, UUID p_454854_, boolean p_455166_, boolean p_454971_, boolean p_455926_) {
      this.name = p_455009_;
      this.uuid = p_454854_;
      this.operator = p_455166_;
      this.accepted = p_454971_;
      this.online = p_455926_;
   }
}
