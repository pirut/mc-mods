package net.minecraft.server.level;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlayerSpawnFinder {
   private static final EntityDimensions PLAYER_DIMENSIONS = EntityType.PLAYER.getDimensions();
   private static final int ABSOLUTE_MAX_ATTEMPTS = 1024;
   private final ServerLevel level;
   private final BlockPos spawnSuggestion;
   private final int radius;
   private final int candidateCount;
   private final int coprime;
   private final int offset;
   private int nextCandidateIndex;
   private final CompletableFuture<Vec3> finishedFuture = new CompletableFuture<>();

   private PlayerSpawnFinder(ServerLevel level, BlockPos spawnSuggestion, int radius) {
      this.level = level;
      this.spawnSuggestion = spawnSuggestion;
      this.radius = radius;
      long l = radius * 2L + 1L;
      this.candidateCount = (int)Math.min(1024L, l * l);
      this.coprime = getCoprime(this.candidateCount);
      this.offset = RandomSource.create().nextInt(this.candidateCount);
   }

   public static CompletableFuture<Vec3> findSpawn(ServerLevel level, BlockPos pos) {
      if (level.dimensionType().hasSkyLight() && level.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
         int i = Math.max(0, level.getGameRules().get(GameRules.RESPAWN_RADIUS));
         int j = Mth.floor(level.getWorldBorder().getDistanceToBorder(pos.getX(), pos.getZ()));
         if (j < i) {
            i = j;
         }

         if (j <= 1) {
            i = 1;
         }

         PlayerSpawnFinder playerSpawnFinder = new PlayerSpawnFinder(level, pos, i);
         playerSpawnFinder.scheduleNext();
         return playerSpawnFinder.finishedFuture;
      } else {
         return CompletableFuture.completedFuture(fixupSpawnHeight(level, pos));
      }
   }

   private void scheduleNext() {
      int i = this.nextCandidateIndex++;
      if (i < this.candidateCount) {
         int j = (this.offset + this.coprime * i) % this.candidateCount;
         int k = j % (this.radius * 2 + 1);
         int l = j / (this.radius * 2 + 1);
         int m = this.spawnSuggestion.getX() + k - this.radius;
         int n = this.spawnSuggestion.getZ() + l - this.radius;
         this.scheduleCandidate(m, n, i, () -> {
            BlockPos blockPos = getOverworldRespawnPos(this.level, m, n);
            return blockPos != null && noCollisionNoLiquid(this.level, blockPos) ? Optional.of(Vec3.atBottomCenterOf(blockPos)) : Optional.empty();
         });
      } else {
         this.scheduleCandidate(
            this.spawnSuggestion.getX(), this.spawnSuggestion.getZ(), i, () -> Optional.of(fixupSpawnHeight(this.level, this.spawnSuggestion))
         );
      }
   }

   private static Vec3 fixupSpawnHeight(CollisionGetter collisionGetter, BlockPos pos) {
      BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

      while (!noCollisionNoLiquid(collisionGetter, mutableBlockPos) && mutableBlockPos.getY() < collisionGetter.getMaxY()) {
         mutableBlockPos.move(Direction.UP);
      }

      mutableBlockPos.move(Direction.DOWN);

      while (noCollisionNoLiquid(collisionGetter, mutableBlockPos) && mutableBlockPos.getY() > collisionGetter.getMinY()) {
         mutableBlockPos.move(Direction.DOWN);
      }

      mutableBlockPos.move(Direction.UP);
      return Vec3.atBottomCenterOf(mutableBlockPos);
   }

   private static boolean noCollisionNoLiquid(CollisionGetter collisionGetter, BlockPos pos) {
      return collisionGetter.noCollision(null, PLAYER_DIMENSIONS.makeBoundingBox(pos.getBottomCenter()), true);
   }

   private static int getCoprime(int candidateCount) {
      return candidateCount <= 16 ? candidateCount - 1 : 17;
   }

   private void scheduleCandidate(int x, int z, int index, Supplier<Optional<Vec3>> calculator) {
      if (!this.finishedFuture.isDone()) {
         int i = SectionPos.blockToSectionCoord(x);
         int j = SectionPos.blockToSectionCoord(z);
         this.level.getChunkSource().addTicketAndLoadWithRadius(TicketType.SPAWN_SEARCH, new ChunkPos(i, j), 0).whenCompleteAsync((object, throwable) -> {
            if (throwable == null) {
               try {
                  Optional<Vec3> optional = calculator.get();
                  if (optional.isPresent()) {
                     this.finishedFuture.complete(optional.get());
                  } else {
                     this.scheduleNext();
                  }
               } catch (Throwable var9) {
                  throwable = var9;
               }
            }

            if (throwable != null) {
               CrashReport crashReport = CrashReport.forThrowable(throwable, "Searching for spawn");
               CrashReportCategory crashReportCategory = crashReport.addCategory("Spawn Lookup");
               crashReportCategory.setDetail("Origin", this.spawnSuggestion::toString);
               crashReportCategory.setDetail("Radius", () -> Integer.toString(this.radius));
               crashReportCategory.setDetail("Candidate", () -> "[" + x + "," + z + "]");
               crashReportCategory.setDetail("Progress", () -> index + " out of " + this.candidateCount);
               this.finishedFuture.completeExceptionally(new ReportedException(crashReport));
            }
         }, this.level.getServer());
      }
   }

   @Nullable
   protected static BlockPos getOverworldRespawnPos(ServerLevel level, int x, int z) {
      boolean bl = level.dimensionType().hasCeiling();
      LevelChunk levelChunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
      int i = bl ? level.getChunkSource().getGenerator().getSpawnHeight(level) : levelChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
      if (i < level.getMinY()) {
         return null;
      } else {
         int j = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
         if (j <= i && j > levelChunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15)) {
            return null;
         } else {
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for (int k = i + 1; k >= level.getMinY(); k--) {
               mutableBlockPos.set(x, k, z);
               BlockState blockState = level.getBlockState(mutableBlockPos);
               if (!blockState.getFluidState().isEmpty()) {
                  break;
               }

               if (Block.isFaceFull(blockState.getCollisionShape(level, mutableBlockPos), Direction.UP)) {
                  return mutableBlockPos.above().immutable();
               }
            }

            return null;
         }
      }
   }

   @Nullable
   public static BlockPos getSpawnPosInChunk(ServerLevel level, ChunkPos chunkPos) {
      if (SharedConstants.debugVoidTerrain(chunkPos)) {
         return null;
      } else {
         for (int i = chunkPos.getMinBlockX(); i <= chunkPos.getMaxBlockX(); i++) {
            for (int j = chunkPos.getMinBlockZ(); j <= chunkPos.getMaxBlockZ(); j++) {
               BlockPos blockPos = getOverworldRespawnPos(level, i, j);
               if (blockPos != null) {
                  return blockPos;
               }
            }
         }

         return null;
      }
   }
}
