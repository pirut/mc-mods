package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class InventoryChangeTrigger extends SimpleCriterionTrigger<InventoryChangeTrigger.TriggerInstance> {
   @Override
   public Codec<InventoryChangeTrigger.TriggerInstance> codec() {
      return InventoryChangeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_469666_, Inventory p_467699_, ItemStack p_469055_) {
      int i = 0;
      int j = 0;
      int k = 0;

      for (int l = 0; l < p_467699_.getContainerSize(); l++) {
         ItemStack itemstack = p_467699_.getItem(l);
         if (itemstack.isEmpty()) {
            j++;
         } else {
            k++;
            if (itemstack.getCount() >= itemstack.getMaxStackSize()) {
               i++;
            }
         }
      }

      this.trigger(p_469666_, p_467699_, p_469055_, i, j, k);
   }

   private void trigger(ServerPlayer p_467782_, Inventory p_469935_, ItemStack p_469240_, int p_468377_, int p_468675_, int p_467614_) {
      this.trigger(p_467782_, p_468203_ -> p_468203_.matches(p_469935_, p_469240_, p_468377_, p_468675_, p_467614_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, InventoryChangeTrigger.TriggerInstance.Slots slots, List<ItemPredicate> items)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<InventoryChangeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_467627_ -> p_467627_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(InventoryChangeTrigger.TriggerInstance::player),
               InventoryChangeTrigger.TriggerInstance.Slots.CODEC
                  .optionalFieldOf("slots", InventoryChangeTrigger.TriggerInstance.Slots.ANY)
                  .forGetter(InventoryChangeTrigger.TriggerInstance::slots),
               ItemPredicate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(InventoryChangeTrigger.TriggerInstance::items)
            )
            .apply(p_467627_, InventoryChangeTrigger.TriggerInstance::new)
      );

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate.Builder... p_469960_) {
         return hasItems(Stream.of(p_469960_).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate... p_469043_) {
         return CriteriaTriggers.INVENTORY_CHANGED
            .createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(p_469043_)));
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemLike... p_468013_) {
         ItemPredicate[] aitempredicate = new ItemPredicate[p_468013_.length];

         for (int i = 0; i < p_468013_.length; i++) {
            aitempredicate[i] = new ItemPredicate(
               Optional.of(HolderSet.direct(p_468013_[i].asItem().builtInRegistryHolder())), MinMaxBounds.Ints.ANY, DataComponentMatchers.ANY
            );
         }

         return hasItems(aitempredicate);
      }

      public boolean matches(Inventory p_467847_, ItemStack p_466994_, int p_469446_, int p_468797_, int p_468595_) {
         if (!this.slots.matches(p_469446_, p_468797_, p_468595_)) {
            return false;
         } else if (this.items.isEmpty()) {
            return true;
         } else if (this.items.size() != 1) {
            List<ItemPredicate> list = new ObjectArrayList(this.items);
            int i = p_467847_.getContainerSize();

            for (int j = 0; j < i; j++) {
               if (list.isEmpty()) {
                  return true;
               }

               ItemStack itemstack = p_467847_.getItem(j);
               if (!itemstack.isEmpty()) {
                  list.removeIf(p_468378_ -> p_468378_.test(itemstack));
               }
            }

            return list.isEmpty();
         } else {
            return !p_466994_.isEmpty() && this.items.get(0).test(p_466994_);
         }
      }

      public record Slots(MinMaxBounds.Ints occupied, MinMaxBounds.Ints full, MinMaxBounds.Ints empty) {
         public static final Codec<InventoryChangeTrigger.TriggerInstance.Slots> CODEC = RecordCodecBuilder.create(
            p_469340_ -> p_469340_.group(
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("occupied", MinMaxBounds.Ints.ANY).forGetter(InventoryChangeTrigger.TriggerInstance.Slots::occupied),
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("full", MinMaxBounds.Ints.ANY).forGetter(InventoryChangeTrigger.TriggerInstance.Slots::full),
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("empty", MinMaxBounds.Ints.ANY).forGetter(InventoryChangeTrigger.TriggerInstance.Slots::empty)
               )
               .apply(p_469340_, InventoryChangeTrigger.TriggerInstance.Slots::new)
         );
         public static final InventoryChangeTrigger.TriggerInstance.Slots ANY = new InventoryChangeTrigger.TriggerInstance.Slots(
            MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY
         );

         public boolean matches(int p_467421_, int p_469628_, int p_469344_) {
            if (!this.full.matches(p_467421_)) {
               return false;
            } else {
               return !this.empty.matches(p_469628_) ? false : this.occupied.matches(p_469344_);
            }
         }
      }
   }
}
