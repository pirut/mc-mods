package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class CopyMemoryWithExpiry {
   public static <E extends LivingEntity, T> BehaviorControl<E> create(
      Predicate<E> canCopyMemory, MemoryModuleType<? extends T> sourceMemory, MemoryModuleType<T> targetMemory, UniformInt durationOfCopy
   ) {
      return BehaviorBuilder.create(
         instance -> instance.group(instance.present(sourceMemory), instance.absent(targetMemory))
            .apply(instance, (memoryAccessor, memoryAccessor2) -> (serverLevel, livingEntity, l) -> {
               if (!canCopyMemory.test((E)livingEntity)) {
                  return false;
               } else {
                  memoryAccessor2.setWithExpiry(instance.get(memoryAccessor), durationOfCopy.sample(serverLevel.random));
                  return true;
               }
            })
      );
   }
}
