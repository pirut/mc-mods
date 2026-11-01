package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.world.item.ItemStack;

public class EntityEquipment {
   public static final Codec<EntityEquipment> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).xmap(p_398046_ -> {
      EnumMap<EquipmentSlot, ItemStack> enummap = new EnumMap<>(EquipmentSlot.class);
      enummap.putAll(p_398046_);
      return new EntityEquipment(enummap);
   }, p_397308_ -> {
      Map<EquipmentSlot, ItemStack> map = new EnumMap<>(p_397308_.items);
      map.values().removeIf(ItemStack::isEmpty);
      return map;
   });
   private final EnumMap<EquipmentSlot, ItemStack> items;

   private EntityEquipment(EnumMap<EquipmentSlot, ItemStack> p_397767_) {
      this.items = p_397767_;
   }

   public EntityEquipment() {
      this(new EnumMap<>(EquipmentSlot.class));
   }

   public ItemStack set(EquipmentSlot p_397821_, ItemStack p_397647_) {
      return Objects.requireNonNullElse(this.items.put(p_397821_, p_397647_), ItemStack.EMPTY);
   }

   public ItemStack get(EquipmentSlot p_397366_) {
      return this.items.getOrDefault(p_397366_, ItemStack.EMPTY);
   }

   public boolean isEmpty() {
      for (ItemStack itemstack : this.items.values()) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void tick(Entity p_401779_) {
      for (Entry<EquipmentSlot, ItemStack> entry : this.items.entrySet()) {
         ItemStack itemstack = entry.getValue();
         if (!itemstack.isEmpty()) {
            itemstack.inventoryTick(p_401779_.level(), p_401779_, entry.getKey());
         }
      }
   }

   public void setAll(EntityEquipment p_401921_) {
      this.items.clear();
      this.items.putAll(p_401921_.items);
   }

   public void dropAll(LivingEntity p_401845_) {
      for (ItemStack itemstack : this.items.values()) {
         p_401845_.drop(itemstack, true, false);
      }

      this.clear();
   }

   public void clear() {
      this.items.replaceAll((p_401733_, p_401734_) -> ItemStack.EMPTY);
   }
}
