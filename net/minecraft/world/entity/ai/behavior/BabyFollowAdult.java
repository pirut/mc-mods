package net.minecraft.world.entity.ai.behavior;

import java.util.function.Function;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class BabyFollowAdult {
   public static OneShot<LivingEntity> create(UniformInt p_260109_, float p_259621_) {
      return create(p_260109_, p_147421_ -> p_259621_, MemoryModuleType.NEAREST_VISIBLE_ADULT, false);
   }

   public static OneShot<LivingEntity> create(
      UniformInt p_259321_, Function<LivingEntity, Float> p_259190_, MemoryModuleType<? extends LivingEntity> p_416063_, boolean p_420043_
   ) {
      return BehaviorBuilder.create(
         p_419440_ -> p_419440_.group(
               p_419440_.present(p_416063_), p_419440_.registered(MemoryModuleType.LOOK_TARGET), p_419440_.absent(MemoryModuleType.WALK_TARGET)
            )
            .apply(
               p_419440_,
               (p_419445_, p_419446_, p_419447_) -> (p_258326_, p_416204_, p_258328_) -> {
                  if (!p_416204_.isBaby()) {
                     return false;
                  } else {
                     LivingEntity livingentity = p_419440_.get(p_419445_);
                     if (p_416204_.closerThan(livingentity, p_259321_.getMaxValue() + 1) && !p_416204_.closerThan(livingentity, p_259321_.getMinValue())) {
                        WalkTarget walktarget = new WalkTarget(
                           new EntityTracker(livingentity, p_420043_, p_420043_), p_259190_.apply(p_416204_), p_259321_.getMinValue() - 1
                        );
                        p_419446_.set(new EntityTracker(livingentity, true, p_420043_));
                        p_419447_.set(walktarget);
                        return true;
                     } else {
                        return false;
                     }
                  }
               }
            )
      );
   }
}
