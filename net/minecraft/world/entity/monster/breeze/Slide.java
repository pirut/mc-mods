package net.minecraft.world.entity.monster.breeze;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class Slide extends Behavior<Breeze> {
   public Slide() {
      super(
         Map.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_JUMP_COOLDOWN,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT,
            MemoryStatus.VALUE_ABSENT
         )
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_311853_, Breeze p_311894_) {
      return p_311894_.onGround() && !p_311894_.isInWater() && p_311894_.getPose() == Pose.STANDING;
   }

   protected void start(ServerLevel p_312325_, Breeze p_312534_, long p_311789_) {
      LivingEntity $$3 = p_312534_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      if ($$3 != null) {
         boolean $$4 = p_312534_.withinInnerCircleRange($$3.position());
         Vec3 $$5 = null;
         if ($$4) {
            Vec3 $$6 = DefaultRandomPos.getPosAway(p_312534_, 5, 5, $$3.position());
            if ($$6 != null && BreezeUtil.hasLineOfSight(p_312534_, $$6) && $$3.distanceToSqr($$6.x, $$6.y, $$6.z) > $$3.distanceToSqr(p_312534_)) {
               $$5 = $$6;
            }
         }

         if ($$5 == null) {
            $$5 = p_312534_.getRandom().nextBoolean()
               ? BreezeUtil.randomPointBehindTarget($$3, p_312534_.getRandom())
               : randomPointInMiddleCircle(p_312534_, $$3);
         }

         p_312534_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(BlockPos.containing($$5), 0.6F, 1));
      }
   }

   private static Vec3 randomPointInMiddleCircle(Breeze p_311931_, LivingEntity p_312413_) {
      Vec3 $$2 = p_312413_.position().subtract(p_311931_.position());
      double $$3 = $$2.length() - Mth.lerp(p_311931_.getRandom().nextDouble(), 8.0, 4.0);
      Vec3 $$4 = $$2.normalize().multiply($$3, $$3, $$3);
      return p_311931_.position().add($$4);
   }
}
