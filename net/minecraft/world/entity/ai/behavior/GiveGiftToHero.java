package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.RaidHeroGift;

public class GiveGiftToHero extends Behavior<Villager> {
   private static final int THROW_GIFT_AT_DISTANCE = 5;
   private static final int MIN_TIME_BETWEEN_GIFTS = 600;
   private static final int MAX_TIME_BETWEEN_GIFTS = 6600;
   private static final int TIME_TO_DELAY_FOR_HEAD_TO_FINISH_TURNING = 20;
   @Deprecated
   private static final Map<ResourceKey<VillagerProfession>, ResourceKey<LootTable>> GIFTS = ImmutableMap.builder()
      .put(VillagerProfession.ARMORER, BuiltInLootTables.ARMORER_GIFT)
      .put(VillagerProfession.BUTCHER, BuiltInLootTables.BUTCHER_GIFT)
      .put(VillagerProfession.CARTOGRAPHER, BuiltInLootTables.CARTOGRAPHER_GIFT)
      .put(VillagerProfession.CLERIC, BuiltInLootTables.CLERIC_GIFT)
      .put(VillagerProfession.FARMER, BuiltInLootTables.FARMER_GIFT)
      .put(VillagerProfession.FISHERMAN, BuiltInLootTables.FISHERMAN_GIFT)
      .put(VillagerProfession.FLETCHER, BuiltInLootTables.FLETCHER_GIFT)
      .put(VillagerProfession.LEATHERWORKER, BuiltInLootTables.LEATHERWORKER_GIFT)
      .put(VillagerProfession.LIBRARIAN, BuiltInLootTables.LIBRARIAN_GIFT)
      .put(VillagerProfession.MASON, BuiltInLootTables.MASON_GIFT)
      .put(VillagerProfession.SHEPHERD, BuiltInLootTables.SHEPHERD_GIFT)
      .put(VillagerProfession.TOOLSMITH, BuiltInLootTables.TOOLSMITH_GIFT)
      .put(VillagerProfession.WEAPONSMITH, BuiltInLootTables.WEAPONSMITH_GIFT)
      .build();
   private static final float SPEED_MODIFIER = 0.5F;
   private int timeUntilNextGift = 600;
   private boolean giftGivenDuringThisRun;
   private long timeSinceStart;

   public GiveGiftToHero(int p_22992_) {
      super(
         ImmutableMap.of(
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.INTERACTION_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryStatus.VALUE_PRESENT
         ),
         p_22992_
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_23003_, Villager p_482144_) {
      if (!this.isHeroVisible(p_482144_)) {
         return false;
      } else if (this.timeUntilNextGift > 0) {
         this.timeUntilNextGift--;
         return false;
      } else {
         return true;
      }
   }

   protected void start(ServerLevel p_23006_, Villager p_481747_, long p_23008_) {
      this.giftGivenDuringThisRun = false;
      this.timeSinceStart = p_23008_;
      Player player = this.getNearestTargetableHero(p_481747_).get();
      p_481747_.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, player);
      BehaviorUtils.lookAtEntity(p_481747_, player);
   }

   protected boolean canStillUse(ServerLevel p_23026_, Villager p_481415_, long p_23028_) {
      return this.isHeroVisible(p_481415_) && !this.giftGivenDuringThisRun;
   }

   protected void tick(ServerLevel p_23036_, Villager p_481399_, long p_23038_) {
      Player player = this.getNearestTargetableHero(p_481399_).get();
      BehaviorUtils.lookAtEntity(p_481399_, player);
      if (this.isWithinThrowingDistance(p_481399_, player)) {
         if (p_23038_ - this.timeSinceStart > 20L) {
            this.throwGift(p_23036_, p_481399_, player);
            this.giftGivenDuringThisRun = true;
         }
      } else {
         BehaviorUtils.setWalkAndLookTargetMemories(p_481399_, player, 0.5F, 5);
      }
   }

   protected void stop(ServerLevel p_23046_, Villager p_481001_, long p_23048_) {
      this.timeUntilNextGift = calculateTimeUntilNextGift(p_23046_);
      p_481001_.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
      p_481001_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      p_481001_.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   private void throwGift(ServerLevel p_376445_, Villager p_478963_, LivingEntity p_23013_) {
      p_478963_.dropFromGiftLootTable(
         p_376445_, getLootTableToThrow(p_478963_), (p_426988_, p_426989_) -> BehaviorUtils.throwItem(p_478963_, p_426989_, p_23013_.position())
      );
   }

   private static ResourceKey<LootTable> getLootTableToThrow(Villager p_477939_) {
      if (p_477939_.isBaby()) {
         return BuiltInLootTables.BABY_VILLAGER_GIFT;
      } else {
         RaidHeroGift gift = p_477939_.getVillagerData().profession().getData(NeoForgeDataMaps.RAID_HERO_GIFTS);
         if (gift != null) {
            return gift.lootTable();
         } else {
            Optional<ResourceKey<VillagerProfession>> optional = p_477939_.getVillagerData().profession().unwrapKey();
            return optional.isEmpty() ? BuiltInLootTables.UNEMPLOYED_GIFT : GIFTS.getOrDefault(optional.get(), BuiltInLootTables.UNEMPLOYED_GIFT);
         }
      }
   }

   private boolean isHeroVisible(Villager p_480484_) {
      return this.getNearestTargetableHero(p_480484_).isPresent();
   }

   private Optional<Player> getNearestTargetableHero(Villager p_479280_) {
      return p_479280_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).filter(this::isHero);
   }

   private boolean isHero(Player p_23018_) {
      return p_23018_.hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
   }

   private boolean isWithinThrowingDistance(Villager p_479726_, Player p_23016_) {
      BlockPos blockpos = p_23016_.blockPosition();
      BlockPos blockpos1 = p_479726_.blockPosition();
      return blockpos1.closerThan(blockpos, 5.0);
   }

   private static int calculateTimeUntilNextGift(ServerLevel p_22994_) {
      return 600 + p_22994_.random.nextInt(6001);
   }
}
