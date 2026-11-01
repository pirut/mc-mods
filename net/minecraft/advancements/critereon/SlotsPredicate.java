package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

public record SlotsPredicate(Map<SlotRange, ItemPredicate> slots) {
   public static final Codec<SlotsPredicate> CODEC = Codec.unboundedMap(SlotRanges.CODEC, ItemPredicate.CODEC).xmap(SlotsPredicate::new, SlotsPredicate::slots);

   public boolean matches(Entity p_332796_) {
      for (Entry<SlotRange, ItemPredicate> $$1 : this.slots.entrySet()) {
         if (!matchSlots(p_332796_, $$1.getValue(), $$1.getKey().slots())) {
            return false;
         }
      }

      return true;
   }

   private static boolean matchSlots(Entity p_332687_, ItemPredicate p_332814_, IntList p_332676_) {
      for (int $$3 = 0; $$3 < p_332676_.size(); $$3++) {
         int $$4 = p_332676_.getInt($$3);
         SlotAccess $$5 = p_332687_.getSlot($$4);
         if (p_332814_.test($$5.get())) {
            return true;
         }
      }

      return false;
   }
}
