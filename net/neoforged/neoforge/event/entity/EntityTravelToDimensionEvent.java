package net.neoforged.neoforge.event.entity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;

public class EntityTravelToDimensionEvent extends EntityEvent implements ICancellableEvent {
   private final ResourceKey<Level> dimension;

   public EntityTravelToDimensionEvent(Entity entity, ResourceKey<Level> dimension) {
      super(entity);
      this.dimension = dimension;
   }

   public ResourceKey<Level> getDimension() {
      return this.dimension;
   }
}
