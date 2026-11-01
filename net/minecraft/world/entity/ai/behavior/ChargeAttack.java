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

   public ChargeAttack(int p_455115_, TargetingConditions p_456091_, float p_454698_, float p_456218_, double p_455279_, double p_455483_, SoundEvent p_470854_) {
      super(ImmutableMap.of(MemoryModuleType.CHARGE_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
      this.timeBetweenAttacks = p_455115_;
      this.chargeTargeting = p_456091_;
      this.speed = p_454698_;
      this.knockbackForce = p_456218_;
      this.maxChargeDistance = p_455279_;
      this.maxTargetDetectionDistance = p_455483_;
      this.chargeSound = p_470854_;
      this.chargeVelocityVector = Vec3.ZERO;
      this.startPosition = Vec3.ZERO;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_455492_, Animal p_455417_) {
      return p_455417_.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
   }

   protected boolean canStillUse(ServerLevel p_455371_, Animal p_455552_, long p_455029_) {
      Brain<?> brain = p_455552_.getBrain();
      Optional<LivingEntity> optional = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
      if (optional.isEmpty()) {
         return false;
      } else {
         LivingEntity livingentity = optional.get();
         if (p_455552_ instanceof TamableAnimal tamableanimal && tamableanimal.isTame()) {
            return false;
         } else if (p_455552_.position().subtract(this.startPosition).lengthSqr() >= this.maxChargeDistance * this.maxChargeDistance) {
            return false;
         } else if (livingentity.position().subtract(p_455552_.position()).lengthSqr() >= this.maxTargetDetectionDistance * this.maxTargetDetectionDistance) {
            return false;
         } else {
            return !p_455552_.hasLineOfSight(livingentity) ? false : !brain.hasMemoryValue(MemoryModuleType.CHARGE_COOLDOWN_TICKS);
         }
      }
   }

   protected void start(ServerLevel p_455124_, Animal p_454774_, long p_455262_) {
      Brain<?> brain = p_454774_.getBrain();
      this.startPosition = p_454774_.position();
      LivingEntity livingentity = brain.getMemory(MemoryModuleType.ATTACK_TARGET).get();
      Vec3 vec3 = livingentity.position().subtract(p_454774_.position()).normalize();
      this.chargeVelocityVector = vec3.scale(this.speed);
      if (this.canStillUse(p_455124_, p_454774_, p_455262_)) {
         p_454774_.playSound(this.chargeSound);
      }
   }

   protected void tick(ServerLevel p_454912_, Animal p_455618_, long p_455532_) {
      Brain<?> brain = p_455618_.getBrain();
      LivingEntity livingentity = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow();
      p_455618_.lookAt(livingentity, 360.0F, 360.0F);
      p_455618_.setDeltaMovement(this.chargeVelocityVector);
      List<LivingEntity> list = new ArrayList<>(1);
      p_454912_.getEntities(
         EntityTypeTest.forClass(LivingEntity.class),
         p_455618_.getBoundingBox(),
         p_455737_ -> this.chargeTargeting.test(p_454912_, p_455618_, p_455737_),
         list,
         1
      );
      if (!list.isEmpty()) {
         LivingEntity livingentity1 = list.get(0);
         if (p_455618_.hasPassenger(livingentity1)) {
            return;
         }

         this.dealDamageToTarget(p_454912_, p_455618_, livingentity1);
         this.dealKnockBack(p_455618_, livingentity1);
         this.stop(p_454912_, p_455618_, p_455532_);
      }
   }

   private void dealDamageToTarget(ServerLevel p_454666_, Animal p_455639_, LivingEntity p_455936_) {
      DamageSource damagesource = p_454666_.damageSources().mobAttack(p_455639_);
      float f = (float)p_455639_.getAttributeValue(Attributes.ATTACK_DAMAGE);
      if (p_455936_.hurtServer(p_454666_, damagesource, f)) {
         EnchantmentHelper.doPostAttackEffects(p_454666_, p_455936_, damagesource);
      }
   }

   private void dealKnockBack(Animal p_455627_, LivingEntity p_456003_) {
      int i = p_455627_.hasEffect(MobEffects.SPEED) ? p_455627_.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
      int j = p_455627_.hasEffect(MobEffects.SLOWNESS) ? p_455627_.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
      float f = 0.25F * (i - j);
      float f1 = Mth.clamp(this.speed * (float)p_455627_.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.2F, 2.0F) + f;
      p_455627_.causeExtraKnockback(p_456003_, f1 * this.knockbackForce, p_455627_.getDeltaMovement());
   }

   protected void stop(ServerLevel p_455505_, Animal p_455790_, long p_454815_) {
      p_455790_.getBrain().setMemory(MemoryModuleType.CHARGE_COOLDOWN_TICKS, this.timeBetweenAttacks);
      p_455790_.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
   }
}
