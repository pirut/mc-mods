package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.K1;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;

public class GoToWantedItem {
   public static BehaviorControl<LivingEntity> create(float speedModifier, boolean hasTarget, int maxDistToWalk) {
      return create(livingEntity -> true, speedModifier, hasTarget, maxDistToWalk);
   }

   public static <E extends LivingEntity> BehaviorControl<E> create(Predicate<E> canWalkToItem, float speedModifier, boolean hasTarget, int maxDistToWalk) {
      return BehaviorBuilder.create(
         instance -> {
            BehaviorBuilder<E, ? extends MemoryAccessor<? extends K1, WalkTarget>> behaviorBuilder = hasTarget
               ? instance.registered(MemoryModuleType.WALK_TARGET)
               : instance.absent(MemoryModuleType.WALK_TARGET);
            return instance.group(
                  instance.registered(MemoryModuleType.LOOK_TARGET),
                  behaviorBuilder,
                  instance.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
                  instance.registered(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)
               )
               .apply(
                  instance,
                  (memoryAccessor, memoryAccessor2, memoryAccessor3, memoryAccessor4) -> (serverLevel, livingEntity, l) -> {
                     ItemEntity itemEntity = instance.get(memoryAccessor3);
                     if (instance.tryGet(memoryAccessor4).isEmpty()
                        && canWalkToItem.test((E)livingEntity)
                        && itemEntity.closerThan(livingEntity, maxDistToWalk)
                        && livingEntity.level().getWorldBorder().isWithinBounds(itemEntity.blockPosition())
                        && livingEntity.canPickUpLoot()) {
                        WalkTarget walkTarget = new WalkTarget(new EntityTracker(itemEntity, false), speedModifier, 0);
                        memoryAccessor.set(new EntityTracker(itemEntity, true));
                        memoryAccessor2.set(walkTarget);
                        return true;
                     } else {
                        return false;
                     }
                  }
               );
         }
      );
   }
}
