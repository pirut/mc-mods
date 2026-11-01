package net.minecraft.world.entity.npc.wanderingtrader;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jspecify.annotations.Nullable;

public class WanderingTraderSpawner implements CustomSpawner {
   private static final int DEFAULT_TICK_DELAY = 1200;
   public static final int DEFAULT_SPAWN_DELAY = 24000;
   private static final int MIN_SPAWN_CHANCE = 25;
   private static final int MAX_SPAWN_CHANCE = 75;
   private static final int SPAWN_CHANCE_INCREASE = 25;
   private static final int SPAWN_ONE_IN_X_CHANCE = 10;
   private static final int NUMBER_OF_SPAWN_ATTEMPTS = 10;
   private final RandomSource random = RandomSource.create();
   private final ServerLevelData serverLevelData;
   private int tickDelay;
   private int spawnDelay;
   private int spawnChance;

   public WanderingTraderSpawner(ServerLevelData p_481813_) {
      this.serverLevelData = p_481813_;
      this.tickDelay = 1200;
      this.spawnDelay = p_481813_.getWanderingTraderSpawnDelay();
      this.spawnChance = p_481813_.getWanderingTraderSpawnChance();
      if (this.spawnDelay == 0 && this.spawnChance == 0) {
         this.spawnDelay = 24000;
         p_481813_.setWanderingTraderSpawnDelay(this.spawnDelay);
         this.spawnChance = 25;
         p_481813_.setWanderingTraderSpawnChance(this.spawnChance);
      }
   }

   @Override
   public void tick(ServerLevel p_480006_, boolean p_480449_) {
      if (p_480006_.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS) && --this.tickDelay <= 0) {
         this.tickDelay = 1200;
         this.spawnDelay -= 1200;
         this.serverLevelData.setWanderingTraderSpawnDelay(this.spawnDelay);
         if (this.spawnDelay <= 0) {
            this.spawnDelay = 24000;
            int i = this.spawnChance;
            this.spawnChance = Mth.clamp(this.spawnChance + 25, 25, 75);
            this.serverLevelData.setWanderingTraderSpawnChance(this.spawnChance);
            if (this.random.nextInt(100) <= i && this.spawn(p_480006_)) {
               this.spawnChance = 25;
            }
         }
      }
   }

   private boolean spawn(ServerLevel p_478959_) {
      Player player = p_478959_.getRandomPlayer();
      if (player == null) {
         return true;
      } else if (this.random.nextInt(10) != 0) {
         return false;
      } else {
         BlockPos blockpos = player.blockPosition();
         int i = 48;
         PoiManager poimanager = p_478959_.getPoiManager();
         Optional<BlockPos> optional = poimanager.find(p_477957_ -> p_477957_.is(PoiTypes.MEETING), p_479947_ -> true, blockpos, 48, PoiManager.Occupancy.ANY);
         BlockPos blockpos1 = optional.orElse(blockpos);
         BlockPos blockpos2 = this.findSpawnPositionNear(p_478959_, blockpos1, 48);
         if (blockpos2 != null && this.hasEnoughSpace(p_478959_, blockpos2)) {
            if (p_478959_.getBiome(blockpos2).is(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
               return false;
            }

            WanderingTrader wanderingtrader = EntityType.WANDERING_TRADER.spawn(p_478959_, blockpos2, EntitySpawnReason.EVENT);
            if (wanderingtrader != null) {
               for (int j = 0; j < 2; j++) {
                  this.tryToSpawnLlamaFor(p_478959_, wanderingtrader, 4);
               }

               this.serverLevelData.setWanderingTraderId(wanderingtrader.getUUID());
               wanderingtrader.setDespawnDelay(48000);
               wanderingtrader.setWanderTarget(blockpos1);
               wanderingtrader.setHomeTo(blockpos1, 16);
               return true;
            }
         }

         return false;
      }
   }

   private void tryToSpawnLlamaFor(ServerLevel p_479125_, WanderingTrader p_478417_, int p_478826_) {
      BlockPos blockpos = this.findSpawnPositionNear(p_479125_, p_478417_.blockPosition(), p_478826_);
      if (blockpos != null) {
         TraderLlama traderllama = EntityType.TRADER_LLAMA.spawn(p_479125_, blockpos, EntitySpawnReason.EVENT);
         if (traderllama != null) {
            traderllama.setLeashedTo(p_478417_, true);
         }
      }
   }

   @Nullable
   private BlockPos findSpawnPositionNear(LevelReader p_478901_, BlockPos p_482027_, int p_478609_) {
      BlockPos blockpos = null;
      SpawnPlacementType spawnplacementtype = SpawnPlacements.getPlacementType(EntityType.WANDERING_TRADER);

      for (int i = 0; i < 10; i++) {
         int j = p_482027_.getX() + this.random.nextInt(p_478609_ * 2) - p_478609_;
         int k = p_482027_.getZ() + this.random.nextInt(p_478609_ * 2) - p_478609_;
         int l = p_478901_.getHeight(Heightmap.Types.WORLD_SURFACE, j, k);
         BlockPos blockpos1 = new BlockPos(j, l, k);
         if (spawnplacementtype.isSpawnPositionOk(p_478901_, blockpos1, EntityType.WANDERING_TRADER)) {
            blockpos = blockpos1;
            break;
         }
      }

      return blockpos;
   }

   private boolean hasEnoughSpace(BlockGetter p_482135_, BlockPos p_478428_) {
      for (BlockPos blockpos : BlockPos.betweenClosed(p_478428_, p_478428_.offset(1, 2, 1))) {
         if (!p_482135_.getBlockState(blockpos).getCollisionShape(p_482135_, blockpos).isEmpty()) {
            return false;
         }
      }

      return true;
   }
}
