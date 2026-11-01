package net.neoforged.neoforge.event.brewing;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class PotionBrewEvent extends Event {
   private NonNullList<ItemStack> stacks;

   protected PotionBrewEvent(NonNullList<ItemStack> stacks) {
      this.stacks = stacks;
   }

   public ItemStack getItem(int index) {
      return index >= 0 && index < this.stacks.size() ? this.stacks.get(index) : ItemStack.EMPTY;
   }

   public void setItem(int index, ItemStack stack) {
      if (index < this.stacks.size()) {
         this.stacks.set(index, stack);
      }
   }

   public int getLength() {
      return this.stacks.size();
   }

   public static class Post extends PotionBrewEvent {
      public Post(NonNullList<ItemStack> stacks) {
         super(stacks);
      }
   }

   public static class Pre extends PotionBrewEvent implements ICancellableEvent {
      public Pre(NonNullList<ItemStack> stacks) {
         super(stacks);
      }
   }
}
