package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.multiplayer.CodeOfConductScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundResetChatPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.ModMismatchDisconnectedScreen;
import net.neoforged.neoforge.client.network.registration.ClientNetworkRegistry;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.ModdedNetworkPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkSetupFailedPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientConfigurationPacketListenerImpl extends ClientCommonPacketListenerImpl implements ClientConfigurationPacketListener, TickablePacketListener {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final Component DISCONNECTED_MESSAGE = Component.translatable("multiplayer.disconnect.code_of_conduct");
   private final LevelLoadTracker levelLoadTracker;
   private final GameProfile localGameProfile;
   private FeatureFlagSet enabledFeatures;
   private final RegistryAccess.Frozen receivedRegistries;
   private final RegistryDataCollector registryDataCollector = new RegistryDataCollector();
   @Nullable
   private KnownPacksManager knownPacks;
   @Nullable
   protected ChatComponent.State chatState;
   private boolean seenCodeOfConduct;
   private ConnectionType connectionType = ConnectionType.OTHER;
   private boolean initializedConnection = false;
   private Map<Identifier, Component> failureReasons = new HashMap<>();

   public ClientConfigurationPacketListenerImpl(Minecraft p_295262_, Connection p_296339_, CommonListenerCookie p_294706_) {
      super(p_295262_, p_296339_, p_294706_);
      this.levelLoadTracker = p_294706_.levelLoadTracker();
      this.localGameProfile = p_294706_.localGameProfile();
      this.receivedRegistries = p_294706_.receivedRegistries();
      this.enabledFeatures = p_294706_.enabledFeatures();
      this.chatState = p_294706_.chatState();
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   protected void handleCustomPayload(CustomPacketPayload p_295411_) {
      this.handleUnknownCustomPayload(p_295411_);
   }

   private void handleUnknownCustomPayload(CustomPacketPayload p_296412_) {
      LOGGER.warn("Unknown custom packet payload: {}", p_296412_.type().id());
   }

   @Override
   public void handleRegistryData(ClientboundRegistryDataPacket p_295757_) {
      PacketUtils.ensureRunningOnSameThread(p_295757_, this, this.minecraft.packetProcessor());
      this.registryDataCollector.appendContents(p_295757_.registry(), p_295757_.entries());
   }

   @Override
   public void handleUpdateTags(ClientboundUpdateTagsPacket p_321623_) {
      PacketUtils.ensureRunningOnSameThread(p_321623_, this, this.minecraft.packetProcessor());
      this.registryDataCollector.appendTags(p_321623_.getTags());
   }

   @Override
   public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket p_294410_) {
      this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(p_294410_.features());
      if (this.connectionType.isOther()) {
         this.initializedConnection = true;
         ClientNetworkRegistry.initializeOtherConnection(this);
      }
   }

   @Override
   public void handleSelectKnownPacks(ClientboundSelectKnownPacks p_325965_) {
      PacketUtils.ensureRunningOnSameThread(p_325965_, this, this.minecraft.packetProcessor());
      if (this.knownPacks == null) {
         this.knownPacks = new KnownPacksManager();
      }

      List<KnownPack> list = this.knownPacks.trySelectingPacks(p_325965_.knownPacks());
      this.send(new ServerboundSelectKnownPacks(list));
   }

   @Override
   public void handleResetChat(ClientboundResetChatPacket p_338215_) {
      this.chatState = null;
   }

   private <T> T runWithResources(Function<ResourceProvider, T> p_325978_) {
      if (this.knownPacks == null) {
         return p_325978_.apply(ResourceProvider.EMPTY);
      } else {
         Object object;
         try (CloseableResourceManager closeableresourcemanager = this.knownPacks.createResourceManager()) {
            object = p_325978_.apply(closeableresourcemanager);
         }

         return (T)object;
      }
   }

   @Override
   public void handleCodeOfConduct(ClientboundCodeOfConductPacket p_439724_) {
      PacketUtils.ensureRunningOnSameThread(p_439724_, this, this.minecraft.packetProcessor());
      if (this.seenCodeOfConduct) {
         throw new IllegalStateException("Server sent duplicate Code of Conduct");
      } else {
         this.seenCodeOfConduct = true;
         String s = p_439724_.codeOfConduct();
         if (this.serverData != null && this.serverData.hasAcceptedCodeOfConduct(s)) {
            this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
         } else {
            Screen screen = this.minecraft.screen;
            this.minecraft.setScreen(new CodeOfConductScreen(this.serverData, screen, s, p_450703_ -> {
               if (p_450703_) {
                  this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
                  this.minecraft.setScreen(screen);
               } else {
                  this.createDialogAccess().disconnect(DISCONNECTED_MESSAGE);
               }
            }));
         }
      }
   }

   @Override
   public void handleConfigurationFinished(ClientboundFinishConfigurationPacket p_294585_) {
      PacketUtils.ensureRunningOnSameThread(p_294585_, this, this.minecraft.packetProcessor());
      RegistryAccess.Frozen registryaccess$frozen = this.runWithResources(
         p_359129_ -> this.registryDataCollector.collectGameRegistries(p_359129_, this.receivedRegistries, this.connection.isMemoryConnection())
      );
      this.connection
         .setupInboundProtocol(
            GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registryaccess$frozen, this.connectionType)),
            new ClientPacketListener(
               this.minecraft,
               this.connection,
               new CommonListenerCookie(
                  this.levelLoadTracker,
                  this.localGameProfile,
                  this.telemetryManager,
                  registryaccess$frozen,
                  this.enabledFeatures,
                  this.serverBrand,
                  this.serverData,
                  this.postDisconnectScreen,
                  this.serverCookies,
                  this.chatState,
                  this.customReportDetails,
                  this.serverLinks(),
                  this.seenPlayers,
                  this.seenInsecureChatWarning,
                  this.connectionType
               )
            )
         );
      NeoForge.EVENT_BUS.post(new TagsUpdatedEvent(registryaccess$frozen, true, this.connection.isMemoryConnection()));
      if (!this.initializedConnection && this.connectionType.isOther()) {
         ClientNetworkRegistry.initializeOtherConnection(this);
      }

      NetworkRegistry.onConfigurationFinished(this);
      this.connection.send(ServerboundFinishConfigurationPacket.INSTANCE);
      this.connection
         .setupOutboundProtocol(
            GameProtocols.SERVERBOUND_TEMPLATE
               .bind(RegistryFriendlyByteBuf.decorator(registryaccess$frozen, this.connectionType), new GameProtocols.Context() {
                  @Override
                  public boolean hasInfiniteMaterials() {
                     return true;
                  }
               })
         );
   }

   @Override
   public void tick() {
      this.sendDeferredPackets();
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_351005_) {
      super.onDisconnect(p_351005_);
      this.minecraft.clearDownloadedResourcePacks();
   }

   @Override
   public void handleCustomPayload(ClientboundCustomPayloadPacket packet) {
      if (packet.payload() instanceof ModdedNetworkQueryPayload) {
         this.connectionType = ConnectionType.NEOFORGE;
         ClientNetworkRegistry.onNetworkQuery(this);
      } else if (packet.payload() instanceof ModdedNetworkPayload moddedNetworkPayload) {
         this.initializedConnection = true;
         ClientNetworkRegistry.initializeNeoForgeConnection(this, moddedNetworkPayload.setup());
      } else if (packet.payload() instanceof ModdedNetworkSetupFailedPayload setupFailedPayload) {
         this.failureReasons = setupFailedPayload.failureReasons();
      } else {
         if (this.connectionType.isOther() && packet.payload() instanceof BrandPayload) {
            this.initializedConnection = true;
            ClientNetworkRegistry.initializeOtherConnection(this);
         }

         super.handleCustomPayload(packet);
      }
   }

   @Override
   protected Screen createDisconnectScreen(DisconnectionDetails p_350769_) {
      Screen superScreen = super.createDisconnectScreen(p_350769_);
      return (Screen)(this.failureReasons.isEmpty()
         ? superScreen
         : new ModMismatchDisconnectedScreen(superScreen, Component.translatable("disconnect.lost"), this.failureReasons));
   }

   @Override
   public ConnectionType getConnectionType() {
      return this.connectionType;
   }

   @Override
   protected DialogConnectionAccess createDialogAccess() {
      return new ClientCommonPacketListenerImpl.CommonDialogAccess() {
         @Override
         public void runCommand(String p_427300_, @Nullable Screen p_427323_) {
            ClientConfigurationPacketListenerImpl.LOGGER.warn("Commands are not supported in configuration phase, trying to run '{}'", p_427300_);
         }
      };
   }
}
