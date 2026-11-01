package net.neoforged.neoforge.items.wrapper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class EntityArmorInvWrapper extends EntityEquipmentInvWrapper {
   public EntityArmorInvWrapper(LivingEntity entity) {
      super(entity, EquipmentSlot.Type.HUMANOID_ARMOR);
   }
}
