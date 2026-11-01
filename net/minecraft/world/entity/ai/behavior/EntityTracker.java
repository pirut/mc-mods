package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.Vec3;

public class EntityTracker implements PositionTracker {
   private final Entity entity;
   private final boolean trackEyeHeight;

   public EntityTracker(Entity p_22849_, boolean p_22850_) {
      this.entity = p_22849_;
      this.trackEyeHeight = p_22850_;
   }

   @Override
   public Vec3 currentPosition() {
      return this.trackEyeHeight ? this.entity.position().add(0.0, this.entity.getEyeHeight(), 0.0) : this.entity.position();
   }

   @Override
   public BlockPos currentBlockPosition() {
      return this.entity.blockPosition();
   }

   @Override
   public boolean isVisibleBy(LivingEntity p_22853_) {
      if (this.entity instanceof LivingEntity $$1) {
         if (!$$1.isAlive()) {
            return false;
         } else {
            Optional<NearestVisibleLivingEntities> $$3 = p_22853_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
            return $$3.isPresent() && $$3.get().contains($$1);
         }
      } else {
         return true;
      }
   }

   public Entity getEntity() {
      return this.entity;
   }

   @Override
   public String toString() {
      return "EntityTracker for " + this.entity;
   }
}
