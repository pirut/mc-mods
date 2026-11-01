package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class EraseMemoryIf {
   public static <E extends LivingEntity> BehaviorControl<E> create(Predicate<E> shouldEraseMemory, MemoryModuleType<?> erasingMemory) {
      return BehaviorBuilder.create(
         instance -> instance.group(instance.present(erasingMemory)).apply(instance, memoryAccessor -> (serverLevel, livingEntity, l) -> {
            if (shouldEraseMemory.test((E)livingEntity)) {
               memoryAccessor.erase();
               return true;
            } else {
               return false;
            }
         })
      );
   }
}
