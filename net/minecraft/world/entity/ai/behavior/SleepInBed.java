package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SleepInBed extends Behavior<LivingEntity> {
   public static final int COOLDOWN_AFTER_BEING_WOKEN = 100;
   private long nextOkStartTime;

   public SleepInBed() {
      super(ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LAST_WOKEN, MemoryStatus.REGISTERED));
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel p_24154_, LivingEntity p_24155_) {
      if (p_24155_.isPassenger()) {
         return false;
      } else {
         Brain<?> $$2 = p_24155_.getBrain();
         GlobalPos $$3 = $$2.getMemory(MemoryModuleType.HOME).get();
         if (p_24154_.dimension() != $$3.dimension()) {
            return false;
         } else {
            Optional<Long> $$4 = $$2.getMemory(MemoryModuleType.LAST_WOKEN);
            if ($$4.isPresent()) {
               long $$5 = p_24154_.getGameTime() - $$4.get();
               if ($$5 > 0L && $$5 < 100L) {
                  return false;
               }
            }

            BlockState $$6 = p_24154_.getBlockState($$3.pos());
            return $$3.pos().closerToCenterThan(p_24155_.position(), 2.0) && $$6.is(BlockTags.BEDS) && !$$6.getValue(BedBlock.OCCUPIED);
         }
      }
   }

   @Override
   protected boolean canStillUse(ServerLevel p_24161_, LivingEntity p_24162_, long p_24163_) {
      Optional<GlobalPos> $$3 = p_24162_.getBrain().getMemory(MemoryModuleType.HOME);
      if ($$3.isEmpty()) {
         return false;
      } else {
         BlockPos $$4 = $$3.get().pos();
         return p_24162_.getBrain().isActive(Activity.REST) && p_24162_.getY() > $$4.getY() + 0.4 && $$4.closerToCenterThan(p_24162_.position(), 1.14);
      }
   }

   @Override
   protected void start(ServerLevel p_24157_, LivingEntity p_24158_, long p_24159_) {
      if (p_24159_ > this.nextOkStartTime) {
         Brain<?> $$3 = p_24158_.getBrain();
         if ($$3.hasMemoryValue(MemoryModuleType.DOORS_TO_CLOSE)) {
            Set<GlobalPos> $$4 = $$3.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get();
            Optional<List<LivingEntity>> $$5;
            if ($$3.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES)) {
               $$5 = $$3.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
            } else {
               $$5 = Optional.empty();
            }

            InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough(p_24157_, p_24158_, null, null, $$4, $$5);
         }

         p_24158_.startSleeping(p_24158_.getBrain().getMemory(MemoryModuleType.HOME).get().pos());
      }
   }

   @Override
   protected boolean timedOut(long p_24152_) {
      return false;
   }

   @Override
   protected void stop(ServerLevel p_24165_, LivingEntity p_24166_, long p_24167_) {
      if (p_24166_.isSleeping()) {
         p_24166_.stopSleeping();
         this.nextOkStartTime = p_24167_ + 40L;
      }
   }
}
