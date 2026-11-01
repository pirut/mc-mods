package net.minecraft.core.dispenser;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public class EquipmentDispenseItemBehavior extends DefaultDispenseItemBehavior {
   public static final EquipmentDispenseItemBehavior INSTANCE = new EquipmentDispenseItemBehavior();

   @Override
   protected ItemStack execute(BlockSource p_371344_, ItemStack p_371463_) {
      return dispenseEquipment(p_371344_, p_371463_) ? p_371463_ : super.execute(p_371344_, p_371463_);
   }

   public static boolean dispenseEquipment(BlockSource p_371324_, ItemStack p_371227_) {
      BlockPos blockpos = p_371324_.pos().relative(p_371324_.state().getValue(DispenserBlock.FACING));
      List<LivingEntity> list = p_371324_.level()
         .getEntitiesOfClass(LivingEntity.class, new AABB(blockpos), p_371713_ -> p_371713_.canEquipWithDispenser(p_371227_));
      if (list.isEmpty()) {
         return false;
      } else {
         LivingEntity livingentity = list.getFirst();
         EquipmentSlot equipmentslot = livingentity.getEquipmentSlotForItem(p_371227_);
         if (!p_371227_.canEquip(equipmentslot, livingentity)) {
            return false;
         } else {
            ItemStack itemstack = p_371227_.split(1);
            livingentity.setItemSlot(equipmentslot, itemstack);
            if (livingentity instanceof Mob mob) {
               mob.setGuaranteedDrop(equipmentslot);
               mob.setPersistenceRequired();
            }

            return true;
         }
      }
   }
}
