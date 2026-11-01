package net.minecraft.world.entity.ai.sensing;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public abstract class Sensor<E extends LivingEntity> {
   private static final RandomSource RANDOM = RandomSource.createThreadSafe();
   private static final int DEFAULT_SCAN_RATE = 20;
   private static final int DEFAULT_TARGETING_RANGE = 16;
   private static final TargetingConditions TARGET_CONDITIONS = TargetingConditions.forNonCombat().range(16.0);
   private static final TargetingConditions TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forNonCombat()
      .range(16.0)
      .ignoreInvisibilityTesting();
   private static final TargetingConditions ATTACK_TARGET_CONDITIONS = TargetingConditions.forCombat().range(16.0);
   private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forCombat()
      .range(16.0)
      .ignoreInvisibilityTesting();
   private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT = TargetingConditions.forCombat().range(16.0).ignoreLineOfSight();
   private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT = TargetingConditions.forCombat()
      .range(16.0)
      .ignoreLineOfSight()
      .ignoreInvisibilityTesting();
   private final int scanRate;
   private long timeToTick;

   public Sensor(int p_26800_) {
      this.scanRate = p_26800_;
      this.timeToTick = RANDOM.nextInt(p_26800_);
   }

   public Sensor() {
      this(20);
   }

   public final void tick(ServerLevel p_26807_, E p_26808_) {
      if (--this.timeToTick <= 0L) {
         this.timeToTick = this.scanRate;
         this.updateTargetingConditionRanges(p_26808_);
         this.doTick(p_26807_, p_26808_);
      }
   }

   private void updateTargetingConditionRanges(E p_364441_) {
      double d0 = p_364441_.getAttributeValue(Attributes.FOLLOW_RANGE);
      TARGET_CONDITIONS.range(d0);
      TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.range(d0);
      ATTACK_TARGET_CONDITIONS.range(d0);
      ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.range(d0);
      ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.range(d0);
      ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.range(d0);
   }

   protected abstract void doTick(ServerLevel var1, E var2);

   public abstract Set<MemoryModuleType<?>> requires();

   public static boolean isEntityTargetable(ServerLevel p_376505_, LivingEntity p_26804_, LivingEntity p_26805_) {
      return p_26804_.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, p_26805_)
         ? TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(p_376505_, p_26804_, p_26805_)
         : TARGET_CONDITIONS.test(p_376505_, p_26804_, p_26805_);
   }

   public static boolean isEntityAttackable(ServerLevel p_376646_, LivingEntity p_148313_, LivingEntity p_148314_) {
      return p_148313_.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, p_148314_)
         ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(p_376646_, p_148313_, p_148314_)
         : ATTACK_TARGET_CONDITIONS.test(p_376646_, p_148313_, p_148314_);
   }

   public static BiPredicate<ServerLevel, LivingEntity> wasEntityAttackableLastNTicks(LivingEntity p_362583_, int p_363318_) {
      return rememberPositives(p_363318_, (p_375764_, p_375765_) -> isEntityAttackable(p_375764_, p_362583_, p_375765_));
   }

   public static boolean isEntityAttackableIgnoringLineOfSight(ServerLevel p_376119_, LivingEntity p_182378_, LivingEntity p_182379_) {
      return p_182378_.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, p_182379_)
         ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.test(p_376119_, p_182378_, p_182379_)
         : ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.test(p_376119_, p_182378_, p_182379_);
   }

   static <T, U> BiPredicate<T, U> rememberPositives(int p_363587_, BiPredicate<T, U> p_376629_) {
      AtomicInteger atomicinteger = new AtomicInteger(0);
      return (p_375761_, p_375762_) -> {
         if (p_376629_.test(p_375761_, p_375762_)) {
            atomicinteger.set(p_363587_);
            return true;
         } else {
            return atomicinteger.decrementAndGet() >= 0;
         }
      };
   }
}
