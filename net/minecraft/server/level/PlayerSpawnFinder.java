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

   private PlayerSpawnFinder(ServerLevel p_433567_, BlockPos p_434029_, int p_435614_) {
      this.level = p_433567_;
      this.spawnSuggestion = p_434029_;
      this.radius = p_435614_;
      long i = p_435614_ * 2L + 1L;
      this.candidateCount = (int)Math.min(1024L, i * i);
      this.coprime = getCoprime(this.candidateCount);
      this.offset = RandomSource.create().nextInt(this.candidateCount);
   }

   public static CompletableFuture<Vec3> findSpawn(ServerLevel p_433317_, BlockPos p_435318_) {
      if (p_433317_.dimensionType().hasSkyLight() && p_433317_.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
         int i = Math.max(0, p_433317_.getGameRules().get(GameRules.RESPAWN_RADIUS));
         int j = Mth.floor(p_433317_.getWorldBorder().getDistanceToBorder(p_435318_.getX(), p_435318_.getZ()));
         if (j < i) {
            i = j;
         }

         if (j <= 1) {
            i = 1;
         }

         PlayerSpawnFinder playerspawnfinder = new PlayerSpawnFinder(p_433317_, p_435318_, i);
         playerspawnfinder.scheduleNext();
         return playerspawnfinder.finishedFuture;
      } else {
         return CompletableFuture.completedFuture(fixupSpawnHeight(p_433317_, p_435318_));
      }
   }

   private void scheduleNext() {
      int i = this.nextCandidateIndex++;
      if (i < this.candidateCount) {
         int j = (this.offset + this.coprime * i) % this.candidateCount;
         int k = j % (this.radius * 2 + 1);
         int l = j / (this.radius * 2 + 1);
         int i1 = this.spawnSuggestion.getX() + k - this.radius;
         int j1 = this.spawnSuggestion.getZ() + l - this.radius;
         this.scheduleCandidate(i1, j1, i, () -> {
            BlockPos blockpos = getOverworldRespawnPos(this.level, i1, j1);
            return blockpos != null && noCollisionNoLiquid(this.level, blockpos) ? Optional.of(Vec3.atBottomCenterOf(blockpos)) : Optional.empty();
         });
      } else {
         this.scheduleCandidate(
            this.spawnSuggestion.getX(), this.spawnSuggestion.getZ(), i, () -> Optional.of(fixupSpawnHeight(this.level, this.spawnSuggestion))
         );
      }
   }

   private static Vec3 fixupSpawnHeight(CollisionGetter p_433902_, BlockPos p_433088_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_433088_.mutable();

      while (!noCollisionNoLiquid(p_433902_, blockpos$mutableblockpos) && blockpos$mutableblockpos.getY() < p_433902_.getMaxY()) {
         blockpos$mutableblockpos.move(Direction.UP);
      }

      blockpos$mutableblockpos.move(Direction.DOWN);

      while (noCollisionNoLiquid(p_433902_, blockpos$mutableblockpos) && blockpos$mutableblockpos.getY() > p_433902_.getMinY()) {
         blockpos$mutableblockpos.move(Direction.DOWN);
      }

      blockpos$mutableblockpos.move(Direction.UP);
      return Vec3.atBottomCenterOf(blockpos$mutableblockpos);
   }

   private static boolean noCollisionNoLiquid(CollisionGetter p_434756_, BlockPos p_433217_) {
      return p_434756_.noCollision(null, PLAYER_DIMENSIONS.makeBoundingBox(p_433217_.getBottomCenter()), true);
   }

   private static int getCoprime(int p_434574_) {
      return p_434574_ <= 16 ? p_434574_ - 1 : 17;
   }

   private void scheduleCandidate(int p_433998_, int p_435937_, int p_434762_, Supplier<Optional<Vec3>> p_435237_) {
      if (!this.finishedFuture.isDone()) {
         int i = SectionPos.blockToSectionCoord(p_433998_);
         int j = SectionPos.blockToSectionCoord(p_435937_);
         this.level.getChunkSource().addTicketAndLoadWithRadius(TicketType.SPAWN_SEARCH, new ChunkPos(i, j), 0).whenCompleteAsync((p_433344_, p_433338_) -> {
            if (p_433338_ == null) {
               try {
                  Optional<Vec3> optional = p_435237_.get();
                  if (optional.isPresent()) {
                     this.finishedFuture.complete(optional.get());
                  } else {
                     this.scheduleNext();
                  }
               } catch (Throwable var9) {
                  p_433338_ = var9;
               }
            }

            if (p_433338_ != null) {
               CrashReport crashreport = CrashReport.forThrowable(p_433338_, "Searching for spawn");
               CrashReportCategory crashreportcategory = crashreport.addCategory("Spawn Lookup");
               crashreportcategory.setDetail("Origin", this.spawnSuggestion::toString);
               crashreportcategory.setDetail("Radius", () -> Integer.toString(this.radius));
               crashreportcategory.setDetail("Candidate", () -> "[" + p_433998_ + "," + p_435937_ + "]");
               crashreportcategory.setDetail("Progress", () -> p_434762_ + " out of " + this.candidateCount);
               this.finishedFuture.completeExceptionally(new ReportedException(crashreport));
            }
         }, this.level.getServer());
      }
   }

   @Nullable
   protected static BlockPos getOverworldRespawnPos(ServerLevel p_432831_, int p_434718_, int p_436011_) {
      boolean flag = p_432831_.dimensionType().hasCeiling();
      LevelChunk levelchunk = p_432831_.getChunk(SectionPos.blockToSectionCoord(p_434718_), SectionPos.blockToSectionCoord(p_436011_));
      int i = flag
         ? p_432831_.getChunkSource().getGenerator().getSpawnHeight(p_432831_)
         : levelchunk.getHeight(Heightmap.Types.MOTION_BLOCKING, p_434718_ & 15, p_436011_ & 15);
      if (i < p_432831_.getMinY()) {
         return null;
      } else {
         int j = levelchunk.getHeight(Heightmap.Types.WORLD_SURFACE, p_434718_ & 15, p_436011_ & 15);
         if (j <= i && j > levelchunk.getHeight(Heightmap.Types.OCEAN_FLOOR, p_434718_ & 15, p_436011_ & 15)) {
            return null;
         } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int k = i + 1; k >= p_432831_.getMinY(); k--) {
               blockpos$mutableblockpos.set(p_434718_, k, p_436011_);
               BlockState blockstate = p_432831_.getBlockState(blockpos$mutableblockpos);
               if (!blockstate.getFluidState().isEmpty()) {
                  break;
               }

               if (Block.isFaceFull(blockstate.getCollisionShape(p_432831_, blockpos$mutableblockpos), Direction.UP)) {
                  return blockpos$mutableblockpos.above().immutable();
               }
            }

            return null;
         }
      }
   }

   @Nullable
   public static BlockPos getSpawnPosInChunk(ServerLevel p_433170_, ChunkPos p_434061_) {
      if (SharedConstants.debugVoidTerrain(p_434061_)) {
         return null;
      } else {
         for (int i = p_434061_.getMinBlockX(); i <= p_434061_.getMaxBlockX(); i++) {
            for (int j = p_434061_.getMinBlockZ(); j <= p_434061_.getMaxBlockZ(); j++) {
               BlockPos blockpos = getOverworldRespawnPos(p_433170_, i, j);
               if (blockpos != null) {
                  return blockpos;
               }
            }
         }

         return null;
      }
   }
}
