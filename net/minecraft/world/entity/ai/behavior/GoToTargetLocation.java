package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class GoToTargetLocation {
   private static BlockPos getNearbyPos(Mob mob, BlockPos pos) {
      RandomSource randomSource = mob.level().random;
      return pos.offset(getRandomOffset(randomSource), 0, getRandomOffset(randomSource));
   }

   private static int getRandomOffset(RandomSource random) {
      return random.nextInt(3) - 1;
   }

   public static <E extends Mob> OneShot<E> create(MemoryModuleType<BlockPos> locationMemory, int closeEnoughDist, float speedModifier) {
      return BehaviorBuilder.create(
         instance -> instance.group(
               instance.present(locationMemory),
               instance.absent(MemoryModuleType.ATTACK_TARGET),
               instance.absent(MemoryModuleType.WALK_TARGET),
               instance.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(instance, (memoryAccessor, memoryAccessor2, memoryAccessor3, memoryAccessor4) -> (serverLevel, mob, l) -> {
               BlockPos blockPos = instance.get(memoryAccessor);
               boolean bl = blockPos.closerThan(mob.blockPosition(), closeEnoughDist);
               if (!bl) {
                  BehaviorUtils.setWalkAndLookTargetMemories(mob, getNearbyPos(mob, blockPos), speedModifier, closeEnoughDist);
               }

               return true;
            })
      );
   }
}
