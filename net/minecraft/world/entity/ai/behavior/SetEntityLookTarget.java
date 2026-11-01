package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetEntityLookTarget {
   public static BehaviorControl<LivingEntity> create(MobCategory category, float makDist) {
      return create(livingEntity -> category.equals(livingEntity.getType().getCategory()), makDist);
   }

   public static OneShot<LivingEntity> create(EntityType<?> entityType, float maxDist) {
      return create(livingEntity -> entityType.equals(livingEntity.getType()), maxDist);
   }

   public static OneShot<LivingEntity> create(float maxDist) {
      return create(livingEntity -> true, maxDist);
   }

   public static OneShot<LivingEntity> create(Predicate<LivingEntity> canLootAtTarget, float maxDist) {
      float f = maxDist * maxDist;
      return BehaviorBuilder.create(
         instance -> instance.group(instance.absent(MemoryModuleType.LOOK_TARGET), instance.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES))
            .apply(
               instance,
               (memoryAccessor, memoryAccessor2) -> (serverLevel, livingEntity, l) -> {
                  Optional<LivingEntity> optional = instance.<NearestVisibleLivingEntities>get(memoryAccessor2)
                     .findClosest(
                        canLootAtTarget.and(livingEntity2 -> livingEntity2.distanceToSqr(livingEntity) <= f && !livingEntity.hasPassenger(livingEntity2))
                     );
                  if (optional.isEmpty()) {
                     return false;
                  } else {
                     memoryAccessor.set(new EntityTracker(optional.get(), true));
                     return true;
                  }
               }
            )
      );
   }
}
