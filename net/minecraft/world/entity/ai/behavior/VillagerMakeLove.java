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

   protected boolean checkExtraStartConditions(ServerLevel p_24623_, Villager p_482178_) {
      return this.isBreedingPossible(p_482178_);
   }

   protected boolean canStillUse(ServerLevel p_24626_, Villager p_481028_, long p_24628_) {
      return p_24628_ <= this.birthTimestamp && this.isBreedingPossible(p_481028_);
   }

   protected void start(ServerLevel p_24652_, Villager p_481220_, long p_24654_) {
      AgeableMob ageablemob = p_481220_.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(p_481220_, ageablemob, 0.5F, 2);
      p_24652_.broadcastEntityEvent(ageablemob, (byte)18);
      p_24652_.broadcastEntityEvent(p_481220_, (byte)18);
      int i = 275 + p_481220_.getRandom().nextInt(50);
      this.birthTimestamp = p_24654_ + i;
   }

   protected void tick(ServerLevel p_24667_, Villager p_481591_, long p_24669_) {
      Villager villager = (Villager)p_481591_.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      if (!(p_481591_.distanceToSqr(villager) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(p_481591_, villager, 0.5F, 2);
         if (p_24669_ >= this.birthTimestamp) {
            p_481591_.eatAndDigestFood();
            villager.eatAndDigestFood();
            this.tryToGiveBirth(p_24667_, p_481591_, villager);
         } else if (p_481591_.getRandom().nextInt(35) == 0) {
            p_24667_.broadcastEntityEvent(villager, (byte)12);
            p_24667_.broadcastEntityEvent(p_481591_, (byte)12);
         }
      }
   }

   private void tryToGiveBirth(ServerLevel p_24630_, Villager p_481693_, Villager p_480869_) {
      Optional<BlockPos> optional = this.takeVacantBed(p_24630_, p_481693_);
      if (optional.isEmpty()) {
         p_24630_.broadcastEntityEvent(p_480869_, (byte)13);
         p_24630_.broadcastEntityEvent(p_481693_, (byte)13);
      } else {
         Optional<Villager> optional1 = this.breed(p_24630_, p_481693_, p_480869_);
         if (optional1.isPresent()) {
            this.giveBedToChild(p_24630_, optional1.get(), optional.get());
         } else {
            p_24630_.getPoiManager().release(optional.get());
            p_24630_.debugSynchronizers().updatePoi(optional.get());
         }
      }
   }

   protected void stop(ServerLevel p_24675_, Villager p_480694_, long p_24677_) {
      p_480694_.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
   }

   private boolean isBreedingPossible(Villager p_481274_) {
      Brain<Villager> brain = p_481274_.getBrain();
      Optional<AgeableMob> optional = brain.getMemory(MemoryModuleType.BREED_TARGET).filter(p_423262_ -> p_423262_.getType() == EntityType.VILLAGER);
      return optional.isEmpty()
         ? false
         : BehaviorUtils.targetIsValid(brain, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && p_481274_.canBreed() && optional.get().canBreed();
   }

   private Optional<BlockPos> takeVacantBed(ServerLevel p_24649_, Villager p_479480_) {
      return p_24649_.getPoiManager()
         .take(
            p_217509_ -> p_217509_.is(PoiTypes.HOME), (p_477852_, p_477853_) -> this.canReach(p_479480_, p_477853_, p_477852_), p_479480_.blockPosition(), 48
         );
   }

   private boolean canReach(Villager p_478179_, BlockPos p_217502_, Holder<PoiType> p_217503_) {
      Path path = p_478179_.getNavigation().createPath(p_217502_, p_217503_.value().validRange());
      return path != null && path.canReach();
   }

   private Optional<Villager> breed(ServerLevel p_24656_, Villager p_478139_, Villager p_481400_) {
      Villager villager = p_478139_.getBreedOffspring(p_24656_, p_481400_);
      if (villager == null) {
         return Optional.empty();
      } else {
         p_478139_.setAge(6000);
         p_481400_.setAge(6000);
         villager.setAge(-24000);
         villager.snapTo(p_478139_.getX(), p_478139_.getY(), p_478139_.getZ(), 0.0F, 0.0F);
         p_24656_.addFreshEntityWithPassengers(villager);
         if (!villager.isAddedToLevel()) {
            return Optional.empty();
         } else {
            p_24656_.broadcastEntityEvent(villager, (byte)12);
            return Optional.of(villager);
         }
      }
   }

   private void giveBedToChild(ServerLevel p_24634_, Villager p_481981_, BlockPos p_24636_) {
      GlobalPos globalpos = GlobalPos.of(p_24634_.dimension(), p_24636_);
      p_481981_.getBrain().setMemory(MemoryModuleType.HOME, globalpos);
   }
}
