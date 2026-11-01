package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;

public class PhantomSpawner implements CustomSpawner {
   private int nextTick;

   @Override
   public void tick(ServerLevel level, boolean spawnEnemies) {
      if (spawnEnemies) {
         if (level.getGameRules().get(GameRules.SPAWN_PHANTOMS)) {
            RandomSource randomSource = level.random;
            this.nextTick--;
            if (this.nextTick <= 0) {
               this.nextTick = this.nextTick + (60 + randomSource.nextInt(60)) * 20;
               if (level.getSkyDarken() >= 5 || !level.dimensionType().hasSkyLight()) {
                  for (ServerPlayer serverPlayer : level.players()) {
                     if (!serverPlayer.isSpectator()) {
                        BlockPos blockPos = serverPlayer.blockPosition();
                        if (!level.dimensionType().hasSkyLight() || blockPos.getY() >= level.getSeaLevel() && level.canSeeSky(blockPos)) {
                           DifficultyInstance difficultyInstance = level.getCurrentDifficultyAt(blockPos);
                           if (difficultyInstance.isHarderThan(randomSource.nextFloat() * 3.0F)) {
                              ServerStatsCounter serverStatsCounter = serverPlayer.getStats();
                              int i = Mth.clamp(serverStatsCounter.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
                              int j = 24000;
                              if (randomSource.nextInt(i) >= 72000) {
                                 BlockPos blockPos2 = blockPos.above(20 + randomSource.nextInt(15))
                                    .east(-10 + randomSource.nextInt(21))
                                    .south(-10 + randomSource.nextInt(21));
                                 BlockState blockState = level.getBlockState(blockPos2);
                                 FluidState fluidState = level.getFluidState(blockPos2);
                                 if (NaturalSpawner.isValidEmptySpawnBlock(level, blockPos2, blockState, fluidState, EntityType.PHANTOM)) {
                                    SpawnGroupData spawnGroupData = null;
                                    int k = 1 + randomSource.nextInt(difficultyInstance.getDifficulty().getId() + 1);

                                    for (int l = 0; l < k; l++) {
                                       Phantom phantom = EntityType.PHANTOM.create(level, EntitySpawnReason.NATURAL);
                                       if (phantom != null) {
                                          phantom.snapTo(blockPos2, 0.0F, 0.0F);
                                          spawnGroupData = phantom.finalizeSpawn(level, difficultyInstance, EntitySpawnReason.NATURAL, spawnGroupData);
                                          level.addFreshEntityWithPassengers(phantom);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
