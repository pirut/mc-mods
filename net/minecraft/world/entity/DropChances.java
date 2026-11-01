package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;

public record DropChances(Map<EquipmentSlot, Float> byEquipment) {
   public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
   public static final float PRESERVE_ITEM_DROP_CHANCE_THRESHOLD = 1.0F;
   public static final int PRESERVE_ITEM_DROP_CHANCE = 2;
   public static final DropChances DEFAULT = new DropChances(Util.makeEnumMap(EquipmentSlot.class, p_393572_ -> 0.085F));
   public static final Codec<DropChances> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ExtraCodecs.NON_NEGATIVE_FLOAT)
      .xmap(DropChances::toEnumMap, DropChances::filterDefaultValues)
      .xmap(DropChances::new, DropChances::byEquipment);

   private static Map<EquipmentSlot, Float> filterDefaultValues(Map<EquipmentSlot, Float> p_394014_) {
      Map<EquipmentSlot, Float> map = new HashMap<>(p_394014_);
      map.values().removeIf(p_393748_ -> p_393748_ == 0.085F);
      return map;
   }

   private static Map<EquipmentSlot, Float> toEnumMap(Map<EquipmentSlot, Float> p_393876_) {
      return Util.makeEnumMap(EquipmentSlot.class, p_394429_ -> p_393876_.getOrDefault(p_394429_, 0.085F));
   }

   public DropChances withGuaranteedDrop(EquipmentSlot p_394388_) {
      return this.withEquipmentChance(p_394388_, 2.0F);
   }

   public DropChances withEquipmentChance(EquipmentSlot p_394616_, float p_394553_) {
      if (p_394553_ < 0.0F) {
         throw new IllegalArgumentException("Tried to set invalid equipment chance " + p_394553_ + " for " + p_394616_);
      } else {
         return this.byEquipment(p_394616_) == p_394553_
            ? this
            : new DropChances(Util.makeEnumMap(EquipmentSlot.class, p_393769_ -> p_393769_ == p_394616_ ? p_394553_ : this.byEquipment(p_393769_)));
      }
   }

   public float byEquipment(EquipmentSlot p_394232_) {
      return this.byEquipment.getOrDefault(p_394232_, 0.085F);
   }

   public boolean isPreserved(EquipmentSlot p_394426_) {
      return this.byEquipment(p_394426_) > 1.0F;
   }
}
