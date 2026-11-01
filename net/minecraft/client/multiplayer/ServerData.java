package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.PngInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ServerData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_ICON_SIZE = 1024;
   public String name;
   public String ip;
   public Component status;
   public Component motd;
   @Nullable
   public ServerStatus.Players players;
   public long ping;
   public int protocol = SharedConstants.getCurrentVersion().protocolVersion();
   public Component version = Component.literal(SharedConstants.getCurrentVersion().name());
   public List<Component> playerList = Collections.emptyList();
   private ServerData.ServerPackStatus packStatus = ServerData.ServerPackStatus.PROMPT;
   @Nullable
   private byte[] iconBytes;
   private ServerData.Type type;
   private int acceptedCodeOfConduct;
   private ServerData.State state = ServerData.State.INITIAL;

   public ServerData(String name, String ip, ServerData.Type type) {
      this.name = name;
      this.ip = ip;
      this.type = type;
   }

   public CompoundTag write() {
      CompoundTag compoundTag = new CompoundTag();
      compoundTag.putString("name", this.name);
      compoundTag.putString("ip", this.ip);
      compoundTag.storeNullable("icon", ExtraCodecs.BASE64_STRING, this.iconBytes);
      compoundTag.store(ServerData.ServerPackStatus.FIELD_CODEC, this.packStatus);
      if (this.acceptedCodeOfConduct != 0) {
         compoundTag.putInt("acceptedCodeOfConduct", this.acceptedCodeOfConduct);
      }

      return compoundTag;
   }

   public ServerData.ServerPackStatus getResourcePackStatus() {
      return this.packStatus;
   }

   public void setResourcePackStatus(ServerData.ServerPackStatus packStatus) {
      this.packStatus = packStatus;
   }

   public static ServerData read(CompoundTag nbtCompound) {
      ServerData serverData = new ServerData(nbtCompound.getStringOr("name", ""), nbtCompound.getStringOr("ip", ""), ServerData.Type.OTHER);
      serverData.setIconBytes(nbtCompound.<byte[]>read("icon", ExtraCodecs.BASE64_STRING).orElse(null));
      serverData.setResourcePackStatus(
         nbtCompound.<ServerData.ServerPackStatus>read(ServerData.ServerPackStatus.FIELD_CODEC).orElse(ServerData.ServerPackStatus.PROMPT)
      );
      serverData.acceptedCodeOfConduct = nbtCompound.getIntOr("acceptedCodeOfConduct", 0);
      return serverData;
   }

   @Nullable
   public byte[] getIconBytes() {
      return this.iconBytes;
   }

   public void setIconBytes(@Nullable byte[] iconBytes) {
      this.iconBytes = iconBytes;
   }

   public boolean isLan() {
      return this.type == ServerData.Type.LAN;
   }

   public boolean isRealm() {
      return this.type == ServerData.Type.REALM;
   }

   public ServerData.Type type() {
      return this.type;
   }

   public boolean hasAcceptedCodeOfConduct(String codeOfConduct) {
      return this.acceptedCodeOfConduct == codeOfConduct.hashCode();
   }

   public void acceptCodeOfConduct(String codeOfConduct) {
      this.acceptedCodeOfConduct = codeOfConduct.hashCode();
   }

   public void clearCodeOfConduct() {
      this.acceptedCodeOfConduct = 0;
   }

   public void copyNameIconFrom(ServerData other) {
      this.ip = other.ip;
      this.name = other.name;
      this.iconBytes = other.iconBytes;
   }

   public void copyFrom(ServerData serverData) {
      this.copyNameIconFrom(serverData);
      this.setResourcePackStatus(serverData.getResourcePackStatus());
      this.type = serverData.type;
   }

   public ServerData.State state() {
      return this.state;
   }

   public void setState(ServerData.State state) {
      this.state = state;
   }

   @Nullable
   public static byte[] validateIcon(@Nullable byte[] icon) {
      if (icon != null) {
         try {
            PngInfo pngInfo = PngInfo.fromBytes(icon);
            if (pngInfo.width() <= 1024 && pngInfo.height() <= 1024) {
               return icon;
            }
         } catch (IOException var2) {
            LOGGER.warn("Failed to decode server icon", var2);
         }
      }

      return null;
   }

   @Environment(EnvType.CLIENT)
   public static enum ServerPackStatus {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      public static final MapCodec<ServerData.ServerPackStatus> FIELD_CODEC = Codec.BOOL
         .optionalFieldOf("acceptTextures")
         .xmap(optional -> optional.<ServerData.ServerPackStatus>map(boolean_ -> boolean_ ? ENABLED : DISABLED).orElse(PROMPT), serverPackStatus -> {
            return switch (serverPackStatus) {
               case ENABLED -> Optional.of(true);
               case DISABLED -> Optional.of(false);
               case PROMPT -> Optional.empty();
            };
         });
      private final Component name;

      private ServerPackStatus(final String name) {
         this.name = Component.translatable("manageServer.resourcePack." + name);
      }

      public Component getName() {
         return this.name;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum State {
      INITIAL,
      PINGING,
      UNREACHABLE,
      INCOMPATIBLE,
      SUCCESSFUL;
   }

   @Environment(EnvType.CLIENT)
   public static enum Type {
      LAN,
      REALM,
      OTHER;
   }
}
