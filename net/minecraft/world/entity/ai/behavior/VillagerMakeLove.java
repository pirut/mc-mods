package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;

public class VillagerMakeLove extends Behavior<Villager> {
   private long birthTimestamp;

   public VillagerMakeLove() {
      super(
         ImmutableMap.of(
            MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
         ),
         350,
         350
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel serverLevel, Villager villager) {
      return this.isBreedingPossible(villager);
   }

   protected boolean canStillUse(ServerLevel serverLevel, Villager villager, long l) {
      return l <= this.birthTimestamp && this.isBreedingPossible(villager);
   }

   protected void start(ServerLevel serverLevel, Villager villager, long l) {
      AgeableMob ageableMob = villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(villager, ageableMob, 0.5F, 2);
      serverLevel.broadcastEntityEvent(ageableMob, (byte)18);
      serverLevel.broadcastEntityEvent(villager, (byte)18);
      int i = 275 + villager.getRandom().nextInt(50);
      this.birthTimestamp = l + i;
   }

   protected void tick(ServerLevel serverLevel, Villager villager, long l) {
      Villager villager2 = (Villager)villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      if (!(villager.distanceToSqr(villager2) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(villager, villager2, 0.5F, 2);
         if (l >= this.birthTimestamp) {
            villager.eatAndDigestFood();
            villager2.eatAndDigestFood();
            this.tryToGiveBirth(serverLevel, villager, villager2);
         } else if (villager.getRandom().nextInt(35) == 0) {
            serverLevel.broadcastEntityEvent(villager2, (byte)12);
            serverLevel.broadcastEntityEvent(villager, (byte)12);
         }
      }
   }

   private void tryToGiveBirth(ServerLevel level, Villager villager, Villager partner) {
      Optional<BlockPos> optional = this.takeVacantBed(level, villager);
      if (optional.isEmpty()) {
         level.broadcastEntityEvent(partner, (byte)13);
         level.broadcastEntityEvent(villager, (byte)13);
      } else {
         Optional<Villager> optional2 = this.breed(level, villager, partner);
         if (optional2.isPresent()) {
            this.giveBedToChild(level, optional2.get(), optional.get());
         } else {
            level.getPoiManager().release(optional.get());
            level.debugSynchronizers().updatePoi(optional.get());
         }
      }
   }

   protected void stop(ServerLevel serverLevel, Villager villager, long l) {
      villager.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
   }

   private boolean isBreedingPossible(Villager villager) {
      Brain<Villager> brain = villager.getBrain();
      Optional<AgeableMob> optional = brain.getMemory(MemoryModuleType.BREED_TARGET).filter(ageableMob -> ageableMob.getType() == EntityType.VILLAGER);
      return optional.isEmpty()
         ? false
         : BehaviorUtils.targetIsValid(brain, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && villager.canBreed() && optional.get().canBreed();
   }

   private Optional<BlockPos> takeVacantBed(ServerLevel level, Villager villager) {
      return level.getPoiManager()
         .take(holder -> holder.is(PoiTypes.HOME), (holder, blockPos) -> this.canReach(villager, blockPos, holder), villager.blockPosition(), 48);
   }

   private boolean canReach(Villager villager, BlockPos pos, Holder<PoiType> poiType) {
      Path path = villager.getNavigation().createPath(pos, poiType.value().validRange());
      return path != null && path.canReach();
   }

   private Optional<Villager> breed(ServerLevel level, Villager villager, Villager partner) {
      Villager villager2 = villager.getBreedOffspring(level, partner);
      if (villager2 == null) {
         return Optional.empty();
      } else {
         villager.setAge(6000);
         partner.setAge(6000);
         villager2.setAge(-24000);
         villager2.snapTo(villager.getX(), villager.getY(), villager.getZ(), 0.0F, 0.0F);
         level.addFreshEntityWithPassengers(villager2);
         level.broadcastEntityEvent(villager2, (byte)12);
         return Optional.of(villager2);
      }
   }

   private void giveBedToChild(ServerLevel level, Villager child, BlockPos pos) {
      GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
      child.getBrain().setMemory(MemoryModuleType.HOME, globalPos);
   }
}
