package net.neoforged.neoforge.event.entity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class EntityAttributeModificationEvent extends Event implements IModBusEvent {
   private final Map<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> entityAttributes;
   private final List<EntityType<? extends LivingEntity>> entityTypes;

   public EntityAttributeModificationEvent(Map<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> mapIn) {
      this.entityAttributes = mapIn;
      this.entityTypes = ImmutableList.copyOf(
         BuiltInRegistries.ENTITY_TYPE.stream().filter(DefaultAttributes::hasSupplier).map(entityType -> entityType).collect(Collectors.toList())
      );
   }

   public void add(EntityType<? extends LivingEntity> entityType, Holder<Attribute> attribute, double value) {
      AttributeSupplier.Builder attributes = this.entityAttributes.computeIfAbsent(entityType, type -> new AttributeSupplier.Builder());
      attributes.add(attribute, value);
   }

   public void add(EntityType<? extends LivingEntity> entityType, Holder<Attribute> attribute) {
      this.add(entityType, attribute, attribute.value().getDefaultValue());
   }

   public boolean has(EntityType<? extends LivingEntity> entityType, Holder<Attribute> attribute) {
      AttributeSupplier globalMap = DefaultAttributes.getSupplier(entityType);
      return globalMap.hasAttribute(attribute)
         || this.entityAttributes.get(entityType) != null && this.entityAttributes.get(entityType).hasAttribute(attribute);
   }

   public List<EntityType<? extends LivingEntity>> getTypes() {
      return this.entityTypes;
   }
}
