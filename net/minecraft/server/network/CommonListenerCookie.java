package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.neoforged.neoforge.network.connection.ConnectionType;

public record CommonListenerCookie(GameProfile gameProfile, int latency, ClientInformation clientInformation, boolean transferred, ConnectionType connectionType) {
   @Deprecated
   public CommonListenerCookie(GameProfile gameProfile, int latency, ClientInformation clientInformation, boolean transferred) {
      this(gameProfile, latency, clientInformation, transferred, ConnectionType.OTHER);
   }

   public static CommonListenerCookie createInitial(GameProfile p_302024_, boolean p_320180_) {
      return new CommonListenerCookie(p_302024_, 0, ClientInformation.createDefault(), p_320180_);
   }
}
