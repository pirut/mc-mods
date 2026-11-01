package net.minecraft.world.item.slot;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public interface SlotCollection {
   SlotCollection EMPTY = Stream::empty;

   Stream<ItemStack> itemCopies();

   default SlotCollection filter(Predicate<ItemStack> p_460912_) {
      return new SlotCollection.Filtered(this, p_460912_);
   }

   default SlotCollection flatMap(Function<ItemStack, ? extends SlotCollection> p_461278_) {
      return new SlotCollection.FlatMapped(this, p_461278_);
   }

   default SlotCollection limit(int p_461220_) {
      return new SlotCollection.Limited(this, p_461220_);
   }

   static SlotCollection of(SlotAccess p_461016_) {
      return () -> Stream.of(p_461016_.get().copy());
   }

   static SlotCollection of(Collection<? extends SlotAccess> p_461012_) {
      return switch (p_461012_.size()) {
         case 0 -> EMPTY;
         case 1 -> of(p_461012_.iterator().next());
         default -> () -> p_461012_.stream().map(SlotAccess::get).map(ItemStack::copy);
      };
   }

   static SlotCollection concat(SlotCollection p_461005_, SlotCollection p_461109_) {
      return () -> Stream.concat(p_461005_.itemCopies(), p_461109_.itemCopies());
   }

   static SlotCollection concat(List<? extends SlotCollection> p_460983_) {
      return switch (p_460983_.size()) {
         case 0 -> EMPTY;
         case 1 -> (SlotCollection)p_460983_.getFirst();
         case 2 -> concat(p_460983_.get(0), p_460983_.get(1));
         default -> () -> p_460983_.stream().flatMap(SlotCollection::itemCopies);
      };
   }

   public record Filtered(SlotCollection slots, Predicate<ItemStack> filter) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().filter(this.filter);
      }

      @Override
      public SlotCollection filter(Predicate<ItemStack> p_461182_) {
         return new SlotCollection.Filtered(this.slots, this.filter.and(p_461182_));
      }
   }

   public record FlatMapped(SlotCollection slots, Function<ItemStack, ? extends SlotCollection> mapper) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().map(this.mapper).flatMap(SlotCollection::itemCopies);
      }
   }

   public record Limited(SlotCollection slots, int limit) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().limit(this.limit);
      }

      @Override
      public SlotCollection limit(int p_461097_) {
         return new SlotCollection.Limited(this.slots, Math.min(this.limit, p_461097_));
      }
   }
}
