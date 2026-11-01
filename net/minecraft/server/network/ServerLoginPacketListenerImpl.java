package net.minecraft.server.network;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.notifications.ServerActivityMonitor;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerLoginPacketListenerImpl implements ServerLoginPacketListener, TickablePacketListener {
   private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_TICKS_BEFORE_LOGIN = 600;
   private final byte[] challenge;
   final MinecraftServer server;
   final Connection connection;
   final ServerActivityMonitor serverActivityMonitor;
   private volatile ServerLoginPacketListenerImpl.State state = ServerLoginPacketListenerImpl.State.HELLO;
   private int tick;
   @Nullable
   String requestedUsername;
   @Nullable
   private GameProfile authenticatedProfile;
   private final String serverId = "";
   private final boolean transferred;

   public ServerLoginPacketListenerImpl(MinecraftServer server, Connection connection, boolean transferred) {
      this.server = server;
      this.connection = connection;
      this.serverActivityMonitor = this.server.getServerActivityMonitor();
      this.challenge = Ints.toByteArray(RandomSource.create().nextInt());
      this.transferred = transferred;
   }

   @Override
   public void tick() {
      if (this.state == ServerLoginPacketListenerImpl.State.VERIFYING) {
         this.verifyLoginAndFinishConnectionSetup(Objects.requireNonNull(this.authenticatedProfile));
      }

      if (this.state == ServerLoginPacketListenerImpl.State.WAITING_FOR_DUPE_DISCONNECT
         && !this.isPlayerAlreadyInWorld(Objects.requireNonNull(this.authenticatedProfile))) {
         this.finishLoginAndWaitForClient(this.authenticatedProfile);
      }

      if (this.tick++ == 600) {
         this.disconnect(Component.translatable("multiplayer.disconnect.slow_login"));
      }
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   public void disconnect(Component reason) {
      try {
         LOGGER.info("Disconnecting {}: {}", this.getUserName(), reason.getString());
         this.connection.send(new ClientboundLoginDisconnectPacket(reason));
         this.connection.disconnect(reason);
      } catch (Exception var3) {
         LOGGER.error("Error whilst disconnecting player", var3);
      }
   }

   private boolean isPlayerAlreadyInWorld(GameProfile profile) {
      return this.server.getPlayerList().getPlayer(profile.id()) != null;
   }

   @Override
   public void onDisconnect(DisconnectionDetails details) {
      LOGGER.info("{} lost connection: {}", this.getUserName(), details.reason().getString());
   }

   public String getUserName() {
      String string = this.connection.getLoggableAddress(this.server.logIPs());
      return this.requestedUsername != null ? this.requestedUsername + " (" + string + ")" : string;
   }

   @Override
   public void handleHello(ServerboundHelloPacket packet) {
      Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet", new Object[0]);
      Validate.validState(StringUtil.isValidPlayerName(packet.name()), "Invalid characters in username", new Object[0]);
      this.requestedUsername = packet.name();
      GameProfile gameProfile = this.server.getSingleplayerProfile();
      if (gameProfile != null && this.requestedUsername.equalsIgnoreCase(gameProfile.name())) {
         this.startClientVerification(gameProfile);
      } else {
         if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
            this.state = ServerLoginPacketListenerImpl.State.KEY;
            this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.challenge, true));
         } else {
            this.startClientVerification(UUIDUtil.createOfflineProfile(this.requestedUsername));
         }
      }
   }

   void startClientVerification(GameProfile authenticatedProfile) {
      this.authenticatedProfile = authenticatedProfile;
      this.state = ServerLoginPacketListenerImpl.State.VERIFYING;
   }

   private void verifyLoginAndFinishConnectionSetup(GameProfile profile) {
      PlayerList playerList = this.server.getPlayerList();
      Component component = playerList.canPlayerLogin(this.connection.getRemoteAddress(), new NameAndId(profile));
      if (component != null) {
         this.disconnect(component);
      } else {
         if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
            this.connection
               .send(
                  new ClientboundLoginCompressionPacket(this.server.getCompressionThreshold()),
                  PacketSendListener.thenRun(() -> this.connection.setupCompression(this.server.getCompressionThreshold(), true))
               );
         }

         boolean bl = playerList.disconnectAllPlayersWithProfile(profile.id());
         if (bl) {
            this.state = ServerLoginPacketListenerImpl.State.WAITING_FOR_DUPE_DISCONNECT;
         } else {
            this.finishLoginAndWaitForClient(profile);
         }
      }
   }

   private void finishLoginAndWaitForClient(GameProfile profile) {
      this.state = ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING;
      this.connection.send(new ClientboundLoginFinishedPacket(profile));
   }

   @Override
   public void handleKey(ServerboundKeyPacket packet) {
      Validate.validState(this.state == ServerLoginPacketListenerImpl.State.KEY, "Unexpected key packet", new Object[0]);

      final String string;
      try {
         PrivateKey privateKey = this.server.getKeyPair().getPrivate();
         if (!packet.isChallengeValid(this.challenge, privateKey)) {
            throw new IllegalStateException("Protocol error");
         }

         SecretKey secretKey = packet.getSecretKey(privateKey);
         Cipher cipher = Crypt.getCipher(2, secretKey);
         Cipher cipher2 = Crypt.getCipher(1, secretKey);
         string = new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), secretKey)).toString(16);
         this.state = ServerLoginPacketListenerImpl.State.AUTHENTICATING;
         this.connection.setEncryptionKey(cipher, cipher2);
      } catch (CryptException var7) {
         throw new IllegalStateException("Protocol error", var7);
      }

      Thread thread = new Thread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
         @Override
         public void run() {
            String stringx = Objects.requireNonNull(ServerLoginPacketListenerImpl.this.requestedUsername, "Player name not initialized");

            try {
               ProfileResult profileResult = ServerLoginPacketListenerImpl.this.server
                  .services()
                  .sessionService()
                  .hasJoinedServer(stringx, string, this.getAddress());
               if (profileResult != null) {
                  GameProfile gameProfile = profileResult.profile();
                  ServerLoginPacketListenerImpl.LOGGER.info("UUID of player {} is {}", gameProfile.name(), gameProfile.id());
                  ServerLoginPacketListenerImpl.this.serverActivityMonitor.reportLoginActivity();
                  ServerLoginPacketListenerImpl.this.startClientVerification(gameProfile);
               } else if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                  ServerLoginPacketListenerImpl.LOGGER.warn("Failed to verify username but will let them in anyway!");
                  ServerLoginPacketListenerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(stringx));
               } else {
                  ServerLoginPacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                  ServerLoginPacketListenerImpl.LOGGER.error("Username '{}' tried to join with an invalid session", stringx);
               }
            } catch (AuthenticationUnavailableException var4) {
               if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                  ServerLoginPacketListenerImpl.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                  ServerLoginPacketListenerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(stringx));
               } else {
                  ServerLoginPacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                  ServerLoginPacketListenerImpl.LOGGER.error("Couldn't verify username because servers are unavailable");
               }
            }
         }

         @Nullable
         private InetAddress getAddress() {
            SocketAddress socketAddress = ServerLoginPacketListenerImpl.this.connection.getRemoteAddress();
            return ServerLoginPacketListenerImpl.this.server.getPreventProxyConnections() && socketAddress instanceof InetSocketAddress
               ? ((InetSocketAddress)socketAddress).getAddress()
               : null;
         }
      };
      thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   @Override
   public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet) {
      this.disconnect(ServerCommonPacketListenerImpl.DISCONNECT_UNEXPECTED_QUERY);
   }

   @Override
   public void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket packet) {
      Validate.validState(this.state == ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING, "Unexpected login acknowledgement packet", new Object[0]);
      this.connection.setupOutboundProtocol(ConfigurationProtocols.CLIENTBOUND);
      CommonListenerCookie commonListenerCookie = CommonListenerCookie.createInitial(Objects.requireNonNull(this.authenticatedProfile), this.transferred);
      ServerConfigurationPacketListenerImpl serverConfigurationPacketListenerImpl = new ServerConfigurationPacketListenerImpl(
         this.server, this.connection, commonListenerCookie
      );
      this.connection.setupInboundProtocol(ConfigurationProtocols.SERVERBOUND, serverConfigurationPacketListenerImpl);
      serverConfigurationPacketListenerImpl.startConfiguration();
      this.state = ServerLoginPacketListenerImpl.State.ACCEPTED;
   }

   @Override
   public void fillListenerSpecificCrashDetails(CrashReport crashReport, CrashReportCategory category) {
      category.setDetail("Login phase", () -> this.state.toString());
   }

   @Override
   public void handleCookieResponse(ServerboundCookieResponsePacket packet) {
      this.disconnect(ServerCommonPacketListenerImpl.DISCONNECT_UNEXPECTED_QUERY);
   }

   static enum State {
      HELLO,
      KEY,
      AUTHENTICATING,
      NEGOTIATING,
      VERIFYING,
      WAITING_FOR_DUPE_DISCONNECT,
      PROTOCOL_SWITCHING,
      ACCEPTED;
   }
}
