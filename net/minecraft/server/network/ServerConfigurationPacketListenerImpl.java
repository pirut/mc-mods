package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.Nullable;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.JoinWorldTask;
import net.minecraft.server.network.config.ServerResourcePackConfigurationTask;
import net.minecraft.server.network.config.SynchronizeRegistriesTask;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.network.ConfigurationInitialization;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;

public class ServerConfigurationPacketListenerImpl extends ServerCommonPacketListenerImpl implements ServerConfigurationPacketListener, TickablePacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component DISCONNECT_REASON_INVALID_DATA = Component.translatable("multiplayer.disconnect.invalid_player_data");
   private final GameProfile gameProfile;
   private final Queue<ConfigurationTask> configurationTasks = new ConcurrentLinkedQueue<>();
   @Nullable
   private ConfigurationTask currentTask;
   private ClientInformation clientInformation;
   @Nullable
   private SynchronizeRegistriesTask synchronizeRegistriesTask;

   public ServerConfigurationPacketListenerImpl(MinecraftServer p_294645_, Connection p_295787_, CommonListenerCookie p_302003_) {
      super(p_294645_, p_295787_, p_302003_);
      this.gameProfile = p_302003_.gameProfile();
      this.clientInformation = p_302003_.clientInformation();
   }

   @Override
   protected GameProfile playerProfile() {
      return this.gameProfile;
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_350569_) {
      LOGGER.info("{} lost connection: {}", this.gameProfile, p_350569_.reason().getString());
      super.onDisconnect(p_350569_);
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   public void startConfiguration() {
      this.send(new MinecraftUnregisterPayload(NetworkRegistry.getInitialServerUnregisterChannels()));
      this.send(new MinecraftRegisterPayload(NetworkRegistry.getInitialListeningChannels(this.flow())));
      this.send(new ModdedNetworkQueryPayload(Map.of()));
      this.send(new ClientboundPingPacket(0));
   }

   private void runConfiguration() {
      this.send(new ClientboundCustomPayloadPacket(new BrandPayload(this.server.getServerModName())));
      ServerLinks serverlinks = this.server.serverLinks();
      if (!serverlinks.isEmpty()) {
         this.send(new ClientboundServerLinksPacket(serverlinks.untrust()));
      }

      LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = this.server.registries();
      List<KnownPack> list = this.server.getResourceManager().listPacks().flatMap(p_325637_ -> p_325637_.location().knownPackInfo().stream()).toList();
      this.send(new ClientboundUpdateEnabledFeaturesPacket(FeatureFlags.REGISTRY.toNames(this.server.getWorldData().enabledFeatures())));
      ConfigurationInitialization.configureEarlyTasks(this, this.configurationTasks::add);
      this.synchronizeRegistriesTask = new SynchronizeRegistriesTask(list, layeredregistryaccess);
      this.configurationTasks.add(this.synchronizeRegistriesTask);
      this.addOptionalTasks();
      this.configurationTasks.add(new JoinWorldTask());
      this.startNextTask();
   }

   public void returnToWorld() {
      this.configurationTasks.add(new JoinWorldTask());
      this.startNextTask();
   }

   private void addOptionalTasks() {
      this.server.getServerResourcePack().ifPresent(p_296496_ -> this.configurationTasks.add(new ServerResourcePackConfigurationTask(p_296496_)));
      this.configurationTasks
         .addAll(((RegisterConfigurationTasksEvent)ModLoader.postEventWithReturn(new RegisterConfigurationTasksEvent(this))).getConfigurationTasks());
   }

   @Override
   public void handleCustomPayload(ServerboundCustomPayloadPacket p_294276_) {
      if (p_294276_.payload() instanceof ModdedNetworkQueryPayload moddedEnvironmentPayload) {
         this.connectionType = ConnectionType.NEOFORGE;
         NetworkRegistry.initializeNeoForgeConnection(this, moddedEnvironmentPayload.queries());
      } else {
         super.handleCustomPayload(p_294276_);
      }
   }

   @Override
   public void handlePong(ServerboundPongPacket p_295142_) {
      super.handlePong(p_295142_);
      if (p_295142_.getId() == 0) {
         if (!this.connectionType.isNeoForge() && !NetworkRegistry.initializeOtherConnection(this)) {
            return;
         }

         this.runConfiguration();
      }
   }

   @Override
   public void handleClientInformation(ServerboundClientInformationPacket p_302032_) {
      this.clientInformation = p_302032_.information();
   }

   @Override
   public void handleResourcePackResponse(ServerboundResourcePackPacket p_294250_) {
      super.handleResourcePackResponse(p_294250_);
      if (p_294250_.action().isTerminal()) {
         this.finishCurrentTask(ServerResourcePackConfigurationTask.TYPE);
      }
   }

   @Override
   public void handleSelectKnownPacks(ServerboundSelectKnownPacks p_326180_) {
      PacketUtils.ensureRunningOnSameThread(p_326180_, this, this.server);
      if (this.synchronizeRegistriesTask == null) {
         throw new IllegalStateException("Unexpected response from client: received pack selection, but no negotiation ongoing");
      } else {
         this.synchronizeRegistriesTask.handleResponse(p_326180_.knownPacks(), this::send);
         this.finishCurrentTask(SynchronizeRegistriesTask.TYPE);
      }
   }

   @Override
   public void handleConfigurationFinished(ServerboundFinishConfigurationPacket p_294283_) {
      PacketUtils.ensureRunningOnSameThread(p_294283_, this, this.server);
      this.finishCurrentTask(JoinWorldTask.TYPE);
      this.connection
         .setupOutboundProtocol(GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(this.server.registryAccess(), this.connectionType)));
      if (this.connectionType == ConnectionType.OTHER) {
         NetworkRegistry.initializeNeoForgeConnection(this, Map.of());
      }

      NetworkRegistry.onConfigurationFinished(this);

      try {
         PlayerList playerlist = this.server.getPlayerList();
         if (playerlist.getPlayer(this.gameProfile.getId()) != null) {
            this.disconnect(PlayerList.DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
            return;
         }

         Component component = playerlist.canPlayerLogin(this.connection.getRemoteAddress(), this.gameProfile);
         if (component != null) {
            this.disconnect(component);
            return;
         }

         ServerPlayer serverplayer = playerlist.getPlayerForLogin(this.gameProfile, this.clientInformation);
         playerlist.placeNewPlayer(this.connection, serverplayer, this.createCookie(this.clientInformation, this.connectionType));
      } catch (Exception var5) {
         LOGGER.error("Couldn't place player in world", var5);
         this.connection.send(new ClientboundDisconnectPacket(DISCONNECT_REASON_INVALID_DATA));
         this.connection.disconnect(DISCONNECT_REASON_INVALID_DATA);
      }
   }

   @Override
   public void tick() {
      this.keepConnectionAlive();
   }

   private void startNextTask() {
      if (this.currentTask != null) {
         throw new IllegalStateException("Task " + this.currentTask.type().id() + " has not finished yet");
      } else {
         if (this.isAcceptingMessages()) {
            ConfigurationTask configurationtask = this.configurationTasks.poll();
            if (configurationtask != null) {
               this.currentTask = configurationtask;
               configurationtask.start(this::send);
            }
         }
      }
   }

   @Override
   public void finishCurrentTask(ConfigurationTask.Type p_294853_) {
      ConfigurationTask.Type configurationtask$type = this.currentTask != null ? this.currentTask.type() : null;
      if (!p_294853_.equals(configurationtask$type)) {
         throw new IllegalStateException("Unexpected request for task finish, current task: " + configurationtask$type + ", requested: " + p_294853_);
      } else {
         this.currentTask = null;
         this.startNextTask();
      }
   }
}
