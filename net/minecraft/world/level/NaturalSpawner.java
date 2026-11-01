package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class NaturalSpawner {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MIN_SPAWN_DISTANCE = 24;
   public static final int SPAWN_DISTANCE_CHUNK = 8;
   public static final int SPAWN_DISTANCE_BLOCK = 128;
   public static final int INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK = Mth.floor(8.0F / Mth.SQRT_OF_TWO);
   static final int MAGIC_NUMBER = (int)Math.pow(17.0, 2.0);
   private static final MobCategory[] SPAWNING_CATEGORIES = Stream.of(MobCategory.values())
      .filter(mobCategory -> mobCategory != MobCategory.MISC)
      .toArray(MobCategory[]::new);

   private NaturalSpawner() {
   }

   public static NaturalSpawner.SpawnState createState(
      int spawnableChunkCount, Iterable<Entity> entities, NaturalSpawner.ChunkGetter chunkGetter, LocalMobCapCalculator calculator
   ) {
      PotentialCalculator potentialCalculator = new PotentialCalculator();
      Object2IntOpenHashMap<MobCategory> object2IntOpenHashMap = new Object2IntOpenHashMap();

      for (Entity entity : entities) {
         if (!(entity instanceof Mob mob && (mob.isPersistenceRequired() || mob.requiresCustomPersistence()))) {
            MobCategory mobCategory = entity.getType().getCategory();
            if (mobCategory != MobCategory.MISC) {
               BlockPos blockPos = entity.blockPosition();
               chunkGetter.query(ChunkPos.asLong(blockPos), levelChunk -> {
                  MobSpawnSettings.MobSpawnCost mobSpawnCost = getRoughBiome(blockPos, levelChunk).getMobSettings().getMobSpawnCost(entity.getType());
                  if (mobSpawnCost != null) {
                     potentialCalculator.addCharge(entity.blockPosition(), mobSpawnCost.charge());
                  }

                  if (entity instanceof Mob) {
                     calculator.addMob(levelChunk.getPos(), mobCategory);
                  }

                  object2IntOpenHashMap.addTo(mobCategory, 1);
               });
            }
         }
      }

      return new NaturalSpawner.SpawnState(spawnableChunkCount, object2IntOpenHashMap, potentialCalculator, calculator);
   }

   static Biome getRoughBiome(BlockPos pos, ChunkAccess chunk) {
      return chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())).value();
   }

   public static List<MobCategory> getFilteredSpawningCategories(
      NaturalSpawner.SpawnState spawnState, boolean spawnFriendlies, boolean spawnEnemies, boolean spawnPassives
   ) {
      List<MobCategory> list = new ArrayList<>(SPAWNING_CATEGORIES.length);

      for (MobCategory mobCategory : SPAWNING_CATEGORIES) {
         if ((spawnFriendlies || !mobCategory.isFriendly())
            && (spawnEnemies || mobCategory.isFriendly())
            && (spawnPassives || !mobCategory.isPersistent())
            && spawnState.canSpawnForCategoryGlobal(mobCategory)) {
            list.add(mobCategory);
         }
      }

      return list;
   }

   public static void spawnForChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState spawnState, List<MobCategory> categories) {
      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("spawner");

      for (MobCategory mobCategory : categories) {
         if (spawnState.canSpawnForCategoryLocal(mobCategory, chunk.getPos())) {
            spawnCategoryForChunk(mobCategory, level, chunk, spawnState::canSpawn, spawnState::afterSpawn);
         }
      }

      profilerFiller.pop();
   }

   public static void spawnCategoryForChunk(
      MobCategory category, ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate filter, NaturalSpawner.AfterSpawnCallback callback
   ) {
      BlockPos blockPos = getRandomPosWithin(level, chunk);
      if (blockPos.getY() >= level.getMinY() + 1) {
         spawnCategoryForPosition(category, level, chunk, blockPos, filter, callback);
      }
   }

   @VisibleForDebug
   public static void spawnCategoryForPosition(MobCategory category, ServerLevel level, BlockPos pos) {
      spawnCategoryForPosition(category, level, level.getChunk(pos), pos, (entityType, blockPos, chunkAccess) -> true, (mob, chunkAccess) -> {});
   }

   public static void spawnCategoryForPosition(
      MobCategory category,
      ServerLevel level,
      ChunkAccess chunk,
      BlockPos pos,
      NaturalSpawner.SpawnPredicate filter,
      NaturalSpawner.AfterSpawnCallback callback
   ) {
      StructureManager structureManager = level.structureManager();
      ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
      int i = pos.getY();
      BlockState blockState = chunk.getBlockState(pos);
      if (!blockState.isRedstoneConductor(chunk, pos)) {
         BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
         int j = 0;

         for (int k = 0; k < 3; k++) {
            int l = pos.getX();
            int m = pos.getZ();
            int n = 6;
            MobSpawnSettings.SpawnerData spawnerData = null;
            SpawnGroupData spawnGroupData = null;
            int o = Mth.ceil(level.random.nextFloat() * 4.0F);
            int p = 0;

            for (int q = 0; q < o; q++) {
               l += level.random.nextInt(6) - level.random.nextInt(6);
               m += level.random.nextInt(6) - level.random.nextInt(6);
               mutableBlockPos.set(l, i, m);
               double d = l + 0.5;
               double e = m + 0.5;
               Player player = level.getNearestPlayer(d, i, e, -1.0, false);
               if (player != null) {
                  double f = player.distanceToSqr(d, i, e);
                  if (isRightDistanceToPlayerAndSpawnPoint(level, chunk, mutableBlockPos, f)) {
                     if (spawnerData == null) {
                        Optional<MobSpawnSettings.SpawnerData> optional = getRandomSpawnMobAt(
                           level, structureManager, chunkGenerator, category, level.random, mutableBlockPos
                        );
                        if (optional.isEmpty()) {
                           break;
                        }

                        spawnerData = optional.get();
                        o = spawnerData.minCount() + level.random.nextInt(1 + spawnerData.maxCount() - spawnerData.minCount());
                     }

                     if (isValidSpawnPostitionForType(level, category, structureManager, chunkGenerator, spawnerData, mutableBlockPos, f)
                        && filter.test(spawnerData.type(), mutableBlockPos, chunk)) {
                        Mob mob = getMobForSpawn(level, spawnerData.type());
                        if (mob == null) {
                           return;
                        }

                        mob.snapTo(d, i, e, level.random.nextFloat() * 360.0F, 0.0F);
                        if (isValidPositionForMob(level, mob, f)) {
                           spawnGroupData = mob.finalizeSpawn(
                              level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.NATURAL, spawnGroupData
                           );
                           j++;
                           p++;
                           level.addFreshEntityWithPassengers(mob);
                           callback.run(mob, chunk);
                           if (j >= mob.getMaxSpawnClusterSize()) {
                              return;
                           }

                           if (mob.isMaxGroupSizeReached(p)) {
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isRightDistanceToPlayerAndSpawnPoint(ServerLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos pos, double distance) {
      if (distance <= 576.0) {
         return false;
      } else {
         LevelData.RespawnData respawnData = level.getRespawnData();
         if (respawnData.dimension() == level.dimension()
            && respawnData.pos().closerToCenterThan(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), 24.0)) {
            return false;
         } else {
            ChunkPos chunkPos = new ChunkPos(pos);
            return Objects.equals(chunkPos, chunk.getPos()) || level.canSpawnEntitiesInChunk(chunkPos);
         }
      }
   }

   private static boolean isValidSpawnPostitionForType(
      ServerLevel level,
      MobCategory category,
      StructureManager structureManager,
      ChunkGenerator generator,
      MobSpawnSettings.SpawnerData data,
      BlockPos.MutableBlockPos pos,
      double distance
   ) {
      EntityType<?> entityType = data.type();
      if (entityType.getCategory() == MobCategory.MISC) {
         return false;
      } else if (!entityType.canSpawnFarFromPlayer()
         && distance > entityType.getCategory().getDespawnDistance() * entityType.getCategory().getDespawnDistance()) {
         return false;
      } else if (!entityType.canSummon() || !canSpawnMobAt(level, structureManager, generator, category, data, pos)) {
         return false;
      } else if (!SpawnPlacements.isSpawnPositionOk(entityType, level, pos)) {
         return false;
      } else {
         return !SpawnPlacements.checkSpawnRules(entityType, level, EntitySpawnReason.NATURAL, pos, level.random)
            ? false
            : level.noCollision(entityType.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
      }
   }

   @Nullable
   private static Mob getMobForSpawn(ServerLevel level, EntityType<?> entityType) {
      try {
         if (entityType.create(level, EntitySpawnReason.NATURAL) instanceof Mob mob) {
            return mob;
         }

         LOGGER.warn("Can't spawn entity of type: {}", BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
      } catch (Exception var4) {
         LOGGER.warn("Failed to create mob", var4);
      }

      return null;
   }

   private static boolean isValidPositionForMob(ServerLevel level, Mob mob, double distance) {
      return distance > mob.getType().getCategory().getDespawnDistance() * mob.getType().getCategory().getDespawnDistance() && mob.removeWhenFarAway(distance)
         ? false
         : mob.checkSpawnRules(level, EntitySpawnReason.NATURAL) && mob.checkSpawnObstruction(level);
   }

   private static Optional<MobSpawnSettings.SpawnerData> getRandomSpawnMobAt(
      ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory category, RandomSource random, BlockPos pos
   ) {
      Holder<Biome> holder = level.getBiome(pos);
      return category == MobCategory.WATER_AMBIENT && holder.is(BiomeTags.REDUCED_WATER_AMBIENT_SPAWNS) && random.nextFloat() < 0.98F
         ? Optional.empty()
         : mobsAt(level, structureManager, generator, category, pos, holder).getRandom(random);
   }

   private static boolean canSpawnMobAt(
      ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory category, MobSpawnSettings.SpawnerData data, BlockPos pos
   ) {
      return mobsAt(level, structureManager, generator, category, pos, null).contains(data);
   }

   private static WeightedList<MobSpawnSettings.SpawnerData> mobsAt(
      ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory category, BlockPos pos, @Nullable Holder<Biome> biome
   ) {
      return isInNetherFortressBounds(pos, level, category, structureManager)
         ? NetherFortressStructure.FORTRESS_ENEMIES
         : generator.getMobsAt(biome != null ? biome : level.getBiome(pos), structureManager, category, pos);
   }

   public static boolean isInNetherFortressBounds(BlockPos pos, ServerLevel level, MobCategory category, StructureManager structureManager) {
      if (category == MobCategory.MONSTER && level.getBlockState(pos.below()).is(Blocks.NETHER_BRICKS)) {
         Structure structure = structureManager.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(BuiltinStructures.FORTRESS);
         return structure == null ? false : structureManager.getStructureAt(pos, structure).isValid();
      } else {
         return false;
      }
   }

   private static BlockPos getRandomPosWithin(Level level, LevelChunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      int i = chunkPos.getMinBlockX() + level.random.nextInt(16);
      int j = chunkPos.getMinBlockZ() + level.random.nextInt(16);
      int k = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, i, j) + 1;
      int l = Mth.randomBetweenInclusive(level.random, level.getMinY(), k);
      return new BlockPos(i, l, j);
   }

   public static boolean isValidEmptySpawnBlock(BlockGetter block, BlockPos pos, BlockState blockState, FluidState fluidState, EntityType<?> entityType) {
      if (blockState.isCollisionShapeFullBlock(block, pos)) {
         return false;
      } else if (blockState.isSignalSource()) {
         return false;
      } else if (!fluidState.isEmpty()) {
         return false;
      } else {
         return blockState.is(BlockTags.PREVENT_MOB_SPAWNING_INSIDE) ? false : !entityType.isBlockDangerous(blockState);
      }
   }

   public static void spawnMobsForChunkGeneration(ServerLevelAccessor levelAccessor, Holder<Biome> biome, ChunkPos chunkPos, RandomSource random) {
      MobSpawnSettings mobSpawnSettings = biome.value().getMobSettings();
      WeightedList<MobSpawnSettings.SpawnerData> weightedList = mobSpawnSettings.getMobs(MobCategory.CREATURE);
      if (!weightedList.isEmpty() && levelAccessor.getLevel().getGameRules().get(GameRules.SPAWN_MOBS)) {
         int i = chunkPos.getMinBlockX();
         int j = chunkPos.getMinBlockZ();

         while (random.nextFloat() < mobSpawnSettings.getCreatureProbability()) {
            Optional<MobSpawnSettings.SpawnerData> optional = weightedList.getRandom(random);
            if (!optional.isEmpty()) {
               MobSpawnSettings.SpawnerData spawnerData = optional.get();
               int k = spawnerData.minCount() + random.nextInt(1 + spawnerData.maxCount() - spawnerData.minCount());
               SpawnGroupData spawnGroupData = null;
               int l = i + random.nextInt(16);
               int m = j + random.nextInt(16);
               int n = l;
               int o = m;

               for (int p = 0; p < k; p++) {
                  boolean bl = false;

                  for (int q = 0; !bl && q < 4; q++) {
                     BlockPos blockPos = getTopNonCollidingPos(levelAccessor, spawnerData.type(), l, m);
                     if (spawnerData.type().canSummon() && SpawnPlacements.isSpawnPositionOk(spawnerData.type(), levelAccessor, blockPos)) {
                        float f = spawnerData.type().getWidth();
                        double d = Mth.clamp((double)l, (double)i + f, i + 16.0 - f);
                        double e = Mth.clamp((double)m, (double)j + f, j + 16.0 - f);
                        if (!levelAccessor.noCollision(spawnerData.type().getSpawnAABB(d, blockPos.getY(), e))
                           || !SpawnPlacements.checkSpawnRules(
                              spawnerData.type(),
                              levelAccessor,
                              EntitySpawnReason.CHUNK_GENERATION,
                              BlockPos.containing(d, blockPos.getY(), e),
                              levelAccessor.getRandom()
                           )) {
                           continue;
                        }

                        Entity entity;
                        try {
                           entity = spawnerData.type().create(levelAccessor.getLevel(), EntitySpawnReason.NATURAL);
                        } catch (Exception var27) {
                           LOGGER.warn("Failed to create mob", var27);
                           continue;
                        }

                        if (entity == null) {
                           continue;
                        }

                        entity.snapTo(d, blockPos.getY(), e, random.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof Mob mob
                           && mob.checkSpawnRules(levelAccessor, EntitySpawnReason.CHUNK_GENERATION)
                           && mob.checkSpawnObstruction(levelAccessor)) {
                           spawnGroupData = mob.finalizeSpawn(
                              levelAccessor, levelAccessor.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.CHUNK_GENERATION, spawnGroupData
                           );
                           levelAccessor.addFreshEntityWithPassengers(mob);
                           bl = true;
                        }
                     }

                     l += random.nextInt(5) - random.nextInt(5);

                     for (m += random.nextInt(5) - random.nextInt(5);
                        l < i || l >= i + 16 || m < j || m >= j + 16;
                        m = o + random.nextInt(5) - random.nextInt(5)
                     ) {
                        l = n + random.nextInt(5) - random.nextInt(5);
                     }
                  }
               }
            }
         }
      }
   }

   private static BlockPos getTopNonCollidingPos(LevelReader level, EntityType<?> entityType, int x, int z) {
      int i = level.getHeight(SpawnPlacements.getHeightmapType(entityType), x, z);
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(x, i, z);
      if (level.dimensionType().hasCeiling()) {
         do {
            mutableBlockPos.move(Direction.DOWN);
         } while (!level.getBlockState(mutableBlockPos).isAir());

         do {
            mutableBlockPos.move(Direction.DOWN);
         } while (level.getBlockState(mutableBlockPos).isAir() && mutableBlockPos.getY() > level.getMinY());
      }

      return SpawnPlacements.getPlacementType(entityType).adjustSpawnPosition(level, mutableBlockPos.immutable());
   }

   @FunctionalInterface
   public interface AfterSpawnCallback {
      void run(Mob mob, ChunkAccess chunkAccess);
   }

   @FunctionalInterface
   public interface ChunkGetter {
      void query(long l, Consumer<LevelChunk> consumer);
   }

   @FunctionalInterface
   public interface SpawnPredicate {
      boolean test(EntityType<?> entityType, BlockPos blockPos, ChunkAccess chunkAccess);
   }

   public static class SpawnState {
      private final int spawnableChunkCount;
      private final Object2IntOpenHashMap<MobCategory> mobCategoryCounts;
      private final PotentialCalculator spawnPotential;
      private final Object2IntMap<MobCategory> unmodifiableMobCategoryCounts;
      private final LocalMobCapCalculator localMobCapCalculator;
      @Nullable
      private BlockPos lastCheckedPos;
      @Nullable
      private EntityType<?> lastCheckedType;
      private double lastCharge;

      SpawnState(
         int spawnableChunkCount,
         Object2IntOpenHashMap<MobCategory> mobCategoryCounts,
         PotentialCalculator spawnPotential,
         LocalMobCapCalculator localMobCapCalculator
      ) {
         this.spawnableChunkCount = spawnableChunkCount;
         this.mobCategoryCounts = mobCategoryCounts;
         this.spawnPotential = spawnPotential;
         this.localMobCapCalculator = localMobCapCalculator;
         this.unmodifiableMobCategoryCounts = Object2IntMaps.unmodifiable(mobCategoryCounts);
      }

      private boolean canSpawn(EntityType<?> entityType, BlockPos pos, ChunkAccess chunk) {
         this.lastCheckedPos = pos;
         this.lastCheckedType = entityType;
         MobSpawnSettings.MobSpawnCost mobSpawnCost = NaturalSpawner.getRoughBiome(pos, chunk).getMobSettings().getMobSpawnCost(entityType);
         if (mobSpawnCost == null) {
            this.lastCharge = 0.0;
            return true;
         } else {
            double d = mobSpawnCost.charge();
            this.lastCharge = d;
            double e = this.spawnPotential.getPotentialEnergyChange(pos, d);
            return e <= mobSpawnCost.energyBudget();
         }
      }

      private void afterSpawn(Mob mob, ChunkAccess chunk) {
         EntityType<?> entityType = mob.getType();
         BlockPos blockPos = mob.blockPosition();
         double d;
         if (blockPos.equals(this.lastCheckedPos) && entityType == this.lastCheckedType) {
            d = this.lastCharge;
         } else {
            MobSpawnSettings.MobSpawnCost mobSpawnCost = NaturalSpawner.getRoughBiome(blockPos, chunk).getMobSettings().getMobSpawnCost(entityType);
            if (mobSpawnCost != null) {
               d = mobSpawnCost.charge();
            } else {
               d = 0.0;
            }
         }

         this.spawnPotential.addCharge(blockPos, d);
         MobCategory mobCategory = entityType.getCategory();
         this.mobCategoryCounts.addTo(mobCategory, 1);
         this.localMobCapCalculator.addMob(new ChunkPos(blockPos), mobCategory);
      }

      public int getSpawnableChunkCount() {
         return this.spawnableChunkCount;
      }

      public Object2IntMap<MobCategory> getMobCategoryCounts() {
         return this.unmodifiableMobCategoryCounts;
      }

      boolean canSpawnForCategoryGlobal(MobCategory category) {
         int i = category.getMaxInstancesPerChunk() * this.spawnableChunkCount / NaturalSpawner.MAGIC_NUMBER;
         return this.mobCategoryCounts.getInt(category) < i;
      }

      boolean canSpawnForCategoryLocal(MobCategory category, ChunkPos chunkPos) {
         return this.localMobCapCalculator.canSpawn(category, chunkPos) || SharedConstants.DEBUG_IGNORE_LOCAL_MOB_CAP;
      }
   }
}
