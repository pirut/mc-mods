package net.minecraft.util;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnUtil {
   public static <T extends Mob> Optional<T> trySpawnMob(
      EntityType<T> entityType,
      EntitySpawnReason spawnReason,
      ServerLevel level,
      BlockPos pos,
      int attempts,
      int range,
      int yOffset,
      SpawnUtil.Strategy strategy,
      boolean checkCollision
   ) {
      BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

      for (int i = 0; i < attempts; i++) {
         int j = Mth.randomBetweenInclusive(level.random, -range, range);
         int k = Mth.randomBetweenInclusive(level.random, -range, range);
         mutableBlockPos.setWithOffset(pos, j, yOffset, k);
         if (level.getWorldBorder().isWithinBounds(mutableBlockPos)
            && moveToPossibleSpawnPosition(level, yOffset, mutableBlockPos, strategy)
            && (
               !checkCollision
                  || level.noCollision(entityType.getSpawnAABB(mutableBlockPos.getX() + 0.5, mutableBlockPos.getY(), mutableBlockPos.getZ() + 0.5))
            )) {
            T mob = (T)entityType.create(level, null, mutableBlockPos, spawnReason, false, false);
            if (mob != null) {
               if (mob.checkSpawnRules(level, spawnReason) && mob.checkSpawnObstruction(level)) {
                  level.addFreshEntityWithPassengers(mob);
                  mob.playAmbientSound();
                  return Optional.of(mob);
               }

               mob.discard();
            }
         }
      }

      return Optional.empty();
   }

   private static boolean moveToPossibleSpawnPosition(ServerLevel level, int yOffset, BlockPos.MutableBlockPos pos, SpawnUtil.Strategy strategy) {
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos().set(pos);
      BlockState blockState = level.getBlockState(mutableBlockPos);

      for (int i = yOffset; i >= -yOffset; i--) {
         pos.move(Direction.DOWN);
         mutableBlockPos.setWithOffset(pos, Direction.UP);
         BlockState blockState2 = level.getBlockState(pos);
         if (strategy.canSpawnOn(level, pos, blockState2, mutableBlockPos, blockState)) {
            pos.move(Direction.UP);
            return true;
         }

         blockState = blockState2;
      }

      return false;
   }

   public interface Strategy {
      @Deprecated
      SpawnUtil.Strategy LEGACY_IRON_GOLEM = (serverLevel, blockPos, blockState, blockPos2, blockState2) -> !blockState.is(Blocks.COBWEB)
            && !blockState.is(Blocks.CACTUS)
            && !blockState.is(Blocks.GLASS_PANE)
            && !(blockState.getBlock() instanceof StainedGlassPaneBlock)
            && !(blockState.getBlock() instanceof StainedGlassBlock)
            && !(blockState.getBlock() instanceof LeavesBlock)
            && !blockState.is(Blocks.CONDUIT)
            && !blockState.is(Blocks.ICE)
            && !blockState.is(Blocks.TNT)
            && !blockState.is(Blocks.GLOWSTONE)
            && !blockState.is(Blocks.BEACON)
            && !blockState.is(Blocks.SEA_LANTERN)
            && !blockState.is(Blocks.FROSTED_ICE)
            && !blockState.is(Blocks.TINTED_GLASS)
            && !blockState.is(Blocks.GLASS)
         ? (blockState2.isAir() || blockState2.liquid()) && (blockState.isSolid() || blockState.is(Blocks.POWDER_SNOW))
         : false;
      SpawnUtil.Strategy ON_TOP_OF_COLLIDER = (serverLevel, blockPos, blockState, blockPos2, blockState2) -> blockState2.getCollisionShape(
               serverLevel, blockPos2
            )
            .isEmpty()
         && Block.isFaceFull(blockState.getCollisionShape(serverLevel, blockPos), Direction.UP);
      SpawnUtil.Strategy ON_TOP_OF_COLLIDER_NO_LEAVES = (serverLevel, blockPos, blockState, blockPos2, blockState2) -> blockState2.getCollisionShape(
               serverLevel, blockPos2
            )
            .isEmpty()
         && !blockState.is(BlockTags.LEAVES)
         && Block.isFaceFull(blockState.getCollisionShape(serverLevel, blockPos), Direction.UP);

      boolean canSpawnOn(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState, BlockPos blockPos2, BlockState blockState2);
   }
}
