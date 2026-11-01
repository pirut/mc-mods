package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class MoveToSkySeeingSpot {
   public static OneShot<LivingEntity> create(float p_259860_) {
      return BehaviorBuilder.create(
         p_258543_ -> p_258543_.group(p_258543_.absent(MemoryModuleType.WALK_TARGET)).apply(p_258543_, p_258545_ -> (p_352756_, p_352757_, p_352758_) -> {
            if (p_352756_.canSeeSky(p_352757_.blockPosition())) {
               return false;
            } else {
               Optional<Vec3> $$5 = Optional.ofNullable(getOutdoorPosition(p_352756_, p_352757_));
               $$5.ifPresent(p_258548_ -> p_258545_.set(new WalkTarget(p_258548_, p_259860_, 0)));
               return true;
            }
         })
      );
   }

   @Nullable
   private static Vec3 getOutdoorPosition(ServerLevel p_23565_, LivingEntity p_23566_) {
      RandomSource $$2 = p_23566_.getRandom();
      BlockPos $$3 = p_23566_.blockPosition();

      for (int $$4 = 0; $$4 < 10; $$4++) {
         BlockPos $$5 = $$3.offset($$2.nextInt(20) - 10, $$2.nextInt(6) - 3, $$2.nextInt(20) - 10);
         if (hasNoBlocksAbove(p_23565_, p_23566_, $$5)) {
            return Vec3.atBottomCenterOf($$5);
         }
      }

      return null;
   }

   public static boolean hasNoBlocksAbove(ServerLevel p_23559_, LivingEntity p_23560_, BlockPos p_23561_) {
      return p_23559_.canSeeSky(p_23561_) && p_23559_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p_23561_).getY() <= p_23560_.getY();
   }
}
