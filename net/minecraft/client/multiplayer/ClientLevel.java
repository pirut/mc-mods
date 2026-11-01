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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ColorResolverManager;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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
   final List<PartEntity<?>> dragonParts = Lists.newArrayList();
   private final Map<MapId, MapItemSavedData> mapData = Maps.newHashMap();
   private int skyFlashTime;
   private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = Util.make(
      new Object2ObjectArrayMap(3),
      p_406186_ -> {
         p_406186_.put(BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache(p_194181_ -> this.calculateBlockTint(p_194181_, BiomeColors.GRASS_COLOR_RESOLVER)));
         p_406186_.put(
            BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache(p_194177_ -> this.calculateBlockTint(p_194177_, BiomeColors.FOLIAGE_COLOR_RESOLVER))
         );
         p_406186_.put(
            BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER, new BlockTintCache(p_406185_ -> this.calculateBlockTint(p_406185_, BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER))
         );
         p_406186_.put(BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache(p_194168_ -> this.calculateBlockTint(p_194168_, BiomeColors.WATER_COLOR_RESOLVER)));
         ColorResolverManager.registerBlockTintCaches(this, p_406186_);
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
   private final ModelDataManager modelDataManager = new ModelDataManager(this);
   private float dayTimeFraction = 0.0F;
   private float dayTimePerTick = -1.0F;

   public void handleBlockChangedAck(int p_233652_) {
      if (SharedConstants.DEBUG_BLOCK_BREAK) {
         LOGGER.debug("ACK {}", p_233652_);
      }

      this.blockStatePredictionHandler.endPredictionsUpTo(p_233652_, this);
   }

   @Override
   public void onBlockEntityAdded(BlockEntity p_427306_) {
      BlockEntityRenderer<BlockEntity, ?> blockentityrenderer = this.minecraft.getBlockEntityRenderDispatcher().getRenderer(p_427306_);
      if (blockentityrenderer != null && blockentityrenderer.shouldRenderOffScreen()) {
         this.globallyRenderedBlockEntities.add(p_427306_);
      }
   }

   public Set<BlockEntity> getGloballyRenderedBlockEntities() {
      return this.globallyRenderedBlockEntities;
   }

   public void setServerVerifiedBlockState(BlockPos p_233654_, BlockState p_233655_, @Block.UpdateFlags int p_233656_) {
      if (!this.blockStatePredictionHandler.updateKnownServerState(p_233654_, p_233655_)) {
         super.setBlock(p_233654_, p_233655_, p_233656_, 512);
      }
   }

   public void syncBlockState(BlockPos p_233648_, BlockState p_233649_, Vec3 p_233650_) {
      BlockState blockstate = this.getBlockState(p_233648_);
      if (blockstate != p_233649_) {
         this.setBlock(p_233648_, p_233649_, 19);
         Player player = this.minecraft.player;
         if (this == player.level() && player.isColliding(p_233648_, p_233649_)) {
            player.absSnapTo(p_233650_.x, p_233650_.y, p_233650_.z);
         }
      }
   }

   BlockStatePredictionHandler getBlockStatePredictionHandler() {
      return this.blockStatePredictionHandler;
   }

   @Override
   public boolean setBlock(BlockPos p_233643_, BlockState p_233644_, @Block.UpdateFlags int p_233645_, int p_233646_) {
      if (this.blockStatePredictionHandler.isPredicting()) {
         BlockSnapshot snapshot = BlockSnapshot.create(this.dimension(), this, p_233643_, p_233645_);
         BlockState blockstate = this.getBlockState(p_233643_);
         boolean flag = super.setBlock(p_233643_, p_233644_, p_233645_, p_233646_);
         if (flag) {
            this.blockStatePredictionHandler.retainKnownServerState(p_233643_, blockstate, this.minecraft.player);
            this.blockStatePredictionHandler.retainSnapshot(p_233643_, snapshot);
         }

         return flag;
      } else {
         return super.setBlock(p_233643_, p_233644_, p_233645_, p_233646_);
      }
   }

   public ClientLevel(
      ClientPacketListener p_365265_,
      ClientLevel.ClientLevelData p_363368_,
      ResourceKey<Level> p_364143_,
      Holder<DimensionType> p_361100_,
      int p_360763_,
      int p_363776_,
      LevelRenderer p_360537_,
      boolean p_362070_,
      long p_362237_,
      int p_361950_
   ) {
      super(p_363368_, p_364143_, p_365265_.registryAccess(), p_361100_, true, p_362070_, p_362237_, 1000000);
      this.connection = p_365265_;
      this.chunkSource = new ClientChunkCache(this, p_360763_);
      this.tickRateManager = new TickRateManager();
      this.clientLevelData = p_363368_;
      this.levelRenderer = p_360537_;
      this.seaLevel = p_361950_;
      this.levelEventHandler = new LevelEventHandler(this.minecraft, this);
      this.endFlashState = p_361100_.value().hasEndFlashes() ? new EndFlashState() : null;
      this.setRespawnData(LevelData.RespawnData.of(p_364143_, new BlockPos(8, 64, 8), 0.0F, 0.0F));
      this.serverSimulationDistance = p_363776_;
      this.environmentAttributes = this.addEnvironmentAttributeLayers(EnvironmentAttributeSystem.builder()).build();
      this.updateSkyBrightness();
      if (this.canHaveWeather()) {
         this.prepareWeather();
      }

      NeoForge.EVENT_BUS.post(new LevelEvent.Load(this));
   }

   private EnvironmentAttributeSystem.Builder addEnvironmentAttributeLayers(EnvironmentAttributeSystem.Builder p_468725_) {
      p_468725_.addDefaultLayers(this);
      int i = ARGB.color(204, 204, 255);
      p_468725_.addTimeBasedLayer(
         EnvironmentAttributes.SKY_COLOR, (p_465550_, p_465551_) -> this.getSkyFlashTime() > 0 ? ARGB.srgbLerp(0.22F, p_465550_, i) : p_465550_
      );
      p_468725_.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR, (p_465547_, p_465548_) -> this.getSkyFlashTime() > 0 ? 1.0F : p_465547_);
      return p_468725_;
   }

   public void queueLightUpdate(Runnable p_194172_) {
      this.lightUpdateQueue.add(p_194172_);
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

   public void tick(BooleanSupplier p_104727_) {
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
         this.chunkSource.tick(p_104727_, true);
      }

      JvmProfiler.INSTANCE.onClientTick(this.minecraft.getFps());
      this.environmentAttributes().invalidateTickCache();
   }

   private void tickTime() {
      this.clientLevelData.setGameTime(this.clientLevelData.getGameTime() + 1L);
      if (this.tickDayTime) {
         this.clientLevelData.setDayTime(this.clientLevelData.getDayTime() + this.advanceDaytime());
      }
   }

   public void setTimeFromServer(long p_376531_, long p_376759_, boolean p_376421_) {
      this.clientLevelData.setGameTime(p_376531_);
      this.clientLevelData.setDayTime(p_376759_);
      this.tickDayTime = p_376421_;
   }

   public Iterable<Entity> entitiesForRendering() {
      return this.getEntities().getAll();
   }

   public void tickEntities() {
      this.tickingEntities.forEach(p_308457_ -> {
         if (!p_308457_.isRemoved() && !p_308457_.isPassenger() && !this.tickRateManager.isEntityFrozen(p_308457_)) {
            this.guardEntityTick(this::tickNonPassenger, p_308457_);
         }
      });
   }

   public boolean isTickingEntity(Entity p_361941_) {
      return this.tickingEntities.contains(p_361941_);
   }

   @Override
   public boolean shouldTickDeath(Entity p_194185_) {
      return p_194185_.chunkPosition().getChessboardDistance(this.minecraft.player.chunkPosition()) <= this.serverSimulationDistance;
   }

   public void tickNonPassenger(Entity p_104640_) {
      p_104640_.setOldPosAndRot();
      p_104640_.tickCount++;
      Profiler.get().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(p_104640_.getType()).toString());
      if (!EventHooks.fireEntityTickPre(p_104640_).isCanceled()) {
         p_104640_.tick();
         EventHooks.fireEntityTickPost(p_104640_);
      }

      Profiler.get().pop();

      for (Entity entity : p_104640_.getPassengers()) {
         this.tickPassenger(p_104640_, entity);
      }
   }

   private void tickPassenger(Entity p_104642_, Entity p_104643_) {
      if (!p_104643_.isRemoved() && p_104643_.getVehicle() == p_104642_) {
         if (p_104643_ instanceof Player || this.tickingEntities.contains(p_104643_)) {
            p_104643_.setOldPosAndRot();
            p_104643_.tickCount++;
            p_104643_.rideTick();

            for (Entity entity : p_104643_.getPassengers()) {
               this.tickPassenger(p_104643_, entity);
            }
         }
      } else {
         p_104643_.stopRiding();
      }
   }

   public void unload(LevelChunk p_104666_) {
      p_104666_.clearAllBlockEntities();
      this.chunkSource.getLightEngine().setLightEnabled(p_104666_.getPos(), false);
      this.entityStorage.stopTicking(p_104666_.getPos());
   }

   public void onChunkLoaded(ChunkPos p_171650_) {
      this.tintCaches.forEach((p_194154_, p_194155_) -> p_194155_.invalidateForChunk(p_171650_.x, p_171650_.z));
      this.entityStorage.startTicking(p_171650_);
   }

   public void onSectionBecomingNonEmpty(long p_366708_) {
      this.levelRenderer.onSectionBecomingNonEmpty(p_366708_);
   }

   public void clearTintCaches() {
      this.tintCaches.forEach((p_194157_, p_194158_) -> p_194158_.invalidateAll());
   }

   @Override
   public boolean hasChunk(int p_104737_, int p_104738_) {
      return true;
   }

   public int getEntityCount() {
      return this.entityStorage.count();
   }

   public void addEntity(Entity p_104741_) {
      if (!((EntityJoinLevelEvent)NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(p_104741_, this))).isCanceled()) {
         this.removeEntity(p_104741_.getId(), Entity.RemovalReason.DISCARDED);
         this.entityStorage.addEntity(p_104741_);
         p_104741_.onAddedToLevel();
      }
   }

   public void removeEntity(int p_171643_, Entity.RemovalReason p_171644_) {
      Entity entity = this.getEntities().get(p_171643_);
      if (entity != null) {
         entity.setRemoved(p_171644_);
         entity.onClientRemoval();
      }
   }

   @Override
   public List<Entity> getPushableEntities(Entity p_393670_, AABB p_394410_) {
      LocalPlayer localplayer = this.minecraft.player;
      return localplayer != null
            && localplayer != p_393670_
            && localplayer.getBoundingBox().intersects(p_394410_)
            && EntitySelector.pushableBy(p_393670_).test(localplayer)
         ? List.of(localplayer)
         : List.of();
   }

   @Nullable
   @Override
   public Entity getEntity(int p_104609_) {
      return this.getEntities().get(p_104609_);
   }

   public void disconnect(Component p_428436_) {
      this.connection.getConnection().disconnect(p_428436_);
   }

   public void animateTick(int p_104785_, int p_104786_, int p_104787_) {
      int i = 32;
      RandomSource randomsource = RandomSource.create();
      Block block = this.getMarkerParticleTarget();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int j = 0; j < 667; j++) {
         this.doAnimateTick(p_104785_, p_104786_, p_104787_, 16, randomsource, block, blockpos$mutableblockpos);
         this.doAnimateTick(p_104785_, p_104786_, p_104787_, 32, randomsource, block, blockpos$mutableblockpos);
      }
   }

   @Nullable
   private Block getMarkerParticleTarget() {
      if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
         ItemStack itemstack = this.minecraft.player.getMainHandItem();
         Item item = itemstack.getItem();
         if (MARKER_PARTICLE_ITEMS.contains(item) && item instanceof BlockItem blockitem) {
            return blockitem.getBlock();
         }
      }

      return null;
   }

   public void doAnimateTick(
      int p_233613_, int p_233614_, int p_233615_, int p_233616_, RandomSource p_233617_, @Nullable Block p_233618_, BlockPos.MutableBlockPos p_233619_
   ) {
      int i = p_233613_ + this.random.nextInt(p_233616_) - this.random.nextInt(p_233616_);
      int j = p_233614_ + this.random.nextInt(p_233616_) - this.random.nextInt(p_233616_);
      int k = p_233615_ + this.random.nextInt(p_233616_) - this.random.nextInt(p_233616_);
      p_233619_.set(i, j, k);
      BlockState blockstate = this.getBlockState(p_233619_);
      blockstate.getBlock().animateTick(blockstate, this, p_233619_, p_233617_);
      FluidState fluidstate = this.getFluidState(p_233619_);
      if (!fluidstate.isEmpty()) {
         fluidstate.animateTick(this, p_233619_, p_233617_);
         ParticleOptions particleoptions = fluidstate.getDripParticle();
         if (particleoptions != null && this.random.nextInt(10) == 0) {
            boolean flag = blockstate.isFaceSturdy(this, p_233619_, Direction.DOWN);
            BlockPos blockpos = p_233619_.below();
            this.trySpawnDripParticles(blockpos, this.getBlockState(blockpos), particleoptions, flag);
         }
      }

      if (p_233618_ == blockstate.getBlock()) {
         this.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockstate), i + 0.5, j + 0.5, k + 0.5, 0.0, 0.0, 0.0);
      }

      if (!blockstate.isCollisionShapeFullBlock(this, p_233619_)) {
         for (AmbientParticle ambientparticle : this.environmentAttributes().getValue(EnvironmentAttributes.AMBIENT_PARTICLES, p_233619_)) {
            if (ambientparticle.canSpawn(this.random)) {
               this.addParticle(
                  ambientparticle.particle(),
                  p_233619_.getX() + this.random.nextDouble(),
                  p_233619_.getY() + this.random.nextDouble(),
                  p_233619_.getZ() + this.random.nextDouble(),
                  0.0,
                  0.0,
                  0.0
               );
            }
         }
      }
   }

   private void trySpawnDripParticles(BlockPos p_104690_, BlockState p_104691_, ParticleOptions p_104692_, boolean p_104693_) {
      if (p_104691_.getFluidState().isEmpty()) {
         VoxelShape voxelshape = p_104691_.getCollisionShape(this, p_104690_);
         double d0 = voxelshape.max(Direction.Axis.Y);
         if (d0 < 1.0) {
            if (p_104693_) {
               this.spawnFluidParticle(p_104690_.getX(), p_104690_.getX() + 1, p_104690_.getZ(), p_104690_.getZ() + 1, p_104690_.getY() + 1 - 0.05, p_104692_);
            }
         } else if (!p_104691_.is(BlockTags.IMPERMEABLE)) {
            double d1 = voxelshape.min(Direction.Axis.Y);
            if (d1 > 0.0) {
               this.spawnParticle(p_104690_, p_104692_, voxelshape, p_104690_.getY() + d1 - 0.05);
            } else {
               BlockPos blockpos = p_104690_.below();
               BlockState blockstate = this.getBlockState(blockpos);
               VoxelShape voxelshape1 = blockstate.getCollisionShape(this, blockpos);
               double d2 = voxelshape1.max(Direction.Axis.Y);
               if (d2 < 1.0 && blockstate.getFluidState().isEmpty()) {
                  this.spawnParticle(p_104690_, p_104692_, voxelshape, p_104690_.getY() - 0.05);
               }
            }
         }
      }
   }

   private void spawnParticle(BlockPos p_104695_, ParticleOptions p_104696_, VoxelShape p_104697_, double p_104698_) {
      this.spawnFluidParticle(
         p_104695_.getX() + p_104697_.min(Direction.Axis.X),
         p_104695_.getX() + p_104697_.max(Direction.Axis.X),
         p_104695_.getZ() + p_104697_.min(Direction.Axis.Z),
         p_104695_.getZ() + p_104697_.max(Direction.Axis.Z),
         p_104698_,
         p_104696_
      );
   }

   private void spawnFluidParticle(double p_104593_, double p_104594_, double p_104595_, double p_104596_, double p_104597_, ParticleOptions p_104598_) {
      this.addParticle(
         p_104598_,
         Mth.lerp(this.random.nextDouble(), p_104593_, p_104594_),
         p_104597_,
         Mth.lerp(this.random.nextDouble(), p_104595_, p_104596_),
         0.0,
         0.0,
         0.0
      );
   }

   @Override
   public CrashReportCategory fillReportDetails(CrashReport p_104729_) {
      CrashReportCategory crashreportcategory = super.fillReportDetails(p_104729_);
      crashreportcategory.setDetail("Server brand", () -> this.minecraft.player.connection.serverBrand());
      crashreportcategory.setDetail(
         "Server type", () -> this.minecraft.getSingleplayerServer() == null ? "Non-integrated multiplayer server" : "Integrated singleplayer server"
      );
      crashreportcategory.setDetail("Tracked entity count", () -> String.valueOf(this.getEntityCount()));
      return crashreportcategory;
   }

   @Override
   public void playSeededSound(
      @Nullable Entity p_393665_,
      double p_263372_,
      double p_263404_,
      double p_263365_,
      Holder<SoundEvent> p_263335_,
      SoundSource p_263417_,
      float p_263416_,
      float p_263349_,
      long p_263408_
   ) {
      PlayLevelSoundEvent.AtPosition event = EventHooks.onPlaySoundAtPosition(this, p_263372_, p_263404_, p_263365_, p_263335_, p_263417_, p_263416_, p_263349_);
      if (!event.isCanceled() && event.getSound() != null) {
         p_263335_ = event.getSound();
         p_263417_ = event.getSource();
         p_263416_ = event.getNewVolume();
         p_263349_ = event.getNewPitch();
         if (p_393665_ == this.minecraft.player) {
            this.playSound(p_263372_, p_263404_, p_263365_, p_263335_.value(), p_263417_, p_263416_, p_263349_, false, p_263408_);
         }
      }
   }

   @Override
   public void playSeededSound(
      @Nullable Entity p_263536_, Entity p_394209_, Holder<SoundEvent> p_263518_, SoundSource p_263487_, float p_263538_, float p_263524_, long p_263509_
   ) {
      PlayLevelSoundEvent.AtEntity event = EventHooks.onPlaySoundAtEntity(p_394209_, p_263518_, p_263487_, p_263538_, p_263524_);
      if (!event.isCanceled() && event.getSound() != null) {
         p_263518_ = event.getSound();
         p_263487_ = event.getSource();
         p_263538_ = event.getNewVolume();
         p_263524_ = event.getNewPitch();
         if (p_263536_ == this.minecraft.player) {
            this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(p_263518_.value(), p_263487_, p_263538_, p_263524_, p_394209_, p_263509_));
         }
      }
   }

   @Override
   public void playLocalSound(Entity p_312749_, SoundEvent p_312635_, SoundSource p_312007_, float p_312298_, float p_312772_) {
      this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(p_312635_, p_312007_, p_312298_, p_312772_, p_312749_, this.random.nextLong()));
   }

   @Override
   public void playPlayerSound(SoundEvent p_401146_, SoundSource p_401176_, float p_401193_, float p_401050_) {
      if (this.minecraft.player != null) {
         this.minecraft
            .getSoundManager()
            .play(new EntityBoundSoundInstance(p_401146_, p_401176_, p_401193_, p_401050_, this.minecraft.player, this.random.nextLong()));
      }
   }

   @Override
   public void playLocalSound(
      double p_104600_, double p_104601_, double p_104602_, SoundEvent p_104603_, SoundSource p_104604_, float p_104605_, float p_104606_, boolean p_104607_
   ) {
      this.playSound(p_104600_, p_104601_, p_104602_, p_104603_, p_104604_, p_104605_, p_104606_, p_104607_, this.random.nextLong());
   }

   private void playSound(
      double p_233603_,
      double p_233604_,
      double p_233605_,
      SoundEvent p_233606_,
      SoundSource p_233607_,
      float p_233608_,
      float p_233609_,
      boolean p_233610_,
      long p_233611_
   ) {
      double d0 = this.minecraft.gameRenderer.getMainCamera().position().distanceToSqr(p_233603_, p_233604_, p_233605_);
      SimpleSoundInstance simplesoundinstance = new SimpleSoundInstance(
         p_233606_, p_233607_, p_233608_, p_233609_, RandomSource.create(p_233611_), p_233603_, p_233604_, p_233605_
      );
      if (p_233610_ && d0 > 100.0) {
         double d1 = Math.sqrt(d0) / 40.0;
         this.minecraft.getSoundManager().playDelayed(simplesoundinstance, (int)(d1 * 20.0));
      } else {
         this.minecraft.getSoundManager().play(simplesoundinstance);
      }
   }

   @Override
   public void createFireworks(
      double p_104585_, double p_104586_, double p_104587_, double p_104588_, double p_104589_, double p_104590_, List<FireworkExplosion> p_330281_
   ) {
      if (p_330281_.isEmpty()) {
         for (int i = 0; i < this.random.nextInt(3) + 2; i++) {
            this.addParticle(ParticleTypes.POOF, p_104585_, p_104586_, p_104587_, this.random.nextGaussian() * 0.05, 0.005, this.random.nextGaussian() * 0.05);
         }
      } else {
         this.minecraft
            .particleEngine
            .add(
               new FireworkParticles.Starter(this, p_104585_, p_104586_, p_104587_, p_104588_, p_104589_, p_104590_, this.minecraft.particleEngine, p_330281_)
            );
      }
   }

   @Override
   public void sendPacketToServer(Packet<?> p_104734_) {
      this.connection.send(p_104734_);
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
   public MapItemSavedData getMapData(MapId p_323873_) {
      return this.mapData.get(p_323873_);
   }

   public void overrideMapData(MapId p_323577_, MapItemSavedData p_259308_) {
      this.mapData.put(p_323577_, p_259308_);
   }

   @Override
   public Scoreboard getScoreboard() {
      return this.connection.scoreboard();
   }

   @Override
   public void sendBlockUpdated(BlockPos p_104685_, BlockState p_104686_, BlockState p_104687_, @Block.UpdateFlags int p_104688_) {
      this.levelRenderer.blockChanged(this, p_104685_, p_104686_, p_104687_, p_104688_);
   }

   @Override
   public void setBlocksDirty(BlockPos p_104759_, BlockState p_104760_, BlockState p_104761_) {
      this.levelRenderer.setBlockDirty(p_104759_, p_104760_, p_104761_);
   }

   public void setSectionDirtyWithNeighbors(int p_104794_, int p_104795_, int p_104796_) {
      this.levelRenderer.setSectionDirtyWithNeighbors(p_104794_, p_104795_, p_104796_);
   }

   public void setSectionRangeDirty(int p_362815_, int p_361646_, int p_361179_, int p_363637_, int p_364381_, int p_361721_) {
      this.levelRenderer.setSectionRangeDirty(p_362815_, p_361646_, p_361179_, p_363637_, p_364381_, p_361721_);
   }

   @Override
   public void destroyBlockProgress(int p_104634_, BlockPos p_104635_, int p_104636_) {
      this.levelRenderer.destroyBlockProgress(p_104634_, p_104635_, p_104636_);
   }

   @Override
   public void globalLevelEvent(int p_104743_, BlockPos p_104744_, int p_104745_) {
      this.levelEventHandler.globalLevelEvent(p_104743_, p_104744_, p_104745_);
   }

   @Override
   public void levelEvent(@Nullable Entity p_394519_, int p_104655_, BlockPos p_104656_, int p_104657_) {
      try {
         this.levelEventHandler.levelEvent(p_104655_, p_104656_, p_104657_);
      } catch (Throwable var8) {
         CrashReport crashreport = CrashReport.forThrowable(var8, "Playing level event");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Level event being played");
         crashreportcategory.setDetail("Block coordinates", CrashReportCategory.formatLocation(this, p_104656_));
         crashreportcategory.setDetail("Event source", p_394519_);
         crashreportcategory.setDetail("Event type", p_104655_);
         crashreportcategory.setDetail("Event data", p_104657_);
         throw new ReportedException(crashreport);
      }
   }

   @Override
   public void addParticle(
      ParticleOptions p_104706_, double p_104707_, double p_104708_, double p_104709_, double p_104710_, double p_104711_, double p_104712_
   ) {
      this.doAddParticle(p_104706_, p_104706_.getType().getOverrideLimiter(), false, p_104707_, p_104708_, p_104709_, p_104710_, p_104711_, p_104712_);
   }

   @Override
   public void addParticle(
      ParticleOptions p_104714_,
      boolean p_104715_,
      boolean p_383197_,
      double p_104716_,
      double p_104717_,
      double p_104718_,
      double p_104719_,
      double p_104720_,
      double p_104721_
   ) {
      this.doAddParticle(
         p_104714_, p_104714_.getType().getOverrideLimiter() || p_104715_, p_383197_, p_104716_, p_104717_, p_104718_, p_104719_, p_104720_, p_104721_
      );
   }

   @Override
   public void addAlwaysVisibleParticle(
      ParticleOptions p_104766_, double p_104767_, double p_104768_, double p_104769_, double p_104770_, double p_104771_, double p_104772_
   ) {
      this.doAddParticle(p_104766_, false, true, p_104767_, p_104768_, p_104769_, p_104770_, p_104771_, p_104772_);
   }

   @Override
   public void addAlwaysVisibleParticle(
      ParticleOptions p_104774_, boolean p_104775_, double p_104776_, double p_104777_, double p_104778_, double p_104779_, double p_104780_, double p_104781_
   ) {
      this.doAddParticle(
         p_104774_, p_104774_.getType().getOverrideLimiter() || p_104775_, true, p_104776_, p_104777_, p_104778_, p_104779_, p_104780_, p_104781_
      );
   }

   private void doAddParticle(
      ParticleOptions p_451273_,
      boolean p_451078_,
      boolean p_451365_,
      double p_451155_,
      double p_451486_,
      double p_451297_,
      double p_451193_,
      double p_450918_,
      double p_451010_
   ) {
      try {
         Camera camera = this.minecraft.gameRenderer.getMainCamera();
         ParticleStatus particlestatus = this.calculateParticleLevel(p_451365_);
         if (p_451078_) {
            this.minecraft.particleEngine.createParticle(p_451273_, p_451155_, p_451486_, p_451297_, p_451193_, p_450918_, p_451010_);
         } else if (!(camera.position().distanceToSqr(p_451155_, p_451486_, p_451297_) > 1024.0) && particlestatus != ParticleStatus.MINIMAL) {
            this.minecraft.particleEngine.createParticle(p_451273_, p_451155_, p_451486_, p_451297_, p_451193_, p_450918_, p_451010_);
         }
      } catch (Throwable var19) {
         CrashReport crashreport = CrashReport.forThrowable(var19, "Exception while adding particle");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
         crashreportcategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(p_451273_.getType()));
         crashreportcategory.setDetail(
            "Parameters", () -> ParticleTypes.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), p_451273_).toString()
         );
         crashreportcategory.setDetail("Position", () -> CrashReportCategory.formatLocation(this, p_451155_, p_451486_, p_451297_));
         throw new ReportedException(crashreport);
      }
   }

   private ParticleStatus calculateParticleLevel(boolean p_450948_) {
      ParticleStatus particlestatus = this.minecraft.options.particles().get();
      if (p_450948_ && particlestatus == ParticleStatus.MINIMAL && this.random.nextInt(10) == 0) {
         particlestatus = ParticleStatus.DECREASED;
      }

      if (particlestatus == ParticleStatus.DECREASED && this.random.nextInt(3) == 0) {
         particlestatus = ParticleStatus.MINIMAL;
      }

      return particlestatus;
   }

   @Override
   public List<AbstractClientPlayer> players() {
      return this.players;
   }

   public List<PartEntity<?>> dragonParts() {
      return this.dragonParts;
   }

   @Override
   public Holder<Biome> getUncachedNoiseBiome(int p_205516_, int p_205517_, int p_205518_) {
      return this.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
   }

   private int getSkyFlashTime() {
      return this.minecraft.options.hideLightningFlash().get() ? 0 : this.skyFlashTime;
   }

   @Override
   public void setSkyFlashTime(int p_104783_) {
      this.skyFlashTime = p_104783_;
   }

   @Override
   public float getShade(Direction p_104703_, boolean p_104704_) {
      DimensionType.CardinalLightType dimensiontype$cardinallighttype = this.dimensionType().cardinalLightType();
      if (!p_104704_) {
         return dimensiontype$cardinallighttype == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
      } else {
         return switch (p_104703_) {
            case DOWN -> dimensiontype$cardinallighttype == DimensionType.CardinalLightType.NETHER ? 0.9F : 0.5F;
            case UP -> dimensiontype$cardinallighttype == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
         };
      }
   }

   @Override
   public int getBlockTint(BlockPos p_104700_, ColorResolver p_104701_) {
      BlockTintCache blocktintcache = (BlockTintCache)this.tintCaches.get(p_104701_);
      return blocktintcache.getColor(p_104700_);
   }

   public int calculateBlockTint(BlockPos p_104763_, ColorResolver p_104764_) {
      int i = Minecraft.getInstance().options.biomeBlendRadius().get();
      if (i == 0) {
         return p_104764_.getColor(this.getBiome(p_104763_).value(), p_104763_.getX(), p_104763_.getZ());
      } else {
         int j = (i * 2 + 1) * (i * 2 + 1);
         int k = 0;
         int l = 0;
         int i1 = 0;
         Cursor3D cursor3d = new Cursor3D(
            p_104763_.getX() - i, p_104763_.getY(), p_104763_.getZ() - i, p_104763_.getX() + i, p_104763_.getY(), p_104763_.getZ() + i
         );
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         while (cursor3d.advance()) {
            blockpos$mutableblockpos.set(cursor3d.nextX(), cursor3d.nextY(), cursor3d.nextZ());
            int j1 = p_104764_.getColor(this.getBiome(blockpos$mutableblockpos).value(), blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ());
            k += (j1 & 0xFF0000) >> 16;
            l += (j1 & 0xFF00) >> 8;
            i1 += j1 & 0xFF;
         }

         return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | i1 / j & 0xFF;
      }
   }

   @Override
   public void setRespawnData(LevelData.RespawnData p_451417_) {
      this.levelData.setSpawn(this.getWorldBorderAdjustedRespawnData(p_451417_));
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
   public void gameEvent(Holder<GameEvent> p_316478_, Vec3 p_233640_, GameEvent.Context p_233641_) {
   }

   protected Map<MapId, MapItemSavedData> getAllMapData() {
      return ImmutableMap.copyOf(this.mapData);
   }

   protected void addMapData(Map<MapId, MapItemSavedData> p_171673_) {
      this.mapData.putAll(p_171673_);
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
   public void addDestroyBlockEffect(BlockPos p_171667_, BlockState p_171668_) {
      if (!p_171668_.isAir() && !IClientBlockExtensions.of(p_171668_).addDestroyEffects(p_171668_, this, p_171667_, this.minecraft.particleEngine)) {
         VoxelShape voxelshape = p_171668_.getShape(this, p_171667_);
         double d0 = 0.25;
         voxelshape.forAllBoxes(
            (p_445188_, p_445189_, p_445190_, p_445191_, p_445192_, p_445193_) -> {
               double d1 = Math.min(1.0, p_445191_ - p_445188_);
               double d2 = Math.min(1.0, p_445192_ - p_445189_);
               double d3 = Math.min(1.0, p_445193_ - p_445190_);
               int i = Math.max(2, Mth.ceil(d1 / 0.25));
               int j = Math.max(2, Mth.ceil(d2 / 0.25));
               int k = Math.max(2, Mth.ceil(d3 / 0.25));

               for (int l = 0; l < i; l++) {
                  for (int i1 = 0; i1 < j; i1++) {
                     for (int j1 = 0; j1 < k; j1++) {
                        double d4 = (l + 0.5) / i;
                        double d5 = (i1 + 0.5) / j;
                        double d6 = (j1 + 0.5) / k;
                        double d7 = d4 * d1 + p_445188_;
                        double d8 = d5 * d2 + p_445189_;
                        double d9 = d6 * d3 + p_445190_;
                        this.minecraft
                           .particleEngine
                           .add(
                              new TerrainParticle(
                                    this,
                                    p_171667_.getX() + d7,
                                    p_171667_.getY() + d8,
                                    p_171667_.getZ() + d9,
                                    d4 - 0.5,
                                    d5 - 0.5,
                                    d6 - 0.5,
                                    p_171668_,
                                    p_171667_
                                 )
                                 .updateSprite(p_171668_, p_171667_)
                           );
                     }
                  }
               }
            }
         );
      }
   }

   /** @deprecated */
   public void addBreakingBlockEffect(BlockPos p_446936_, Direction p_446144_) {
      this.addBreakingBlockEffect(p_446936_, p_446144_, null);
   }

   public void addBreakingBlockEffect(BlockPos p_446936_, Direction p_446144_, @Nullable HitResult hitResult) {
      BlockState blockstate = this.getBlockState(p_446936_);
      if (blockstate.getRenderShape() != RenderShape.INVISIBLE
         && !IClientBlockExtensions.of(blockstate).addHitEffects(blockstate, this, hitResult, this.minecraft.particleEngine)) {
         int i = p_446936_.getX();
         int j = p_446936_.getY();
         int k = p_446936_.getZ();
         float f = 0.1F;
         AABB aabb = blockstate.getShape(this, p_446936_).bounds();
         double d0 = i + this.random.nextDouble() * (aabb.maxX - aabb.minX - 0.2F) + 0.1F + aabb.minX;
         double d1 = j + this.random.nextDouble() * (aabb.maxY - aabb.minY - 0.2F) + 0.1F + aabb.minY;
         double d2 = k + this.random.nextDouble() * (aabb.maxZ - aabb.minZ - 0.2F) + 0.1F + aabb.minZ;
         if (p_446144_ == Direction.DOWN) {
            d1 = j + aabb.minY - 0.1F;
         }

         if (p_446144_ == Direction.UP) {
            d1 = j + aabb.maxY + 0.1F;
         }

         if (p_446144_ == Direction.NORTH) {
            d2 = k + aabb.minZ - 0.1F;
         }

         if (p_446144_ == Direction.SOUTH) {
            d2 = k + aabb.maxZ + 0.1F;
         }

         if (p_446144_ == Direction.WEST) {
            d0 = i + aabb.minX - 0.1F;
         }

         if (p_446144_ == Direction.EAST) {
            d0 = i + aabb.maxX + 0.1F;
         }

         this.minecraft
            .particleEngine
            .add(new TerrainParticle(this, d0, d1, d2, 0.0, 0.0, 0.0, blockstate, p_446936_).updateSprite(blockstate, p_446936_).setPower(0.2F).scale(0.6F));
      }
   }

   public void setServerSimulationDistance(int p_194175_) {
      this.serverSimulationDistance = p_194175_;
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
      @Nullable Entity p_363686_,
      @Nullable DamageSource p_365268_,
      @Nullable ExplosionDamageCalculator p_363795_,
      double p_360494_,
      double p_360591_,
      double p_364949_,
      float p_361233_,
      boolean p_364900_,
      Level.ExplosionInteraction p_365033_,
      ParticleOptions p_361889_,
      ParticleOptions p_361473_,
      WeightedList<ExplosionParticleInfo> p_437196_,
      Holder<SoundEvent> p_362359_
   ) {
   }

   @Override
   public int getSeaLevel() {
      return this.seaLevel;
   }

   @Override
   public int getClientLeafTintColor(BlockPos p_400248_) {
      return Minecraft.getInstance().getBlockColors().getColor(this.getBlockState(p_400248_), this, p_400248_, 0);
   }

   @Override
   public void registerForCleaning(CacheSlot<ClientLevel, ?> p_399472_) {
      this.connection.registerForCleaning(p_399472_);
   }

   public void trackExplosionEffects(Vec3 p_437247_, float p_437243_, int p_437352_, WeightedList<ExplosionParticleInfo> p_437277_) {
      this.explosionTracker.track(p_437247_, p_437243_, p_437352_, p_437277_);
   }

   @Override
   public ModelDataManager getModelDataManager() {
      return this.modelDataManager;
   }

   @Override
   public ModelData getModelData(BlockPos pos) {
      return this.modelDataManager.getAt(pos);
   }

   @Override
   public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
      DimensionType.CardinalLightType cardinalLightType = this.dimensionType().cardinalLightType();
      boolean constantAmbientLight = cardinalLightType == DimensionType.CardinalLightType.NETHER;
      if (!shade) {
         return constantAmbientLight ? 0.9F : 1.0F;
      } else {
         float yFactor = constantAmbientLight ? 0.9F : (normalY < 0.0F ? 0.5F : 1.0F);
         return Math.min(normalX * normalX * 0.6F + normalY * normalY * yFactor + normalZ * normalZ * 0.8F, 1.0F);
      }
   }

   @Internal
   @Override
   public void setDayTimeFraction(float dayTimeFraction) {
      this.dayTimeFraction = dayTimeFraction;
   }

   @Internal
   @Override
   public float getDayTimeFraction() {
      return this.dayTimeFraction;
   }

   @Override
   public float getDayTimePerTick() {
      return this.dayTimePerTick;
   }

   @Internal
   @Override
   public void setDayTimePerTick(float dayTimePerTick) {
      this.dayTimePerTick = dayTimePerTick;
   }

   @OnlyIn(Dist.CLIENT)
   public static class ClientLevelData implements WritableLevelData {
      private final boolean hardcore;
      private final boolean isFlat;
      private LevelData.RespawnData respawnData;
      private long gameTime;
      private long dayTime;
      private boolean raining;
      private Difficulty difficulty;
      private boolean difficultyLocked;

      public ClientLevelData(Difficulty p_104843_, boolean p_104844_, boolean p_104845_) {
         this.difficulty = p_104843_;
         this.hardcore = p_104844_;
         this.isFlat = p_104845_;
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

      public void setGameTime(long p_104850_) {
         this.gameTime = p_104850_;
      }

      public void setDayTime(long p_104864_) {
         this.dayTime = p_104864_;
      }

      @Override
      public void setSpawn(LevelData.RespawnData p_451388_) {
         this.respawnData = p_451388_;
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
      public void setRaining(boolean p_104866_) {
         this.raining = p_104866_;
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
      public void fillCrashReportCategory(CrashReportCategory p_171690_, LevelHeightAccessor p_171691_) {
         WritableLevelData.super.fillCrashReportCategory(p_171690_, p_171691_);
      }

      public void setDifficulty(Difficulty p_104852_) {
         CommonHooks.onDifficultyChange(p_104852_, this.difficulty);
         this.difficulty = p_104852_;
      }

      public void setDifficultyLocked(boolean p_104859_) {
         this.difficultyLocked = p_104859_;
      }

      public double getHorizonHeight(LevelHeightAccessor p_171688_) {
         return this.isFlat ? p_171688_.getMinY() : 63.0;
      }

      public float voidDarknessOnsetRange() {
         return this.isFlat ? 1.0F : 32.0F;
      }
   }

   @OnlyIn(Dist.CLIENT)
   final class EntityCallbacks implements LevelCallback<Entity> {
      public void onCreated(Entity p_171696_) {
      }

      public void onDestroyed(Entity p_171700_) {
      }

      public void onTickingStart(Entity p_171704_) {
         ClientLevel.this.tickingEntities.add(p_171704_);
      }

      public void onTickingEnd(Entity p_171708_) {
         ClientLevel.this.tickingEntities.remove(p_171708_);
      }

      public void onTrackingStart(Entity p_171712_) {
         switch (p_171712_) {
            case AbstractClientPlayer abstractclientplayer:
               ClientLevel.this.players.add(abstractclientplayer);
               break;
            case EnderDragon enderdragon:
               ClientLevel.this.dragonParts.addAll(Arrays.asList(enderdragon.getSubEntities()));
               break;
            default:
               if (p_171712_.isMultipartEntity()) {
                  ClientLevel.this.dragonParts.addAll(Arrays.asList(p_171712_.getParts()));
               }
         }
      }

      public void onTrackingEnd(Entity p_171716_) {
         p_171716_.unRide();
         switch (p_171716_) {
            case AbstractClientPlayer abstractclientplayer:
               ClientLevel.this.players.remove(abstractclientplayer);
               break;
            case EnderDragon enderdragon:
               ClientLevel.this.dragonParts.removeAll(Arrays.asList(enderdragon.getSubEntities()));
               break;
            default:
               if (p_171716_.isMultipartEntity()) {
                  ClientLevel.this.dragonParts.removeAll(Arrays.asList(p_171716_.getParts()));
               }
         }

         p_171716_.onRemovedFromLevel();
         NeoForge.EVENT_BUS.post(new EntityLeaveLevelEvent(p_171716_, ClientLevel.this));
      }

      public void onSectionChange(Entity p_233660_) {
      }
   }
}
