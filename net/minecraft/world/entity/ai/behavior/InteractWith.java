package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InteractWith {
   public static <T extends LivingEntity> BehaviorControl<LivingEntity> of(
      EntityType<? extends T> type, int interactionRange, MemoryModuleType<T> interactMemory, float speedModifier, int maxDist
   ) {
      return of(type, interactionRange, livingEntity -> true, livingEntity -> true, interactMemory, speedModifier, maxDist);
   }

   public static <E extends LivingEntity, T extends LivingEntity> BehaviorControl<E> of(
      EntityType<? extends T> type,
      int interactionRange,
      Predicate<E> selfFilter,
      Predicate<T> targetFilter,
      MemoryModuleType<T> memory,
      float speedModifier,
      int maxDist
   ) {
      int i = interactionRange * interactionRange;
      Predicate<LivingEntity> predicate = livingEntity -> type.equals(livingEntity.getType()) && targetFilter.test((T)livingEntity);
      return BehaviorBuilder.create(
         instance -> instance.group(
               instance.registered(memory),
               instance.registered(MemoryModuleType.LOOK_TARGET),
               instance.absent(MemoryModuleType.WALK_TARGET),
               instance.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(
               instance,
               (memoryAccessor, memoryAccessor2, memoryAccessor3, memoryAccessor4) -> (serverLevel, livingEntity, l) -> {
                  NearestVisibleLivingEntities nearestVisibleLivingEntities = instance.get(memoryAccessor4);
                  if (selfFilter.test((E)livingEntity) && nearestVisibleLivingEntities.contains(predicate)) {
                     Optional<LivingEntity> optional = nearestVisibleLivingEntities.findClosest(
                        livingEntity2 -> livingEntity2.distanceToSqr(livingEntity) <= i && predicate.test(livingEntity2)
                     );
                     optional.ifPresent(livingEntityx -> {
                        memoryAccessor.set(livingEntityx);
                        memoryAccessor2.set(new EntityTracker(livingEntityx, true));
                        memoryAccessor3.set(new WalkTarget(new EntityTracker(livingEntityx, false), speedModifier, maxDist));
                     });
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }
}
