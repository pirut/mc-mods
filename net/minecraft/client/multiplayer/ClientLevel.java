package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.EndFlashState;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.sounds.DirectionalSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientLevel extends Level implements CacheSlot.Cleaner<ClientLevel> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Component DEFAULT_QUIT_MESSAGE = Component.translatable("multiplayer.status.quitting");
   private static final double FLUID_PARTICLE_SPAWN_OFFSET = 0.05;
   private static final int NORMAL_LIGHT_UPDATES_PER_FRAME = 10;
   private static final int LIGHT_UPDATE_QUEUE_SIZE_THRESHOLD = 1000;
   final EntityTickList tickingEntities = new EntityTickList();
   private final TransientEntitySectionManager<Entity> entityStorage = new TransientEntitySectionManager<>(Entity.class, new ClientLevel.EntityCallbacks());
   private final ClientPacketListener connection;
   private final LevelRenderer levelRenderer;
   private final LevelEventHandler levelEventHandler;
   private final ClientLevel.ClientLevelData clientLevelData;
   private final TickRateManager tickRateManager;
   @Nullable
   private final EndFlashState endFlashState;
   private final Minecraft minecraft = Minecraft.getInstance();
   final List<AbstractClientPlayer> players = Lists.newArrayList();
   final List<EnderDragonPart> dragonParts = Lists.newArrayList();
   private final Map<MapId, MapItemSavedData> mapData = Maps.newHashMap();
   private int skyFlashTime;
   private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = Util.make(
      new Object2ObjectArrayMap(3),
      object2ObjectArrayMap -> {
         object2ObjectArrayMap.put(
            BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache(blockPos -> this.calculateBlockTint(blockPos, BiomeColors.GRASS_COLOR_RESOLVER))
         );
         object2ObjectArrayMap.put(
            BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache(blockPos -> this.calculateBlockTint(blockPos, BiomeColors.FOLIAGE_COLOR_RESOLVER))
         );
         object2ObjectArrayMap.put(
            BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER, new BlockTintCache(blockPos -> this.calculateBlockTint(blockPos, BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER))
         );
         object2ObjectArrayMap.put(
            BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache(blockPos -> this.calculateBlockTint(blockPos, BiomeColors.WATER_COLOR_RESOLVER))
         );
      }
   );
   private final ClientChunkCache chunkSource;
   private final Deque<Runnable> lightUpdateQueue = Queues.newArrayDeque();
   private int serverSimulationDistance;
   private final BlockStatePredictionHandler blockStatePredictionHandler = new BlockStatePredictionHandler();
   private final Set<BlockEntity> globallyRenderedBlockEntities = new ReferenceOpenHashSet();
   private final ClientExplosionTracker explosionTracker = new ClientExplosionTracker();
   private final WorldBorder worldBorder = new WorldBorder();
   private final EnvironmentAttributeSystem environmentAttributes;
   private final int seaLevel;
   private boolean tickDayTime;
   private static final Set<Item> MARKER_PARTICLE_ITEMS = Set.of(Items.BARRIER, Items.LIGHT);

   public void handleBlockChangedAck(int sequence) {
      if (SharedConstants.DEBUG_BLOCK_BREAK) {
         LOGGER.debug("ACK {}", sequence);
      }

      this.blockStatePredictionHandler.endPredictionsUpTo(sequence, this);
   }

   @Override
   public void onBlockEntityAdded(BlockEntity entity) {
      BlockEntityRenderer<BlockEntity, ?> blockEntityRenderer = this.minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
      if (blockEntityRenderer != null && blockEntityRenderer.shouldRenderOffScreen()) {
         this.globallyRenderedBlockEntities.add(entity);
      }
   }

   public Set<BlockEntity> getGloballyRenderedBlockEntities() {
      return this.globallyRenderedBlockEntities;
   }

   public void setServerVerifiedBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
      if (!this.blockStatePredictionHandler.updateKnownServerState(pos, state)) {
         super.setBlock(pos, state, flags, 512);
      }
   }

   public void syncBlockState(BlockPos pos, BlockState state, Vec3 playerPos) {
      BlockState blockState = this.getBlockState(pos);
      if (blockState != state) {
         this.setBlock(pos, state, 19);
         Player player = this.minecraft.player;
         if (this == player.level() && player.isColliding(pos, state)) {
            player.absSnapTo(playerPos.x, playerPos.y, playerPos.z);
         }
      }
   }

   BlockStatePredictionHandler getBlockStatePredictionHandler() {
      return this.blockStatePredictionHandler;
   }

   @Override
   public boolean setBlock(BlockPos pos, BlockState state, @Block.UpdateFlags int flags, int recursionLeft) {
      if (this.blockStatePredictionHandler.isPredicting()) {
         BlockState blockState = this.getBlockState(pos);
         boolean bl = super.setBlock(pos, state, flags, recursionLeft);
         if (bl) {
            this.blockStatePredictionHandler.retainKnownServerState(pos, blockState, this.minecraft.player);
         }

         return bl;
      } else {
         return super.setBlock(pos, state, flags, recursionLeft);
      }
   }

   public ClientLevel(
      ClientPacketListener connection,
      ClientLevel.ClientLevelData levelData,
      ResourceKey<Level> dimension,
      Holder<DimensionType> dimensionTypeRegistration,
      int viewDistance,
      int serverSimulationDistance,
      LevelRenderer levelRenderer,
      boolean isDebug,
      long biomeZoomSeed,
      int seaLevel
   ) {
      super(levelData, dimension, connection.registryAccess(), dimensionTypeRegistration, true, isDebug, biomeZoomSeed, 1000000);
      this.connection = connection;
      this.chunkSource = new ClientChunkCache(this, viewDistance);
      this.tickRateManager = new TickRateManager();
      this.clientLevelData = levelData;
      this.levelRenderer = levelRenderer;
      this.seaLevel = seaLevel;
      this.levelEventHandler = new LevelEventHandler(this.minecraft, this);
      this.endFlashState = dimensionTypeRegistration.value().hasEndFlashes() ? new EndFlashState() : null;
      this.setRespawnData(LevelData.RespawnData.of(dimension, new BlockPos(8, 64, 8), 0.0F, 0.0F));
      this.serverSimulationDistance = serverSimulationDistance;
      this.environmentAttributes = this.addEnvironmentAttributeLayers(EnvironmentAttributeSystem.builder()).build();
      this.updateSkyBrightness();
      if (this.canHaveWeather()) {
         this.prepareWeather();
      }
   }

   private EnvironmentAttributeSystem.Builder addEnvironmentAttributeLayers(EnvironmentAttributeSystem.Builder builder) {
      builder.addDefaultLayers(this);
      int i = ARGB.color(204, 204, 255);
      builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR, (integer, j) -> this.getSkyFlashTime() > 0 ? ARGB.srgbLerp(0.22F, integer, i) : integer);
      builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR, (float_, ix) -> this.getSkyFlashTime() > 0 ? 1.0F : float_);
      return builder;
   }

   public void queueLightUpdate(Runnable task) {
      this.lightUpdateQueue.add(task);
   }

   public void pollLightUpdates() {
      int i = this.lightUpdateQueue.size();
      int j = i < 1000 ? Math.max(10, i / 10) : i;

      for (int k = 0; k < j; k++) {
         Runnable runnable = this.lightUpdateQueue.poll();
         if (runnable == null) {
            break;
         }

         runnable.run();
      }
   }

   @Nullable
   public EndFlashState endFlashState() {
      return this.endFlashState;
   }

   public void tick(BooleanSupplier hasTimeLeft) {
      this.updateSkyBrightness();
      if (this.tickRateManager().runsNormally()) {
         this.getWorldBorder().tick();
         this.tickTime();
      }

      if (this.skyFlashTime > 0) {
         this.setSkyFlashTime(this.skyFlashTime - 1);
      }

      if (this.endFlashState != null) {
         this.endFlashState.tick(this.getGameTime());
         if (this.endFlashState.flashStartedThisTick() && !(this.minecraft.screen instanceof WinScreen)) {
            this.minecraft
               .getSoundManager()
               .playDelayed(
                  new DirectionalSoundInstance(
                     SoundEvents.WEATHER_END_FLASH,
                     SoundSource.WEATHER,
                     this.random,
                     this.minecraft.gameRenderer.getMainCamera(),
                     this.endFlashState.getXAngle(),
                     this.endFlashState.getYAngle()
                  ),
                  30
               );
         }
      }

      this.explosionTracker.tick(this);

      try (Zone zone = Profiler.get().zone("blocks")) {
         this.chunkSource.tick(hasTimeLeft, true);
      }

      JvmProfiler.INSTANCE.onClientTick(this.minecraft.getFps());
      this.environmentAttributes().invalidateTickCache();
   }

   private void tickTime() {
      this.clientLevelData.setGameTime(this.clientLevelData.getGameTime() + 1L);
      if (this.tickDayTime) {
         this.clientLevelData.setDayTime(this.clientLevelData.getDayTime() + 1L);
      }
   }

   public void setTimeFromServer(long gameTime, long dayTime, boolean tickDayTime) {
      this.clientLevelData.setGameTime(gameTime);
      this.clientLevelData.setDayTime(dayTime);
      this.tickDayTime = tickDayTime;
   }

   public Iterable<Entity> entitiesForRendering() {
      return this.getEntities().getAll();
   }

   public void tickEntities() {
      this.tickingEntities.forEach(entity -> {
         if (!entity.isRemoved() && !entity.isPassenger() && !this.tickRateManager.isEntityFrozen(entity)) {
            this.guardEntityTick(this::tickNonPassenger, entity);
         }
      });
   }

   public boolean isTickingEntity(Entity entity) {
      return this.tickingEntities.contains(entity);
   }

   @Override
   public boolean shouldTickDeath(Entity entity) {
      return entity.chunkPosition().getChessboardDistance(this.minecraft.player.chunkPosition()) <= this.serverSimulationDistance;
   }

   public void tickNonPassenger(Entity entity) {
      entity.setOldPosAndRot();
      entity.tickCount++;
      Profiler.get().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
      entity.tick();
      Profiler.get().pop();

      for (Entity entity2 : entity.getPassengers()) {
         this.tickPassenger(entity, entity2);
      }
   }

   private void tickPassenger(Entity mount, Entity rider) {
      if (rider.isRemoved() || rider.getVehicle() != mount) {
         rider.stopRiding();
      } else if (rider instanceof Player || this.tickingEntities.contains(rider)) {
         rider.setOldPosAndRot();
         rider.tickCount++;
         rider.rideTick();

         for (Entity entity : rider.getPassengers()) {
            this.tickPassenger(rider, entity);
         }
      }
   }

   public void unload(LevelChunk chunk) {
      chunk.clearAllBlockEntities();
      this.chunkSource.getLightEngine().setLightEnabled(chunk.getPos(), false);
      this.entityStorage.stopTicking(chunk.getPos());
   }

   public void onChunkLoaded(ChunkPos chunkPos) {
      this.tintCaches.forEach((colorResolver, blockTintCache) -> blockTintCache.invalidateForChunk(chunkPos.x, chunkPos.z));
      this.entityStorage.startTicking(chunkPos);
   }

   public void onSectionBecomingNonEmpty(long sectionPos) {
      this.levelRenderer.onSectionBecomingNonEmpty(sectionPos);
   }

   public void clearTintCaches() {
      this.tintCaches.forEach((colorResolver, blockTintCache) -> blockTintCache.invalidateAll());
   }

   @Override
   public boolean hasChunk(int chunkX, int chunkZ) {
      return true;
   }

   public int getEntityCount() {
      return this.entityStorage.count();
   }

   public void addEntity(Entity entity) {
      this.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
      this.entityStorage.addEntity(entity);
   }

   public void removeEntity(int entityId, Entity.RemovalReason reason) {
      Entity entity = this.getEntities().get(entityId);
      if (entity != null) {
         entity.setRemoved(reason);
         entity.onClientRemoval();
      }
   }

   @Override
   public List<Entity> getPushableEntities(Entity entity, AABB boundingBox) {
      LocalPlayer localPlayer = this.minecraft.player;
      return localPlayer != null
            && localPlayer != entity
            && localPlayer.getBoundingBox().intersects(boundingBox)
            && EntitySelector.pushableBy(entity).test(localPlayer)
         ? List.of(localPlayer)
         : List.of();
   }

   @Nullable
   @Override
   public Entity getEntity(int id) {
      return this.getEntities().get(id);
   }

   public void disconnect(Component reason) {
      this.connection.getConnection().disconnect(reason);
   }

   public void animateTick(int posX, int posY, int posZ) {
      int i = 32;
      RandomSource randomSource = RandomSource.create();
      Block block = this.getMarkerParticleTarget();
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

      for (int j = 0; j < 667; j++) {
         this.doAnimateTick(posX, posY, posZ, 16, randomSource, block, mutableBlockPos);
         this.doAnimateTick(posX, posY, posZ, 32, randomSource, block, mutableBlockPos);
      }
   }

   @Nullable
   private Block getMarkerParticleTarget() {
      if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
         ItemStack itemStack = this.minecraft.player.getMainHandItem();
         Item item = itemStack.getItem();
         if (MARKER_PARTICLE_ITEMS.contains(item) && item instanceof BlockItem blockItem) {
            return blockItem.getBlock();
         }
      }

      return null;
   }

   public void doAnimateTick(int posX, int posY, int posZ, int range, RandomSource random, @Nullable Block block, BlockPos.MutableBlockPos blockPos) {
      int i = posX + this.random.nextInt(range) - this.random.nextInt(range);
      int j = posY + this.random.nextInt(range) - this.random.nextInt(range);
      int k = posZ + this.random.nextInt(range) - this.random.nextInt(range);
      blockPos.set(i, j, k);
      BlockState blockState = this.getBlockState(blockPos);
      blockState.getBlock().animateTick(blockState, this, blockPos, random);
      FluidState fluidState = this.getFluidState(blockPos);
      if (!fluidState.isEmpty()) {
         fluidState.animateTick(this, blockPos, random);
         ParticleOptions particleOptions = fluidState.getDripParticle();
         if (particleOptions != null && this.random.nextInt(10) == 0) {
            boolean bl = blockState.isFaceSturdy(this, blockPos, Direction.DOWN);
            BlockPos blockPos2 = blockPos.below();
            this.trySpawnDripParticles(blockPos2, this.getBlockState(blockPos2), particleOptions, bl);
         }
      }

      if (block == blockState.getBlock()) {
         this.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockState), i + 0.5, j + 0.5, k + 0.5, 0.0, 0.0, 0.0);
      }

      if (!blockState.isCollisionShapeFullBlock(this, blockPos)) {
         for (AmbientParticle ambientParticle : this.environmentAttributes().getValue(EnvironmentAttributes.AMBIENT_PARTICLES, blockPos)) {
            if (ambientParticle.canSpawn(this.random)) {
               this.addParticle(
                  ambientParticle.particle(),
                  blockPos.getX() + this.random.nextDouble(),
                  blockPos.getY() + this.random.nextDouble(),
                  blockPos.getZ() + this.random.nextDouble(),
                  0.0,
                  0.0,
                  0.0
               );
            }
         }
      }
   }

   private void trySpawnDripParticles(BlockPos blockPos, BlockState blockState, ParticleOptions particleData, boolean shapeDownSolid) {
      if (blockState.getFluidState().isEmpty()) {
         VoxelShape voxelShape = blockState.getCollisionShape(this, blockPos);
         double d = voxelShape.max(Direction.Axis.Y);
         if (d < 1.0) {
            if (shapeDownSolid) {
               this.spawnFluidParticle(blockPos.getX(), blockPos.getX() + 1, blockPos.getZ(), blockPos.getZ() + 1, blockPos.getY() + 1 - 0.05, particleData);
            }
         } else if (!blockState.is(BlockTags.IMPERMEABLE)) {
            double e = voxelShape.min(Direction.Axis.Y);
            if (e > 0.0) {
               this.spawnParticle(blockPos, particleData, voxelShape, blockPos.getY() + e - 0.05);
            } else {
               BlockPos blockPos2 = blockPos.below();
               BlockState blockState2 = this.getBlockState(blockPos2);
               VoxelShape voxelShape2 = blockState2.getCollisionShape(this, blockPos2);
               double f = voxelShape2.max(Direction.Axis.Y);
               if (f < 1.0 && blockState2.getFluidState().isEmpty()) {
                  this.spawnParticle(blockPos, particleData, voxelShape, blockPos.getY() - 0.05);
               }
            }
         }
      }
   }

   private void spawnParticle(BlockPos pos, ParticleOptions particleData, VoxelShape voxelShape, double y) {
      this.spawnFluidParticle(
         pos.getX() + voxelShape.min(Direction.Axis.X),
         pos.getX() + voxelShape.max(Direction.Axis.X),
         pos.getZ() + voxelShape.min(Direction.Axis.Z),
         pos.getZ() + voxelShape.max(Direction.Axis.Z),
         y,
         particleData
      );
   }

   private void spawnFluidParticle(double xStart, double xEnd, double zStart, double zEnd, double y, ParticleOptions particleData) {
      this.addParticle(particleData, Mth.lerp(this.random.nextDouble(), xStart, xEnd), y, Mth.lerp(this.random.nextDouble(), zStart, zEnd), 0.0, 0.0, 0.0);
   }

   @Override
   public CrashReportCategory fillReportDetails(CrashReport report) {
      CrashReportCategory crashReportCategory = super.fillReportDetails(report);
      crashReportCategory.setDetail("Server brand", () -> this.minecraft.player.connection.serverBrand());
      crashReportCategory.setDetail(
         "Server type", () -> this.minecraft.getSingleplayerServer() == null ? "Non-integrated multiplayer server" : "Integrated singleplayer server"
      );
      crashReportCategory.setDetail("Tracked entity count", () -> String.valueOf(this.getEntityCount()));
      return crashReportCategory;
   }

   @Override
   public void playSeededSound(
      @Nullable Entity entity, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed
   ) {
      if (entity == this.minecraft.player) {
         this.playSound(x, y, z, sound.value(), source, volume, pitch, false, seed);
      }
   }

   @Override
   public void playSeededSound(@Nullable Entity entity, Entity sourceEntity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
      if (entity == this.minecraft.player) {
         this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(sound.value(), source, volume, pitch, sourceEntity, seed));
      }
   }

   @Override
   public void playLocalSound(Entity entity, SoundEvent sound, SoundSource source, float volume, float pitch) {
      this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(sound, source, volume, pitch, entity, this.random.nextLong()));
   }

   @Override
   public void playPlayerSound(SoundEvent sound, SoundSource source, float volume, float pitch) {
      if (this.minecraft.player != null) {
         this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(sound, source, volume, pitch, this.minecraft.player, this.random.nextLong()));
      }
   }

   @Override
   public void playLocalSound(double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay) {
      this.playSound(x, y, z, sound, source, volume, pitch, distanceDelay, this.random.nextLong());
   }

   private void playSound(double x, double y, double z, SoundEvent soundEvent, SoundSource source, float volume, float pitch, boolean distanceDelay, long seed) {
      double d = this.minecraft.gameRenderer.getMainCamera().position().distanceToSqr(x, y, z);
      SimpleSoundInstance simpleSoundInstance = new SimpleSoundInstance(soundEvent, source, volume, pitch, RandomSource.create(seed), x, y, z);
      if (distanceDelay && d > 100.0) {
         double e = Math.sqrt(d) / 40.0;
         this.minecraft.getSoundManager().playDelayed(simpleSoundInstance, (int)(e * 20.0));
      } else {
         this.minecraft.getSoundManager().play(simpleSoundInstance);
      }
   }

   @Override
   public void createFireworks(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, List<FireworkExplosion> explosions) {
      if (explosions.isEmpty()) {
         for (int i = 0; i < this.random.nextInt(3) + 2; i++) {
            this.addParticle(ParticleTypes.POOF, x, y, z, this.random.nextGaussian() * 0.05, 0.005, this.random.nextGaussian() * 0.05);
         }
      } else {
         this.minecraft.particleEngine.add(new FireworkParticles.Starter(this, x, y, z, xSpeed, ySpeed, zSpeed, this.minecraft.particleEngine, explosions));
      }
   }

   @Override
   public void sendPacketToServer(Packet<?> packet) {
      this.connection.send(packet);
   }

   @Override
   public WorldBorder getWorldBorder() {
      return this.worldBorder;
   }

   @Override
   public RecipeAccess recipeAccess() {
      return this.connection.recipes();
   }

   @Override
   public TickRateManager tickRateManager() {
      return this.tickRateManager;
   }

   @Override
   public EnvironmentAttributeSystem environmentAttributes() {
      return this.environmentAttributes;
   }

   @Override
   public LevelTickAccess<Block> getBlockTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   @Override
   public LevelTickAccess<Fluid> getFluidTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   public ClientChunkCache getChunkSource() {
      return this.chunkSource;
   }

   @Nullable
   @Override
   public MapItemSavedData getMapData(MapId mapId) {
      return this.mapData.get(mapId);
   }

   public void overrideMapData(MapId mapId, MapItemSavedData mapData) {
      this.mapData.put(mapId, mapData);
   }

   @Override
   public Scoreboard getScoreboard() {
      return this.connection.scoreboard();
   }

   @Override
   public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, @Block.UpdateFlags int flags) {
      this.levelRenderer.blockChanged(this, pos, oldState, newState, flags);
   }

   @Override
   public void setBlocksDirty(BlockPos blockPos, BlockState oldState, BlockState newState) {
      this.levelRenderer.setBlockDirty(blockPos, oldState, newState);
   }

   public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ) {
      this.levelRenderer.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
   }

   public void setSectionRangeDirty(int minY, int minX, int minZ, int maxY, int maxX, int maxZ) {
      this.levelRenderer.setSectionRangeDirty(minY, minX, minZ, maxY, maxX, maxZ);
   }

   @Override
   public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {
      this.levelRenderer.destroyBlockProgress(breakerId, pos, progress);
   }

   @Override
   public void globalLevelEvent(int id, BlockPos pos, int data) {
      this.levelEventHandler.globalLevelEvent(id, pos, data);
   }

   @Override
   public void levelEvent(@Nullable Entity entity, int type, BlockPos pos, int data) {
      try {
         this.levelEventHandler.levelEvent(type, pos, data);
      } catch (Throwable var8) {
         CrashReport crashReport = CrashReport.forThrowable(var8, "Playing level event");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Level event being played");
         crashReportCategory.setDetail("Block coordinates", CrashReportCategory.formatLocation(this, pos));
         crashReportCategory.setDetail("Event source", entity);
         crashReportCategory.setDetail("Event type", type);
         crashReportCategory.setDetail("Event data", data);
         throw new ReportedException(crashReport);
      }
   }

   @Override
   public void addParticle(ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
      this.doAddParticle(particle, particle.getType().getOverrideLimiter(), false, x, y, z, xSpeed, ySpeed, zSpeed);
   }

   @Override
   public void addParticle(
      ParticleOptions particle, boolean overrideLimiter, boolean alwaysShow, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
   ) {
      this.doAddParticle(particle, particle.getType().getOverrideLimiter() || overrideLimiter, alwaysShow, x, y, z, xSpeed, ySpeed, zSpeed);
   }

   @Override
   public void addAlwaysVisibleParticle(ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
      this.doAddParticle(particle, false, true, x, y, z, xSpeed, ySpeed, zSpeed);
   }

   @Override
   public void addAlwaysVisibleParticle(
      ParticleOptions particle, boolean overrideLimiter, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
   ) {
      this.doAddParticle(particle, particle.getType().getOverrideLimiter() || overrideLimiter, true, x, y, z, xSpeed, ySpeed, zSpeed);
   }

   private void doAddParticle(
      ParticleOptions particle, boolean overrideLimiter, boolean alwaysShow, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
   ) {
      try {
         Camera camera = this.minecraft.gameRenderer.getMainCamera();
         ParticleStatus particleStatus = this.calculateParticleLevel(alwaysShow);
         if (overrideLimiter) {
            this.minecraft.particleEngine.createParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
         } else if (!(camera.position().distanceToSqr(x, y, z) > 1024.0)) {
            if (particleStatus != ParticleStatus.MINIMAL) {
               this.minecraft.particleEngine.createParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
            }
         }
      } catch (Throwable var19) {
         CrashReport crashReport = CrashReport.forThrowable(var19, "Exception while adding particle");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being added");
         crashReportCategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType()));
         crashReportCategory.setDetail(
            "Parameters", () -> ParticleTypes.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), particle).toString()
         );
         crashReportCategory.setDetail("Position", () -> CrashReportCategory.formatLocation(this, x, y, z));
         throw new ReportedException(crashReport);
      }
   }

   private ParticleStatus calculateParticleLevel(boolean alwaysShow) {
      ParticleStatus particleStatus = this.minecraft.options.particles().get();
      if (alwaysShow && particleStatus == ParticleStatus.MINIMAL && this.random.nextInt(10) == 0) {
         particleStatus = ParticleStatus.DECREASED;
      }

      if (particleStatus == ParticleStatus.DECREASED && this.random.nextInt(3) == 0) {
         particleStatus = ParticleStatus.MINIMAL;
      }

      return particleStatus;
   }

   @Override
   public List<AbstractClientPlayer> players() {
      return this.players;
   }

   public List<EnderDragonPart> dragonParts() {
      return this.dragonParts;
   }

   @Override
   public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
      return this.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
   }

   private int getSkyFlashTime() {
      return this.minecraft.options.hideLightningFlash().get() ? 0 : this.skyFlashTime;
   }

   @Override
   public void setSkyFlashTime(int timeFlash) {
      this.skyFlashTime = timeFlash;
   }

   @Override
   public float getShade(Direction direction, boolean shade) {
      DimensionType.CardinalLightType cardinalLightType = this.dimensionType().cardinalLightType();
      if (!shade) {
         return cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
      } else {
         return switch (direction) {
            case DOWN -> cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 0.5F;
            case UP -> cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
         };
      }
   }

   @Override
   public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
      BlockTintCache blockTintCache = (BlockTintCache)this.tintCaches.get(colorResolver);
      return blockTintCache.getColor(blockPos);
   }

   public int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
      int i = Minecraft.getInstance().options.biomeBlendRadius().get();
      if (i == 0) {
         return colorResolver.getColor(this.getBiome(blockPos).value(), blockPos.getX(), blockPos.getZ());
      } else {
         int j = (i * 2 + 1) * (i * 2 + 1);
         int k = 0;
         int l = 0;
         int m = 0;
         Cursor3D cursor3D = new Cursor3D(blockPos.getX() - i, blockPos.getY(), blockPos.getZ() - i, blockPos.getX() + i, blockPos.getY(), blockPos.getZ() + i);
         BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

         while (cursor3D.advance()) {
            mutableBlockPos.set(cursor3D.nextX(), cursor3D.nextY(), cursor3D.nextZ());
            int n = colorResolver.getColor(this.getBiome(mutableBlockPos).value(), mutableBlockPos.getX(), mutableBlockPos.getZ());
            k += (n & 0xFF0000) >> 16;
            l += (n & 0xFF00) >> 8;
            m += n & 0xFF;
         }

         return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | m / j & 0xFF;
      }
   }

   @Override
   public void setRespawnData(LevelData.RespawnData respawnData) {
      this.levelData.setSpawn(this.getWorldBorderAdjustedRespawnData(respawnData));
   }

   @Override
   public LevelData.RespawnData getRespawnData() {
      return this.levelData.getRespawnData();
   }

   @Override
   public String toString() {
      return "ClientLevel";
   }

   public ClientLevel.ClientLevelData getLevelData() {
      return this.clientLevelData;
   }

   @Override
   public void gameEvent(Holder<GameEvent> gameEvent, Vec3 pos, GameEvent.Context context) {
   }

   protected Map<MapId, MapItemSavedData> getAllMapData() {
      return ImmutableMap.copyOf(this.mapData);
   }

   protected void addMapData(Map<MapId, MapItemSavedData> map) {
      this.mapData.putAll(map);
   }

   @Override
   protected LevelEntityGetter<Entity> getEntities() {
      return this.entityStorage.getEntityGetter();
   }

   @Override
   public String gatherChunkSourceStats() {
      return "Chunks[C] W: " + this.chunkSource.gatherStats() + " E: " + this.entityStorage.gatherStats();
   }

   @Override
   public void addDestroyBlockEffect(BlockPos pos, BlockState state) {
      if (!state.isAir() && state.shouldSpawnTerrainParticles()) {
         VoxelShape voxelShape = state.getShape(this, pos);
         double d = 0.25;
         voxelShape.forAllBoxes(
            (dx, e, f, g, h, i) -> {
               double j = Math.min(1.0, g - dx);
               double k = Math.min(1.0, h - e);
               double l = Math.min(1.0, i - f);
               int m = Math.max(2, Mth.ceil(j / 0.25));
               int n = Math.max(2, Mth.ceil(k / 0.25));
               int o = Math.max(2, Mth.ceil(l / 0.25));

               for (int p = 0; p < m; p++) {
                  for (int q = 0; q < n; q++) {
                     for (int r = 0; r < o; r++) {
                        double s = (p + 0.5) / m;
                        double t = (q + 0.5) / n;
                        double u = (r + 0.5) / o;
                        double v = s * j + dx;
                        double w = t * k + e;
                        double x = u * l + f;
                        this.minecraft
                           .particleEngine
                           .add(new TerrainParticle(this, pos.getX() + v, pos.getY() + w, pos.getZ() + x, s - 0.5, t - 0.5, u - 0.5, state, pos));
                     }
                  }
               }
            }
         );
      }
   }

   public void addBreakingBlockEffect(BlockPos pos, Direction direction) {
      BlockState blockState = this.getBlockState(pos);
      if (blockState.getRenderShape() != RenderShape.INVISIBLE && blockState.shouldSpawnTerrainParticles()) {
         int i = pos.getX();
         int j = pos.getY();
         int k = pos.getZ();
         float f = 0.1F;
         AABB aABB = blockState.getShape(this, pos).bounds();
         double d = i + this.random.nextDouble() * (aABB.maxX - aABB.minX - 0.2F) + 0.1F + aABB.minX;
         double e = j + this.random.nextDouble() * (aABB.maxY - aABB.minY - 0.2F) + 0.1F + aABB.minY;
         double g = k + this.random.nextDouble() * (aABB.maxZ - aABB.minZ - 0.2F) + 0.1F + aABB.minZ;
         if (direction == Direction.DOWN) {
            e = j + aABB.minY - 0.1F;
         }

         if (direction == Direction.UP) {
            e = j + aABB.maxY + 0.1F;
         }

         if (direction == Direction.NORTH) {
            g = k + aABB.minZ - 0.1F;
         }

         if (direction == Direction.SOUTH) {
            g = k + aABB.maxZ + 0.1F;
         }

         if (direction == Direction.WEST) {
            d = i + aABB.minX - 0.1F;
         }

         if (direction == Direction.EAST) {
            d = i + aABB.maxX + 0.1F;
         }

         this.minecraft.particleEngine.add(new TerrainParticle(this, d, e, g, 0.0, 0.0, 0.0, blockState, pos).setPower(0.2F).scale(0.6F));
      }
   }

   public void setServerSimulationDistance(int serverSimulationDistance) {
      this.serverSimulationDistance = serverSimulationDistance;
   }

   public int getServerSimulationDistance() {
      return this.serverSimulationDistance;
   }

   @Override
   public FeatureFlagSet enabledFeatures() {
      return this.connection.enabledFeatures();
   }

   @Override
   public PotionBrewing potionBrewing() {
      return this.connection.potionBrewing();
   }

   @Override
   public FuelValues fuelValues() {
      return this.connection.fuelValues();
   }

   @Override
   public void explode(
      @Nullable Entity source,
      @Nullable DamageSource damageSource,
      @Nullable ExplosionDamageCalculator damageCalculator,
      double x,
      double y,
      double z,
      float radius,
      boolean fire,
      Level.ExplosionInteraction explosionInteraction,
      ParticleOptions smallExplosionParticles,
      ParticleOptions largeExplosionParticles,
      WeightedList<ExplosionParticleInfo> blockParticles,
      Holder<SoundEvent> explosionSound
   ) {
   }

   @Override
   public int getSeaLevel() {
      return this.seaLevel;
   }

   @Override
   public int getClientLeafTintColor(BlockPos pos) {
      return Minecraft.getInstance().getBlockColors().getColor(this.getBlockState(pos), this, pos, 0);
   }

   @Override
   public void registerForCleaning(CacheSlot<ClientLevel, ?> cacheSlot) {
      this.connection.registerForCleaning(cacheSlot);
   }

   public void trackExplosionEffects(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
      this.explosionTracker.track(center, radius, blockCount, blockParticles);
   }

   @Environment(EnvType.CLIENT)
   public static class ClientLevelData implements WritableLevelData {
      private final boolean hardcore;
      private final boolean isFlat;
      private LevelData.RespawnData respawnData;
      private long gameTime;
      private long dayTime;
      private boolean raining;
      private Difficulty difficulty;
      private boolean difficultyLocked;

      public ClientLevelData(Difficulty difficulty, boolean hardcore, boolean isFlat) {
         this.difficulty = difficulty;
         this.hardcore = hardcore;
         this.isFlat = isFlat;
      }

      @Override
      public LevelData.RespawnData getRespawnData() {
         return this.respawnData;
      }

      @Override
      public long getGameTime() {
         return this.gameTime;
      }

      @Override
      public long getDayTime() {
         return this.dayTime;
      }

      public void setGameTime(long gameTime) {
         this.gameTime = gameTime;
      }

      public void setDayTime(long dayTime) {
         this.dayTime = dayTime;
      }

      @Override
      public void setSpawn(LevelData.RespawnData spawn) {
         this.respawnData = spawn;
      }

      @Override
      public boolean isThundering() {
         return false;
      }

      @Override
      public boolean isRaining() {
         return this.raining;
      }

      @Override
      public void setRaining(boolean raining) {
         this.raining = raining;
      }

      @Override
      public boolean isHardcore() {
         return this.hardcore;
      }

      @Override
      public Difficulty getDifficulty() {
         return this.difficulty;
      }

      @Override
      public boolean isDifficultyLocked() {
         return this.difficultyLocked;
      }

      @Override
      public void fillCrashReportCategory(CrashReportCategory crashReportCategory, LevelHeightAccessor level) {
         WritableLevelData.super.fillCrashReportCategory(crashReportCategory, level);
      }

      public void setDifficulty(Difficulty difficulty) {
         this.difficulty = difficulty;
      }

      public void setDifficultyLocked(boolean difficultyLocked) {
         this.difficultyLocked = difficultyLocked;
      }

      public double getHorizonHeight(LevelHeightAccessor level) {
         return this.isFlat ? level.getMinY() : 63.0;
      }

      public float voidDarknessOnsetRange() {
         return this.isFlat ? 1.0F : 32.0F;
      }
   }

   @Environment(EnvType.CLIENT)
   final class EntityCallbacks implements LevelCallback<Entity> {
      public void onCreated(Entity entity) {
      }

      public void onDestroyed(Entity entity) {
      }

      public void onTickingStart(Entity entity) {
         ClientLevel.this.tickingEntities.add(entity);
      }

      public void onTickingEnd(Entity entity) {
         ClientLevel.this.tickingEntities.remove(entity);
      }

      public void onTrackingStart(Entity entity) {
         switch (entity) {
            case AbstractClientPlayer abstractClientPlayer:
               ClientLevel.this.players.add(abstractClientPlayer);
               break;
            case EnderDragon enderDragon:
               ClientLevel.this.dragonParts.addAll(Arrays.asList(enderDragon.getSubEntities()));
               break;
            default:
         }
      }

      public void onTrackingEnd(Entity entity) {
         entity.unRide();
         switch (entity) {
            case AbstractClientPlayer abstractClientPlayer:
               ClientLevel.this.players.remove(abstractClientPlayer);
               break;
            case EnderDragon enderDragon:
               ClientLevel.this.dragonParts.removeAll(Arrays.asList(enderDragon.getSubEntities()));
               break;
            default:
         }
      }

      public void onSectionChange(Entity entity) {
      }
   }
}
