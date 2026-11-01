package net.neoforged.neoforge.transfer.item;

import com.google.common.collect.MapMaker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class VanillaContainerWrapper implements ResourceHandler<ItemResource> {
   private static final Map<Container, VanillaContainerWrapper> wrappers = new MapMaker().weakKeys().weakValues().makeMap();
   private final Container container;
   int size;
   final List<VanillaContainerWrapper.SlotWrapper> slotWrappers = new ArrayList<>();
   private final RootCommitJournal setChangedJournal;

   public static ResourceHandler<ItemResource> of(Container container) {
      return internalOf(container);
   }

   static VanillaContainerWrapper internalOf(Container container) {
      VanillaContainerWrapper wrapper = wrappers.computeIfAbsent(
         container,
         cont -> (VanillaContainerWrapper)(cont instanceof Inventory inventory ? new PlayerInventoryWrapper(inventory) : new VanillaContainerWrapper(cont))
      );
      wrapper.resize();
      return wrapper;
   }

   VanillaContainerWrapper(Container container) {
      this.container = container;
      this.setChangedJournal = new RootCommitJournal(this::onRootCommit);
   }

   void resize() {
      this.size = this.container.getContainerSize();

      while (this.slotWrappers.size() < this.size) {
         this.slotWrappers.add(new VanillaContainerWrapper.SlotWrapper(this.slotWrappers.size()));
      }
   }

   VanillaContainerWrapper.SlotWrapper getSlotWrapper(int index) {
      Objects.checkIndex(index, this.size());
      return this.slotWrappers.get(index);
   }

   void onRootCommit() {
      this.container.setChanged();
   }

   @Override
   public int size() {
      return this.size;
   }

   public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
      return this.getSlotWrapper(index).insert(0, resource, amount, transaction);
   }

   public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
      return this.getSlotWrapper(index).extract(0, resource, amount, transaction);
   }

   public ItemResource getResource(int index) {
      return this.getSlotWrapper(index).getResource(0);
   }

   @Override
   public long getAmountAsLong(int index) {
      return this.getSlotWrapper(index).getAmountAsLong(0);
   }

   public long getCapacityAsLong(int index, ItemResource resource) {
      return this.getSlotWrapper(index).getCapacityAsLong(0, resource);
   }

   public boolean isValid(int index, ItemResource resource) {
      return this.getSlotWrapper(index).isValid(0, resource);
   }

   @Override
   public String toString() {
      return "VanillaContainerWrapper{container=%s}".formatted(this.container);
   }

   class SlotWrapper extends ItemStackResourceHandler {
      private final int index;

      SlotWrapper(int index) {
         this.index = index;
      }

      @Override
      protected ItemStack getStack() {
         return VanillaContainerWrapper.this.container.getItem(this.index);
      }

      @Override
      protected void setStack(ItemStack item) {
         VanillaContainerWrapper.this.container.setItem(this.index, item, true);
      }

      @Override
      protected boolean isValid(ItemResource resource) {
         return VanillaContainerWrapper.this.container.canPlaceItem(this.index, resource.toStack());
      }

      @Override
      protected int getCapacity(ItemResource resource) {
         if (this.index == 1 && resource.is(Items.BUCKET) && VanillaContainerWrapper.this.container instanceof AbstractFurnaceBlockEntity) {
            return 1;
         } else if (this.index < 3 && VanillaContainerWrapper.this.container instanceof BrewingStandBlockEntity) {
            return 1;
         } else {
            return resource.isEmpty()
               ? VanillaContainerWrapper.this.container.getMaxStackSize()
               : VanillaContainerWrapper.this.container.getMaxStackSize(resource.toStack());
         }
      }

      @Override
      public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
         int inserted = super.insert(index, resource, amount, transaction);
         if (inserted > 0) {
            VanillaContainerWrapper.this.container.onTransfer(this.index, inserted, transaction);
         }

         return inserted;
      }

      @Override
      public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
         int extracted = super.extract(index, resource, amount, transaction);
         if (extracted > 0) {
            VanillaContainerWrapper.this.container.onTransfer(this.index, -extracted, transaction);
         }

         return extracted;
      }

      @Override
      public void updateSnapshots(TransactionContext transaction) {
         super.updateSnapshots(transaction);
         VanillaContainerWrapper.this.setChangedJournal.updateSnapshots(transaction);
         if (VanillaContainerWrapper.this.container instanceof ChestBlockEntity chest && chest.getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos otherChestPos = chest.getBlockPos().relative(ChestBlock.getConnectedDirection(chest.getBlockState()));
            if (chest.getLevel().getBlockEntity(otherChestPos) instanceof ChestBlockEntity otherChest) {
               VanillaContainerWrapper.internalOf(otherChest).setChangedJournal.updateSnapshots(transaction);
            }
         }
      }

      protected void onRootCommit(ItemStack original) {
         ItemStack currentStack = this.getStack();
         if (!original.isEmpty() && original.getItem() == currentStack.getItem()) {
            ((PatchedDataComponentMap)original.getComponents()).restorePatch(currentStack.getComponentsPatch());
            original.setCount(currentStack.getCount());
            this.setStack(original);
         } else {
            original.setCount(0);
         }
      }

      @Override
      public String toString() {
         return "vanilla container wrapper[container=" + VanillaContainerWrapper.this.container + ",slot=" + this.index + "]";
      }
   }
}
