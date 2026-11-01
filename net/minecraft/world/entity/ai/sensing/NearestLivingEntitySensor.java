package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.AABB;

public class NearestLivingEntitySensor<T extends LivingEntity> extends Sensor<T> {
   @Override
   protected void doTick(ServerLevel p_26710_, T p_26711_) {
      AABB $$2 = p_26711_.getBoundingBox().inflate(this.radiusXZ(), this.radiusY(), this.radiusXZ());
      List<LivingEntity> $$3 = p_26710_.getEntitiesOfClass(LivingEntity.class, $$2, p_26717_ -> p_26717_ != p_26711_ && p_26717_.isAlive());
      $$3.sort(Comparator.comparingDouble(p_26711_::distanceToSqr));
      Brain<?> $$4 = p_26711_.getBrain();
      $$4.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, $$3);
      $$4.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, new NearestVisibleLivingEntities(p_26711_, $$3));
   }

   protected int radiusXZ() {
      return 16;
   }

   protected int radiusY() {
      return 16;
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }
}
