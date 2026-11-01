package net.neoforged.neoforge.event.entity.living;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ArmorHurtEvent extends LivingEvent implements ICancellableEvent {
   private final DamageSource source;
   private final EnumMap<EquipmentSlot, ArmorHurtEvent.ArmorEntry> armorEntries;

   @Internal
   public ArmorHurtEvent(EnumMap<EquipmentSlot, ArmorHurtEvent.ArmorEntry> armorMap, LivingEntity player, DamageSource source) {
      super(player);
      this.armorEntries = armorMap;
      this.source = source;
   }

   public ItemStack getArmorItemStack(EquipmentSlot slot) {
      return this.armorEntries.containsKey(slot) ? this.armorEntries.get(slot).armorItemStack : ItemStack.EMPTY;
   }

   public Float getOriginalDamage(EquipmentSlot slot) {
      return this.armorEntries.containsKey(slot) ? this.armorEntries.get(slot).originalDamage : 0.0F;
   }

   public Float getNewDamage(EquipmentSlot slot) {
      return this.armorEntries.containsKey(slot) ? this.armorEntries.get(slot).newDamage : 0.0F;
   }

   public void setNewDamage(EquipmentSlot slot, float damage) {
      if (this.armorEntries.containsKey(slot)) {
         this.armorEntries.get(slot).newDamage = damage;
      }
   }

   public Map<EquipmentSlot, ArmorHurtEvent.ArmorEntry> getArmorMap() {
      return this.armorEntries;
   }

   public DamageSource getDamageSource() {
      return this.source;
   }

   public static class ArmorEntry {
      public ItemStack armorItemStack;
      public final float originalDamage;
      public float newDamage;

      public ArmorEntry(ItemStack armorStack, float damageIn) {
         this.armorItemStack = armorStack;
         this.originalDamage = damageIn;
         this.newDamage = damageIn;
      }
   }
}
