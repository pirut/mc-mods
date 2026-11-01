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

   public AttributeMap(AttributeSupplier p_22144_) {
      this.supplier = p_22144_;
   }

   private void onAttributeModified(AttributeInstance p_22158_) {
      this.attributesToUpdate.add(p_22158_);
      if (p_22158_.getAttribute().value().isClientSyncable()) {
         this.attributesToSync.add(p_22158_);
      }
   }

   public Set<AttributeInstance> getAttributesToSync() {
      return this.attributesToSync;
   }

   public Set<AttributeInstance> getAttributesToUpdate() {
      return this.attributesToUpdate;
   }

   public Collection<AttributeInstance> getSyncableAttributes() {
      return this.attributes.values().stream().filter(p_315935_ -> p_315935_.getAttribute().value().isClientSyncable()).collect(Collectors.toList());
   }

   @Nullable
   public AttributeInstance getInstance(Holder<Attribute> p_250010_) {
      return this.attributes.computeIfAbsent(p_250010_, p_315936_ -> this.supplier.createInstance(this::onAttributeModified, (Holder<Attribute>)p_315936_));
   }

   public boolean hasAttribute(Holder<Attribute> p_248893_) {
      return this.attributes.get(p_248893_) != null || this.supplier.hasAttribute(p_248893_);
   }

   public boolean hasModifier(Holder<Attribute> p_250299_, Identifier p_467583_) {
      AttributeInstance attributeinstance = this.attributes.get(p_250299_);
      return attributeinstance != null ? attributeinstance.getModifier(p_467583_) != null : this.supplier.hasModifier(p_250299_, p_467583_);
   }

   public double getValue(Holder<Attribute> p_316690_) {
      AttributeInstance attributeinstance = this.attributes.get(p_316690_);
      return attributeinstance != null ? attributeinstance.getValue() : this.supplier.getValue(p_316690_);
   }

   public double getBaseValue(Holder<Attribute> p_316270_) {
      AttributeInstance attributeinstance = this.attributes.get(p_316270_);
      return attributeinstance != null ? attributeinstance.getBaseValue() : this.supplier.getBaseValue(p_316270_);
   }

   public double getModifierValue(Holder<Attribute> p_251534_, Identifier p_467492_) {
      AttributeInstance attributeinstance = this.attributes.get(p_251534_);
      return attributeinstance != null ? attributeinstance.getModifier(p_467492_).amount() : this.supplier.getModifierValue(p_251534_, p_467492_);
   }

   public void addTransientAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> p_345244_) {
      p_345244_.forEach((p_466547_, p_466548_) -> {
         AttributeInstance attributeinstance = this.getInstance(p_466547_);
         if (attributeinstance != null) {
            attributeinstance.removeModifier(p_466548_.id());
            attributeinstance.addTransientModifier(p_466548_);
         }
      });
   }

   public void removeAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> p_344776_) {
      p_344776_.asMap().forEach((p_344297_, p_344298_) -> {
         AttributeInstance attributeinstance = this.attributes.get(p_344297_);
         if (attributeinstance != null) {
            p_344298_.forEach(p_466550_ -> attributeinstance.removeModifier(p_466550_.id()));
         }
      });
   }

   public void assignAllValues(AttributeMap p_348501_) {
      p_348501_.attributes.values().forEach(p_315934_ -> {
         AttributeInstance attributeinstance = this.getInstance(p_315934_.getAttribute());
         if (attributeinstance != null) {
            attributeinstance.replaceFrom(p_315934_);
         }
      });
   }

   public void assignBaseValues(AttributeMap p_22160_) {
      p_22160_.attributes.values().forEach(p_348165_ -> {
         AttributeInstance attributeinstance = this.getInstance(p_348165_.getAttribute());
         if (attributeinstance != null) {
            attributeinstance.setBaseValue(p_348165_.getBaseValue());
         }
      });
   }

   public void assignPermanentModifiers(AttributeMap p_364660_) {
      p_364660_.attributes.values().forEach(p_359721_ -> {
         AttributeInstance attributeinstance = this.getInstance(p_359721_.getAttribute());
         if (attributeinstance != null) {
            attributeinstance.addPermanentModifiers(p_359721_.getPermanentModifiers());
         }
      });
   }

   public boolean resetBaseValue(Holder<Attribute> p_383183_) {
      if (!this.supplier.hasAttribute(p_383183_)) {
         return false;
      } else {
         AttributeInstance attributeinstance = this.attributes.get(p_383183_);
         if (attributeinstance != null) {
            attributeinstance.setBaseValue(this.supplier.getBaseValue(p_383183_));
         }

         return true;
      }
   }

   public List<AttributeInstance.Packed> pack() {
      List<AttributeInstance.Packed> list = new ArrayList<>(this.attributes.values().size());

      for (AttributeInstance attributeinstance : this.attributes.values()) {
         list.add(attributeinstance.pack());
      }

      return list;
   }

   public void apply(List<AttributeInstance.Packed> p_422173_) {
      for (AttributeInstance.Packed attributeinstance$packed : p_422173_) {
         AttributeInstance attributeinstance = this.getInstance(attributeinstance$packed.attribute());
         if (attributeinstance != null) {
            attributeinstance.apply(attributeinstance$packed);
         }
      }
   }
}
