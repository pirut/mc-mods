package net.neoforged.neoforge.transfer.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.world.inventory.StackCopySlot;

public class ResourceHandlerSlot extends StackCopySlot {
   private final ResourceHandler<ItemResource> handler;
   private final IndexModifier<ItemResource> slotModifier;
   private final int index;

   public ResourceHandlerSlot(ResourceHandler<ItemResource> handler, IndexModifier<ItemResource> slotModifier, int index, int xPosition, int yPosition) {
      super(xPosition, yPosition);
      this.handler = handler;
      this.slotModifier = slotModifier;
      this.index = index;
   }

   @Override
   public boolean mayPlace(ItemStack stack) {
      return stack.isEmpty() ? false : this.handler.isValid(this.index, ItemResource.of(stack));
   }

   @Override
   protected ItemStack getStackCopy() {
      return this.handler.getResource(this.index).toStack(this.handler.getAmountAsInt(this.index));
   }

   @Override
   protected void setStackCopy(ItemStack stack) {
      this.slotModifier.set(this.index, ItemResource.of(stack), stack.getCount());
   }

   @Override
   public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {
   }

   @Override
   public int getMaxStackSize() {
      return this.handler.getCapacityAsInt(this.index, ItemResource.EMPTY);
   }

   @Override
   public int getMaxStackSize(ItemStack stack) {
      return this.handler.getCapacityAsInt(this.index, ItemResource.of(stack));
   }

   @Override
   public boolean mayPickup(Player player) {
      ItemResource resource = this.handler.getResource(this.index);
      if (resource.isEmpty()) {
         return false;
      } else {
         boolean var4;
         try (Transaction tx = Transaction.openRoot()) {
            var4 = this.handler.extract(this.index, resource, 1, tx) == 1;
         }

         return var4;
      }
   }

   public ResourceHandler<ItemResource> getResourceHandler() {
      return this.handler;
   }

   @Override
   public boolean isSameInventory(Slot other) {
      return other instanceof ResourceHandlerSlot rhs && rhs.handler == this.handler;
   }
}
