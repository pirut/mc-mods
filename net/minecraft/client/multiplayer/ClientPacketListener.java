package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DemoIntroScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.NautilusInventoryScreen;
import net.minecraft.client.gui.screens.inventory.TestInstanceBlockEditScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.BeeSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMountScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Crypt;
import net.minecraft.util.HashOps;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.NautilusInventoryMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientPacketListener extends ClientCommonPacketListenerImpl implements ClientGamePacketListener, TickablePacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable("multiplayer.unsecureserver.toast.title");
   private static final Component UNSERURE_SERVER_TOAST = Component.translatable("multiplayer.unsecureserver.toast");
   private static final Component INVALID_PACKET = Component.translatable("multiplayer.disconnect.invalid_packet");
   private static final Component RECONFIGURE_SCREEN_MESSAGE = Component.translatable("connect.reconfiguring");
   private static final Component BAD_CHAT_INDEX = Component.translatable("multiplayer.disconnect.bad_chat_index");
   private static final Component COMMAND_SEND_CONFIRM_TITLE = Component.translatable("multiplayer.confirm_command.title");
   private static final Component BUTTON_RUN_COMMAND = Component.translatable("multiplayer.confirm_command.run_command");
   private static final Component BUTTON_SUGGEST_COMMAND = Component.translatable("multiplayer.confirm_command.suggest_command");
   private static final int PENDING_OFFSET_THRESHOLD = 64;
   public static final int TELEPORT_INTERPOLATION_THRESHOLD = 64;
   private static final Permission RESTRICTED_COMMAND = Permission.Atom.create("client/commands/restricted");
   static final PermissionCheck RESTRICTED_COMMAND_CHECK = new PermissionCheck.Require(RESTRICTED_COMMAND);
   private static final PermissionSet ALLOW_RESTRICTED_COMMANDS = permission -> permission.equals(RESTRICTED_COMMAND);
   private static final ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> COMMAND_NODE_BUILDER = new ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider>() {
      @Override
      public ArgumentBuilder<ClientSuggestionProvider, ?> createLiteral(String id) {
         return LiteralArgumentBuilder.literal(id);
      }

      @Override
      public ArgumentBuilder<ClientSuggestionProvider, ?> createArgument(String id, ArgumentType<?> type, @Nullable Identifier suggestionId) {
         RequiredArgumentBuilder<ClientSuggestionProvider, ?> requiredArgumentBuilder = RequiredArgumentBuilder.argument(id, type);
         if (suggestionId != null) {
            requiredArgumentBuilder.suggests(SuggestionProviders.getProvider(suggestionId));
         }

         return requiredArgumentBuilder;
      }

      @Override
      public ArgumentBuilder<ClientSuggestionProvider, ?> configure(
         ArgumentBuilder<ClientSuggestionProvider, ?> argumentBuilder, boolean executable, boolean restricted
      ) {
         if (executable) {
            argumentBuilder.executes(commandContext -> 0);
         }

         if (restricted) {
            argumentBuilder.requires(Commands.hasPermission(ClientPacketListener.RESTRICTED_COMMAND_CHECK));
         }

         return argumentBuilder;
      }
   };
   private final GameProfile localGameProfile;
   private ClientLevel level;
   private ClientLevel.ClientLevelData levelData;
   private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
   private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet();
   private final ClientAdvancements advancements;
   private final ClientSuggestionProvider suggestionsProvider;
   private final ClientSuggestionProvider restrictedSuggestionsProvider;
   private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
   private int serverChunkRadius = 3;
   private int serverSimulationDistance = 3;
   private final RandomSource random = RandomSource.createThreadSafe();
   private CommandDispatcher<ClientSuggestionProvider> commands = new CommandDispatcher();
   private ClientRecipeContainer recipes = new ClientRecipeContainer(Map.of(), SelectableRecipe.SingleInputSet.empty());
   private final UUID id = UUID.randomUUID();
   private Set<ResourceKey<Level>> levels;
   private final RegistryAccess.Frozen registryAccess;
   private final FeatureFlagSet enabledFeatures;
   private final PotionBrewing potionBrewing;
   private FuelValues fuelValues;
   private final HashedPatchMap.HashGenerator decoratedHashOpsGenerator;
   private OptionalInt removedPlayerVehicleId = OptionalInt.empty();
   @Nullable
   private LocalChatSession chatSession;
   private SignedMessageChain.Encoder signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
   private int nextChatIndex;
   private LastSeenMessagesTracker lastSeenMessages = new LastSeenMessagesTracker(20);
   private MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
   @Nullable
   private CompletableFuture<Optional<ProfileKeyPair>> keyPairFuture;
   @Nullable
   private ClientInformation remoteClientInformation;
   private final ChunkBatchSizeCalculator chunkBatchSizeCalculator = new ChunkBatchSizeCalculator();
   private final PingDebugMonitor pingDebugMonitor;
   private final ClientDebugSubscriber debugSubscriber;
   @Nullable
   private LevelLoadTracker levelLoadTracker;
   private boolean serverEnforcesSecureChat;
   private volatile boolean closed;
   private final Scoreboard scoreboard = new Scoreboard();
   private final ClientWaypointManager waypointManager = new ClientWaypointManager();
   private final SessionSearchTrees searchTrees = new SessionSearchTrees();
   private final List<WeakReference<CacheSlot<?, ?>>> cacheSlots = new ArrayList<>();
   private boolean clientLoaded;

   public ClientPacketListener(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
      super(minecraft, connection, commonListenerCookie);
      this.localGameProfile = commonListenerCookie.localGameProfile();
      this.registryAccess = commonListenerCookie.receivedRegistries();
      RegistryOps<HashCode> registryOps = this.registryAccess.createSerializationContext(HashOps.CRC32C_INSTANCE);
      this.decoratedHashOpsGenerator = typedDataComponent -> ((HashCode)typedDataComponent.encodeValue(registryOps)
            .getOrThrow(string -> new IllegalArgumentException("Failed to hash " + typedDataComponent + ": " + string)))
         .asInt();
      this.enabledFeatures = commonListenerCookie.enabledFeatures();
      this.advancements = new ClientAdvancements(minecraft, this.telemetryManager);
      PermissionSet permissionSet = permission -> {
         LocalPlayer localPlayer = minecraft.player;
         return localPlayer != null && localPlayer.permissions().hasPermission(permission);
      };
      this.suggestionsProvider = new ClientSuggestionProvider(this, minecraft, permissionSet.union(ALLOW_RESTRICTED_COMMANDS));
      this.restrictedSuggestionsProvider = new ClientSuggestionProvider(this, minecraft, PermissionSet.NO_PERMISSIONS);
      this.pingDebugMonitor = new PingDebugMonitor(this, minecraft.getDebugOverlay().getPingLogger());
      this.debugSubscriber = new ClientDebugSubscriber(this, minecraft.getDebugOverlay());
      if (commonListenerCookie.chatState() != null) {
         minecraft.gui.getChat().restoreState(commonListenerCookie.chatState());
      }

      this.potionBrewing = PotionBrewing.bootstrap(this.enabledFeatures);
      this.fuelValues = FuelValues.vanillaBurnTimes(commonListenerCookie.receivedRegistries(), this.enabledFeatures);
      this.levelLoadTracker = commonListenerCookie.levelLoadTracker();
   }

   public ClientSuggestionProvider getSuggestionsProvider() {
      return this.suggestionsProvider;
   }

   public void close() {
      this.closed = true;
      this.clearLevel();
      this.telemetryManager.onDisconnect();
   }

   public void clearLevel() {
      this.clearCacheSlots();
      this.level = null;
      this.levelLoadTracker = null;
   }

   private void clearCacheSlots() {
      for (WeakReference<CacheSlot<?, ?>> weakReference : this.cacheSlots) {
         CacheSlot<?, ?> cacheSlot = weakReference.get();
         if (cacheSlot != null) {
            cacheSlot.clear();
         }
      }

      this.cacheSlots.clear();
   }

   public RecipeAccess recipes() {
      return this.recipes;
   }

   @Override
   public void handleLogin(ClientboundLoginPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
      CommonPlayerSpawnInfo commonPlayerSpawnInfo = packet.commonPlayerSpawnInfo();
      List<ResourceKey<Level>> list = Lists.newArrayList(packet.levels());
      Collections.shuffle(list);
      this.levels = Sets.newLinkedHashSet(list);
      ResourceKey<Level> resourceKey = commonPlayerSpawnInfo.dimension();
      Holder<DimensionType> holder = commonPlayerSpawnInfo.dimensionType();
      this.serverChunkRadius = packet.chunkRadius();
      this.serverSimulationDistance = packet.simulationDistance();
      boolean bl = commonPlayerSpawnInfo.isDebug();
      boolean bl2 = commonPlayerSpawnInfo.isFlat();
      int i = commonPlayerSpawnInfo.seaLevel();
      ClientLevel.ClientLevelData clientLevelData = new ClientLevel.ClientLevelData(Difficulty.NORMAL, packet.hardcore(), bl2);
      this.levelData = clientLevelData;
      this.level = new ClientLevel(
         this,
         clientLevelData,
         resourceKey,
         holder,
         this.serverChunkRadius,
         this.serverSimulationDistance,
         this.minecraft.levelRenderer,
         bl,
         commonPlayerSpawnInfo.seed(),
         i
      );
      this.minecraft.setLevel(this.level);
      if (this.minecraft.player == null) {
         this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
         this.minecraft.player.setYRot(-180.0F);
         if (this.minecraft.getSingleplayerServer() != null) {
            this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
         }
      }

      this.setClientLoaded(false);
      this.debugSubscriber.clear();
      this.minecraft.levelRenderer.debugRenderer.refreshRendererList();
      this.minecraft.player.resetPos();
      this.minecraft.player.setId(packet.playerId());
      this.level.addEntity(this.minecraft.player);
      this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
      this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
      this.minecraft.setCameraEntity(this.minecraft.player);
      this.startWaitingForNewLevel(this.minecraft.player, this.level, LevelLoadingScreen.Reason.OTHER);
      this.minecraft.player.setReducedDebugInfo(packet.reducedDebugInfo());
      this.minecraft.player.setShowDeathScreen(packet.showDeathScreen());
      this.minecraft.player.setDoLimitedCrafting(packet.doLimitedCrafting());
      this.minecraft.player.setLastDeathLocation(commonPlayerSpawnInfo.lastDeathLocation());
      this.minecraft.player.setPortalCooldown(commonPlayerSpawnInfo.portalCooldown());
      this.minecraft.gameMode.setLocalMode(commonPlayerSpawnInfo.gameType(), commonPlayerSpawnInfo.previousGameType());
      this.minecraft.options.setServerRenderDistance(packet.chunkRadius());
      this.chatSession = null;
      this.signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
      this.nextChatIndex = 0;
      this.lastSeenMessages = new LastSeenMessagesTracker(20);
      this.messageSignatureCache = MessageSignatureCache.createDefault();
      if (this.connection.isEncrypted()) {
         this.prepareKeyPair();
      }

      this.telemetryManager.onPlayerInfoReceived(commonPlayerSpawnInfo.gameType(), packet.hardcore());
      this.minecraft.quickPlayLog().log(this.minecraft);
      this.serverEnforcesSecureChat = packet.enforcesSecureChat();
      if (this.serverData != null && !this.seenInsecureChatWarning && !this.enforcesSecureChat()) {
         SystemToast systemToast = SystemToast.multiline(
            this.minecraft, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST
         );
         this.minecraft.getToastManager().addToast(systemToast);
         this.seenInsecureChatWarning = true;
      }
   }

   @Override
   public void handleAddEntity(ClientboundAddEntityPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.removedPlayerVehicleId.isPresent() && this.removedPlayerVehicleId.getAsInt() == packet.getId()) {
         this.removedPlayerVehicleId = OptionalInt.empty();
      }

      Entity entity = this.createEntityFromPacket(packet);
      if (entity != null) {
         entity.recreateFromPacket(packet);
         this.level.addEntity(entity);
         this.postAddEntitySoundInstance(entity);
      } else {
         LOGGER.warn("Skipping Entity with id {}", packet.getType());
      }

      if (entity instanceof Player player) {
         UUID uUID = player.getUUID();
         PlayerInfo playerInfo = this.playerInfoMap.get(uUID);
         if (playerInfo != null) {
            this.seenPlayers.put(uUID, playerInfo);
         }
      }
   }

   @Nullable
   private Entity createEntityFromPacket(ClientboundAddEntityPacket packet) {
      EntityType<?> entityType = packet.getType();
      if (entityType == EntityType.PLAYER) {
         PlayerInfo playerInfo = this.getPlayerInfo(packet.getUUID());
         if (playerInfo == null) {
            LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", packet.getUUID());
            return null;
         } else {
            return new RemotePlayer(this.level, playerInfo.getProfile());
         }
      } else {
         return entityType.create(this.level, EntitySpawnReason.LOAD);
      }
   }

   private void postAddEntitySoundInstance(Entity entity) {
      if (entity instanceof AbstractMinecart abstractMinecart) {
         this.minecraft.getSoundManager().play(new MinecartSoundInstance(abstractMinecart));
      } else if (entity instanceof Bee bee) {
         boolean bl = bee.isAngry();
         BeeSoundInstance beeSoundInstance;
         if (bl) {
            beeSoundInstance = new BeeAggressiveSoundInstance(bee);
         } else {
            beeSoundInstance = new BeeFlyingSoundInstance(bee);
         }

         this.minecraft.getSoundManager().queueTickingSound(beeSoundInstance);
      }
   }

   @Override
   public void handleSetEntityMotion(ClientboundSetEntityMotionPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.getId());
      if (entity != null) {
         entity.lerpMotion(packet.getMovement());
      }
   }

   @Override
   public void handleSetEntityData(ClientboundSetEntityDataPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.id());
      if (entity != null) {
         entity.getEntityData().assignValues(packet.packedItems());
      }
   }

   @Override
   public void handleEntityPositionSync(ClientboundEntityPositionSyncPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.id());
      if (entity != null) {
         Vec3 vec3 = packet.values().position();
         entity.getPositionCodec().setBase(vec3);
         if (!entity.isLocalInstanceAuthoritative()) {
            float f = packet.values().yRot();
            float g = packet.values().xRot();
            boolean bl = entity.position().distanceToSqr(vec3) > 4096.0;
            if (this.level.isTickingEntity(entity) && !bl) {
               entity.moveOrInterpolateTo(vec3, f, g);
            } else {
               entity.snapTo(vec3, f, g);
            }

            if (!entity.isInterpolating() && entity.hasIndirectPassenger(this.minecraft.player)) {
               entity.positionRider(this.minecraft.player);
               this.minecraft.player.setOldPosAndRot();
            }

            entity.setOnGround(packet.onGround());
         }
      }
   }

   @Override
   public void handleTeleportEntity(ClientboundTeleportEntityPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.id());
      if (entity == null) {
         if (this.removedPlayerVehicleId.isPresent() && this.removedPlayerVehicleId.getAsInt() == packet.id()) {
            LOGGER.debug("Trying to teleport entity with id {}, that was formerly player vehicle, applying teleport to player instead", packet.id());
            setValuesFromPositionPacket(packet.change(), packet.relatives(), this.minecraft.player, false);
            this.connection
               .send(
                  new ServerboundMovePlayerPacket.PosRot(
                     this.minecraft.player.getX(),
                     this.minecraft.player.getY(),
                     this.minecraft.player.getZ(),
                     this.minecraft.player.getYRot(),
                     this.minecraft.player.getXRot(),
                     false,
                     false
                  )
               );
         }
      } else {
         boolean bl = packet.relatives().contains(Relative.X) || packet.relatives().contains(Relative.Y) || packet.relatives().contains(Relative.Z);
         boolean bl2 = this.level.isTickingEntity(entity) || !entity.isLocalInstanceAuthoritative() || bl;
         boolean bl3 = setValuesFromPositionPacket(packet.change(), packet.relatives(), entity, bl2);
         entity.setOnGround(packet.onGround());
         if (!bl3 && entity.hasIndirectPassenger(this.minecraft.player)) {
            entity.positionRider(this.minecraft.player);
            this.minecraft.player.setOldPosAndRot();
            if (entity.isLocalInstanceAuthoritative()) {
               this.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
            }
         }
      }
   }

   @Override
   public void handleTickingState(ClientboundTickingStatePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.minecraft.level != null) {
         TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
         tickRateManager.setTickRate(packet.tickRate());
         tickRateManager.setFrozen(packet.isFrozen());
      }
   }

   @Override
   public void handleTickingStep(ClientboundTickingStepPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.minecraft.level != null) {
         TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
         tickRateManager.setFrozenTicksToRun(packet.tickSteps());
      }
   }

   @Override
   public void handleSetHeldSlot(ClientboundSetHeldSlotPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (Inventory.isHotbarSlot(packet.slot())) {
         this.minecraft.player.getInventory().setSelectedSlot(packet.slot());
      }
   }

   @Override
   public void handleMoveEntity(ClientboundMoveEntityPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = packet.getEntity(this.level);
      if (entity != null) {
         if (entity.isLocalInstanceAuthoritative()) {
            VecDeltaCodec vecDeltaCodec = entity.getPositionCodec();
            Vec3 vec3 = vecDeltaCodec.decode(packet.getXa(), packet.getYa(), packet.getZa());
            vecDeltaCodec.setBase(vec3);
         } else {
            if (packet.hasPosition()) {
               VecDeltaCodec vecDeltaCodec = entity.getPositionCodec();
               Vec3 vec3 = vecDeltaCodec.decode(packet.getXa(), packet.getYa(), packet.getZa());
               vecDeltaCodec.setBase(vec3);
               if (packet.hasRotation()) {
                  entity.moveOrInterpolateTo(vec3, packet.getYRot(), packet.getXRot());
               } else {
                  entity.moveOrInterpolateTo(vec3);
               }
            } else if (packet.hasRotation()) {
               entity.moveOrInterpolateTo(packet.getYRot(), packet.getXRot());
            }

            entity.setOnGround(packet.isOnGround());
         }
      }
   }

   @Override
   public void handleMinecartAlongTrack(ClientboundMoveMinecartPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (packet.getEntity(this.level) instanceof AbstractMinecart abstractMinecart) {
         if (abstractMinecart.getBehavior() instanceof NewMinecartBehavior newMinecartBehavior) {
            newMinecartBehavior.lerpSteps.addAll(packet.lerpSteps());
         }
      }
   }

   @Override
   public void handleRotateMob(ClientboundRotateHeadPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = packet.getEntity(this.level);
      if (entity != null) {
         entity.lerpHeadTo(packet.getYHeadRot(), 3);
      }
   }

   @Override
   public void handleRemoveEntities(ClientboundRemoveEntitiesPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      packet.getEntityIds().forEach(i -> {
         Entity entity = this.level.getEntity(i);
         if (entity != null) {
            if (entity.hasIndirectPassenger(this.minecraft.player)) {
               LOGGER.debug("Remove entity {}:{} that has player as passenger", entity.getType(), i);
               this.removedPlayerVehicleId = OptionalInt.of(i);
            }

            this.level.removeEntity(i, Entity.RemovalReason.DISCARDED);
            this.debugSubscriber.dropEntity(entity);
         }
      });
   }

   @Override
   public void handleMovePlayer(ClientboundPlayerPositionPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Player player = this.minecraft.player;
      if (!player.isPassenger()) {
         setValuesFromPositionPacket(packet.change(), packet.relatives(), player, false);
      }

      this.connection.send(new ServerboundAcceptTeleportationPacket(packet.id()));
      this.connection
         .send(new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false, false));
   }

   private static boolean setValuesFromPositionPacket(PositionMoveRotation positionMoveRotation, Set<Relative> relatives, Entity entity, boolean lerp) {
      PositionMoveRotation positionMoveRotation2 = PositionMoveRotation.of(entity);
      PositionMoveRotation positionMoveRotation3 = PositionMoveRotation.calculateAbsolute(positionMoveRotation2, positionMoveRotation, relatives);
      boolean bl = positionMoveRotation2.position().distanceToSqr(positionMoveRotation3.position()) > 4096.0;
      if (lerp && !bl) {
         entity.moveOrInterpolateTo(positionMoveRotation3.position(), positionMoveRotation3.yRot(), positionMoveRotation3.xRot());
         entity.setDeltaMovement(positionMoveRotation3.deltaMovement());
         return true;
      } else {
         entity.setPos(positionMoveRotation3.position());
         entity.setDeltaMovement(positionMoveRotation3.deltaMovement());
         entity.setYRot(positionMoveRotation3.yRot());
         entity.setXRot(positionMoveRotation3.xRot());
         PositionMoveRotation positionMoveRotation4 = new PositionMoveRotation(entity.oldPosition(), Vec3.ZERO, entity.yRotO, entity.xRotO);
         PositionMoveRotation positionMoveRotation5 = PositionMoveRotation.calculateAbsolute(positionMoveRotation4, positionMoveRotation, relatives);
         entity.setOldPosAndRot(positionMoveRotation5.position(), positionMoveRotation5.yRot(), positionMoveRotation5.xRot());
         return false;
      }
   }

   @Override
   public void handleRotatePlayer(ClientboundPlayerRotationPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Player player = this.minecraft.player;
      Set<Relative> set = Relative.rotation(packet.relativeY(), packet.relativeX());
      PositionMoveRotation positionMoveRotation = PositionMoveRotation.of(player);
      PositionMoveRotation positionMoveRotation2 = PositionMoveRotation.calculateAbsolute(
         positionMoveRotation, positionMoveRotation.withRotation(packet.yRot(), packet.xRot()), set
      );
      player.setYRot(positionMoveRotation2.yRot());
      player.setXRot(positionMoveRotation2.xRot());
      player.setOldRot();
      this.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), false, false));
   }

   @Override
   public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      packet.runUpdates((blockPos, blockState) -> this.level.setServerVerifiedBlockState(blockPos, blockState, 19));
   }

   @Override
   public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      int i = packet.getX();
      int j = packet.getZ();
      this.updateLevelChunk(i, j, packet.getChunkData());
      ClientboundLightUpdatePacketData clientboundLightUpdatePacketData = packet.getLightData();
      this.level.queueLightUpdate(() -> {
         this.applyLightData(i, j, clientboundLightUpdatePacketData, false);
         LevelChunk levelChunk = this.level.getChunkSource().getChunk(i, j, false);
         if (levelChunk != null) {
            this.enableChunkLight(levelChunk, i, j);
            this.minecraft.levelRenderer.onChunkReadyToRender(levelChunk.getPos());
         }
      });
   }

   @Override
   public void handleChunksBiomes(ClientboundChunksBiomesPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());

      for (ClientboundChunksBiomesPacket.ChunkBiomeData chunkBiomeData : packet.chunkBiomeData()) {
         this.level.getChunkSource().replaceBiomes(chunkBiomeData.pos().x, chunkBiomeData.pos().z, chunkBiomeData.getReadBuffer());
      }

      for (ClientboundChunksBiomesPacket.ChunkBiomeData chunkBiomeData : packet.chunkBiomeData()) {
         this.level.onChunkLoaded(new ChunkPos(chunkBiomeData.pos().x, chunkBiomeData.pos().z));
      }

      for (ClientboundChunksBiomesPacket.ChunkBiomeData chunkBiomeData : packet.chunkBiomeData()) {
         for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
               for (int k = this.level.getMinSectionY(); k <= this.level.getMaxSectionY(); k++) {
                  this.minecraft.levelRenderer.setSectionDirty(chunkBiomeData.pos().x + i, k, chunkBiomeData.pos().z + j);
               }
            }
         }
      }
   }

   private void updateLevelChunk(int x, int z, ClientboundLevelChunkPacketData data) {
      this.level.getChunkSource().replaceWithPacketData(x, z, data.getReadBuffer(), data.getHeightmaps(), data.getBlockEntitiesTagsConsumer(x, z));
   }

   private void enableChunkLight(LevelChunk chunk, int x, int z) {
      LevelLightEngine levelLightEngine = this.level.getChunkSource().getLightEngine();
      LevelChunkSection[] levelChunkSections = chunk.getSections();
      ChunkPos chunkPos = chunk.getPos();

      for (int i = 0; i < levelChunkSections.length; i++) {
         LevelChunkSection levelChunkSection = levelChunkSections[i];
         int j = this.level.getSectionYFromSectionIndex(i);
         levelLightEngine.updateSectionStatus(SectionPos.of(chunkPos, j), levelChunkSection.hasOnlyAir());
      }

      this.level.setSectionRangeDirty(x - 1, this.level.getMinSectionY(), z - 1, x + 1, this.level.getMaxSectionY(), z + 1);
   }

   @Override
   public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.getChunkSource().drop(packet.pos());
      this.debugSubscriber.dropChunk(packet.pos());
      this.queueLightRemoval(packet);
   }

   private void queueLightRemoval(ClientboundForgetLevelChunkPacket packet) {
      ChunkPos chunkPos = packet.pos();
      this.level.queueLightUpdate(() -> {
         LevelLightEngine levelLightEngine = this.level.getLightEngine();
         levelLightEngine.setLightEnabled(chunkPos, false);

         for (int i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); i++) {
            SectionPos sectionPos = SectionPos.of(chunkPos, i);
            levelLightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, null);
            levelLightEngine.queueSectionData(LightLayer.SKY, sectionPos, null);
         }

         for (int i = this.level.getMinSectionY(); i <= this.level.getMaxSectionY(); i++) {
            levelLightEngine.updateSectionStatus(SectionPos.of(chunkPos, i), true);
         }
      });
   }

   @Override
   public void handleBlockUpdate(ClientboundBlockUpdatePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.setServerVerifiedBlockState(packet.getPos(), packet.getBlockState(), 19);
   }

   @Override
   public void handleConfigurationStart(ClientboundStartConfigurationPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.getChatListener().flushQueue();
      this.sendChatAcknowledgement();
      ChatComponent.State state = this.minecraft.gui.getChat().storeState();
      this.minecraft.clearClientLevel(new ServerReconfigScreen(RECONFIGURE_SCREEN_MESSAGE, this.connection));
      this.connection
         .setupInboundProtocol(
            ConfigurationProtocols.CLIENTBOUND,
            new ClientConfigurationPacketListenerImpl(
               this.minecraft,
               this.connection,
               new CommonListenerCookie(
                  new LevelLoadTracker(),
                  this.localGameProfile,
                  this.telemetryManager,
                  this.registryAccess,
                  this.enabledFeatures,
                  this.serverBrand,
                  this.serverData,
                  this.postDisconnectScreen,
                  this.serverCookies,
                  state,
                  this.customReportDetails,
                  this.serverLinks(),
                  this.seenPlayers,
                  this.seenInsecureChatWarning
               )
            )
         );
      this.send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
      this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
   }

   @Override
   public void handleTakeItemEntity(ClientboundTakeItemEntityPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.getItemId());
      LivingEntity livingEntity = (LivingEntity)this.level.getEntity(packet.getPlayerId());
      if (livingEntity == null) {
         livingEntity = this.minecraft.player;
      }

      if (entity != null) {
         if (entity instanceof ExperienceOrb) {
            this.level
               .playLocalSound(
                  entity.getX(),
                  entity.getY(),
                  entity.getZ(),
                  SoundEvents.EXPERIENCE_ORB_PICKUP,
                  SoundSource.PLAYERS,
                  0.1F,
                  (this.random.nextFloat() - this.random.nextFloat()) * 0.35F + 0.9F,
                  false
               );
         } else {
            this.level
               .playLocalSound(
                  entity.getX(),
                  entity.getY(),
                  entity.getZ(),
                  SoundEvents.ITEM_PICKUP,
                  SoundSource.PLAYERS,
                  0.2F,
                  (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F,
                  false
               );
         }

         EntityRenderState entityRenderState = this.minecraft.getEntityRenderDispatcher().extractEntity(entity, 1.0F);
         this.minecraft.particleEngine.add(new ItemPickupParticle(this.level, entityRenderState, livingEntity, entity.getDeltaMovement()));
         if (entity instanceof ItemEntity itemEntity) {
            ItemStack itemStack = itemEntity.getItem();
            if (!itemStack.isEmpty()) {
               itemStack.shrink(packet.getAmount());
            }

            if (itemStack.isEmpty()) {
               this.level.removeEntity(packet.getItemId(), Entity.RemovalReason.DISCARDED);
            }
         } else if (!(entity instanceof ExperienceOrb)) {
            this.level.removeEntity(packet.getItemId(), Entity.RemovalReason.DISCARDED);
         }
      }
   }

   @Override
   public void handleSystemChat(ClientboundSystemChatPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.getChatListener().handleSystemMessage(packet.content(), packet.overlay());
   }

   @Override
   public void handlePlayerChat(ClientboundPlayerChatPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      int i = this.nextChatIndex++;
      if (packet.globalIndex() != i) {
         LOGGER.error("Missing or out-of-order chat message from server, expected index {} but got {}", i, packet.globalIndex());
         this.connection.disconnect(BAD_CHAT_INDEX);
      } else {
         Optional<SignedMessageBody> optional = packet.body().unpack(this.messageSignatureCache);
         if (optional.isEmpty()) {
            LOGGER.error("Message from player with ID {} referenced unrecognized signature id", packet.sender());
            this.connection.disconnect(INVALID_PACKET);
         } else {
            this.messageSignatureCache.push(optional.get(), packet.signature());
            UUID uUID = packet.sender();
            PlayerInfo playerInfo = this.getPlayerInfo(uUID);
            if (playerInfo == null) {
               LOGGER.error("Received player chat packet for unknown player with ID: {}", uUID);
               this.minecraft.getChatListener().handleChatMessageError(uUID, packet.signature(), packet.chatType());
            } else {
               RemoteChatSession remoteChatSession = playerInfo.getChatSession();
               SignedMessageLink signedMessageLink;
               if (remoteChatSession != null) {
                  signedMessageLink = new SignedMessageLink(packet.index(), uUID, remoteChatSession.sessionId());
               } else {
                  signedMessageLink = SignedMessageLink.unsigned(uUID);
               }

               PlayerChatMessage playerChatMessage = new PlayerChatMessage(
                  signedMessageLink, packet.signature(), optional.get(), packet.unsignedContent(), packet.filterMask()
               );
               playerChatMessage = playerInfo.getMessageValidator().updateAndValidate(playerChatMessage);
               if (playerChatMessage != null) {
                  this.minecraft.getChatListener().handlePlayerChatMessage(playerChatMessage, playerInfo.getProfile(), packet.chatType());
               } else {
                  this.minecraft.getChatListener().handleChatMessageError(uUID, packet.signature(), packet.chatType());
               }
            }
         }
      }
   }

   @Override
   public void handleDisguisedChat(ClientboundDisguisedChatPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.getChatListener().handleDisguisedChatMessage(packet.message(), packet.chatType());
   }

   @Override
   public void handleDeleteChat(ClientboundDeleteChatPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Optional<MessageSignature> optional = packet.messageSignature().unpack(this.messageSignatureCache);
      if (optional.isEmpty()) {
         this.connection.disconnect(INVALID_PACKET);
      } else {
         this.lastSeenMessages.ignorePending(optional.get());
         if (!this.minecraft.getChatListener().removeFromDelayedMessageQueue(optional.get())) {
            this.minecraft.gui.getChat().deleteMessage(optional.get());
         }
      }
   }

   @Override
   public void handleAnimate(ClientboundAnimatePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.getId());
      if (entity != null) {
         if (packet.getAction() == 0) {
            LivingEntity livingEntity = (LivingEntity)entity;
            livingEntity.swing(InteractionHand.MAIN_HAND);
         } else if (packet.getAction() == 3) {
            LivingEntity livingEntity = (LivingEntity)entity;
            livingEntity.swing(InteractionHand.OFF_HAND);
         } else if (packet.getAction() == 2) {
            Player player = (Player)entity;
            player.stopSleepInBed(false, false);
         } else if (packet.getAction() == 4) {
            this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
         } else if (packet.getAction() == 5) {
            this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
         }
      }
   }

   @Override
   public void handleHurtAnimation(ClientboundHurtAnimationPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.id());
      if (entity != null) {
         entity.animateHurt(packet.yaw());
      }
   }

   @Override
   public void handleSetTime(ClientboundSetTimePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.setTimeFromServer(packet.gameTime(), packet.dayTime(), packet.tickDayTime());
      this.telemetryManager.setTime(packet.gameTime());
   }

   @Override
   public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.level.setRespawnData(packet.respawnData());
   }

   @Override
   public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.getVehicle());
      if (entity == null) {
         LOGGER.warn("Received passengers for unknown entity");
      } else {
         boolean bl = entity.hasIndirectPassenger(this.minecraft.player);
         entity.ejectPassengers();

         for (int i : packet.getPassengers()) {
            Entity entity2 = this.level.getEntity(i);
            if (entity2 != null) {
               entity2.startRiding(entity, true, false);
               if (entity2 == this.minecraft.player) {
                  this.removedPlayerVehicleId = OptionalInt.empty();
                  if (!bl) {
                     if (entity instanceof AbstractBoat) {
                        this.minecraft.player.yRotO = entity.getYRot();
                        this.minecraft.player.setYRot(entity.getYRot());
                        this.minecraft.player.setYHeadRot(entity.getYRot());
                     }

                     Component component = Component.translatable("mount.onboard", this.minecraft.options.keyShift.getTranslatedKeyMessage());
                     this.minecraft.gui.setOverlayMessage(component, false);
                     this.minecraft.getNarrator().saySystemNow(component);
                  }
               }
            }
         }
      }
   }

   @Override
   public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.level.getEntity(packet.getSourceId()) instanceof Leashable leashable) {
         leashable.setDelayedLeashHolderId(packet.getDestId());
      }
   }

   private static ItemStack findTotem(Player player) {
      for (InteractionHand interactionHand : InteractionHand.values()) {
         ItemStack itemStack = player.getItemInHand(interactionHand);
         if (itemStack.has(DataComponents.DEATH_PROTECTION)) {
            return itemStack;
         }
      }

      return new ItemStack(Items.TOTEM_OF_UNDYING);
   }

   @Override
   public void handleEntityEvent(ClientboundEntityEventPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = packet.getEntity(this.level);
      if (entity != null) {
         switch (packet.getEventId()) {
            case 21:
               this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)entity));
               break;
            case 35:
               int i = 40;
               this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
               this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, entity.getSoundSource(), 1.0F, 1.0F, false);
               if (entity == this.minecraft.player) {
                  this.minecraft.gameRenderer.displayItemActivation(findTotem(this.minecraft.player));
               }
               break;
            case 63:
               this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)entity));
               break;
            default:
               entity.handleEntityEvent(packet.getEventId());
         }
      }
   }

   @Override
   public void handleDamageEvent(ClientboundDamageEventPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.entityId());
      if (entity != null) {
         entity.handleDamageEvent(packet.getSource(this.level));
      }
   }

   @Override
   public void handleSetHealth(ClientboundSetHealthPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.player.hurtTo(packet.getHealth());
      this.minecraft.player.getFoodData().setFoodLevel(packet.getFood());
      this.minecraft.player.getFoodData().setSaturation(packet.getSaturation());
   }

   @Override
   public void handleSetExperience(ClientboundSetExperiencePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.player.setExperienceValues(packet.getExperienceProgress(), packet.getTotalExperience(), packet.getExperienceLevel());
   }

   @Override
   public void handleRespawn(ClientboundRespawnPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      CommonPlayerSpawnInfo commonPlayerSpawnInfo = packet.commonPlayerSpawnInfo();
      ResourceKey<Level> resourceKey = commonPlayerSpawnInfo.dimension();
      Holder<DimensionType> holder = commonPlayerSpawnInfo.dimensionType();
      LocalPlayer localPlayer = this.minecraft.player;
      ResourceKey<Level> resourceKey2 = localPlayer.level().dimension();
      boolean bl = resourceKey != resourceKey2;
      LevelLoadingScreen.Reason reason = this.determineLevelLoadingReason(localPlayer.isDeadOrDying(), resourceKey, resourceKey2);
      if (bl) {
         Map<MapId, MapItemSavedData> map = this.level.getAllMapData();
         boolean bl2 = commonPlayerSpawnInfo.isDebug();
         boolean bl3 = commonPlayerSpawnInfo.isFlat();
         int i = commonPlayerSpawnInfo.seaLevel();
         ClientLevel.ClientLevelData clientLevelData = new ClientLevel.ClientLevelData(this.levelData.getDifficulty(), this.levelData.isHardcore(), bl3);
         this.levelData = clientLevelData;
         this.level = new ClientLevel(
            this,
            clientLevelData,
            resourceKey,
            holder,
            this.serverChunkRadius,
            this.serverSimulationDistance,
            this.minecraft.levelRenderer,
            bl2,
            commonPlayerSpawnInfo.seed(),
            i
         );
         this.level.addMapData(map);
         this.minecraft.setLevel(this.level);
         this.debugSubscriber.dropLevel();
      }

      this.minecraft.setCameraEntity(null);
      if (localPlayer.hasContainerOpen()) {
         localPlayer.closeContainer();
      }

      LocalPlayer localPlayer2;
      if (packet.shouldKeep((byte)2)) {
         localPlayer2 = this.minecraft
            .gameMode
            .createPlayer(this.level, localPlayer.getStats(), localPlayer.getRecipeBook(), localPlayer.getLastSentInput(), localPlayer.isSprinting());
      } else {
         localPlayer2 = this.minecraft.gameMode.createPlayer(this.level, localPlayer.getStats(), localPlayer.getRecipeBook());
      }

      this.setClientLoaded(false);
      this.startWaitingForNewLevel(localPlayer2, this.level, reason);
      localPlayer2.setId(localPlayer.getId());
      this.minecraft.player = localPlayer2;
      if (bl) {
         this.minecraft.getMusicManager().stopPlaying();
      }

      this.minecraft.setCameraEntity(localPlayer2);
      if (packet.shouldKeep((byte)2)) {
         List<SynchedEntityData.DataValue<?>> list = localPlayer.getEntityData().getNonDefaultValues();
         if (list != null) {
            localPlayer2.getEntityData().assignValues(list);
         }

         localPlayer2.setDeltaMovement(localPlayer.getDeltaMovement());
         localPlayer2.setYRot(localPlayer.getYRot());
         localPlayer2.setXRot(localPlayer.getXRot());
      } else {
         localPlayer2.resetPos();
         localPlayer2.setYRot(-180.0F);
      }

      if (packet.shouldKeep((byte)1)) {
         localPlayer2.getAttributes().assignAllValues(localPlayer.getAttributes());
      } else {
         localPlayer2.getAttributes().assignBaseValues(localPlayer.getAttributes());
      }

      this.level.addEntity(localPlayer2);
      localPlayer2.input = new KeyboardInput(this.minecraft.options);
      this.minecraft.gameMode.adjustPlayer(localPlayer2);
      localPlayer2.setReducedDebugInfo(localPlayer.isReducedDebugInfo());
      localPlayer2.setShowDeathScreen(localPlayer.shouldShowDeathScreen());
      localPlayer2.setLastDeathLocation(commonPlayerSpawnInfo.lastDeathLocation());
      localPlayer2.setPortalCooldown(commonPlayerSpawnInfo.portalCooldown());
      localPlayer2.portalEffectIntensity = localPlayer.portalEffectIntensity;
      localPlayer2.oPortalEffectIntensity = localPlayer.oPortalEffectIntensity;
      if (this.minecraft.screen instanceof DeathScreen || this.minecraft.screen instanceof DeathScreen.TitleConfirmScreen) {
         this.minecraft.setScreen(null);
      }

      this.minecraft.gameMode.setLocalMode(commonPlayerSpawnInfo.gameType(), commonPlayerSpawnInfo.previousGameType());
   }

   private LevelLoadingScreen.Reason determineLevelLoadingReason(boolean fromDeath, ResourceKey<Level> spawnDimension, ResourceKey<Level> playerDimension) {
      LevelLoadingScreen.Reason reason = LevelLoadingScreen.Reason.OTHER;
      if (!fromDeath) {
         if (spawnDimension == Level.NETHER || playerDimension == Level.NETHER) {
            reason = LevelLoadingScreen.Reason.NETHER_PORTAL;
         } else if (spawnDimension == Level.END || playerDimension == Level.END) {
            reason = LevelLoadingScreen.Reason.END_PORTAL;
         }
      }

      return reason;
   }

   @Override
   public void handleExplosion(ClientboundExplodePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Vec3 vec3 = packet.center();
      this.minecraft
         .level
         .playLocalSound(
            vec3.x(),
            vec3.y(),
            vec3.z(),
            packet.explosionSound().value(),
            SoundSource.BLOCKS,
            4.0F,
            (1.0F + (this.minecraft.level.random.nextFloat() - this.minecraft.level.random.nextFloat()) * 0.2F) * 0.7F,
            false
         );
      this.minecraft.level.addParticle(packet.explosionParticle(), vec3.x(), vec3.y(), vec3.z(), 1.0, 0.0, 0.0);
      this.minecraft.level.trackExplosionEffects(vec3, packet.radius(), packet.blockCount(), packet.blockParticles());
      packet.playerKnockback().ifPresent(this.minecraft.player::addDeltaMovement);
   }

   @Override
   public void handleMountScreenOpen(ClientboundMountScreenOpenPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.getEntityId());
      LocalPlayer localPlayer = this.minecraft.player;
      int i = packet.getInventoryColumns();
      SimpleContainer simpleContainer = new SimpleContainer(AbstractMountInventoryMenu.getInventorySize(i));
      if (entity instanceof AbstractHorse abstractHorse) {
         HorseInventoryMenu horseInventoryMenu = new HorseInventoryMenu(packet.getContainerId(), localPlayer.getInventory(), simpleContainer, abstractHorse, i);
         localPlayer.containerMenu = horseInventoryMenu;
         this.minecraft.setScreen(new HorseInventoryScreen(horseInventoryMenu, localPlayer.getInventory(), abstractHorse, i));
      } else if (entity instanceof AbstractNautilus abstractNautilus) {
         NautilusInventoryMenu nautilusInventoryMenu = new NautilusInventoryMenu(
            packet.getContainerId(), localPlayer.getInventory(), simpleContainer, abstractNautilus, i
         );
         localPlayer.containerMenu = nautilusInventoryMenu;
         this.minecraft.setScreen(new NautilusInventoryScreen(nautilusInventoryMenu, localPlayer.getInventory(), abstractNautilus, i));
      }
   }

   @Override
   public void handleOpenScreen(ClientboundOpenScreenPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      MenuScreens.create(packet.getType(), this.minecraft, packet.getContainerId(), packet.getTitle());
   }

   @Override
   public void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Player player = this.minecraft.player;
      ItemStack itemStack = packet.getItem();
      int i = packet.getSlot();
      this.minecraft.getTutorial().onGetItem(itemStack);
      boolean bl;
      if (this.minecraft.screen instanceof CreativeModeInventoryScreen creativeModeInventoryScreen) {
         bl = !creativeModeInventoryScreen.isInventoryOpen();
      } else {
         bl = false;
      }

      if (packet.getContainerId() == 0) {
         if (InventoryMenu.isHotbarSlot(i) && !itemStack.isEmpty()) {
            ItemStack itemStack2 = player.inventoryMenu.getSlot(i).getItem();
            if (itemStack2.isEmpty() || itemStack2.getCount() < itemStack.getCount()) {
               itemStack.setPopTime(5);
            }
         }

         player.inventoryMenu.setItem(i, packet.getStateId(), itemStack);
      } else if (packet.getContainerId() == player.containerMenu.containerId && (packet.getContainerId() != 0 || !bl)) {
         player.containerMenu.setItem(i, packet.getStateId(), itemStack);
      }

      if (this.minecraft.screen instanceof CreativeModeInventoryScreen) {
         player.inventoryMenu.setRemoteSlot(i, itemStack);
         player.inventoryMenu.broadcastChanges();
      }
   }

   @Override
   public void handleSetCursorItem(ClientboundSetCursorItemPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.getTutorial().onGetItem(packet.contents());
      if (!(this.minecraft.screen instanceof CreativeModeInventoryScreen)) {
         this.minecraft.player.containerMenu.setCarried(packet.contents());
      }
   }

   @Override
   public void handleSetPlayerInventory(ClientboundSetPlayerInventoryPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.getTutorial().onGetItem(packet.contents());
      this.minecraft.player.getInventory().setItem(packet.slot(), packet.contents());
   }

   @Override
   public void handleContainerContent(ClientboundContainerSetContentPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Player player = this.minecraft.player;
      if (packet.containerId() == 0) {
         player.inventoryMenu.initializeContents(packet.stateId(), packet.items(), packet.carriedItem());
      } else if (packet.containerId() == player.containerMenu.containerId) {
         player.containerMenu.initializeContents(packet.stateId(), packet.items(), packet.carriedItem());
      }
   }

   @Override
   public void handleOpenSignEditor(ClientboundOpenSignEditorPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      BlockPos blockPos = packet.getPos();
      if (this.level.getBlockEntity(blockPos) instanceof SignBlockEntity signBlockEntity) {
         this.minecraft.player.openTextEdit(signBlockEntity, packet.isFrontText());
      } else {
         LOGGER.warn("Ignoring openTextEdit on an invalid entity: {} at pos {}", this.level.getBlockEntity(blockPos), blockPos);
      }
   }

   @Override
   public void handleBlockEntityData(ClientboundBlockEntityDataPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      BlockPos blockPos = packet.getPos();
      this.minecraft.level.getBlockEntity(blockPos, packet.getType()).ifPresent(blockEntity -> {
         ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER);

         try {
            blockEntity.loadWithComponents(TagValueInput.create(scopedCollector, this.registryAccess, packet.getTag()));
         } catch (Throwable var7) {
            try {
               scopedCollector.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         scopedCollector.close();
         if (blockEntity instanceof CommandBlockEntity && this.minecraft.screen instanceof CommandBlockEditScreen) {
            ((CommandBlockEditScreen)this.minecraft.screen).updateGui();
         }
      });
   }

   @Override
   public void handleContainerSetData(ClientboundContainerSetDataPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Player player = this.minecraft.player;
      if (player.containerMenu.containerId == packet.getContainerId()) {
         player.containerMenu.setData(packet.getId(), packet.getValue());
      }
   }

   @Override
   public void handleSetEquipment(ClientboundSetEquipmentPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.level.getEntity(packet.getEntity()) instanceof LivingEntity livingEntity) {
         packet.getSlots().forEach(pair -> livingEntity.setItemSlot((EquipmentSlot)pair.getFirst(), (ItemStack)pair.getSecond()));
      }
   }

   @Override
   public void handleContainerClose(ClientboundContainerClosePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.player.clientSideCloseContainer();
   }

   @Override
   public void handleBlockEvent(ClientboundBlockEventPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.level.blockEvent(packet.getPos(), packet.getBlock(), packet.getB0(), packet.getB1());
   }

   @Override
   public void handleBlockDestruction(ClientboundBlockDestructionPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.level.destroyBlockProgress(packet.getId(), packet.getPos(), packet.getProgress());
   }

   @Override
   public void handleGameEvent(ClientboundGameEventPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Player player = this.minecraft.player;
      ClientboundGameEventPacket.Type type = packet.getEvent();
      float f = packet.getParam();
      int i = Mth.floor(f + 0.5F);
      if (type == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
         player.displayClientMessage(Component.translatable("block.minecraft.spawn.not_valid"), false);
      } else if (type == ClientboundGameEventPacket.START_RAINING) {
         this.level.getLevelData().setRaining(true);
         this.level.setRainLevel(0.0F);
      } else if (type == ClientboundGameEventPacket.STOP_RAINING) {
         this.level.getLevelData().setRaining(false);
         this.level.setRainLevel(1.0F);
      } else if (type == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
         this.minecraft.gameMode.setLocalMode(GameType.byId(i));
      } else if (type == ClientboundGameEventPacket.WIN_GAME) {
         this.minecraft.setScreen(new WinScreen(true, () -> {
            this.minecraft.player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            this.minecraft.setScreen(null);
         }));
      } else if (type == ClientboundGameEventPacket.DEMO_EVENT) {
         Options options = this.minecraft.options;
         Component component = null;
         if (f == 0.0F) {
            this.minecraft.setScreen(new DemoIntroScreen());
         } else if (f == 101.0F) {
            component = Component.translatable(
               "demo.help.movement",
               options.keyUp.getTranslatedKeyMessage(),
               options.keyLeft.getTranslatedKeyMessage(),
               options.keyDown.getTranslatedKeyMessage(),
               options.keyRight.getTranslatedKeyMessage()
            );
         } else if (f == 102.0F) {
            component = Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage());
         } else if (f == 103.0F) {
            component = Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage());
         } else if (f == 104.0F) {
            component = Component.translatable("demo.day.6", options.keyScreenshot.getTranslatedKeyMessage());
         }

         if (component != null) {
            this.minecraft.gui.getChat().addMessage(component);
            this.minecraft.getNarrator().saySystemQueued(component);
         }
      } else if (type == ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND) {
         this.level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18F, 0.45F);
      } else if (type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
         this.level.setRainLevel(f);
      } else if (type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
         this.level.setThunderLevel(f);
      } else if (type == ClientboundGameEventPacket.PUFFER_FISH_STING) {
         this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0F, 1.0F);
      } else if (type == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
         this.level.addParticle(ParticleTypes.ELDER_GUARDIAN, player.getX(), player.getY(), player.getZ(), 0.0, 0.0, 0.0);
         if (i == 1) {
            this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
         }
      } else if (type == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
         this.minecraft.player.setShowDeathScreen(f == 0.0F);
      } else if (type == ClientboundGameEventPacket.LIMITED_CRAFTING) {
         this.minecraft.player.setDoLimitedCrafting(f == 1.0F);
      } else if (type == ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START && this.levelLoadTracker != null) {
         this.levelLoadTracker.loadingPacketsReceived();
      }
   }

   private void startWaitingForNewLevel(LocalPlayer player, ClientLevel level, LevelLoadingScreen.Reason reason) {
      if (this.levelLoadTracker == null) {
         this.levelLoadTracker = new LevelLoadTracker();
      }

      this.levelLoadTracker.startClientLoad(player, level, this.minecraft.levelRenderer);
      if (this.minecraft.screen instanceof LevelLoadingScreen levelLoadingScreen) {
         levelLoadingScreen.update(this.levelLoadTracker, reason);
      } else {
         this.minecraft.gui.getChat().preserveCurrentChatScreen();
         this.minecraft.setScreenAndShow(new LevelLoadingScreen(this.levelLoadTracker, reason));
      }
   }

   @Override
   public void handleMapItemData(ClientboundMapItemDataPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      MapId mapId = packet.mapId();
      MapItemSavedData mapItemSavedData = this.minecraft.level.getMapData(mapId);
      if (mapItemSavedData == null) {
         mapItemSavedData = MapItemSavedData.createForClient(packet.scale(), packet.locked(), this.minecraft.level.dimension());
         this.minecraft.level.overrideMapData(mapId, mapItemSavedData);
      }

      packet.applyToMap(mapItemSavedData);
      this.minecraft.getMapTextureManager().update(mapId, mapItemSavedData);
   }

   @Override
   public void handleLevelEvent(ClientboundLevelEventPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (packet.isGlobalEvent()) {
         this.minecraft.level.globalLevelEvent(packet.getType(), packet.getPos(), packet.getData());
      } else {
         this.minecraft.level.levelEvent(packet.getType(), packet.getPos(), packet.getData());
      }
   }

   @Override
   public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.advancements.update(packet);
   }

   @Override
   public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Identifier identifier = packet.getTab();
      if (identifier == null) {
         this.advancements.setSelectedTab(null, false);
      } else {
         AdvancementHolder advancementHolder = this.advancements.get(identifier);
         this.advancements.setSelectedTab(advancementHolder, false);
      }
   }

   @Override
   public void handleCommands(ClientboundCommandsPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.commands = new CommandDispatcher(packet.getRoot(CommandBuildContext.simple(this.registryAccess, this.enabledFeatures), COMMAND_NODE_BUILDER));
   }

   @Override
   public void handleStopSoundEvent(ClientboundStopSoundPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.getSoundManager().stop(packet.getName(), packet.getSource());
   }

   @Override
   public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.suggestionsProvider.completeCustomSuggestions(packet.id(), packet.toSuggestions());
   }

   @Override
   public void handleUpdateRecipes(ClientboundUpdateRecipesPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.recipes = new ClientRecipeContainer(packet.itemSets(), packet.stonecutterRecipes());
   }

   @Override
   public void handleLookAt(ClientboundPlayerLookAtPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Vec3 vec3 = packet.getPosition(this.level);
      if (vec3 != null) {
         this.minecraft.player.lookAt(packet.getFromAnchor(), vec3);
      }
   }

   @Override
   public void handleTagQueryPacket(ClientboundTagQueryPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (!this.debugQueryHandler.handleResponse(packet.getTransactionId(), packet.getTag())) {
         LOGGER.debug("Got unhandled response to tag query {}", packet.getTransactionId());
      }
   }

   @Override
   public void handleAwardStats(ClientboundAwardStatsPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      ObjectIterator statsScreen = packet.stats().object2IntEntrySet().iterator();

      while (statsScreen.hasNext()) {
         Entry<Stat<?>> entry = (Entry<Stat<?>>)statsScreen.next();
         Stat<?> stat = (Stat<?>)entry.getKey();
         int i = entry.getIntValue();
         this.minecraft.player.getStats().setValue(this.minecraft.player, stat, i);
      }

      if (this.minecraft.screen instanceof StatsScreen statsScreenx) {
         statsScreenx.onStatsUpdated();
      }
   }

   @Override
   public void handleRecipeBookAdd(ClientboundRecipeBookAddPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      ClientRecipeBook clientRecipeBook = this.minecraft.player.getRecipeBook();
      if (packet.replace()) {
         clientRecipeBook.clear();
      }

      for (ClientboundRecipeBookAddPacket.Entry entry : packet.entries()) {
         clientRecipeBook.add(entry.contents());
         if (entry.highlight()) {
            clientRecipeBook.addHighlight(entry.contents().id());
         }

         if (entry.notification()) {
            RecipeToast.addOrUpdate(this.minecraft.getToastManager(), entry.contents().display());
         }
      }

      this.refreshRecipeBook(clientRecipeBook);
   }

   @Override
   public void handleRecipeBookRemove(ClientboundRecipeBookRemovePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      ClientRecipeBook clientRecipeBook = this.minecraft.player.getRecipeBook();

      for (RecipeDisplayId recipeDisplayId : packet.recipes()) {
         clientRecipeBook.remove(recipeDisplayId);
      }

      this.refreshRecipeBook(clientRecipeBook);
   }

   @Override
   public void handleRecipeBookSettings(ClientboundRecipeBookSettingsPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      ClientRecipeBook clientRecipeBook = this.minecraft.player.getRecipeBook();
      clientRecipeBook.setBookSettings(packet.bookSettings());
      this.refreshRecipeBook(clientRecipeBook);
   }

   private void refreshRecipeBook(ClientRecipeBook recipeBook) {
      recipeBook.rebuildCollections();
      this.searchTrees.updateRecipes(recipeBook, this.level);
      if (this.minecraft.screen instanceof RecipeUpdateListener recipeUpdateListener) {
         recipeUpdateListener.recipesUpdated();
      }
   }

   @Override
   public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.getEntityId());
      if (entity instanceof LivingEntity) {
         Holder<MobEffect> holder = packet.getEffect();
         MobEffectInstance mobEffectInstance = new MobEffectInstance(
            holder,
            packet.getEffectDurationTicks(),
            packet.getEffectAmplifier(),
            packet.isEffectAmbient(),
            packet.isEffectVisible(),
            packet.effectShowsIcon(),
            null
         );
         if (!packet.shouldBlend()) {
            mobEffectInstance.skipBlending();
         }

         ((LivingEntity)entity).forceAddEffect(mobEffectInstance, null);
      }
   }

   private <T> Registry.PendingTags<T> updateTags(ResourceKey<? extends Registry<? extends T>> registryKey, TagNetworkSerialization.NetworkPayload payload) {
      Registry<T> registry = this.registryAccess.lookupOrThrow(registryKey);
      return registry.prepareTagReload(payload.resolve(registry));
   }

   @Override
   public void handleUpdateTags(ClientboundUpdateTagsPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      List<Registry.PendingTags<?>> list = new ArrayList<>(packet.getTags().size());
      boolean bl = this.connection.isMemoryConnection();
      packet.getTags().forEach((resourceKey, networkPayload) -> {
         if (!bl || RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)resourceKey)) {
            list.add(this.updateTags((ResourceKey<? extends Registry<?>>)resourceKey, networkPayload));
         }
      });
      list.forEach(Registry.PendingTags::apply);
      this.fuelValues = FuelValues.vanillaBurnTimes(this.registryAccess, this.enabledFeatures);
      List<ItemStack> list2 = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
      this.searchTrees.updateCreativeTags(list2);
   }

   @Override
   public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket packet) {
   }

   @Override
   public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket packet) {
   }

   @Override
   public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.playerId());
      if (entity == this.minecraft.player) {
         if (this.minecraft.player.shouldShowDeathScreen()) {
            this.minecraft.setScreen(new DeathScreen(packet.message(), this.level.getLevelData().isHardcore(), this.minecraft.player));
         } else {
            this.minecraft.player.respawn();
         }
      }
   }

   @Override
   public void handleChangeDifficulty(ClientboundChangeDifficultyPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.levelData.setDifficulty(packet.difficulty());
      this.levelData.setDifficultyLocked(packet.locked());
   }

   @Override
   public void handleSetCamera(ClientboundSetCameraPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = packet.getEntity(this.level);
      if (entity != null) {
         this.minecraft.setCameraEntity(entity);
      }
   }

   @Override
   public void handleInitializeBorder(ClientboundInitializeBorderPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      WorldBorder worldBorder = this.level.getWorldBorder();
      worldBorder.setCenter(packet.getNewCenterX(), packet.getNewCenterZ());
      long l = packet.getLerpTime();
      if (l > 0L) {
         worldBorder.lerpSizeBetween(packet.getOldSize(), packet.getNewSize(), l, this.level.getGameTime());
      } else {
         worldBorder.setSize(packet.getNewSize());
      }

      worldBorder.setAbsoluteMaxSize(packet.getNewAbsoluteMaxSize());
      worldBorder.setWarningBlocks(packet.getWarningBlocks());
      worldBorder.setWarningTime(packet.getWarningTime());
   }

   @Override
   public void handleSetBorderCenter(ClientboundSetBorderCenterPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.getWorldBorder().setCenter(packet.getNewCenterX(), packet.getNewCenterZ());
   }

   @Override
   public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.getWorldBorder().lerpSizeBetween(packet.getOldSize(), packet.getNewSize(), packet.getLerpTime(), this.level.getGameTime());
   }

   @Override
   public void handleSetBorderSize(ClientboundSetBorderSizePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.getWorldBorder().setSize(packet.getSize());
   }

   @Override
   public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.getWorldBorder().setWarningBlocks(packet.getWarningBlocks());
   }

   @Override
   public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.getWorldBorder().setWarningTime(packet.getWarningDelay());
   }

   @Override
   public void handleTitlesClear(ClientboundClearTitlesPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.gui.clearTitles();
      if (packet.shouldResetTimes()) {
         this.minecraft.gui.resetTitleTimes();
      }
   }

   @Override
   public void handleServerData(ClientboundServerDataPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.serverData != null) {
         this.serverData.motd = packet.motd();
         packet.iconBytes().map(ServerData::validateIcon).ifPresent(this.serverData::setIconBytes);
         ServerList.saveSingleServer(this.serverData);
      }
   }

   @Override
   public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.suggestionsProvider.modifyCustomCompletions(packet.action(), packet.entries());
   }

   @Override
   public void setActionBarText(ClientboundSetActionBarTextPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.gui.setOverlayMessage(packet.text(), false);
   }

   @Override
   public void setTitleText(ClientboundSetTitleTextPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.gui.setTitle(packet.text());
   }

   @Override
   public void setSubtitleText(ClientboundSetSubtitleTextPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.gui.setSubtitle(packet.text());
   }

   @Override
   public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.gui.setTimes(packet.getFadeIn(), packet.getStay(), packet.getFadeOut());
   }

   @Override
   public void handleTabListCustomisation(ClientboundTabListPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.gui.getTabList().setHeader(packet.header().getString().isEmpty() ? null : packet.header());
      this.minecraft.gui.getTabList().setFooter(packet.footer().getString().isEmpty() ? null : packet.footer());
   }

   @Override
   public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (packet.getEntity(this.level) instanceof LivingEntity livingEntity) {
         livingEntity.removeEffectNoUpdate(packet.effect());
      }
   }

   @Override
   public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());

      for (UUID uUID : packet.profileIds()) {
         this.minecraft.getPlayerSocialManager().removePlayer(uUID);
         PlayerInfo playerInfo = this.playerInfoMap.remove(uUID);
         if (playerInfo != null) {
            this.listedPlayers.remove(playerInfo);
         }
      }
   }

   @Override
   public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());

      for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {
         PlayerInfo playerInfo = new PlayerInfo(Objects.requireNonNull(entry.profile()), this.enforcesSecureChat());
         if (this.playerInfoMap.putIfAbsent(entry.profileId(), playerInfo) == null) {
            this.minecraft.getPlayerSocialManager().addPlayer(playerInfo);
         }
      }

      for (ClientboundPlayerInfoUpdatePacket.Entry entryx : packet.entries()) {
         PlayerInfo playerInfo = this.playerInfoMap.get(entryx.profileId());
         if (playerInfo == null) {
            LOGGER.warn("Ignoring player info update for unknown player {} ({})", entryx.profileId(), packet.actions());
         } else {
            for (ClientboundPlayerInfoUpdatePacket.Action action : packet.actions()) {
               this.applyPlayerInfoUpdate(action, entryx, playerInfo);
            }
         }
      }
   }

   private void applyPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket.Action action, ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo) {
      switch (action) {
         case INITIALIZE_CHAT:
            this.initializeChatSession(entry, playerInfo);
            break;
         case UPDATE_GAME_MODE:
            if (playerInfo.getGameMode() != entry.gameMode() && this.minecraft.player != null && this.minecraft.player.getUUID().equals(entry.profileId())) {
               this.minecraft.player.onGameModeChanged(entry.gameMode());
            }

            playerInfo.setGameMode(entry.gameMode());
            break;
         case UPDATE_LISTED:
            if (entry.listed()) {
               this.listedPlayers.add(playerInfo);
            } else {
               this.listedPlayers.remove(playerInfo);
            }
            break;
         case UPDATE_LATENCY:
            playerInfo.setLatency(entry.latency());
            break;
         case UPDATE_DISPLAY_NAME:
            playerInfo.setTabListDisplayName(entry.displayName());
            break;
         case UPDATE_HAT:
            playerInfo.setShowHat(entry.showHat());
            break;
         case UPDATE_LIST_ORDER:
            playerInfo.setTabListOrder(entry.listOrder());
      }
   }

   private void initializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo) {
      GameProfile gameProfile = playerInfo.getProfile();
      SignatureValidator signatureValidator = this.minecraft.services().profileKeySignatureValidator();
      if (signatureValidator == null) {
         LOGGER.warn("Ignoring chat session from {} due to missing Services public key", gameProfile.name());
         playerInfo.clearChatSession(this.enforcesSecureChat());
      } else {
         RemoteChatSession.Data data = entry.chatSession();
         if (data != null) {
            try {
               RemoteChatSession remoteChatSession = data.validate(gameProfile, signatureValidator);
               playerInfo.setChatSession(remoteChatSession);
            } catch (ProfilePublicKey.ValidationException var7) {
               LOGGER.error("Failed to validate profile key for player: '{}'", gameProfile.name(), var7);
               playerInfo.clearChatSession(this.enforcesSecureChat());
            }
         } else {
            playerInfo.clearChatSession(this.enforcesSecureChat());
         }
      }
   }

   private boolean enforcesSecureChat() {
      return this.minecraft.services().canValidateProfileKeys() && this.serverEnforcesSecureChat;
   }

   @Override
   public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Player player = this.minecraft.player;
      player.getAbilities().flying = packet.isFlying();
      player.getAbilities().instabuild = packet.canInstabuild();
      player.getAbilities().invulnerable = packet.isInvulnerable();
      player.getAbilities().mayfly = packet.canFly();
      player.getAbilities().setFlyingSpeed(packet.getFlyingSpeed());
      player.getAbilities().setWalkingSpeed(packet.getWalkingSpeed());
   }

   @Override
   public void handleSoundEvent(ClientboundSoundPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft
         .level
         .playSeededSound(
            this.minecraft.player,
            packet.getX(),
            packet.getY(),
            packet.getZ(),
            packet.getSound(),
            packet.getSource(),
            packet.getVolume(),
            packet.getPitch(),
            packet.getSeed()
         );
   }

   @Override
   public void handleSoundEntityEvent(ClientboundSoundEntityPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.getId());
      if (entity != null) {
         this.minecraft
            .level
            .playSeededSound(this.minecraft.player, entity, packet.getSound(), packet.getSource(), packet.getVolume(), packet.getPitch(), packet.getSeed());
      }
   }

   @Override
   public void handleBossUpdate(ClientboundBossEventPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.gui.getBossOverlay().update(packet);
   }

   @Override
   public void handleItemCooldown(ClientboundCooldownPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (packet.duration() == 0) {
         this.minecraft.player.getCooldowns().removeCooldown(packet.cooldownGroup());
      } else {
         this.minecraft.player.getCooldowns().addCooldown(packet.cooldownGroup(), packet.duration());
      }
   }

   @Override
   public void handleMoveVehicle(ClientboundMoveVehiclePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.minecraft.player.getRootVehicle();
      if (entity != this.minecraft.player && entity.isLocalInstanceAuthoritative()) {
         Vec3 vec3 = packet.position();
         Vec3 vec32;
         if (entity.isInterpolating()) {
            vec32 = entity.getInterpolation().position();
         } else {
            vec32 = entity.position();
         }

         if (vec3.distanceTo(vec32) > 1.0E-5F) {
            if (entity.isInterpolating()) {
               entity.getInterpolation().cancel();
            }

            entity.absSnapTo(vec3.x(), vec3.y(), vec3.z(), packet.yRot(), packet.xRot());
         }

         this.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
      }
   }

   @Override
   public void handleOpenBook(ClientboundOpenBookPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      ItemStack itemStack = this.minecraft.player.getItemInHand(packet.getHand());
      BookViewScreen.BookAccess bookAccess = BookViewScreen.BookAccess.fromItem(itemStack);
      if (bookAccess != null) {
         this.minecraft.setScreen(new BookViewScreen(bookAccess));
      }
   }

   @Override
   public void handleCustomPayload(CustomPacketPayload payload) {
      this.handleUnknownCustomPayload(payload);
   }

   private void handleUnknownCustomPayload(CustomPacketPayload packet) {
      LOGGER.warn("Unknown custom packet payload: {}", packet.type().id());
   }

   @Override
   public void handleAddObjective(ClientboundSetObjectivePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      String string = packet.getObjectiveName();
      if (packet.getMethod() == 0) {
         this.scoreboard
            .addObjective(string, ObjectiveCriteria.DUMMY, packet.getDisplayName(), packet.getRenderType(), false, packet.getNumberFormat().orElse(null));
      } else {
         Objective objective = this.scoreboard.getObjective(string);
         if (objective != null) {
            if (packet.getMethod() == 1) {
               this.scoreboard.removeObjective(objective);
            } else if (packet.getMethod() == 2) {
               objective.setRenderType(packet.getRenderType());
               objective.setDisplayName(packet.getDisplayName());
               objective.setNumberFormat(packet.getNumberFormat().orElse(null));
            }
         }
      }
   }

   @Override
   public void handleSetScore(ClientboundSetScorePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      String string = packet.objectiveName();
      ScoreHolder scoreHolder = ScoreHolder.forNameOnly(packet.owner());
      Objective objective = this.scoreboard.getObjective(string);
      if (objective != null) {
         ScoreAccess scoreAccess = this.scoreboard.getOrCreatePlayerScore(scoreHolder, objective, true);
         scoreAccess.set(packet.score());
         scoreAccess.display(packet.display().orElse(null));
         scoreAccess.numberFormatOverride(packet.numberFormat().orElse(null));
      } else {
         LOGGER.warn("Received packet for unknown scoreboard objective: {}", string);
      }
   }

   @Override
   public void handleResetScore(ClientboundResetScorePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      String string = packet.objectiveName();
      ScoreHolder scoreHolder = ScoreHolder.forNameOnly(packet.owner());
      if (string == null) {
         this.scoreboard.resetAllPlayerScores(scoreHolder);
      } else {
         Objective objective = this.scoreboard.getObjective(string);
         if (objective != null) {
            this.scoreboard.resetSinglePlayerScore(scoreHolder, objective);
         } else {
            LOGGER.warn("Received packet for unknown scoreboard objective: {}", string);
         }
      }
   }

   @Override
   public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      String string = packet.getObjectiveName();
      Objective objective = string == null ? null : this.scoreboard.getObjective(string);
      this.scoreboard.setDisplayObjective(packet.getSlot(), objective);
   }

   @Override
   public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      ClientboundSetPlayerTeamPacket.Action action = packet.getTeamAction();
      PlayerTeam playerTeam;
      if (action == ClientboundSetPlayerTeamPacket.Action.ADD) {
         playerTeam = this.scoreboard.addPlayerTeam(packet.getName());
      } else {
         playerTeam = this.scoreboard.getPlayerTeam(packet.getName());
         if (playerTeam == null) {
            LOGGER.warn(
               "Received packet for unknown team {}: team action: {}, player action: {}",
               new Object[]{packet.getName(), packet.getTeamAction(), packet.getPlayerAction()}
            );
            return;
         }
      }

      Optional<ClientboundSetPlayerTeamPacket.Parameters> optional = packet.getParameters();
      optional.ifPresent(parameters -> {
         playerTeam.setDisplayName(parameters.getDisplayName());
         playerTeam.setColor(parameters.getColor());
         playerTeam.unpackOptions(parameters.getOptions());
         playerTeam.setNameTagVisibility(parameters.getNametagVisibility());
         playerTeam.setCollisionRule(parameters.getCollisionRule());
         playerTeam.setPlayerPrefix(parameters.getPlayerPrefix());
         playerTeam.setPlayerSuffix(parameters.getPlayerSuffix());
      });
      ClientboundSetPlayerTeamPacket.Action action2 = packet.getPlayerAction();
      if (action2 == ClientboundSetPlayerTeamPacket.Action.ADD) {
         for (String string : packet.getPlayers()) {
            this.scoreboard.addPlayerToTeam(string, playerTeam);
         }
      } else if (action2 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
         for (String string : packet.getPlayers()) {
            this.scoreboard.removePlayerFromTeam(string, playerTeam);
         }
      }

      if (action == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
         this.scoreboard.removePlayerTeam(playerTeam);
      }
   }

   @Override
   public void handleParticleEvent(ClientboundLevelParticlesPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (packet.getCount() == 0) {
         double d = packet.getMaxSpeed() * packet.getXDist();
         double e = packet.getMaxSpeed() * packet.getYDist();
         double f = packet.getMaxSpeed() * packet.getZDist();

         try {
            this.level.addParticle(packet.getParticle(), packet.isOverrideLimiter(), packet.alwaysShow(), packet.getX(), packet.getY(), packet.getZ(), d, e, f);
         } catch (Throwable var17) {
            LOGGER.warn("Could not spawn particle effect {}", packet.getParticle());
         }
      } else {
         for (int i = 0; i < packet.getCount(); i++) {
            double g = this.random.nextGaussian() * packet.getXDist();
            double h = this.random.nextGaussian() * packet.getYDist();
            double j = this.random.nextGaussian() * packet.getZDist();
            double k = this.random.nextGaussian() * packet.getMaxSpeed();
            double l = this.random.nextGaussian() * packet.getMaxSpeed();
            double m = this.random.nextGaussian() * packet.getMaxSpeed();

            try {
               this.level
                  .addParticle(
                     packet.getParticle(), packet.isOverrideLimiter(), packet.alwaysShow(), packet.getX() + g, packet.getY() + h, packet.getZ() + j, k, l, m
                  );
            } catch (Throwable var16) {
               LOGGER.warn("Could not spawn particle effect {}", packet.getParticle());
               return;
            }
         }
      }
   }

   @Override
   public void handleUpdateAttributes(ClientboundUpdateAttributesPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.getEntityId());
      if (entity != null) {
         if (!(entity instanceof LivingEntity)) {
            throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + entity + ")");
         } else {
            AttributeMap attributeMap = ((LivingEntity)entity).getAttributes();

            for (ClientboundUpdateAttributesPacket.AttributeSnapshot attributeSnapshot : packet.getValues()) {
               AttributeInstance attributeInstance = attributeMap.getInstance(attributeSnapshot.attribute());
               if (attributeInstance == null) {
                  LOGGER.warn("Entity {} does not have attribute {}", entity, attributeSnapshot.attribute().getRegisteredName());
               } else {
                  attributeInstance.setBaseValue(attributeSnapshot.base());
                  attributeInstance.removeModifiers();

                  for (AttributeModifier attributeModifier : attributeSnapshot.modifiers()) {
                     attributeInstance.addTransientModifier(attributeModifier);
                  }
               }
            }
         }
      }
   }

   @Override
   public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      AbstractContainerMenu abstractContainerMenu = this.minecraft.player.containerMenu;
      if (abstractContainerMenu.containerId == packet.containerId()) {
         if (this.minecraft.screen instanceof RecipeUpdateListener recipeUpdateListener) {
            recipeUpdateListener.fillGhostRecipe(packet.recipeDisplay());
         }
      }
   }

   @Override
   public void handleLightUpdatePacket(ClientboundLightUpdatePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      int i = packet.getX();
      int j = packet.getZ();
      ClientboundLightUpdatePacketData clientboundLightUpdatePacketData = packet.getLightData();
      this.level.queueLightUpdate(() -> this.applyLightData(i, j, clientboundLightUpdatePacketData, true));
   }

   private void applyLightData(int x, int z, ClientboundLightUpdatePacketData data, boolean update) {
      LevelLightEngine levelLightEngine = this.level.getChunkSource().getLightEngine();
      BitSet bitSet = data.getSkyYMask();
      BitSet bitSet2 = data.getEmptySkyYMask();
      Iterator<byte[]> iterator = data.getSkyUpdates().iterator();
      this.readSectionList(x, z, levelLightEngine, LightLayer.SKY, bitSet, bitSet2, iterator, update);
      BitSet bitSet3 = data.getBlockYMask();
      BitSet bitSet4 = data.getEmptyBlockYMask();
      Iterator<byte[]> iterator2 = data.getBlockUpdates().iterator();
      this.readSectionList(x, z, levelLightEngine, LightLayer.BLOCK, bitSet3, bitSet4, iterator2, update);
      levelLightEngine.setLightEnabled(new ChunkPos(x, z), true);
   }

   @Override
   public void handleMerchantOffers(ClientboundMerchantOffersPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      AbstractContainerMenu abstractContainerMenu = this.minecraft.player.containerMenu;
      if (packet.getContainerId() == abstractContainerMenu.containerId && abstractContainerMenu instanceof MerchantMenu merchantMenu) {
         merchantMenu.setOffers(packet.getOffers());
         merchantMenu.setXp(packet.getVillagerXp());
         merchantMenu.setMerchantLevel(packet.getVillagerLevel());
         merchantMenu.setShowProgressBar(packet.showProgress());
         merchantMenu.setCanRestock(packet.canRestock());
      }
   }

   @Override
   public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.serverChunkRadius = packet.getRadius();
      this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
      this.level.getChunkSource().updateViewRadius(packet.getRadius());
   }

   @Override
   public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.serverSimulationDistance = packet.simulationDistance();
      this.level.setServerSimulationDistance(this.serverSimulationDistance);
   }

   @Override
   public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.getChunkSource().updateViewCenter(packet.getX(), packet.getZ());
   }

   @Override
   public void handleBlockChangedAck(ClientboundBlockChangedAckPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.level.handleBlockChangedAck(packet.sequence());
   }

   @Override
   public void handleBundlePacket(ClientboundBundlePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());

      for (Packet<? super ClientGamePacketListener> packet2 : packet.subPackets()) {
         packet2.handle(this);
      }
   }

   @Override
   public void handleProjectilePowerPacket(ClientboundProjectilePowerPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.level.getEntity(packet.getId()) instanceof AbstractHurtingProjectile abstractHurtingProjectile) {
         abstractHurtingProjectile.accelerationPower = packet.getAccelerationPower();
      }
   }

   @Override
   public void handleChunkBatchStart(ClientboundChunkBatchStartPacket packet) {
      this.chunkBatchSizeCalculator.onBatchStart();
   }

   @Override
   public void handleChunkBatchFinished(ClientboundChunkBatchFinishedPacket packet) {
      this.chunkBatchSizeCalculator.onBatchFinished(packet.batchSize());
      this.send(new ServerboundChunkBatchReceivedPacket(this.chunkBatchSizeCalculator.getDesiredChunksPerTick()));
   }

   @Override
   public void handleDebugSample(ClientboundDebugSamplePacket packet) {
      this.minecraft.getDebugOverlay().logRemoteSample(packet.sample(), packet.debugSampleType());
   }

   @Override
   public void handlePongResponse(ClientboundPongResponsePacket packet) {
      this.pingDebugMonitor.onPongReceived(packet);
   }

   @Override
   public void handleTestInstanceBlockStatus(ClientboundTestInstanceBlockStatus status) {
      PacketUtils.ensureRunningOnSameThread(status, this, this.minecraft.packetProcessor());
      if (this.minecraft.screen instanceof TestInstanceBlockEditScreen testInstanceBlockEditScreen) {
         testInstanceBlockEditScreen.setStatus(status.status(), status.size());
      }
   }

   @Override
   public void handleWaypoint(ClientboundTrackedWaypointPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      packet.apply(this.waypointManager);
   }

   @Override
   public void handleDebugChunkValue(ClientboundDebugChunkValuePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.debugSubscriber.updateChunk(this.level.getGameTime(), packet.chunkPos(), packet.update());
   }

   @Override
   public void handleDebugBlockValue(ClientboundDebugBlockValuePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.debugSubscriber.updateBlock(this.level.getGameTime(), packet.blockPos(), packet.update());
   }

   @Override
   public void handleDebugEntityValue(ClientboundDebugEntityValuePacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      Entity entity = this.level.getEntity(packet.entityId());
      if (entity != null) {
         this.debugSubscriber.updateEntity(this.level.getGameTime(), entity, packet.update());
      }
   }

   @Override
   public void handleDebugEvent(ClientboundDebugEventPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.debugSubscriber.pushEvent(this.level.getGameTime(), packet.event());
   }

   @Override
   public void handleGameTestHighlightPos(ClientboundGameTestHighlightPosPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.minecraft.levelRenderer.gameTestBlockHighlightRenderer.highlightPos(packet.absolutePos(), packet.relativePos());
   }

   private void readSectionList(
      int x, int z, LevelLightEngine lightEngine, LightLayer lightLayer, BitSet skyYMask, BitSet emptySkyYMask, Iterator<byte[]> skyUpdates, boolean update
   ) {
      for (int i = 0; i < lightEngine.getLightSectionCount(); i++) {
         int j = lightEngine.getMinLightSection() + i;
         boolean bl = skyYMask.get(i);
         boolean bl2 = emptySkyYMask.get(i);
         if (bl || bl2) {
            lightEngine.queueSectionData(lightLayer, SectionPos.of(x, j, z), bl ? new DataLayer((byte[])skyUpdates.next().clone()) : new DataLayer());
            if (update) {
               this.level.setSectionDirtyWithNeighbors(x, j, z);
            }
         }
      }
   }

   public Connection getConnection() {
      return this.connection;
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected() && !this.closed;
   }

   public Collection<PlayerInfo> getListedOnlinePlayers() {
      return this.listedPlayers;
   }

   public Collection<PlayerInfo> getOnlinePlayers() {
      return this.playerInfoMap.values();
   }

   public Collection<UUID> getOnlinePlayerIds() {
      return this.playerInfoMap.keySet();
   }

   @Nullable
   public PlayerInfo getPlayerInfo(UUID uuid) {
      return this.playerInfoMap.get(uuid);
   }

   @Nullable
   public PlayerInfo getPlayerInfo(String name) {
      for (PlayerInfo playerInfo : this.playerInfoMap.values()) {
         if (playerInfo.getProfile().name().equals(name)) {
            return playerInfo;
         }
      }

      return null;
   }

   public Map<UUID, PlayerInfo> getSeenPlayers() {
      return this.seenPlayers;
   }

   @Nullable
   public PlayerInfo getPlayerInfoIgnoreCase(String playerName) {
      for (PlayerInfo playerInfo : this.playerInfoMap.values()) {
         if (playerInfo.getProfile().name().equalsIgnoreCase(playerName)) {
            return playerInfo;
         }
      }

      return null;
   }

   public GameProfile getLocalGameProfile() {
      return this.localGameProfile;
   }

   public ClientAdvancements getAdvancements() {
      return this.advancements;
   }

   public CommandDispatcher<ClientSuggestionProvider> getCommands() {
      return this.commands;
   }

   public ClientLevel getLevel() {
      return this.level;
   }

   public DebugQueryHandler getDebugQueryHandler() {
      return this.debugQueryHandler;
   }

   public UUID getId() {
      return this.id;
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public RegistryAccess.Frozen registryAccess() {
      return this.registryAccess;
   }

   public void markMessageAsProcessed(MessageSignature signature, boolean acknowledged) {
      if (this.lastSeenMessages.addPending(signature, acknowledged) && this.lastSeenMessages.offset() > 64) {
         this.sendChatAcknowledgement();
      }
   }

   private void sendChatAcknowledgement() {
      int i = this.lastSeenMessages.getAndClearOffset();
      if (i > 0) {
         this.send(new ServerboundChatAckPacket(i));
      }
   }

   public void sendChat(String message) {
      Instant instant = Instant.now();
      long l = Crypt.SaltSupplier.getLong();
      LastSeenMessagesTracker.Update update = this.lastSeenMessages.generateAndApplyUpdate();
      MessageSignature messageSignature = this.signedMessageEncoder.pack(new SignedMessageBody(message, instant, l, update.lastSeen()));
      this.send(new ServerboundChatPacket(message, instant, l, messageSignature, update.update()));
   }

   public void sendCommand(String command) {
      SignableCommand<ClientSuggestionProvider> signableCommand = SignableCommand.of(this.commands.parse(command, this.suggestionsProvider));
      if (signableCommand.arguments().isEmpty()) {
         this.send(new ServerboundChatCommandPacket(command));
      } else {
         Instant instant = Instant.now();
         long l = Crypt.SaltSupplier.getLong();
         LastSeenMessagesTracker.Update update = this.lastSeenMessages.generateAndApplyUpdate();
         ArgumentSignatures argumentSignatures = ArgumentSignatures.signCommand(signableCommand, string -> {
            SignedMessageBody signedMessageBody = new SignedMessageBody(string, instant, l, update.lastSeen());
            return this.signedMessageEncoder.pack(signedMessageBody);
         });
         this.send(new ServerboundChatCommandSignedPacket(command, instant, l, argumentSignatures, update.update()));
      }
   }

   public void sendUnattendedCommand(String command, @Nullable Screen previousScreen) {
      switch (this.verifyCommand(command)) {
         case NO_ISSUES:
            this.send(new ServerboundChatCommandPacket(command));
            this.minecraft.setScreen(previousScreen);
            break;
         case PARSE_ERRORS:
            this.openCommandSendConfirmationWindow(command, "multiplayer.confirm_command.parse_errors", previousScreen);
            break;
         case SIGNATURE_REQUIRED:
            this.openSignedCommandSendConfirmationWindow(command, "multiplayer.confirm_command.signature_required", previousScreen);
            break;
         case PERMISSIONS_REQUIRED:
            this.openCommandSendConfirmationWindow(command, "multiplayer.confirm_command.permissions_required", previousScreen);
      }
   }

   private ClientPacketListener.CommandCheckResult verifyCommand(String command) {
      ParseResults<ClientSuggestionProvider> parseResults = this.commands.parse(command, this.suggestionsProvider);
      if (!isValidCommand(parseResults)) {
         return ClientPacketListener.CommandCheckResult.PARSE_ERRORS;
      } else if (SignableCommand.hasSignableArguments(parseResults)) {
         return ClientPacketListener.CommandCheckResult.SIGNATURE_REQUIRED;
      } else {
         ParseResults<ClientSuggestionProvider> parseResults2 = this.commands.parse(command, this.restrictedSuggestionsProvider);
         return !isValidCommand(parseResults2)
            ? ClientPacketListener.CommandCheckResult.PERMISSIONS_REQUIRED
            : ClientPacketListener.CommandCheckResult.NO_ISSUES;
      }
   }

   private static boolean isValidCommand(ParseResults<?> parseResults) {
      return !parseResults.getReader().canRead() && parseResults.getExceptions().isEmpty() && parseResults.getContext().getLastChild().getCommand() != null;
   }

   private void openSendConfirmationWindow(String command, String titleKey, Component buttonText, Runnable onConfirm) {
      Screen screen = this.minecraft.screen;
      this.minecraft
         .setScreen(
            new ConfirmScreen(
               bl -> {
                  if (bl) {
                     onConfirm.run();
                  } else {
                     this.minecraft.setScreen(screen);
                  }
               },
               COMMAND_SEND_CONFIRM_TITLE,
               Component.translatable(titleKey, Component.literal(command).withStyle(ChatFormatting.YELLOW)),
               buttonText,
               screen != null ? CommonComponents.GUI_BACK : CommonComponents.GUI_CANCEL
            )
         );
   }

   private void openCommandSendConfirmationWindow(String command, String titleKey, @Nullable Screen previousScreen) {
      this.openSendConfirmationWindow(command, titleKey, BUTTON_RUN_COMMAND, () -> {
         this.send(new ServerboundChatCommandPacket(command));
         this.minecraft.setScreen(previousScreen);
      });
   }

   private void openSignedCommandSendConfirmationWindow(String command, String titleKey, @Nullable Screen previousScreen) {
      boolean bl = previousScreen == null && this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer());
      this.openSendConfirmationWindow(command, titleKey, bl ? BUTTON_SUGGEST_COMMAND : CommonComponents.GUI_COPY_TO_CLIPBOARD, () -> {
         if (bl) {
            this.minecraft.openChatScreen(ChatComponent.ChatMethod.COMMAND);
            if (this.minecraft.screen instanceof ChatScreen chatScreen) {
               chatScreen.insertText(command, false);
            }
         } else {
            this.minecraft.keyboardHandler.setClipboard("/" + command);
            this.minecraft.setScreen(previousScreen);
         }
      });
   }

   public void broadcastClientInformation(ClientInformation information) {
      if (!information.equals(this.remoteClientInformation)) {
         this.send(new ServerboundClientInformationPacket(information));
         this.remoteClientInformation = information;
      }
   }

   @Override
   public void tick() {
      if (this.chatSession != null && this.minecraft.getProfileKeyPairManager().shouldRefreshKeyPair()) {
         this.prepareKeyPair();
      }

      if (this.keyPairFuture != null && this.keyPairFuture.isDone()) {
         this.keyPairFuture.join().ifPresent(this::setKeyPair);
         this.keyPairFuture = null;
      }

      this.sendDeferredPackets();
      if (this.minecraft.getDebugOverlay().showNetworkCharts()) {
         this.pingDebugMonitor.tick();
      }

      if (this.level != null) {
         this.debugSubscriber.tick(this.level.getGameTime());
      }

      this.telemetryManager.tick();
      if (this.levelLoadTracker != null) {
         this.levelLoadTracker.tickClientLoad();
         if (this.levelLoadTracker.isLevelReady()) {
            this.notifyPlayerLoaded();
            this.levelLoadTracker = null;
         }
      }
   }

   private void notifyPlayerLoaded() {
      if (!this.hasClientLoaded()) {
         this.connection.send(new ServerboundPlayerLoadedPacket());
         this.setClientLoaded(true);
      }
   }

   public void prepareKeyPair() {
      this.keyPairFuture = this.minecraft.getProfileKeyPairManager().prepareKeyPair();
   }

   private void setKeyPair(ProfileKeyPair keyPair) {
      if (this.minecraft.isLocalPlayer(this.localGameProfile.id())) {
         if (this.chatSession == null || !this.chatSession.keyPair().equals(keyPair)) {
            this.chatSession = LocalChatSession.create(keyPair);
            this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.id());
            this.send(new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData()));
         }
      }
   }

   @Override
   protected DialogConnectionAccess createDialogAccess() {
      return new ClientCommonPacketListenerImpl.CommonDialogAccess() {
         @Override
         public void runCommand(String command, @Nullable Screen previousScreen) {
            ClientPacketListener.this.sendUnattendedCommand(command, previousScreen);
         }
      };
   }

   @Nullable
   public ServerData getServerData() {
      return this.serverData;
   }

   public FeatureFlagSet enabledFeatures() {
      return this.enabledFeatures;
   }

   public boolean isFeatureEnabled(FeatureFlagSet enabledFeatures) {
      return enabledFeatures.isSubsetOf(this.enabledFeatures());
   }

   public Scoreboard scoreboard() {
      return this.scoreboard;
   }

   public PotionBrewing potionBrewing() {
      return this.potionBrewing;
   }

   public FuelValues fuelValues() {
      return this.fuelValues;
   }

   public void updateSearchTrees() {
      this.searchTrees.rebuildAfterLanguageChange();
   }

   public SessionSearchTrees searchTrees() {
      return this.searchTrees;
   }

   public void registerForCleaning(CacheSlot<?, ?> cacheSlot) {
      this.cacheSlots.add(new WeakReference<>(cacheSlot));
   }

   public HashedPatchMap.HashGenerator decoratedHashOpsGenenerator() {
      return this.decoratedHashOpsGenerator;
   }

   public ClientWaypointManager getWaypointManager() {
      return this.waypointManager;
   }

   public DebugValueAccess createDebugValueAccess() {
      return this.debugSubscriber.createDebugValueAccess(this.level);
   }

   public boolean hasClientLoaded() {
      return this.clientLoaded;
   }

   private void setClientLoaded(boolean clientLoaded) {
      this.clientLoaded = clientLoaded;
   }

   @Environment(EnvType.CLIENT)
   static enum CommandCheckResult {
      NO_ISSUES,
      PARSE_ERRORS,
      SIGNATURE_REQUIRED,
      PERMISSIONS_REQUIRED;
   }
}
