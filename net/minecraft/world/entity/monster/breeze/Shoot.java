package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.windcharge.BreezeWindCharge;
import net.minecraft.world.phys.Vec3;

public class Shoot extends Behavior<Breeze> {
   private static final int ATTACK_RANGE_MIN_SQRT = 4;
   private static final int ATTACK_RANGE_MAX_SQRT = 256;
   private static final int UNCERTAINTY_BASE = 5;
   private static final int UNCERTAINTY_MULTIPLIER = 4;
   private static final float PROJECTILE_MOVEMENT_SCALE = 0.7F;
   private static final int SHOOT_INITIAL_DELAY_TICKS = Math.round(15.0F);
   private static final int SHOOT_RECOVER_DELAY_TICKS = Math.round(4.0F);
   private static final int SHOOT_COOLDOWN_TICKS = Math.round(10.0F);

   @VisibleForTesting
   public Shoot() {
      super(
         ImmutableMap.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.BREEZE_SHOOT_COOLDOWN,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT_CHARGING,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT_RECOVERING,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_JUMP_TARGET,
            MemoryStatus.VALUE_ABSENT
         ),
         SHOOT_INITIAL_DELAY_TICKS + 1 + SHOOT_RECOVER_DELAY_TICKS
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_312041_, Breeze p_312169_) {
      return p_312169_.getPose() != Pose.STANDING
         ? false
         : p_312169_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(p_312632_ -> isTargetWithinRange(p_312169_, p_312632_)).map(p_312737_ -> {
            if (!p_312737_) {
               p_312169_.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
            }

            return (Boolean)p_312737_;
         }).orElse(false);
   }

   protected boolean canStillUse(ServerLevel p_312535_, Breeze p_312174_, long p_311812_) {
      return p_312174_.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && p_312174_.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_SHOOT);
   }

   protected void start(ServerLevel p_311932_, Breeze p_312618_, long p_311781_) {
      p_312618_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(p_312833_ -> p_312618_.setPose(Pose.SHOOTING));
      p_312618_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_CHARGING, Unit.INSTANCE, SHOOT_INITIAL_DELAY_TICKS);
      p_312618_.playSound(SoundEvents.BREEZE_INHALE, 1.0F, 1.0F);
   }

   protected void stop(ServerLevel p_312137_, Breeze p_311803_, long p_312309_) {
      if (p_311803_.getPose() == Pose.SHOOTING) {
         p_311803_.setPose(Pose.STANDING);
      }

      p_311803_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_COOLDOWN, Unit.INSTANCE, SHOOT_COOLDOWN_TICKS);
      p_311803_.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
   }

   protected void tick(ServerLevel p_312907_, Breeze p_312605_, long p_312804_) {
      Brain<Breeze> $$3 = p_312605_.getBrain();
      LivingEntity $$4 = $$3.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      if ($$4 != null) {
         p_312605_.lookAt(EntityAnchorArgument.Anchor.EYES, $$4.position());
         if (!$$3.getMemory(MemoryModuleType.BREEZE_SHOOT_CHARGING).isPresent() && !$$3.getMemory(MemoryModuleType.BREEZE_SHOOT_RECOVERING).isPresent()) {
            $$3.setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_RECOVERING, Unit.INSTANCE, SHOOT_RECOVER_DELAY_TICKS);
            if (isFacingTarget(p_312605_, $$4)) {
               double $$5 = $$4.getX() - p_312605_.getX();
               double $$6 = $$4.getY($$4.isPassenger() ? 0.8 : 0.3) - p_312605_.getY(0.5);
               double $$7 = $$4.getZ() - p_312605_.getZ();
               BreezeWindCharge $$8 = new BreezeWindCharge(p_312605_, p_312907_);
               p_312605_.playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 1.0F);
               $$8.shoot($$5, $$6, $$7, 0.7F, 5 - p_312907_.getDifficulty().getId() * 4);
               p_312907_.addFreshEntity($$8);
            }
         }
      }
   }

   @VisibleForTesting
   public static boolean isFacingTarget(Breeze p_311845_, LivingEntity p_312453_) {
      Vec3 $$2 = p_311845_.getViewVector(1.0F);
      Vec3 $$3 = p_312453_.position().subtract(p_311845_.position()).normalize();
      return $$2.dot($$3) > 0.5;
   }

   private static boolean isTargetWithinRange(Breeze p_312114_, LivingEntity p_312647_) {
      double $$2 = p_312114_.position().distanceToSqr(p_312647_.position());
      return $$2 > 4.0 && $$2 < 256.0;
   }
}
