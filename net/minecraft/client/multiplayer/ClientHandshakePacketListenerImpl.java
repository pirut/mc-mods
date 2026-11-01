package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.ForcedUsernameChangeException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserBannedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Crypt;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientHandshakePacketListenerImpl implements ClientLoginPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   @Nullable
   private final ServerData serverData;
   @Nullable
   private final Screen parent;
   private final Consumer<Component> updateStatus;
   private final Connection connection;
   private final boolean newWorld;
   @Nullable
   private final Duration worldLoadDuration;
   @Nullable
   private String minigameName;
   private final Map<ResourceLocation, byte[]> cookies;
   private final boolean wasTransferredTo;
   private final AtomicReference<ClientHandshakePacketListenerImpl.State> state = new AtomicReference<>(ClientHandshakePacketListenerImpl.State.CONNECTING);

   public ClientHandshakePacketListenerImpl(
      Connection p_261697_,
      Minecraft p_261835_,
      @Nullable ServerData p_261938_,
      @Nullable Screen p_261783_,
      boolean p_261562_,
      @Nullable Duration p_261673_,
      Consumer<Component> p_261945_,
      @Nullable TransferState p_320762_
   ) {
      this.connection = p_261697_;
      this.minecraft = p_261835_;
      this.serverData = p_261938_;
      this.parent = p_261783_;
      this.updateStatus = p_261945_;
      this.newWorld = p_261562_;
      this.worldLoadDuration = p_261673_;
      this.cookies = p_320762_ != null ? new HashMap<>(p_320762_.cookies()) : new HashMap<>();
      this.wasTransferredTo = p_320762_ != null;
   }

   private void switchState(ClientHandshakePacketListenerImpl.State p_302230_) {
      ClientHandshakePacketListenerImpl.State $$1 = this.state.updateAndGet(p_339288_ -> {
         if (!p_302230_.fromStates.contains(p_339288_)) {
            throw new IllegalStateException("Tried to switch to " + p_302230_ + " from " + p_339288_ + ", but expected one of " + p_302230_.fromStates);
         } else {
            return p_302230_;
         }
      });
      this.updateStatus.accept($$1.message);
   }

   @Override
   public void handleHello(ClientboundHelloPacket p_104549_) {
      this.switchState(ClientHandshakePacketListenerImpl.State.AUTHORIZING);

      Cipher $$4;
      Cipher $$5;
      String $$3;
      ServerboundKeyPacket $$7;
      try {
         SecretKey $$1 = Crypt.generateSecretKey();
         PublicKey $$2 = p_104549_.getPublicKey();
         $$3 = new BigInteger(Crypt.digestData(p_104549_.getServerId(), $$2, $$1)).toString(16);
         $$4 = Crypt.getCipher(2, $$1);
         $$5 = Crypt.getCipher(1, $$1);
         byte[] $$6 = p_104549_.getChallenge();
         $$7 = new ServerboundKeyPacket($$1, $$2, $$6);
      } catch (Exception var9) {
         throw new IllegalStateException("Protocol error", var9);
      }

      if (p_104549_.shouldAuthenticate()) {
         Util.ioPool().submit(() -> {
            Component $$4x = this.authenticateServer($$3);
            if ($$4x != null) {
               if (this.serverData == null || !this.serverData.isLan()) {
                  this.connection.disconnect($$4x);
                  return;
               }

               LOGGER.warn($$4x.getString());
            }

            this.setEncryption($$7, $$4, $$5);
         });
      } else {
         this.setEncryption($$7, $$4, $$5);
      }
   }

   private void setEncryption(ServerboundKeyPacket p_319860_, Cipher p_320550_, Cipher p_320442_) {
      this.switchState(ClientHandshakePacketListenerImpl.State.ENCRYPTING);
      this.connection.send(p_319860_, PacketSendListener.thenRun(() -> this.connection.setEncryptionKey(p_320550_, p_320442_)));
   }

   @Nullable
   private Component authenticateServer(String p_104532_) {
      try {
         this.getMinecraftSessionService().joinServer(this.minecraft.getUser().getProfileId(), this.minecraft.getUser().getAccessToken(), p_104532_);
         return null;
      } catch (AuthenticationUnavailableException var3) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.serversUnavailable"));
      } catch (InvalidCredentialsException var4) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.invalidSession"));
      } catch (InsufficientPrivilegesException var5) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.insufficientPrivileges"));
      } catch (ForcedUsernameChangeException | UserBannedException var6) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.userBanned"));
      } catch (AuthenticationException var7) {
         return Component.translatable("disconnect.loginFailedInfo", var7.getMessage());
      }
   }

   private MinecraftSessionService getMinecraftSessionService() {
      return this.minecraft.getMinecraftSessionService();
   }

   @Override
   public void handleGameProfile(ClientboundGameProfilePacket p_104547_) {
      this.switchState(ClientHandshakePacketListenerImpl.State.JOINING);
      GameProfile $$1 = p_104547_.gameProfile();
      this.connection
         .setupInboundProtocol(
            ConfigurationProtocols.CLIENTBOUND,
            new ClientConfigurationPacketListenerImpl(
               this.minecraft,
               this.connection,
               new CommonListenerCookie(
                  $$1,
                  this.minecraft.getTelemetryManager().createWorldSessionManager(this.newWorld, this.worldLoadDuration, this.minigameName),
                  ClientRegistryLayer.createRegistryAccess().compositeAccess(),
                  FeatureFlags.DEFAULT_FLAGS,
                  null,
                  this.serverData,
                  this.parent,
                  this.cookies,
                  null,
                  p_104547_.strictErrorHandling(),
                  Map.of(),
                  ServerLinks.EMPTY
               )
            )
         );
      this.connection.send(ServerboundLoginAcknowledgedPacket.INSTANCE);
      this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
      this.connection.send(new ServerboundCustomPayloadPacket(new BrandPayload(ClientBrandRetriever.getClientModName())));
      this.connection.send(new ServerboundClientInformationPacket(this.minecraft.options.buildPlayerInformation()));
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_350923_) {
      Component $$1 = this.wasTransferredTo ? CommonComponents.TRANSFER_CONNECT_FAILED : CommonComponents.CONNECT_FAILED;
      if (this.serverData != null && this.serverData.isRealm()) {
         this.minecraft.setScreen(new DisconnectedRealmsScreen(this.parent, $$1, p_350923_.reason()));
      } else {
         this.minecraft.setScreen(new DisconnectedScreen(this.parent, $$1, p_350923_));
      }
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   public void handleDisconnect(ClientboundLoginDisconnectPacket p_104553_) {
      this.connection.disconnect(p_104553_.getReason());
   }

   @Override
   public void handleCompression(ClientboundLoginCompressionPacket p_104551_) {
      if (!this.connection.isMemoryConnection()) {
         this.connection.setupCompression(p_104551_.getCompressionThreshold(), false);
      }
   }

   @Override
   public void handleCustomQuery(ClientboundCustomQueryPacket p_104545_) {
      this.updateStatus.accept(Component.translatable("connect.negotiating"));
      this.connection.send(new ServerboundCustomQueryAnswerPacket(p_104545_.transactionId(), null));
   }

   public void setMinigameName(@Nullable String p_286653_) {
      this.minigameName = p_286653_;
   }

   @Override
   public void handleRequestCookie(ClientboundCookieRequestPacket p_319938_) {
      this.connection.send(new ServerboundCookieResponsePacket(p_319938_.key(), this.cookies.get(p_319938_.key())));
   }

   @Override
   public void fillListenerSpecificCrashDetails(CrashReport p_350555_, CrashReportCategory p_315015_) {
      p_315015_.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<unknown>");
      p_315015_.setDetail("Login phase", () -> this.state.get().toString());
   }

   @OnlyIn(Dist.CLIENT)
   static enum State {
      CONNECTING(Component.translatable("connect.connecting"), Set.of()),
      AUTHORIZING(Component.translatable("connect.authorizing"), Set.of(CONNECTING)),
      ENCRYPTING(Component.translatable("connect.encrypting"), Set.of(AUTHORIZING)),
      JOINING(Component.translatable("connect.joining"), Set.of(ENCRYPTING, CONNECTING));

      final Component message;
      final Set<ClientHandshakePacketListenerImpl.State> fromStates;

      private State(Component p_302239_, Set<ClientHandshakePacketListenerImpl.State> p_302236_) {
         this.message = p_302239_;
         this.fromStates = p_302236_;
      }
   }
}
