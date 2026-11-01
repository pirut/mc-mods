package net.neoforged.neoforge.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ItemHandlerCopySlot extends StackCopySlot {
   private final SlotItemHandler slotItemHandler;

   public ItemHandlerCopySlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
      super(index, xPosition, yPosition);
      this.slotItemHandler = new SlotItemHandler(itemHandler, index, xPosition, yPosition);
   }

   public ItemHandlerCopySlot(SlotItemHandler slotItemHandler) {
      super(slotItemHandler.getSlotIndex(), slotItemHandler.x, slotItemHandler.y);
      this.slotItemHandler = slotItemHandler;
   }

   @Override
   public boolean mayPlace(ItemStack stack) {
      return this.slotItemHandler.mayPlace(stack);
   }

   @Override
   protected ItemStack getStackCopy() {
      return this.slotItemHandler.getItem();
   }

   @Override
   protected void setStackCopy(ItemStack stack) {
      ((IItemHandlerModifiable)this.slotItemHandler.getItemHandler()).setStackInSlot(this.slotItemHandler.index, stack);
   }

   @Override
   public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {
      this.slotItemHandler.onQuickCraft(oldStackIn, newStackIn);
   }

   @Override
   public int getMaxStackSize() {
      return this.slotItemHandler.getMaxStackSize();
   }

   @Override
   public int getMaxStackSize(ItemStack stack) {
      return this.slotItemHandler.getMaxStackSize(stack);
   }

   @Override
   public boolean mayPickup(Player playerIn) {
      return this.slotItemHandler.mayPickup(playerIn);
   }

   @Override
   public boolean isSameInventory(Slot other) {
      return this.slotItemHandler.isSameInventory(other);
   }

   public IItemHandler getItemHandler() {
      return this.slotItemHandler.getItemHandler();
   }
}
