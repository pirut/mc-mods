package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jspecify.annotations.Nullable;

public class SpearApproach extends Behavior<PathfinderMob> {
   double speedModifierWhenRepositioning;
   float approachDistanceSq;

   public SpearApproach(double p_479201_, float p_481586_) {
      super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_ABSENT));
      this.speedModifierWhenRepositioning = p_479201_;
      this.approachDistanceSq = p_481586_ * p_481586_;
   }

   private boolean ableToAttack(PathfinderMob p_481485_) {
      return this.getTarget(p_481485_) != null && p_481485_.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
   }

   protected boolean checkExtraStartConditions(ServerLevel p_479816_, PathfinderMob p_480371_) {
      return this.ableToAttack(p_480371_) && !p_480371_.isUsingItem();
   }

   protected void start(ServerLevel p_480370_, PathfinderMob p_481666_, long p_478777_) {
      p_481666_.setAggressive(true);
      p_481666_.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.APPROACH);
      super.start(p_480370_, p_481666_, p_478777_);
   }

   @Nullable
   private LivingEntity getTarget(PathfinderMob p_480633_) {
      return p_480633_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   protected boolean canStillUse(ServerLevel p_479731_, PathfinderMob p_477963_, long p_480663_) {
      return this.ableToAttack(p_477963_) && this.farEnough(p_477963_);
   }

   private boolean farEnough(PathfinderMob p_481168_) {
      LivingEntity livingentity = this.getTarget(p_481168_);
      double d0 = p_481168_.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
      return d0 > this.approachDistanceSq;
   }

   protected void tick(ServerLevel p_481133_, PathfinderMob p_478677_, long p_478612_) {
      LivingEntity livingentity = this.getTarget(p_478677_);
      Entity entity = p_478677_.getRootVehicle();
      float f = 1.0F;
      if (entity instanceof Mob mob) {
         f = mob.chargeSpeedModifier();
      }

      p_478677_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
      p_478677_.getNavigation().moveTo(livingentity, f * this.speedModifierWhenRepositioning);
   }

   protected void stop(ServerLevel p_480695_, PathfinderMob p_480316_, long p_480772_) {
      p_480316_.getNavigation().stop();
      p_480316_.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.CHARGING);
   }

   @Override
   protected boolean timedOut(long p_479534_) {
      return false;
   }
}
