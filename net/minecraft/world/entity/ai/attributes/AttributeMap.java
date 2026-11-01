package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class AttributeMap {
   private final Map<Holder<Attribute>, AttributeInstance> attributes = new Object2ObjectOpenHashMap();
   private final Set<AttributeInstance> attributesToSync = new ObjectOpenHashSet();
   private final Set<AttributeInstance> attributesToUpdate = new ObjectOpenHashSet();
   private final AttributeSupplier supplier;

   public AttributeMap(AttributeSupplier supplier) {
      this.supplier = supplier;
   }

   private void onAttributeModified(AttributeInstance instance) {
      this.attributesToUpdate.add(instance);
      if (instance.getAttribute().value().isClientSyncable()) {
         this.attributesToSync.add(instance);
      }
   }

   public Set<AttributeInstance> getAttributesToSync() {
      return this.attributesToSync;
   }

   public Set<AttributeInstance> getAttributesToUpdate() {
      return this.attributesToUpdate;
   }

   public Collection<AttributeInstance> getSyncableAttributes() {
      return this.attributes
         .values()
         .stream()
         .filter(attributeInstance -> attributeInstance.getAttribute().value().isClientSyncable())
         .collect(Collectors.toList());
   }

   @Nullable
   public AttributeInstance getInstance(Holder<Attribute> attribute) {
      return this.attributes.computeIfAbsent(attribute, holder -> this.supplier.createInstance(this::onAttributeModified, (Holder<Attribute>)holder));
   }

   public boolean hasAttribute(Holder<Attribute> attribute) {
      return this.attributes.get(attribute) != null || this.supplier.hasAttribute(attribute);
   }

   public boolean hasModifier(Holder<Attribute> attribute, Identifier id) {
      AttributeInstance attributeInstance = this.attributes.get(attribute);
      return attributeInstance != null ? attributeInstance.getModifier(id) != null : this.supplier.hasModifier(attribute, id);
   }

   public double getValue(Holder<Attribute> attribute) {
      AttributeInstance attributeInstance = this.attributes.get(attribute);
      return attributeInstance != null ? attributeInstance.getValue() : this.supplier.getValue(attribute);
   }

   public double getBaseValue(Holder<Attribute> attribute) {
      AttributeInstance attributeInstance = this.attributes.get(attribute);
      return attributeInstance != null ? attributeInstance.getBaseValue() : this.supplier.getBaseValue(attribute);
   }

   public double getModifierValue(Holder<Attribute> attribute, Identifier id) {
      AttributeInstance attributeInstance = this.attributes.get(attribute);
      return attributeInstance != null ? attributeInstance.getModifier(id).amount() : this.supplier.getModifierValue(attribute, id);
   }

   public void addTransientAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
      modifiers.forEach((holder, attributeModifier) -> {
         AttributeInstance attributeInstance = this.getInstance(holder);
         if (attributeInstance != null) {
            attributeInstance.removeModifier(attributeModifier.id());
            attributeInstance.addTransientModifier(attributeModifier);
         }
      });
   }

   public void removeAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
      modifiers.asMap().forEach((holder, collection) -> {
         AttributeInstance attributeInstance = this.attributes.get(holder);
         if (attributeInstance != null) {
            collection.forEach(attributeModifier -> attributeInstance.removeModifier(attributeModifier.id()));
         }
      });
   }

   public void assignAllValues(AttributeMap map) {
      map.attributes.values().forEach(attributeInstance -> {
         AttributeInstance attributeInstance2 = this.getInstance(attributeInstance.getAttribute());
         if (attributeInstance2 != null) {
            attributeInstance2.replaceFrom(attributeInstance);
         }
      });
   }

   public void assignBaseValues(AttributeMap map) {
      map.attributes.values().forEach(attributeInstance -> {
         AttributeInstance attributeInstance2 = this.getInstance(attributeInstance.getAttribute());
         if (attributeInstance2 != null) {
            attributeInstance2.setBaseValue(attributeInstance.getBaseValue());
         }
      });
   }

   public void assignPermanentModifiers(AttributeMap map) {
      map.attributes.values().forEach(attributeInstance -> {
         AttributeInstance attributeInstance2 = this.getInstance(attributeInstance.getAttribute());
         if (attributeInstance2 != null) {
            attributeInstance2.addPermanentModifiers(attributeInstance.getPermanentModifiers());
         }
      });
   }

   public boolean resetBaseValue(Holder<Attribute> attribute) {
      if (!this.supplier.hasAttribute(attribute)) {
         return false;
      } else {
         AttributeInstance attributeInstance = this.attributes.get(attribute);
         if (attributeInstance != null) {
            attributeInstance.setBaseValue(this.supplier.getBaseValue(attribute));
         }

         return true;
      }
   }

   public List<AttributeInstance.Packed> pack() {
      List<AttributeInstance.Packed> list = new ArrayList<>(this.attributes.values().size());

      for (AttributeInstance attributeInstance : this.attributes.values()) {
         list.add(attributeInstance.pack());
      }

      return list;
   }

   public void apply(List<AttributeInstance.Packed> attributes) {
      for (AttributeInstance.Packed packed : attributes) {
         AttributeInstance attributeInstance = this.getInstance(packed.attribute());
         if (attributeInstance != null) {
            attributeInstance.apply(packed);
         }
      }
   }
}
