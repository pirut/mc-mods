package net.neoforged.neoforge.transfer.item;

import com.google.common.collect.MapMaker;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;

public class LivingEntityEquipmentWrapper {
   private static final Map<LivingEntity, LivingEntityEquipmentWrapper> wrappers = new MapMaker().weakKeys().weakValues().makeMap();
   private final LivingEntity entity;
   private final Map<EquipmentSlot.Type, LivingEntityEquipmentWrapper.EquipmentTypeWrapper> byType;

   public static ResourceHandler<ItemResource> of(LivingEntity entity, EquipmentSlot.Type equipmentType) {
      if (entity instanceof Player player) {
         return switch (equipmentType) {
            case HAND -> PlayerInventoryWrapper.of(player).getHandSlots();
            case HUMANOID_ARMOR -> PlayerInventoryWrapper.of(player).getArmorSlots();
            default -> throw new IllegalArgumentException("Wrapping the equipment type " + equipmentType + " of a player is not supported.");
         };
      } else {
         return internalOf(entity, equipmentType);
      }
   }

   public static ResourceHandler<ItemResource> of(LivingEntity entity, EquipmentSlot equipmentSlot) {
      if (entity instanceof Player player) {
         if (equipmentSlot == EquipmentSlot.MAINHAND) {
            return PlayerInventoryWrapper.of(player).getMainHandSlot();
         } else if (equipmentSlot == EquipmentSlot.OFFHAND) {
            return PlayerInventoryWrapper.of(player).getHandSlot(InteractionHand.OFF_HAND);
         } else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return PlayerInventoryWrapper.of(player).getArmorSlot(equipmentSlot);
         } else {
            throw new IllegalArgumentException("Wrapping the equipment slot " + equipmentSlot + " of a player is not supported.");
         }
      } else {
         return internalOf(entity, equipmentSlot.getType()).getSlotWrapper(equipmentSlot.getIndex());
      }
   }

   private static LivingEntityEquipmentWrapper.EquipmentTypeWrapper internalOf(LivingEntity entity, EquipmentSlot.Type equipmentType) {
      LivingEntityEquipmentWrapper wrapper = wrappers.computeIfAbsent(entity, LivingEntityEquipmentWrapper::new);
      return wrapper.byType.get(equipmentType);
   }

   private LivingEntityEquipmentWrapper(LivingEntity entity) {
      this.entity = entity;
      this.byType = new EnumMap<>(EquipmentSlot.Type.class);

      for (EquipmentSlot.Type equipmentType : EquipmentSlot.Type.values()) {
         ArrayList<LivingEntityEquipmentWrapper.SlotWrapper> slotWrappers = new ArrayList<>();

         for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            if (equipmentSlot.getType() == equipmentType) {
               slotWrappers.add(new LivingEntityEquipmentWrapper.SlotWrapper(equipmentSlot));
            }
         }

         this.byType
            .put(equipmentType, new LivingEntityEquipmentWrapper.EquipmentTypeWrapper(slotWrappers.toArray(LivingEntityEquipmentWrapper.SlotWrapper[]::new)));
      }
   }

   private class EquipmentTypeWrapper extends CombinedResourceHandler<ItemResource> {
      EquipmentTypeWrapper(LivingEntityEquipmentWrapper.SlotWrapper... handlers) {
         super(handlers);
      }

      LivingEntityEquipmentWrapper.SlotWrapper getSlotWrapper(int index) {
         return (LivingEntityEquipmentWrapper.SlotWrapper)this.getHandlerFromIndex(index);
      }
   }

   private class SlotWrapper extends ItemStackResourceHandler {
      private final EquipmentSlot slot;

      private SlotWrapper(EquipmentSlot slot) {
         this.slot = slot;
      }

      @Override
      protected ItemStack getStack() {
         return LivingEntityEquipmentWrapper.this.entity.getItemBySlot(this.slot);
      }

      @Override
      protected void setStack(ItemStack stack) {
         LivingEntityEquipmentWrapper.this.entity.setItemSlot(this.slot, stack, true);
      }

      @Override
      protected boolean isValid(ItemResource resource) {
         return resource.toStack().canEquip(this.slot, LivingEntityEquipmentWrapper.this.entity);
      }

      @Override
      protected int getCapacity(ItemResource resource) {
         int slotLimit = this.slot.countLimit == 0 ? 99 : this.slot.countLimit;
         return resource.isEmpty() ? slotLimit : Math.min(slotLimit, resource.getMaxStackSize());
      }

      protected void onRootCommit(ItemStack originalState) {
         LivingEntityEquipmentWrapper.this.entity.onEquipItem(this.slot, originalState, this.getStack());
      }

      @Override
      public String toString() {
         return "entity equipment wrapper[entity=" + LivingEntityEquipmentWrapper.this.entity + ",slot=" + this.slot + "]";
      }
   }
}
