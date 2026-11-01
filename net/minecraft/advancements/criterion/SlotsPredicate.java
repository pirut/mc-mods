package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

public record SlotsPredicate(Map<SlotRange, ItemPredicate> slots) {
   public static final Codec<SlotsPredicate> CODEC = Codec.unboundedMap(SlotRanges.CODEC, ItemPredicate.CODEC).xmap(SlotsPredicate::new, SlotsPredicate::slots);

   public boolean matches(SlotProvider p_468920_) {
      for (Entry<SlotRange, ItemPredicate> entry : this.slots.entrySet()) {
         if (!matchSlots(p_468920_, entry.getValue(), entry.getKey().slots())) {
            return false;
         }
      }

      return true;
   }

   private static boolean matchSlots(SlotProvider p_468351_, ItemPredicate p_468035_, IntList p_467545_) {
      for (int i = 0; i < p_467545_.size(); i++) {
         int j = p_467545_.getInt(i);
         SlotAccess slotaccess = p_468351_.getSlot(j);
         if (slotaccess != null && p_468035_.test(slotaccess.get())) {
            return true;
         }
      }

      return false;
   }
}
