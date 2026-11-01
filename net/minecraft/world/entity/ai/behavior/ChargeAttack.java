package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

public class ChargeAttack extends Behavior<Animal> {
   private final int timeBetweenAttacks;
   private final TargetingConditions chargeTargeting;
   private final float speed;
   private final float knockbackForce;
   private final double maxTargetDetectionDistance;
   private final double maxChargeDistance;
   private final SoundEvent chargeSound;
   private Vec3 chargeVelocityVector;
   private Vec3 startPosition;

   public ChargeAttack(
      int timeBetweenAttacks,
      TargetingConditions chargeTargeting,
      float spped,
      float knockbackForce,
      double maxChargeDistance,
      double maxTargetDetectionDistance,
      SoundEvent chargeSound
   ) {
      super(ImmutableMap.of(MemoryModuleType.CHARGE_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
      this.timeBetweenAttacks = timeBetweenAttacks;
      this.chargeTargeting = chargeTargeting;
      this.speed = spped;
      this.knockbackForce = knockbackForce;
      this.maxChargeDistance = maxChargeDistance;
      this.maxTargetDetectionDistance = maxTargetDetectionDistance;
      this.chargeSound = chargeSound;
      this.chargeVelocityVector = Vec3.ZERO;
      this.startPosition = Vec3.ZERO;
   }

   protected boolean checkExtraStartConditions(ServerLevel serverLevel, Animal animal) {
      return animal.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
   }

   protected boolean canStillUse(ServerLevel serverLevel, Animal animal, long l) {
      Brain<?> brain = animal.getBrain();
      Optional<LivingEntity> optional = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
      if (optional.isEmpty()) {
         return false;
      } else {
         LivingEntity livingEntity = optional.get();
         if (animal instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            return false;
         } else if (animal.position().subtract(this.startPosition).lengthSqr() >= this.maxChargeDistance * this.maxChargeDistance) {
            return false;
         } else if (livingEntity.position().subtract(animal.position()).lengthSqr() >= this.maxTargetDetectionDistance * this.maxTargetDetectionDistance) {
            return false;
         } else {
            return !animal.hasLineOfSight(livingEntity) ? false : !brain.hasMemoryValue(MemoryModuleType.CHARGE_COOLDOWN_TICKS);
         }
      }
   }

   protected void start(ServerLevel serverLevel, Animal animal, long l) {
      Brain<?> brain = animal.getBrain();
      this.startPosition = animal.position();
      LivingEntity livingEntity = brain.getMemory(MemoryModuleType.ATTACK_TARGET).get();
      Vec3 vec3 = livingEntity.position().subtract(animal.position()).normalize();
      this.chargeVelocityVector = vec3.scale(this.speed);
      if (this.canStillUse(serverLevel, animal, l)) {
         animal.playSound(this.chargeSound);
      }
   }

   protected void tick(ServerLevel serverLevel, Animal animal, long l) {
      Brain<?> brain = animal.getBrain();
      LivingEntity livingEntity = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow();
      animal.lookAt(livingEntity, 360.0F, 360.0F);
      animal.setDeltaMovement(this.chargeVelocityVector);
      List<LivingEntity> list = new ArrayList<>(1);
      serverLevel.getEntities(
         EntityTypeTest.forClass(LivingEntity.class),
         animal.getBoundingBox(),
         livingEntityx -> this.chargeTargeting.test(serverLevel, animal, livingEntityx),
         list,
         1
      );
      if (!list.isEmpty()) {
         LivingEntity livingEntity2 = list.get(0);
         if (animal.hasPassenger(livingEntity2)) {
            return;
         }

         this.dealDamageToTarget(serverLevel, animal, livingEntity2);
         this.dealKnockBack(animal, livingEntity2);
         this.stop(serverLevel, animal, l);
      }
   }

   private void dealDamageToTarget(ServerLevel level, Animal owner, LivingEntity target) {
      DamageSource damageSource = level.damageSources().mobAttack(owner);
      float f = (float)owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
      if (target.hurtServer(level, damageSource, f)) {
         EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
      }
   }

   private void dealKnockBack(Animal owner, LivingEntity target) {
      int i = owner.hasEffect(MobEffects.SPEED) ? owner.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
      int j = owner.hasEffect(MobEffects.SLOWNESS) ? owner.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
      float f = 0.25F * (i - j);
      float g = Mth.clamp(this.speed * (float)owner.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.2F, 2.0F) + f;
      owner.causeExtraKnockback(target, g * this.knockbackForce, owner.getDeltaMovement());
   }

   protected void stop(ServerLevel serverLevel, Animal animal, long l) {
      animal.getBrain().setMemory(MemoryModuleType.CHARGE_COOLDOWN_TICKS, this.timeBetweenAttacks);
      animal.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
   }
}
