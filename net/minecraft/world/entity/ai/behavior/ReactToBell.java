package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ReactToBell {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         p_259349_ -> p_259349_.group(p_259349_.present(MemoryModuleType.HEARD_BELL_TIME)).apply(p_259349_, p_259472_ -> (p_352761_, p_352762_, p_352763_) -> {
            Raid $$3 = p_352761_.getRaidAt(p_352762_.blockPosition());
            if ($$3 == null) {
               p_352762_.getBrain().setActiveActivityIfPossible(Activity.HIDE);
            }

            return true;
         })
      );
   }
}
