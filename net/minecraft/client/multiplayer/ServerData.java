package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.PngInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ExtendedServerListData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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
   public ExtendedServerListData neoForgeData = null;

   public ServerData(String p_105375_, String p_105376_, ServerData.Type p_295587_) {
      this.name = p_105375_;
      this.ip = p_105376_;
      this.type = p_295587_;
   }

   public CompoundTag write() {
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putString("name", this.name);
      compoundtag.putString("ip", this.ip);
      compoundtag.storeNullable("icon", ExtraCodecs.BASE64_STRING, this.iconBytes);
      compoundtag.store(ServerData.ServerPackStatus.FIELD_CODEC, this.packStatus);
      if (this.acceptedCodeOfConduct != 0) {
         compoundtag.putInt("acceptedCodeOfConduct", this.acceptedCodeOfConduct);
      }

      return compoundtag;
   }

   public ServerData.ServerPackStatus getResourcePackStatus() {
      return this.packStatus;
   }

   public void setResourcePackStatus(ServerData.ServerPackStatus p_105380_) {
      this.packStatus = p_105380_;
   }

   public static ServerData read(CompoundTag p_105386_) {
      ServerData serverdata = new ServerData(p_105386_.getStringOr("name", ""), p_105386_.getStringOr("ip", ""), ServerData.Type.OTHER);
      serverdata.setIconBytes(p_105386_.<byte[]>read("icon", ExtraCodecs.BASE64_STRING).orElse(null));
      serverdata.setResourcePackStatus(
         p_105386_.<ServerData.ServerPackStatus>read(ServerData.ServerPackStatus.FIELD_CODEC).orElse(ServerData.ServerPackStatus.PROMPT)
      );
      serverdata.acceptedCodeOfConduct = p_105386_.getIntOr("acceptedCodeOfConduct", 0);
      return serverdata;
   }

   @Nullable
   public byte[] getIconBytes() {
      return this.iconBytes;
   }

   public void setIconBytes(@Nullable byte[] p_272760_) {
      this.iconBytes = p_272760_;
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

   public boolean hasAcceptedCodeOfConduct(String p_439606_) {
      return this.acceptedCodeOfConduct == p_439606_.hashCode();
   }

   public void acceptCodeOfConduct(String p_439473_) {
      this.acceptedCodeOfConduct = p_439473_.hashCode();
   }

   public void clearCodeOfConduct() {
      this.acceptedCodeOfConduct = 0;
   }

   public void copyNameIconFrom(ServerData p_233804_) {
      this.ip = p_233804_.ip;
      this.name = p_233804_.name;
      this.iconBytes = p_233804_.iconBytes;
   }

   public void copyFrom(ServerData p_105382_) {
      this.copyNameIconFrom(p_105382_);
      this.setResourcePackStatus(p_105382_.getResourcePackStatus());
      this.type = p_105382_.type;
   }

   public ServerData.State state() {
      return this.state;
   }

   public void setState(ServerData.State p_316214_) {
      this.state = p_316214_;
   }

   @Nullable
   public static byte[] validateIcon(@Nullable byte[] p_302394_) {
      if (p_302394_ != null) {
         try {
            PngInfo pnginfo = PngInfo.fromBytes(p_302394_);
            if (pnginfo.width() <= 1024 && pnginfo.height() <= 1024) {
               return p_302394_;
            }
         } catch (IOException var2) {
            LOGGER.warn("Failed to decode server icon", var2);
         }
      }

      return null;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum ServerPackStatus {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      public static final MapCodec<ServerData.ServerPackStatus> FIELD_CODEC = Codec.BOOL
         .optionalFieldOf("acceptTextures")
         .xmap(p_404910_ -> p_404910_.<ServerData.ServerPackStatus>map(p_404952_ -> p_404952_ ? ENABLED : DISABLED).orElse(PROMPT), p_405078_ -> {
            return switch (p_405078_) {
               case ENABLED -> Optional.of(true);
               case DISABLED -> Optional.of(false);
               case PROMPT -> Optional.empty();
            };
         });
      private final Component name;

      private ServerPackStatus(String p_105399_) {
         this.name = Component.translatable("manageServer.resourcePack." + p_105399_);
      }

      public Component getName() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum State {
      INITIAL,
      PINGING,
      UNREACHABLE,
      INCOMPATIBLE,
      SUCCESSFUL;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      LAN,
      REALM,
      OTHER;
   }
}
