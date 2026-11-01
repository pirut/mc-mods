package net.minecraft.world.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Inventory;

public class NautilusInventoryMenu extends AbstractMountInventoryMenu {
   private static final Identifier SADDLE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/saddle");
   private static final Identifier ARMOR_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/nautilus_armor_inventory");

   public NautilusInventoryMenu(int p_470588_, Inventory p_470553_, Container p_470714_, final AbstractNautilus p_470676_, int p_470843_) {
      super(p_470588_, p_470553_, p_470714_, p_470676_);
      Container container = p_470676_.createEquipmentSlotContainer(EquipmentSlot.SADDLE);
      this.addSlot(new ArmorSlot(container, p_470676_, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SLOT_SPRITE) {
         @Override
         public boolean isActive() {
            return p_470676_.canUseSlot(EquipmentSlot.SADDLE);
         }
      });
      Container container1 = p_470676_.createEquipmentSlotContainer(EquipmentSlot.BODY);
      this.addSlot(new ArmorSlot(container1, p_470676_, EquipmentSlot.BODY, 0, 8, 36, ARMOR_SLOT_SPRITE) {
         @Override
         public boolean isActive() {
            return p_470676_.canUseSlot(EquipmentSlot.BODY);
         }
      });
      this.addStandardInventorySlots(p_470553_, 8, 84);
   }

   @Override
   protected boolean hasInventoryChanged(Container p_470578_) {
      return ((AbstractNautilus)this.mount).hasInventoryChanged(p_470578_);
   }
}
