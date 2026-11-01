package net.minecraft.world.entity.ai.attributes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class AttributeInstance {
   private static final String BASE_FIELD = "base";
   private static final String MODIFIERS_FIELD = "modifiers";
   public static final String ID_FIELD = "id";
   private final Holder<Attribute> attribute;
   private final Map<AttributeModifier.Operation, Map<ResourceLocation, AttributeModifier>> modifiersByOperation = Maps.newEnumMap(
      AttributeModifier.Operation.class
   );
   private final Map<ResourceLocation, AttributeModifier> modifierById = new Object2ObjectArrayMap();
   private final Map<ResourceLocation, AttributeModifier> permanentModifiers = new Object2ObjectArrayMap();
   private double baseValue;
   private boolean dirty = true;
   private double cachedValue;
   private final Consumer<AttributeInstance> onDirty;

   public AttributeInstance(Holder<Attribute> p_316357_, Consumer<AttributeInstance> p_22098_) {
      this.attribute = p_316357_;
      this.onDirty = p_22098_;
      this.baseValue = p_316357_.value().getDefaultValue();
   }

   public Holder<Attribute> getAttribute() {
      return this.attribute;
   }

   public double getBaseValue() {
      return this.baseValue;
   }

   public void setBaseValue(double p_22101_) {
      if (p_22101_ != this.baseValue) {
         this.baseValue = p_22101_;
         this.setDirty();
      }
   }

   @VisibleForTesting
   Map<ResourceLocation, AttributeModifier> getModifiers(AttributeModifier.Operation p_22105_) {
      return this.modifiersByOperation.computeIfAbsent(p_22105_, p_332604_ -> new Object2ObjectOpenHashMap());
   }

   public Set<AttributeModifier> getModifiers() {
      return ImmutableSet.copyOf(this.modifierById.values());
   }

   @Nullable
   public AttributeModifier getModifier(ResourceLocation p_351007_) {
      return this.modifierById.get(p_351007_);
   }

   public boolean hasModifier(ResourceLocation p_350421_) {
      return this.modifierById.get(p_350421_) != null;
   }

   private void addModifier(AttributeModifier p_22134_) {
      AttributeModifier $$1 = this.modifierById.putIfAbsent(p_22134_.id(), p_22134_);
      if ($$1 != null) {
         throw new IllegalArgumentException("Modifier is already applied on this attribute!");
      } else {
         this.getModifiers(p_22134_.operation()).put(p_22134_.id(), p_22134_);
         this.setDirty();
      }
   }

   public void addOrUpdateTransientModifier(AttributeModifier p_316482_) {
      AttributeModifier $$1 = this.modifierById.put(p_316482_.id(), p_316482_);
      if (p_316482_ != $$1) {
         this.getModifiers(p_316482_.operation()).put(p_316482_.id(), p_316482_);
         this.setDirty();
      }
   }

   public void addTransientModifier(AttributeModifier p_22119_) {
      this.addModifier(p_22119_);
   }

   public void addOrReplacePermanentModifier(AttributeModifier p_353041_) {
      this.removeModifier(p_353041_.id());
      this.addModifier(p_353041_);
      this.permanentModifiers.put(p_353041_.id(), p_353041_);
   }

   public void addPermanentModifier(AttributeModifier p_22126_) {
      this.addModifier(p_22126_);
      this.permanentModifiers.put(p_22126_.id(), p_22126_);
   }

   protected void setDirty() {
      this.dirty = true;
      this.onDirty.accept(this);
   }

   public void removeModifier(AttributeModifier p_22131_) {
      this.removeModifier(p_22131_.id());
   }

   public boolean removeModifier(ResourceLocation p_350300_) {
      AttributeModifier $$1 = this.modifierById.remove(p_350300_);
      if ($$1 == null) {
         return false;
      } else {
         this.getModifiers($$1.operation()).remove(p_350300_);
         this.permanentModifiers.remove(p_350300_);
         this.setDirty();
         return true;
      }
   }

   public void removeModifiers() {
      for (AttributeModifier $$0 : this.getModifiers()) {
         this.removeModifier($$0);
      }
   }

   public double getValue() {
      if (this.dirty) {
         this.cachedValue = this.calculateValue();
         this.dirty = false;
      }

      return this.cachedValue;
   }

   private double calculateValue() {
      double $$0 = this.getBaseValue();

      for (AttributeModifier $$1 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_VALUE)) {
         $$0 += $$1.amount();
      }

      double $$2 = $$0;

      for (AttributeModifier $$3 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
         $$2 += $$0 * $$3.amount();
      }

      for (AttributeModifier $$4 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
         $$2 *= 1.0 + $$4.amount();
      }

      return this.attribute.value().sanitizeValue($$2);
   }

   private Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation p_22117_) {
      return this.modifiersByOperation.getOrDefault(p_22117_, Map.of()).values();
   }

   public void replaceFrom(AttributeInstance p_22103_) {
      this.baseValue = p_22103_.baseValue;
      this.modifierById.clear();
      this.modifierById.putAll(p_22103_.modifierById);
      this.permanentModifiers.clear();
      this.permanentModifiers.putAll(p_22103_.permanentModifiers);
      this.modifiersByOperation.clear();
      p_22103_.modifiersByOperation
         .forEach((p_332605_, p_332606_) -> this.getModifiers(p_332605_).putAll((Map<? extends ResourceLocation, ? extends AttributeModifier>)p_332606_));
      this.setDirty();
   }

   public CompoundTag save() {
      CompoundTag $$0 = new CompoundTag();
      ResourceKey<Attribute> $$1 = this.attribute.unwrapKey().orElseThrow(() -> new IllegalStateException("Tried to serialize unregistered attribute"));
      $$0.putString("id", $$1.location().toString());
      $$0.putDouble("base", this.baseValue);
      if (!this.permanentModifiers.isEmpty()) {
         ListTag $$2 = new ListTag();

         for (AttributeModifier $$3 : this.permanentModifiers.values()) {
            $$2.add($$3.save());
         }

         $$0.put("modifiers", $$2);
      }

      return $$0;
   }

   public void load(CompoundTag p_22114_) {
      this.baseValue = p_22114_.getDouble("base");
      if (p_22114_.contains("modifiers", 9)) {
         ListTag $$1 = p_22114_.getList("modifiers", 10);

         for (int $$2 = 0; $$2 < $$1.size(); $$2++) {
            AttributeModifier $$3 = AttributeModifier.load($$1.getCompound($$2));
            if ($$3 != null) {
               this.modifierById.put($$3.id(), $$3);
               this.getModifiers($$3.operation()).put($$3.id(), $$3);
               this.permanentModifiers.put($$3.id(), $$3);
            }
         }
      }

      this.setDirty();
   }
}
