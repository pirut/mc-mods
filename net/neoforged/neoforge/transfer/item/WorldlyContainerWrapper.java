package net.neoforged.neoforge.transfer.item;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class WorldlyContainerWrapper implements ResourceHandler<ItemResource> {
   private final WorldlyContainer container;
   private final VanillaContainerWrapper wrapper;
   @Nullable
   private final Direction side;

   public WorldlyContainerWrapper(WorldlyContainer container, @Nullable Direction side) {
      this.container = container;
      this.wrapper = VanillaContainerWrapper.internalOf(container);
      this.side = side;
   }

   private int convertSlot(int slot) {
      if (slot < 0) {
         throw new IndexOutOfBoundsException("Cannot access container with negative slot index: " + slot);
      } else if (this.side == null) {
         return slot;
      } else {
         int[] slots = this.container.getSlotsForFace(this.side);
         if (slot >= slots.length) {
            throw new IndexOutOfBoundsException(
               "Cannot access worldly container on side " + this.side + " : out of bounds slot index " + slot + " with size " + slots.length
            );
         } else {
            return slots[slot];
         }
      }
   }

   @Override
   public int size() {
      return this.side == null ? this.container.getContainerSize() : this.container.getSlotsForFace(this.side).length;
   }

   public ItemResource getResource(int index) {
      return this.wrapper.getResource(this.convertSlot(index));
   }

   @Override
   public long getAmountAsLong(int index) {
      return this.wrapper.getAmountAsLong(this.convertSlot(index));
   }

   public long getCapacityAsLong(int index, ItemResource resource) {
      return this.wrapper.getCapacityAsLong(this.convertSlot(index), resource);
   }

   public boolean isValid(int index, ItemResource resource) {
      return this.wrapper.isValid(this.convertSlot(index), resource);
   }

   public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
      int convertedSlot = this.convertSlot(index);
      return !this.container.canPlaceItemThroughFace(convertedSlot, resource.toStack(), this.side)
         ? 0
         : this.wrapper.insert(convertedSlot, resource, amount, transaction);
   }

   public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
      int convertedSlot = this.convertSlot(index);
      return this.side != null && !this.container.canTakeItemThroughFace(convertedSlot, resource.toStack(), this.side)
         ? 0
         : this.wrapper.extract(convertedSlot, resource, amount, transaction);
   }
}
