package net.neoforged.neoforge.items;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class SlotItemHandler extends Slot {
   private static final Container EMPTY_INVENTORY = new SimpleContainer(0);
   private final IItemHandler itemHandler;
   protected final int index;

   public SlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
      super(EMPTY_INVENTORY, index, xPosition, yPosition);
      this.itemHandler = itemHandler;
      this.index = index;
   }

   @Override
   public boolean mayPlace(ItemStack stack) {
      return stack.isEmpty() ? false : this.itemHandler.isItemValid(this.index, stack);
   }

   @Override
   public ItemStack getItem() {
      return this.getItemHandler().getStackInSlot(this.index);
   }

   @Override
   public void set(ItemStack stack) {
      ((IItemHandlerModifiable)this.getItemHandler()).setStackInSlot(this.index, stack);
      this.setChanged();
   }

   public void initialize(ItemStack stack) {
      ((IItemHandlerModifiable)this.getItemHandler()).setStackInSlot(this.index, stack);
      this.setChanged();
   }

   @Override
   public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {
   }

   @Override
   public int getMaxStackSize() {
      return this.itemHandler.getSlotLimit(this.index);
   }

   @Override
   public int getMaxStackSize(ItemStack stack) {
      return Math.min(stack.getMaxStackSize(), this.itemHandler.getSlotLimit(this.index));
   }

   @Override
   public boolean mayPickup(Player playerIn) {
      return !this.getItemHandler().extractItem(this.index, 1, true).isEmpty();
   }

   @Override
   public ItemStack remove(int amount) {
      return this.getItemHandler().extractItem(this.index, amount, false);
   }

   public IItemHandler getItemHandler() {
      return this.itemHandler;
   }

   @Override
   public boolean isSameInventory(Slot other) {
      return other instanceof SlotItemHandler sih && sih.itemHandler == this.itemHandler;
   }
}
