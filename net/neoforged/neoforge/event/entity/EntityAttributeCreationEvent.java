package net.neoforged.neoforge.event.entity;

import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class EntityAttributeCreationEvent extends Event implements IModBusEvent {
   private final Map<EntityType<? extends LivingEntity>, AttributeSupplier> map;

   public EntityAttributeCreationEvent(Map<EntityType<? extends LivingEntity>, AttributeSupplier> map) {
      this.map = map;
   }

   public void put(EntityType<? extends LivingEntity> entity, AttributeSupplier map) {
      if (DefaultAttributes.hasSupplier(entity)) {
         throw new IllegalStateException("Duplicate DefaultAttributes entry: " + entity);
      } else {
         this.map.put(entity, map);
      }
   }
}
