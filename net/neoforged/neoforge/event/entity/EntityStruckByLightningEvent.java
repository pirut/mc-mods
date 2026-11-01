package net.neoforged.neoforge.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.bus.api.ICancellableEvent;

public class EntityStruckByLightningEvent extends EntityEvent implements ICancellableEvent {
   private final LightningBolt lightning;

   public EntityStruckByLightningEvent(Entity entity, LightningBolt lightning) {
      super(entity);
      this.lightning = lightning;
   }

   public LightningBolt getLightning() {
      return this.lightning;
   }
}
