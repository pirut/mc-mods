package net.minecraft.world.entity.ai.behavior;

import java.util.function.BiPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class DismountOrSkipMounting {
   public static <E extends LivingEntity> BehaviorControl<E> create(int maxDistanceFromVehicle, BiPredicate<E, Entity> shouldStopRiding) {
      return BehaviorBuilder.create(
         instance -> instance.group(instance.registered(MemoryModuleType.RIDE_TARGET)).apply(instance, memoryAccessor -> (serverLevel, livingEntity, l) -> {
            Entity entity = livingEntity.getVehicle();
            Entity entity2 = instance.<Entity>tryGet(memoryAccessor).orElse(null);
            if (entity == null && entity2 == null) {
               return false;
            } else {
               Entity entity3 = entity == null ? entity2 : entity;
               if (isVehicleValid(livingEntity, entity3, maxDistanceFromVehicle) && !shouldStopRiding.test((E)livingEntity, entity3)) {
                  return false;
               } else {
                  livingEntity.stopRiding();
                  memoryAccessor.erase();
                  return true;
               }
            }
         })
      );
   }

   private static boolean isVehicleValid(LivingEntity entity, Entity vehicle, int distance) {
      return vehicle.isAlive() && vehicle.closerThan(entity, distance) && vehicle.level() == entity.level();
   }
}
