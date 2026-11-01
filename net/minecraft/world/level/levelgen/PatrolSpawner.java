package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class PatrolSpawner implements CustomSpawner {
   private int nextTick;

   @Override
   public int tick(ServerLevel p_64570_, boolean p_64571_, boolean p_64572_) {
      if (!p_64571_) {
         return 0;
      } else if (!p_64570_.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
         return 0;
      } else {
         RandomSource $$3 = p_64570_.random;
         this.nextTick--;
         if (this.nextTick > 0) {
            return 0;
         } else {
            this.nextTick = this.nextTick + 12000 + $$3.nextInt(1200);
            long $$4 = p_64570_.getDayTime() / 24000L;
            if ($$4 < 5L || !p_64570_.isDay()) {
               return 0;
            } else if ($$3.nextInt(5) != 0) {
               return 0;
            } else {
               int $$5 = p_64570_.players().size();
               if ($$5 < 1) {
                  return 0;
               } else {
                  Player $$6 = p_64570_.players().get($$3.nextInt($$5));
                  if ($$6.isSpectator()) {
                     return 0;
                  } else if (p_64570_.isCloseToVillage($$6.blockPosition(), 2)) {
                     return 0;
                  } else {
                     int $$7 = (24 + $$3.nextInt(24)) * ($$3.nextBoolean() ? -1 : 1);
                     int $$8 = (24 + $$3.nextInt(24)) * ($$3.nextBoolean() ? -1 : 1);
                     BlockPos.MutableBlockPos $$9 = $$6.blockPosition().mutable().move($$7, 0, $$8);
                     int $$10 = 10;
                     if (!p_64570_.hasChunksAt($$9.getX() - 10, $$9.getZ() - 10, $$9.getX() + 10, $$9.getZ() + 10)) {
                        return 0;
                     } else {
                        Holder<Biome> $$11 = p_64570_.getBiome($$9);
                        if ($$11.is(BiomeTags.WITHOUT_PATROL_SPAWNS)) {
                           return 0;
                        } else {
                           int $$12 = 0;
                           int $$13 = (int)Math.ceil(p_64570_.getCurrentDifficultyAt($$9).getEffectiveDifficulty()) + 1;

                           for (int $$14 = 0; $$14 < $$13; $$14++) {
                              $$12++;
                              $$9.setY(p_64570_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, $$9).getY());
                              if ($$14 == 0) {
                                 if (!this.spawnPatrolMember(p_64570_, $$9, $$3, true)) {
                                    break;
                                 }
                              } else {
                                 this.spawnPatrolMember(p_64570_, $$9, $$3, false);
                              }

                              $$9.setX($$9.getX() + $$3.nextInt(5) - $$3.nextInt(5));
                              $$9.setZ($$9.getZ() + $$3.nextInt(5) - $$3.nextInt(5));
                           }

                           return $$12;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean spawnPatrolMember(ServerLevel p_224533_, BlockPos p_224534_, RandomSource p_224535_, boolean p_224536_) {
      BlockState $$4 = p_224533_.getBlockState(p_224534_);
      if (!NaturalSpawner.isValidEmptySpawnBlock(p_224533_, p_224534_, $$4, $$4.getFluidState(), EntityType.PILLAGER)) {
         return false;
      } else if (!PatrollingMonster.checkPatrollingMonsterSpawnRules(EntityType.PILLAGER, p_224533_, MobSpawnType.PATROL, p_224534_, p_224535_)) {
         return false;
      } else {
         PatrollingMonster $$5 = EntityType.PILLAGER.create(p_224533_);
         if ($$5 != null) {
            if (p_224536_) {
               $$5.setPatrolLeader(true);
               $$5.findPatrolTarget();
            }

            $$5.setPos(p_224534_.getX(), p_224534_.getY(), p_224534_.getZ());
            $$5.finalizeSpawn(p_224533_, p_224533_.getCurrentDifficultyAt(p_224534_), MobSpawnType.PATROL, null);
            p_224533_.addFreshEntityWithPassengers($$5);
            return true;
         } else {
            return false;
         }
      }
   }
}
