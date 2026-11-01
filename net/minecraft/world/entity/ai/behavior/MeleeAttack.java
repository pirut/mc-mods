package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class MeleeAttack {
   public static <T extends Mob> OneShot<T> create(int p_259758_) {
      return create(p_379104_ -> true, p_259758_);
   }

   public static <T extends Mob> OneShot<T> create(Predicate<T> p_379852_, int p_380056_) {
      return BehaviorBuilder.create(
         p_379103_ -> p_379103_.group(
               p_379103_.registered(MemoryModuleType.LOOK_TARGET),
               p_379103_.present(MemoryModuleType.ATTACK_TARGET),
               p_379103_.absent(MemoryModuleType.ATTACK_COOLING_DOWN),
               p_379103_.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(
               p_379103_,
               (p_379118_, p_379119_, p_379120_, p_379121_) -> (p_477814_, p_477815_, p_477816_) -> {
                  LivingEntity livingentity = p_379103_.get(p_379119_);
                  if (p_379852_.test((T)p_477815_)
                     && !isHoldingUsableNonMeleeWeapon(p_477815_)
                     && p_477815_.isWithinMeleeAttackRange(livingentity)
                     && p_379103_.<NearestVisibleLivingEntities>get(p_379121_).contains(livingentity)) {
                     p_379118_.set(new EntityTracker(livingentity, true));
                     p_477815_.swing(InteractionHand.MAIN_HAND);
                     p_477815_.doHurtTarget(p_477814_, livingentity);
                     p_379120_.setWithExpiry(true, p_380056_);
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }

   private static boolean isHoldingUsableNonMeleeWeapon(Mob p_480045_) {
      return p_480045_.isHolding(p_480045_::canUseNonMeleeWeapon);
   }
}
