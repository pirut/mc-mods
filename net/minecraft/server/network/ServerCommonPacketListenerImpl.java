package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.CommonRegisterPayload;
import net.neoforged.neoforge.network.payload.CommonVersionPayload;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ServerCommonPacketListenerImpl implements ServerCommonPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int LATENCY_CHECK_INTERVAL = 15000;
   private static final int CLOSED_LISTENER_TIMEOUT = 15000;
   private static final Component TIMEOUT_DISCONNECTION_MESSAGE = Component.translatable("disconnect.timeout");
   static final Component DISCONNECT_UNEXPECTED_QUERY = Component.translatable("multiplayer.disconnect.unexpected_query_response");
   protected final MinecraftServer server;
   protected final Connection connection;
   private final boolean transferred;
   private long keepAliveTime;
   private boolean keepAlivePending;
   private long keepAliveChallenge;
   private long closedListenerTime;
   private boolean closed = false;
   private int latency;
   private volatile boolean suspendFlushingOnServerThread = false;
   protected ConnectionType connectionType;

   public ServerCommonPacketListenerImpl(MinecraftServer p_295057_, Connection p_294822_, CommonListenerCookie p_301980_) {
      this.server = p_295057_;
      this.connection = p_294822_;
      this.keepAliveTime = Util.getMillis();
      this.latency = p_301980_.latency();
      this.transferred = p_301980_.transferred();
      this.connectionType = p_301980_.connectionType();
   }

   private void close() {
      if (!this.closed) {
         this.closedListenerTime = Util.getMillis();
         this.closed = true;
      }
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_350605_) {
      if (this.isSingleplayerOwner()) {
         LOGGER.info("Stopping singleplayer server as player logged out");
         this.server.halt(false);
      }
   }

   @Override
   public void onPacketError(Packet p_365354_, Exception p_363385_) throws ReportedException {
      ServerCommonPacketListener.super.onPacketError(p_365354_, p_363385_);
      this.server.reportPacketHandlingException(p_363385_, p_365354_.type());
   }

   @Override
   public void handleKeepAlive(ServerboundKeepAlivePacket p_294627_) {
      if (this.keepAlivePending && p_294627_.getId() == this.keepAliveChallenge) {
         int i = (int)(Util.getMillis() - this.keepAliveTime);
         this.latency = (this.latency * 3 + i) / 4;
         this.keepAlivePending = false;
      } else if (!this.isSingleplayerOwner()) {
         this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
      }
   }

   @Override
   public void handlePong(ServerboundPongPacket p_295142_) {
   }

   @Override
   public void handleCustomPayload(ServerboundCustomPayloadPacket p_294276_) {
      if (p_294276_.payload() instanceof MinecraftRegisterPayload minecraftRegisterPayload) {
         NetworkRegistry.onMinecraftRegister(this.getConnection(), minecraftRegisterPayload.newChannels());
      } else if (p_294276_.payload() instanceof MinecraftUnregisterPayload minecraftUnregisterPayload) {
         NetworkRegistry.onMinecraftUnregister(this.getConnection(), minecraftUnregisterPayload.forgottenChannels());
      } else if (p_294276_.payload() instanceof CommonVersionPayload commonVersionPayload) {
         NetworkRegistry.checkCommonVersion(this, commonVersionPayload);
      } else if (p_294276_.payload() instanceof CommonRegisterPayload commonRegisterPayload) {
         NetworkRegistry.onCommonRegister(this, commonRegisterPayload);
      } else if (NetworkRegistry.isModdedPayload(p_294276_.payload())) {
         NetworkRegistry.handleModdedPayload(this, p_294276_);
      }
   }

   @Override
   public void handleCustomClickAction(ServerboundCustomClickActionPacket p_427379_) {
      PacketUtils.ensureRunningOnSameThread(p_427379_, this, this.server.packetProcessor());
      this.server.handleCustomClickAction(p_427379_.id(), p_427379_.payload());
   }

   @Override
   public void handleResourcePackResponse(ServerboundResourcePackPacket p_295695_) {
      PacketUtils.ensureRunningOnSameThread(p_295695_, this, this.server.packetProcessor());
      if (p_295695_.action() == ServerboundResourcePackPacket.Action.DECLINED && this.server.isResourcePackRequired()) {
         LOGGER.info("Disconnecting {} due to resource pack {} rejection", this.playerProfile().name(), p_295695_.id());
         this.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
      }
   }

   @Override
   public void handleCookieResponse(ServerboundCookieResponsePacket p_320918_) {
      this.disconnect(DISCONNECT_UNEXPECTED_QUERY);
   }

   protected void keepConnectionAlive() {
      Profiler.get().push("keepAlive");
      long i = Util.getMillis();
      if (!this.isSingleplayerOwner() && i - this.keepAliveTime >= 15000L) {
         if (this.keepAlivePending) {
            this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
         } else if (this.checkIfClosed(i)) {
            this.keepAlivePending = true;
            this.keepAliveTime = i;
            this.keepAliveChallenge = i;
            this.send(new ClientboundKeepAlivePacket(this.keepAliveChallenge));
         }
      }

      Profiler.get().pop();
   }

   private boolean checkIfClosed(long p_339648_) {
      if (this.closed) {
         if (p_339648_ - this.closedListenerTime >= 15000L) {
            this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
         }

         return false;
      } else {
         return true;
      }
   }

   public void suspendFlushing() {
      this.suspendFlushingOnServerThread = true;
   }

   public void resumeFlushing() {
      this.suspendFlushingOnServerThread = false;
      this.connection.flushChannel();
   }

   @Override
   public void send(Packet<?> p_294278_) {
      this.send(p_294278_, null);
   }

   @Override
   public void send(Packet<?> p_295099_, @Nullable ChannelFutureListener p_428378_) {
      NetworkRegistry.checkPacket(p_295099_, this);
      if (p_295099_.isTerminal()) {
         this.close();
      }

      boolean flag = !this.suspendFlushingOnServerThread || !this.server.isSameThread();

      try {
         this.connection.send(p_295099_, p_428378_, flag);
      } catch (Throwable var7) {
         CrashReport crashreport = CrashReport.forThrowable(var7, "Sending packet");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Packet being sent");
         crashreportcategory.setDetail("Packet class", () -> p_295099_.getClass().getCanonicalName());
         throw new ReportedException(crashreport);
      }
   }

   @Override
   public void disconnect(Component p_294116_) {
      this.disconnect(new DisconnectionDetails(p_294116_));
   }

   public void disconnect(DisconnectionDetails p_350316_) {
      this.connection.send(new ClientboundDisconnectPacket(p_350316_.reason()), PacketSendListener.thenRun(() -> this.connection.disconnect(p_350316_)));
      this.connection.setReadOnly();
      this.server.executeBlocking(this.connection::handleDisconnection);
   }

   protected boolean isSingleplayerOwner() {
      return this.server.isSingleplayerOwner(new NameAndId(this.playerProfile()));
   }

   protected abstract GameProfile playerProfile();

   @VisibleForDebug
   public GameProfile getOwner() {
      return this.playerProfile();
   }

   public int latency() {
      return this.latency;
   }

   @Deprecated
   protected CommonListenerCookie createCookie(ClientInformation p_301973_) {
      return new CommonListenerCookie(this.playerProfile(), this.latency, p_301973_, this.transferred);
   }

   protected CommonListenerCookie createCookie(ClientInformation p_301973_, ConnectionType connectionType) {
      return new CommonListenerCookie(this.playerProfile(), this.latency, p_301973_, this.transferred, connectionType);
   }

   @Override
   public Connection getConnection() {
      return this.connection;
   }

   @Override
   public PacketProcessor getPacketProcessor() {
      return this.server.packetProcessor();
   }

   @Override
   public ConnectionType getConnectionType() {
      return this.connectionType;
   }
}
