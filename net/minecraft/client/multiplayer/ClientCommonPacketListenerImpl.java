package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.gui.screens.dialog.DialogScreens;
import net.minecraft.client.gui.screens.dialog.WaitingForResponseScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.registration.ClientNetworkRegistry;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.CommonRegisterPayload;
import net.neoforged.neoforge.network.payload.CommonVersionPayload;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class ClientCommonPacketListenerImpl implements ClientCommonPacketListener {
   private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Minecraft minecraft;
   protected final Connection connection;
   @Nullable
   protected final ServerData serverData;
   @Nullable
   protected String serverBrand;
   protected final WorldSessionTelemetryManager telemetryManager;
   @Nullable
   protected final Screen postDisconnectScreen;
   protected boolean isTransferring;
   private final List<ClientCommonPacketListenerImpl.DeferredPacket> deferredPackets = new ArrayList<>();
   protected final Map<Identifier, byte[]> serverCookies;
   protected Map<String, String> customReportDetails;
   private ServerLinks serverLinks;
   protected final Map<UUID, PlayerInfo> seenPlayers;
   protected boolean seenInsecureChatWarning;
   protected ConnectionType connectionType = ConnectionType.OTHER;

   protected ClientCommonPacketListenerImpl(Minecraft p_295454_, Connection p_294773_, CommonListenerCookie p_294647_) {
      this.minecraft = p_295454_;
      this.connection = p_294773_;
      this.serverData = p_294647_.serverData();
      this.serverBrand = p_294647_.serverBrand();
      this.telemetryManager = p_294647_.telemetryManager();
      this.postDisconnectScreen = p_294647_.postDisconnectScreen();
      this.serverCookies = p_294647_.serverCookies();
      this.customReportDetails = p_294647_.customReportDetails();
      this.serverLinks = p_294647_.serverLinks();
      this.seenPlayers = new HashMap<>(p_294647_.seenPlayers());
      this.seenInsecureChatWarning = p_294647_.seenInsecureChatWarning();
      this.connectionType = p_294647_.connectionType();
   }

   public ServerLinks serverLinks() {
      return this.serverLinks;
   }

   @Override
   public void onPacketError(Packet p_341624_, Exception p_341639_) {
      LOGGER.error("Failed to handle packet {}, disconnecting", p_341624_, p_341639_);
      Optional<Path> optional = this.storeDisconnectionReport(p_341624_, p_341639_);
      Optional<URI> optional1 = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
      this.connection.disconnect(new DisconnectionDetails(Component.translatable("disconnect.packetError"), optional, optional1));
   }

   @Override
   public DisconnectionDetails createDisconnectionInfo(Component p_350683_, Throwable p_350813_) {
      Optional<Path> optional = this.storeDisconnectionReport(null, p_350813_);
      Optional<URI> optional1 = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
      return new DisconnectionDetails(p_350683_, optional, optional1);
   }

   private Optional<Path> storeDisconnectionReport(@Nullable Packet p_350430_, Throwable p_350533_) {
      CrashReport crashreport = CrashReport.forThrowable(p_350533_, "Packet handling error");
      PacketUtils.fillCrashReport(crashreport, this, p_350430_);
      Path path = this.minecraft.gameDirectory.toPath().resolve("debug");
      Path path1 = path.resolve("disconnect-" + Util.getFilenameFormattedDateTime() + "-client.txt");
      Optional<ServerLinks.Entry> optional = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT);
      List<String> list = optional.<List<String>>map(p_351668_ -> List.of("Server bug reporting link: " + p_351668_.link())).orElse(List.of());
      return crashreport.saveToFile(path1, ReportType.NETWORK_PROTOCOL_ERROR, list) ? Optional.of(path1) : Optional.empty();
   }

   @Override
   public boolean shouldHandleMessage(Packet<?> p_341905_) {
      return ClientCommonPacketListener.super.shouldHandleMessage(p_341905_)
         ? true
         : this.isTransferring && (p_341905_ instanceof ClientboundStoreCookiePacket || p_341905_ instanceof ClientboundTransferPacket);
   }

   @Override
   public void handleKeepAlive(ClientboundKeepAlivePacket p_295361_) {
      this.sendWhen(new ServerboundKeepAlivePacket(p_295361_.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
   }

   @Override
   public void handlePing(ClientboundPingPacket p_295594_) {
      PacketUtils.ensureRunningOnSameThread(p_295594_, this, this.minecraft.packetProcessor());
      this.send(new ServerboundPongPacket(p_295594_.getId()));
   }

   @Override
   public void handleCustomPayload(ClientboundCustomPayloadPacket p_295727_) {
      if (p_295727_.payload() instanceof MinecraftRegisterPayload minecraftRegisterPayload) {
         NetworkRegistry.onMinecraftRegister(this.getConnection(), minecraftRegisterPayload.newChannels());
      } else if (p_295727_.payload() instanceof MinecraftUnregisterPayload minecraftUnregisterPayload) {
         NetworkRegistry.onMinecraftUnregister(this.getConnection(), minecraftUnregisterPayload.forgottenChannels());
      } else if (p_295727_.payload() instanceof CommonVersionPayload commonVersionPayload) {
         NetworkRegistry.checkCommonVersion(this, commonVersionPayload);
      } else if (p_295727_.payload() instanceof CommonRegisterPayload commonRegisterPayload) {
         NetworkRegistry.onCommonRegister(this, commonRegisterPayload);
      } else if (NetworkRegistry.isModdedPayload(p_295727_.payload())) {
         ClientNetworkRegistry.handleModdedPayload(this, p_295727_);
      } else {
         CustomPacketPayload custompacketpayload = p_295727_.payload();
         if (!(custompacketpayload instanceof DiscardedPayload)) {
            PacketUtils.ensureRunningOnSameThread(p_295727_, this, this.minecraft.packetProcessor());
            if (custompacketpayload instanceof BrandPayload brandpayload) {
               this.serverBrand = brandpayload.brand();
               this.telemetryManager.onServerBrandReceived(brandpayload.brand());
            } else {
               this.handleCustomPayload(custompacketpayload);
            }
         }
      }
   }

   protected abstract void handleCustomPayload(CustomPacketPayload var1);

   @Override
   public void handleResourcePackPush(ClientboundResourcePackPushPacket p_314606_) {
      PacketUtils.ensureRunningOnSameThread(p_314606_, this, this.minecraft.packetProcessor());
      UUID uuid = p_314606_.id();
      URL url = parseResourcePackUrl(p_314606_.url());
      if (url == null) {
         this.connection.send(new ServerboundResourcePackPacket(uuid, ServerboundResourcePackPacket.Action.INVALID_URL));
      } else {
         String s = p_314606_.hash();
         boolean flag = p_314606_.required();
         ServerData.ServerPackStatus serverdata$serverpackstatus = this.serverData != null
            ? this.serverData.getResourcePackStatus()
            : ServerData.ServerPackStatus.PROMPT;
         if (serverdata$serverpackstatus == ServerData.ServerPackStatus.PROMPT || flag && serverdata$serverpackstatus == ServerData.ServerPackStatus.DISABLED) {
            this.minecraft.setScreen(this.addOrUpdatePackPrompt(uuid, url, s, flag, p_314606_.prompt().orElse(null)));
         } else {
            this.minecraft.getDownloadedPackSource().pushPack(uuid, url, s);
         }
      }
   }

   @Override
   public void handleResourcePackPop(ClientboundResourcePackPopPacket p_314537_) {
      PacketUtils.ensureRunningOnSameThread(p_314537_, this, this.minecraft.packetProcessor());
      p_314537_.id()
         .ifPresentOrElse(p_314401_ -> this.minecraft.getDownloadedPackSource().popPack(p_314401_), () -> this.minecraft.getDownloadedPackSource().popAll());
   }

   static Component preparePackPrompt(Component p_296200_, @Nullable Component p_295584_) {
      return (Component)(p_295584_ == null ? p_296200_ : Component.translatable("multiplayer.texturePrompt.serverPrompt", p_296200_, p_295584_));
   }

   @Nullable
   private static URL parseResourcePackUrl(String p_295495_) {
      try {
         URL url = new URL(p_295495_);
         String s = url.getProtocol();
         return !"http".equals(s) && !"https".equals(s) ? null : url;
      } catch (MalformedURLException var3) {
         return null;
      }
   }

   @Override
   public void handleRequestCookie(ClientboundCookieRequestPacket p_320212_) {
      PacketUtils.ensureRunningOnSameThread(p_320212_, this, this.minecraft.packetProcessor());
      this.connection.send(new ServerboundCookieResponsePacket(p_320212_.key(), this.serverCookies.get(p_320212_.key())));
   }

   @Override
   public void handleStoreCookie(ClientboundStoreCookiePacket p_320008_) {
      PacketUtils.ensureRunningOnSameThread(p_320008_, this, this.minecraft.packetProcessor());
      this.serverCookies.put(p_320008_.key(), p_320008_.payload());
   }

   @Override
   public void handleCustomReportDetails(ClientboundCustomReportDetailsPacket p_350638_) {
      PacketUtils.ensureRunningOnSameThread(p_350638_, this, this.minecraft.packetProcessor());
      this.customReportDetails = p_350638_.details();
   }

   @Override
   public void handleServerLinks(ClientboundServerLinksPacket p_350990_) {
      PacketUtils.ensureRunningOnSameThread(p_350990_, this, this.minecraft.packetProcessor());
      List<ServerLinks.UntrustedEntry> list = p_350990_.links();
      Builder<ServerLinks.Entry> builder = ImmutableList.builderWithExpectedSize(list.size());

      for (ServerLinks.UntrustedEntry serverlinks$untrustedentry : list) {
         try {
            URI uri = Util.parseAndValidateUntrustedUri(serverlinks$untrustedentry.link());
            builder.add(new ServerLinks.Entry(serverlinks$untrustedentry.type(), uri));
         } catch (Exception var7) {
            LOGGER.warn("Received invalid link for type {}:{}", new Object[]{serverlinks$untrustedentry.type(), serverlinks$untrustedentry.link(), var7});
         }
      }

      this.serverLinks = new ServerLinks(builder.build());
   }

   @Override
   public void handleShowDialog(ClientboundShowDialogPacket p_425904_) {
      PacketUtils.ensureRunningOnSameThread(p_425904_, this, this.minecraft.packetProcessor());
      this.showDialog(p_425904_.dialog(), this.minecraft.screen);
   }

   protected abstract DialogConnectionAccess createDialogAccess();

   public void showDialog(Holder<Dialog> p_426010_, @Nullable Screen p_428285_) {
      this.showDialog(p_426010_, this.createDialogAccess(), p_428285_);
   }

   protected void showDialog(Holder<Dialog> p_428370_, DialogConnectionAccess p_428565_, @Nullable Screen p_428432_) {
      if (p_428432_ instanceof DialogScreen.WarningScreen dialogscreen$warningscreen) {
         Screen screen2 = dialogscreen$warningscreen.returnScreen();
         Screen screen3 = screen2 instanceof DialogScreen<?> dialogscreen1 ? dialogscreen1.previousScreen() : screen2;
         DialogScreen<?> dialogscreen2 = DialogScreens.createFromData(p_428370_.value(), screen3, p_428565_);
         if (dialogscreen2 != null) {
            dialogscreen$warningscreen.updateReturnScreen(dialogscreen2);
         } else {
            LOGGER.warn("Failed to show dialog for data {}", p_428370_);
         }
      } else {
         Screen screen;
         if (p_428432_ instanceof DialogScreen<?> dialogscreen) {
            screen = dialogscreen.previousScreen();
         } else if (p_428432_ instanceof WaitingForResponseScreen waitingforresponsescreen) {
            screen = waitingforresponsescreen.previousScreen();
         } else {
            screen = p_428432_;
         }

         Screen screen1 = DialogScreens.createFromData(p_428370_.value(), screen, p_428565_);
         if (screen1 != null) {
            this.minecraft.setScreen(screen1);
         } else {
            LOGGER.warn("Failed to show dialog for data {}", p_428370_);
         }
      }
   }

   @Override
   public void handleClearDialog(ClientboundClearDialogPacket p_426184_) {
      PacketUtils.ensureRunningOnSameThread(p_426184_, this, this.minecraft.packetProcessor());
      this.clearDialog();
   }

   public void clearDialog() {
      if (this.minecraft.screen instanceof DialogScreen.WarningScreen dialogscreen$warningscreen) {
         if (dialogscreen$warningscreen.returnScreen() instanceof DialogScreen<?> dialogscreen1) {
            dialogscreen$warningscreen.updateReturnScreen(dialogscreen1.previousScreen());
         }
      } else if (this.minecraft.screen instanceof DialogScreen<?> dialogscreen) {
         this.minecraft.setScreen(dialogscreen.previousScreen());
      }
   }

   @Override
   public void handleTransfer(ClientboundTransferPacket p_320739_) {
      this.isTransferring = true;
      PacketUtils.ensureRunningOnSameThread(p_320739_, this, this.minecraft.packetProcessor());
      if (this.serverData == null) {
         throw new IllegalStateException("Cannot transfer to server from singleplayer");
      } else {
         this.connection.disconnect(Component.translatable("disconnect.transfer"));
         this.connection.setReadOnly();
         this.connection.handleDisconnection();
         ServerAddress serveraddress = new ServerAddress(p_320739_.host(), p_320739_.port());
         ConnectScreen.startConnecting(
            Objects.requireNonNullElseGet(this.postDisconnectScreen, TitleScreen::new),
            this.minecraft,
            serveraddress,
            this.serverData,
            false,
            new TransferState(this.serverCookies, this.seenPlayers, this.seenInsecureChatWarning)
         );
      }
   }

   @Override
   public void handleDisconnect(ClientboundDisconnectPacket p_296159_) {
      this.connection.disconnect(p_296159_.reason());
   }

   protected void sendDeferredPackets() {
      Iterator<ClientCommonPacketListenerImpl.DeferredPacket> iterator = this.deferredPackets.iterator();

      while (iterator.hasNext()) {
         ClientCommonPacketListenerImpl.DeferredPacket clientcommonpacketlistenerimpl$deferredpacket = iterator.next();
         if (clientcommonpacketlistenerimpl$deferredpacket.sendCondition().getAsBoolean()) {
            this.send(clientcommonpacketlistenerimpl$deferredpacket.packet);
            iterator.remove();
         } else if (clientcommonpacketlistenerimpl$deferredpacket.expirationTime() <= Util.getMillis()) {
            iterator.remove();
         }
      }
   }

   @Override
   public void send(Packet<?> p_295097_) {
      NetworkRegistry.checkPacket(p_295097_, this);
      this.connection.send(p_295097_);
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_350760_) {
      this.telemetryManager.onDisconnect();
      this.minecraft.disconnect(this.createDisconnectScreen(p_350760_), this.isTransferring);
      LOGGER.warn("Client disconnected with reason: {}", p_350760_.reason().getString());
   }

   @Override
   public void fillListenerSpecificCrashDetails(CrashReport p_350364_, CrashReportCategory p_315011_) {
      p_315011_.setDetail("Is Local", () -> String.valueOf(this.connection.isMemoryConnection()));
      p_315011_.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<none>");
      p_315011_.setDetail("Server brand", () -> this.serverBrand);
      if (!this.customReportDetails.isEmpty()) {
         CrashReportCategory crashreportcategory = p_350364_.addCategory("Custom Server Details");
         this.customReportDetails.forEach(crashreportcategory::setDetail);
      }
   }

   protected Screen createDisconnectScreen(DisconnectionDetails p_350769_) {
      Screen screen = Objects.requireNonNullElseGet(
         this.postDisconnectScreen, () -> (Screen)(this.serverData != null ? new JoinMultiplayerScreen(new TitleScreen()) : new TitleScreen())
      );
      return this.serverData != null && this.serverData.isRealm()
         ? new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, p_350769_, CommonComponents.GUI_BACK)
         : new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, p_350769_);
   }

   @Nullable
   public String serverBrand() {
      return this.serverBrand;
   }

   private void sendWhen(Packet<? extends ServerboundPacketListener> p_296259_, BooleanSupplier p_296086_, Duration p_294812_) {
      if (p_296086_.getAsBoolean()) {
         this.send(p_296259_);
      } else {
         this.deferredPackets.add(new ClientCommonPacketListenerImpl.DeferredPacket(p_296259_, p_296086_, Util.getMillis() + p_294812_.toMillis()));
      }
   }

   private Screen addOrUpdatePackPrompt(UUID p_314948_, URL p_315012_, String p_314981_, boolean p_315013_, @Nullable Component p_314960_) {
      Screen screen = this.minecraft.screen;
      return screen instanceof ClientCommonPacketListenerImpl.PackConfirmScreen clientcommonpacketlistenerimpl$packconfirmscreen
         ? clientcommonpacketlistenerimpl$packconfirmscreen.update(this.minecraft, p_314948_, p_315012_, p_314981_, p_315013_, p_314960_)
         : new ClientCommonPacketListenerImpl.PackConfirmScreen(
            this.minecraft,
            screen,
            List.of(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(p_314948_, p_315012_, p_314981_)),
            p_315013_,
            p_314960_
         );
   }

   @Override
   public Connection getConnection() {
      return this.connection;
   }

   @Override
   public PacketProcessor getPacketProcessor() {
      return this.minecraft.packetProcessor();
   }

   @OnlyIn(Dist.CLIENT)
   protected abstract class CommonDialogAccess implements DialogConnectionAccess {
      @Override
      public void disconnect(Component p_451055_) {
         ClientCommonPacketListenerImpl.this.connection.disconnect(p_451055_);
         ClientCommonPacketListenerImpl.this.connection.handleDisconnection();
      }

      @Override
      public void openDialog(Holder<Dialog> p_437297_, @Nullable Screen p_437186_) {
         ClientCommonPacketListenerImpl.this.showDialog(p_437297_, this, p_437186_);
      }

      @Override
      public void sendCustomAction(Identifier p_467437_, Optional<Tag> p_437224_) {
         ClientCommonPacketListenerImpl.this.send(new ServerboundCustomClickActionPacket(p_467437_, p_437224_));
      }

      @Override
      public ServerLinks serverLinks() {
         return ClientCommonPacketListenerImpl.this.serverLinks();
      }
   }

   @OnlyIn(Dist.CLIENT)
   record DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
   }

   @OnlyIn(Dist.CLIENT)
   class PackConfirmScreen extends ConfirmScreen {
      private final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> requests;
      @Nullable
      private final Screen parentScreen;

      PackConfirmScreen(
         Minecraft p_314973_,
         @Nullable Screen p_315016_,
         List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> p_314994_,
         boolean p_314923_,
         @Nullable Component p_314940_
      ) {
         super(
            p_315005_ -> {
               p_314973_.setScreen(p_315016_);
               DownloadedPackSource downloadedpacksource = p_314973_.getDownloadedPackSource();
               if (p_315005_) {
                  if (ClientCommonPacketListenerImpl.this.serverData != null) {
                     ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                  }

                  downloadedpacksource.allowServerPacks();
               } else {
                  downloadedpacksource.rejectServerPacks();
                  if (p_314923_) {
                     ClientCommonPacketListenerImpl.this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                  } else if (ClientCommonPacketListenerImpl.this.serverData != null) {
                     ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                  }
               }

               for (ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest : p_314994_) {
                  downloadedpacksource.pushPack(
                     clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.id,
                     clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.url,
                     clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.hash
                  );
               }

               if (ClientCommonPacketListenerImpl.this.serverData != null) {
                  ServerList.saveSingleServer(ClientCommonPacketListenerImpl.this.serverData);
               }
            },
            p_314923_ ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"),
            ClientCommonPacketListenerImpl.preparePackPrompt(
               p_314923_
                  ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                  : Component.translatable("multiplayer.texturePrompt.line2"),
               p_314940_
            ),
            p_314923_ ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES,
            p_314923_ ? CommonComponents.GUI_DISCONNECT : CommonComponents.GUI_NO
         );
         this.requests = p_314994_;
         this.parentScreen = p_315016_;
      }

      public ClientCommonPacketListenerImpl.PackConfirmScreen update(
         Minecraft p_314946_, UUID p_314980_, URL p_314930_, String p_315003_, boolean p_314916_, @Nullable Component p_314991_
      ) {
         List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> list = ImmutableList.builderWithExpectedSize(this.requests.size() + 1)
            .addAll(this.requests)
            .add(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(p_314980_, p_314930_, p_315003_))
            .build();
         return ClientCommonPacketListenerImpl.this.new PackConfirmScreen(p_314946_, this.parentScreen, list, p_314916_, p_314991_);
      }

      @OnlyIn(Dist.CLIENT)
      record PendingRequest(UUID id, URL url, String hash) {
      }
   }
}
