package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

public class StartAttacking {
   public static <E extends Mob> BehaviorControl<E> create(Function<E, Optional<? extends LivingEntity>> p_259868_) {
      return create(p_24212_ -> true, p_259868_);
   }

   public static <E extends Mob> BehaviorControl<E> create(Predicate<E> p_259618_, Function<E, Optional<? extends LivingEntity>> p_259435_) {
      return BehaviorBuilder.create(
         p_258782_ -> p_258782_.group(p_258782_.absent(MemoryModuleType.ATTACK_TARGET), p_258782_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
            .apply(
               p_258782_,
               (p_258778_, p_258779_) -> (p_258773_, p_258774_, p_258775_) -> {
                  if (!p_259618_.test((E)p_258774_)) {
                     return false;
                  } else {
                     Optional<? extends LivingEntity> optional = p_259435_.apply((E)p_258774_);
                     if (optional.isEmpty()) {
                        return false;
                     } else {
                        LivingEntity livingentity = optional.get();
                        if (!p_258774_.canAttack(livingentity)) {
                           return false;
                        } else {
                           LivingChangeTargetEvent changeTargetEvent = CommonHooks.onLivingChangeTarget(
                              p_258774_, livingentity, LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET
                           );
                           if (!changeTargetEvent.isCanceled() && changeTargetEvent.getNewAboutToBeSetTarget() != null) {
                              p_258778_.set(changeTargetEvent.getNewAboutToBeSetTarget());
                              p_258779_.erase();
                              return true;
                           } else {
                              return false;
                           }
                        }
                     }
                  }
               }
            )
      );
   }
}
