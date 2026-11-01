package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class GoToTargetLocation {
   private static BlockPos getNearbyPos(Mob p_217251_, BlockPos p_217252_) {
      RandomSource $$2 = p_217251_.level().random;
      return p_217252_.offset(getRandomOffset($$2), 0, getRandomOffset($$2));
   }

   private static int getRandomOffset(RandomSource p_217247_) {
      return p_217247_.nextInt(3) - 1;
   }

   public static <E extends Mob> OneShot<E> create(MemoryModuleType<BlockPos> p_259938_, int p_259740_, float p_259957_) {
      return BehaviorBuilder.create(
         p_259997_ -> p_259997_.group(
               p_259997_.present(p_259938_),
               p_259997_.absent(MemoryModuleType.ATTACK_TARGET),
               p_259997_.absent(MemoryModuleType.WALK_TARGET),
               p_259997_.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(p_259997_, (p_259831_, p_259115_, p_259521_, p_259223_) -> (p_352718_, p_352719_, p_352720_) -> {
               BlockPos $$7 = p_259997_.get(p_259831_);
               boolean $$8 = $$7.closerThan(p_352719_.blockPosition(), p_259740_);
               if (!$$8) {
                  BehaviorUtils.setWalkAndLookTargetMemories(p_352719_, getNearbyPos(p_352719_, $$7), p_259957_, p_259740_);
               }

               return true;
            })
      );
   }
}
