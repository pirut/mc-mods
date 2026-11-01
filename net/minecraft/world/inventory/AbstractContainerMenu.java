package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractContainerMenu {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int SLOT_CLICKED_OUTSIDE = -999;
   public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
   public static final int QUICKCRAFT_TYPE_GREEDY = 1;
   public static final int QUICKCRAFT_TYPE_CLONE = 2;
   public static final int QUICKCRAFT_HEADER_START = 0;
   public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
   public static final int QUICKCRAFT_HEADER_END = 2;
   public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
   public static final int SLOTS_PER_ROW = 9;
   public static final int SLOT_SIZE = 18;
   private final NonNullList<ItemStack> lastSlots = NonNullList.create();
   public final NonNullList<Slot> slots = NonNullList.create();
   private final List<DataSlot> dataSlots = Lists.newArrayList();
   private ItemStack carried = ItemStack.EMPTY;
   private final NonNullList<RemoteSlot> remoteSlots = NonNullList.create();
   private final IntList remoteDataSlots = new IntArrayList();
   private RemoteSlot remoteCarried = RemoteSlot.PLACEHOLDER;
   private int stateId;
   @Nullable
   private final MenuType<?> menuType;
   public final int containerId;
   private int quickcraftType = -1;
   private int quickcraftStatus;
   private final Set<Slot> quickcraftSlots = Sets.newHashSet();
   private final List<ContainerListener> containerListeners = Lists.newArrayList();
   @Nullable
   private ContainerSynchronizer synchronizer;
   private boolean suppressRemoteUpdates;

   protected AbstractContainerMenu(@Nullable MenuType<?> menuType, int containerId) {
      this.menuType = menuType;
      this.containerId = containerId;
   }

   protected void addInventoryHotbarSlots(Container container, int x, int y) {
      for (int i = 0; i < 9; i++) {
         this.addSlot(new Slot(container, i, x + i * 18, y));
      }
   }

   protected void addInventoryExtendedSlots(Container container, int x, int y) {
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(container, j + (i + 1) * 9, x + j * 18, y + i * 18));
         }
      }
   }

   protected void addStandardInventorySlots(Container container, int x, int y) {
      this.addInventoryExtendedSlots(container, x, y);
      int i = 4;
      int j = 58;
      this.addInventoryHotbarSlots(container, x, y + 58);
   }

   protected static boolean stillValid(ContainerLevelAccess access, Player player, Block targetBlock) {
      return access.evaluate(
         (level, blockPos) -> !level.getBlockState(blockPos).is(targetBlock) ? false : player.isWithinBlockInteractionRange(blockPos, 4.0), true
      );
   }

   public MenuType<?> getType() {
      if (this.menuType == null) {
         throw new UnsupportedOperationException("Unable to construct this menu by type");
      } else {
         return this.menuType;
      }
   }

   protected static void checkContainerSize(Container container, int minSize) {
      int i = container.getContainerSize();
      if (i < minSize) {
         throw new IllegalArgumentException("Container size " + i + " is smaller than expected " + minSize);
      }
   }

   protected static void checkContainerDataCount(ContainerData intArray, int minSize) {
      int i = intArray.getCount();
      if (i < minSize) {
         throw new IllegalArgumentException("Container data count " + i + " is smaller than expected " + minSize);
      }
   }

   public boolean isValidSlotIndex(int slotIndex) {
      return slotIndex == -1 || slotIndex == -999 || slotIndex < this.slots.size();
   }

   protected Slot addSlot(Slot slot) {
      slot.index = this.slots.size();
      this.slots.add(slot);
      this.lastSlots.add(ItemStack.EMPTY);
      this.remoteSlots.add(this.synchronizer != null ? this.synchronizer.createSlot() : RemoteSlot.PLACEHOLDER);
      return slot;
   }

   protected DataSlot addDataSlot(DataSlot intValue) {
      this.dataSlots.add(intValue);
      this.remoteDataSlots.add(0);
      return intValue;
   }

   protected void addDataSlots(ContainerData array) {
      for (int i = 0; i < array.getCount(); i++) {
         this.addDataSlot(DataSlot.forContainer(array, i));
      }
   }

   public void addSlotListener(ContainerListener listener) {
      if (!this.containerListeners.contains(listener)) {
         this.containerListeners.add(listener);
         this.broadcastChanges();
      }
   }

   public void setSynchronizer(ContainerSynchronizer synchronizer) {
      this.synchronizer = synchronizer;
      this.remoteCarried = synchronizer.createSlot();
      this.remoteSlots.replaceAll(remoteSlot -> synchronizer.createSlot());
      this.sendAllDataToRemote();
   }

   public void sendAllDataToRemote() {
      List<ItemStack> list = new ArrayList<>(this.slots.size());
      int i = 0;

      for (int j = this.slots.size(); i < j; i++) {
         ItemStack itemStack = this.slots.get(i).getItem();
         list.add(itemStack.copy());
         this.remoteSlots.get(i).force(itemStack);
      }

      ItemStack itemStack2 = this.getCarried();
      this.remoteCarried.force(itemStack2);
      int j = 0;

      for (int k = this.dataSlots.size(); j < k; j++) {
         this.remoteDataSlots.set(j, this.dataSlots.get(j).get());
      }

      if (this.synchronizer != null) {
         this.synchronizer.sendInitialData(this, list, itemStack2.copy(), this.remoteDataSlots.toIntArray());
      }
   }

   public void removeSlotListener(ContainerListener listener) {
      this.containerListeners.remove(listener);
   }

   public NonNullList<ItemStack> getItems() {
      NonNullList<ItemStack> nonNullList = NonNullList.create();

      for (Slot slot : this.slots) {
         nonNullList.add(slot.getItem());
      }

      return nonNullList;
   }

   public void broadcastChanges() {
      for (int i = 0; i < this.slots.size(); i++) {
         ItemStack itemStack = this.slots.get(i).getItem();
         Supplier<ItemStack> supplier = Suppliers.memoize(itemStack::copy);
         this.triggerSlotListeners(i, itemStack, supplier);
         this.synchronizeSlotToRemote(i, itemStack, supplier);
      }

      this.synchronizeCarriedToRemote();

      for (int i = 0; i < this.dataSlots.size(); i++) {
         DataSlot dataSlot = this.dataSlots.get(i);
         int j = dataSlot.get();
         if (dataSlot.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(i, j);
         }

         this.synchronizeDataSlotToRemote(i, j);
      }
   }

   public void broadcastFullState() {
      for (int i = 0; i < this.slots.size(); i++) {
         ItemStack itemStack = this.slots.get(i).getItem();
         this.triggerSlotListeners(i, itemStack, itemStack::copy);
      }

      for (int i = 0; i < this.dataSlots.size(); i++) {
         DataSlot dataSlot = this.dataSlots.get(i);
         if (dataSlot.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(i, dataSlot.get());
         }
      }

      this.sendAllDataToRemote();
   }

   private void updateDataSlotListeners(int slotIndex, int value) {
      for (ContainerListener containerListener : this.containerListeners) {
         containerListener.dataChanged(this, slotIndex, value);
      }
   }

   private void triggerSlotListeners(int slotIndex, ItemStack stack, Supplier<ItemStack> supplier) {
      ItemStack itemStack = this.lastSlots.get(slotIndex);
      if (!ItemStack.matches(itemStack, stack)) {
         ItemStack itemStack2 = supplier.get();
         this.lastSlots.set(slotIndex, itemStack2);

         for (ContainerListener containerListener : this.containerListeners) {
            containerListener.slotChanged(this, slotIndex, itemStack2);
         }
      }
   }

   private void synchronizeSlotToRemote(int slotIndex, ItemStack stack, Supplier<ItemStack> supplier) {
      if (!this.suppressRemoteUpdates) {
         RemoteSlot remoteSlot = this.remoteSlots.get(slotIndex);
         if (!remoteSlot.matches(stack)) {
            remoteSlot.force(stack);
            if (this.synchronizer != null) {
               this.synchronizer.sendSlotChange(this, slotIndex, supplier.get());
            }
         }
      }
   }

   private void synchronizeDataSlotToRemote(int slotIndex, int value) {
      if (!this.suppressRemoteUpdates) {
         int i = this.remoteDataSlots.getInt(slotIndex);
         if (i != value) {
            this.remoteDataSlots.set(slotIndex, value);
            if (this.synchronizer != null) {
               this.synchronizer.sendDataChange(this, slotIndex, value);
            }
         }
      }
   }

   private void synchronizeCarriedToRemote() {
      if (!this.suppressRemoteUpdates) {
         ItemStack itemStack = this.getCarried();
         if (!this.remoteCarried.matches(itemStack)) {
            this.remoteCarried.force(itemStack);
            if (this.synchronizer != null) {
               this.synchronizer.sendCarriedChange(this, itemStack.copy());
            }
         }
      }
   }

   public void setRemoteSlot(int slot, ItemStack stack) {
      this.remoteSlots.get(slot).force(stack);
   }

   public void setRemoteSlotUnsafe(int slot, HashedStack stack) {
      if (slot >= 0 && slot < this.remoteSlots.size()) {
         this.remoteSlots.get(slot).receive(stack);
      } else {
         LOGGER.debug("Incorrect slot index: {} available slots: {}", slot, this.remoteSlots.size());
      }
   }

   public void setRemoteCarried(HashedStack stack) {
      this.remoteCarried.receive(stack);
   }

   public boolean clickMenuButton(Player player, int id) {
      return false;
   }

   public Slot getSlot(int slotId) {
      return this.slots.get(slotId);
   }

   public abstract ItemStack quickMoveStack(Player player, int index);

   public void setSelectedBundleItemIndex(int slotIndex, int bundleItemIndex) {
      if (slotIndex >= 0 && slotIndex < this.slots.size()) {
         ItemStack itemStack = this.slots.get(slotIndex).getItem();
         BundleItem.toggleSelectedItem(itemStack, bundleItemIndex);
      }
   }

   public void clicked(int slotId, int button, ClickType clickType, Player player) {
      try {
         this.doClick(slotId, button, clickType, player);
      } catch (Exception var8) {
         CrashReport crashReport = CrashReport.forThrowable(var8, "Container click");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Click info");
         crashReportCategory.setDetail("Menu Type", () -> this.menuType != null ? BuiltInRegistries.MENU.getKey(this.menuType).toString() : "<no type>");
         crashReportCategory.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
         crashReportCategory.setDetail("Slot Count", this.slots.size());
         crashReportCategory.setDetail("Slot", slotId);
         crashReportCategory.setDetail("Button", button);
         crashReportCategory.setDetail("Type", clickType);
         throw new ReportedException(crashReport);
      }
   }

   private void doClick(int slotId, int button, ClickType clickType, Player player) {
      Inventory inventory = player.getInventory();
      if (clickType == ClickType.QUICK_CRAFT) {
         int i = this.quickcraftStatus;
         this.quickcraftStatus = getQuickcraftHeader(button);
         if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
            this.resetQuickCraft();
         } else if (this.getCarried().isEmpty()) {
            this.resetQuickCraft();
         } else if (this.quickcraftStatus == 0) {
            this.quickcraftType = getQuickcraftType(button);
            if (isValidQuickcraftType(this.quickcraftType, player)) {
               this.quickcraftStatus = 1;
               this.quickcraftSlots.clear();
            } else {
               this.resetQuickCraft();
            }
         } else if (this.quickcraftStatus == 1) {
            Slot slot = this.slots.get(slotId);
            ItemStack itemStack = this.getCarried();
            if (canItemQuickReplace(slot, itemStack, true)
               && slot.mayPlace(itemStack)
               && (this.quickcraftType == 2 || itemStack.getCount() > this.quickcraftSlots.size())
               && this.canDragTo(slot)) {
               this.quickcraftSlots.add(slot);
            }
         } else if (this.quickcraftStatus == 2) {
            if (!this.quickcraftSlots.isEmpty()) {
               if (this.quickcraftSlots.size() == 1) {
                  int j = this.quickcraftSlots.iterator().next().index;
                  this.resetQuickCraft();
                  this.doClick(j, this.quickcraftType, ClickType.PICKUP, player);
                  return;
               }

               ItemStack itemStack2 = this.getCarried().copy();
               if (itemStack2.isEmpty()) {
                  this.resetQuickCraft();
                  return;
               }

               int k = this.getCarried().getCount();

               for (Slot slot2 : this.quickcraftSlots) {
                  ItemStack itemStack3 = this.getCarried();
                  if (slot2 != null
                     && canItemQuickReplace(slot2, itemStack3, true)
                     && slot2.mayPlace(itemStack3)
                     && (this.quickcraftType == 2 || itemStack3.getCount() >= this.quickcraftSlots.size())
                     && this.canDragTo(slot2)) {
                     int l = slot2.hasItem() ? slot2.getItem().getCount() : 0;
                     int m = Math.min(itemStack2.getMaxStackSize(), slot2.getMaxStackSize(itemStack2));
                     int n = Math.min(getQuickCraftPlaceCount(this.quickcraftSlots, this.quickcraftType, itemStack2) + l, m);
                     k -= n - l;
                     slot2.setByPlayer(itemStack2.copyWithCount(n));
                  }
               }

               itemStack2.setCount(k);
               this.setCarried(itemStack2);
            }

            this.resetQuickCraft();
         } else {
            this.resetQuickCraft();
         }
      } else if (this.quickcraftStatus != 0) {
         this.resetQuickCraft();
      } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (button == 0 || button == 1)) {
         ClickAction clickAction = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
         if (slotId == -999) {
            if (!this.getCarried().isEmpty()) {
               if (clickAction == ClickAction.PRIMARY) {
                  player.drop(this.getCarried(), true);
                  this.setCarried(ItemStack.EMPTY);
               } else {
                  player.drop(this.getCarried().split(1), true);
               }
            }
         } else if (clickType == ClickType.QUICK_MOVE) {
            if (slotId < 0) {
               return;
            }

            Slot slot = this.slots.get(slotId);
            if (!slot.mayPickup(player)) {
               return;
            }

            ItemStack itemStack = this.quickMoveStack(player, slotId);

            while (!itemStack.isEmpty() && ItemStack.isSameItem(slot.getItem(), itemStack)) {
               itemStack = this.quickMoveStack(player, slotId);
            }
         } else {
            if (slotId < 0) {
               return;
            }

            Slot slot = this.slots.get(slotId);
            ItemStack itemStack = slot.getItem();
            ItemStack itemStack4 = this.getCarried();
            player.updateTutorialInventoryAction(itemStack4, slot.getItem(), clickAction);
            if (!this.tryItemClickBehaviourOverride(player, clickAction, slot, itemStack, itemStack4)) {
               if (itemStack.isEmpty()) {
                  if (!itemStack4.isEmpty()) {
                     int o = clickAction == ClickAction.PRIMARY ? itemStack4.getCount() : 1;
                     this.setCarried(slot.safeInsert(itemStack4, o));
                  }
               } else if (slot.mayPickup(player)) {
                  if (itemStack4.isEmpty()) {
                     int o = clickAction == ClickAction.PRIMARY ? itemStack.getCount() : (itemStack.getCount() + 1) / 2;
                     Optional<ItemStack> optional = slot.tryRemove(o, Integer.MAX_VALUE, player);
                     optional.ifPresent(itemStackx -> {
                        this.setCarried(itemStackx);
                        slot.onTake(player, itemStackx);
                     });
                  } else if (slot.mayPlace(itemStack4)) {
                     if (ItemStack.isSameItemSameComponents(itemStack, itemStack4)) {
                        int o = clickAction == ClickAction.PRIMARY ? itemStack4.getCount() : 1;
                        this.setCarried(slot.safeInsert(itemStack4, o));
                     } else if (itemStack4.getCount() <= slot.getMaxStackSize(itemStack4)) {
                        this.setCarried(itemStack);
                        slot.setByPlayer(itemStack4);
                     }
                  } else if (ItemStack.isSameItemSameComponents(itemStack, itemStack4)) {
                     Optional<ItemStack> optional2 = slot.tryRemove(itemStack.getCount(), itemStack4.getMaxStackSize() - itemStack4.getCount(), player);
                     optional2.ifPresent(itemStack2x -> {
                        itemStack4.grow(itemStack2x.getCount());
                        slot.onTake(player, itemStack2x);
                     });
                  }
               }
            }

            slot.setChanged();
         }
      } else if (clickType == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)) {
         ItemStack itemStack5 = inventory.getItem(button);
         Slot slot = this.slots.get(slotId);
         ItemStack itemStack = slot.getItem();
         if (!itemStack5.isEmpty() || !itemStack.isEmpty()) {
            if (itemStack5.isEmpty()) {
               if (slot.mayPickup(player)) {
                  inventory.setItem(button, itemStack);
                  slot.onSwapCraft(itemStack.getCount());
                  slot.setByPlayer(ItemStack.EMPTY);
                  slot.onTake(player, itemStack);
               }
            } else if (itemStack.isEmpty()) {
               if (slot.mayPlace(itemStack5)) {
                  int p = slot.getMaxStackSize(itemStack5);
                  if (itemStack5.getCount() > p) {
                     slot.setByPlayer(itemStack5.split(p));
                  } else {
                     inventory.setItem(button, ItemStack.EMPTY);
                     slot.setByPlayer(itemStack5);
                  }
               }
            } else if (slot.mayPickup(player) && slot.mayPlace(itemStack5)) {
               int p = slot.getMaxStackSize(itemStack5);
               if (itemStack5.getCount() > p) {
                  slot.setByPlayer(itemStack5.split(p));
                  slot.onTake(player, itemStack);
                  if (!inventory.add(itemStack)) {
                     player.drop(itemStack, true);
                  }
               } else {
                  inventory.setItem(button, itemStack);
                  slot.setByPlayer(itemStack5);
                  slot.onTake(player, itemStack);
               }
            }
         }
      } else if (clickType == ClickType.CLONE && player.hasInfiniteMaterials() && this.getCarried().isEmpty() && slotId >= 0) {
         Slot slot3 = this.slots.get(slotId);
         if (slot3.hasItem()) {
            ItemStack itemStack2 = slot3.getItem();
            this.setCarried(itemStack2.copyWithCount(itemStack2.getMaxStackSize()));
         }
      } else if (clickType == ClickType.THROW && this.getCarried().isEmpty() && slotId >= 0) {
         Slot slot3 = this.slots.get(slotId);
         int j = button == 0 ? 1 : slot3.getItem().getCount();
         if (!player.canDropItems()) {
            return;
         }

         ItemStack itemStack = slot3.safeTake(j, Integer.MAX_VALUE, player);
         player.drop(itemStack, true);
         player.handleCreativeModeItemDrop(itemStack);
         if (button == 1) {
            while (!itemStack.isEmpty() && ItemStack.isSameItem(slot3.getItem(), itemStack)) {
               if (!player.canDropItems()) {
                  return;
               }

               itemStack = slot3.safeTake(j, Integer.MAX_VALUE, player);
               player.drop(itemStack, true);
               player.handleCreativeModeItemDrop(itemStack);
            }
         }
      } else if (clickType == ClickType.PICKUP_ALL && slotId >= 0) {
         Slot slot3x = this.slots.get(slotId);
         ItemStack itemStack2 = this.getCarried();
         if (!itemStack2.isEmpty() && (!slot3x.hasItem() || !slot3x.mayPickup(player))) {
            int k = button == 0 ? 0 : this.slots.size() - 1;
            int p = button == 0 ? 1 : -1;

            for (int o = 0; o < 2; o++) {
               for (int q = k; q >= 0 && q < this.slots.size() && itemStack2.getCount() < itemStack2.getMaxStackSize(); q += p) {
                  Slot slot4 = this.slots.get(q);
                  if (slot4.hasItem()
                     && canItemQuickReplace(slot4, itemStack2, true)
                     && slot4.mayPickup(player)
                     && this.canTakeItemForPickAll(itemStack2, slot4)) {
                     ItemStack itemStack6 = slot4.getItem();
                     if (o != 0 || itemStack6.getCount() != itemStack6.getMaxStackSize()) {
                        ItemStack itemStack7 = slot4.safeTake(itemStack6.getCount(), itemStack2.getMaxStackSize() - itemStack2.getCount(), player);
                        itemStack2.grow(itemStack7.getCount());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryItemClickBehaviourOverride(Player player, ClickAction action, Slot slot, ItemStack clickedItem, ItemStack carriedItem) {
      FeatureFlagSet featureFlagSet = player.level().enabledFeatures();
      return carriedItem.isItemEnabled(featureFlagSet) && carriedItem.overrideStackedOnOther(slot, action, player)
         ? true
         : clickedItem.isItemEnabled(featureFlagSet) && clickedItem.overrideOtherStackedOnMe(carriedItem, slot, action, player, this.createCarriedSlotAccess());
   }

   private SlotAccess createCarriedSlotAccess() {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return AbstractContainerMenu.this.getCarried();
         }

         @Override
         public boolean set(ItemStack item) {
            AbstractContainerMenu.this.setCarried(item);
            return true;
         }
      };
   }

   public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
      return true;
   }

   public void removed(Player player) {
      if (player instanceof ServerPlayer) {
         ItemStack itemStack = this.getCarried();
         if (!itemStack.isEmpty()) {
            dropOrPlaceInInventory(player, itemStack);
            this.setCarried(ItemStack.EMPTY);
         }
      }
   }

   private static void dropOrPlaceInInventory(Player player, ItemStack stack) {
      boolean bl = player.isRemoved() && player.getRemovalReason() != Entity.RemovalReason.CHANGED_DIMENSION;
      boolean bl2 = player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected();
      if (bl || bl2) {
         player.drop(stack, false);
      } else if (player instanceof ServerPlayer) {
         player.getInventory().placeItemBackInInventory(stack);
      }
   }

   protected void clearContainer(Player player, Container container) {
      for (int i = 0; i < container.getContainerSize(); i++) {
         dropOrPlaceInInventory(player, container.removeItemNoUpdate(i));
      }
   }

   public void slotsChanged(Container container) {
      this.broadcastChanges();
   }

   public void setItem(int slotId, int stateId, ItemStack stack) {
      this.getSlot(slotId).set(stack);
      this.stateId = stateId;
   }

   public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
      for (int i = 0; i < items.size(); i++) {
         this.getSlot(i).set(items.get(i));
      }

      this.carried = carried;
      this.stateId = stateId;
   }

   public void setData(int id, int data) {
      this.dataSlots.get(id).set(data);
   }

   public abstract boolean stillValid(Player player);

   protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
      boolean bl = false;
      int i = startIndex;
      if (reverseDirection) {
         i = endIndex - 1;
      }

      if (stack.isStackable()) {
         while (!stack.isEmpty() && (reverseDirection ? i >= startIndex : i < endIndex)) {
            Slot slot = this.slots.get(i);
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, itemStack)) {
               int j = itemStack.getCount() + stack.getCount();
               int k = slot.getMaxStackSize(itemStack);
               if (j <= k) {
                  stack.setCount(0);
                  itemStack.setCount(j);
                  slot.setChanged();
                  bl = true;
               } else if (itemStack.getCount() < k) {
                  stack.shrink(k - itemStack.getCount());
                  itemStack.setCount(k);
                  slot.setChanged();
                  bl = true;
               }
            }

            if (reverseDirection) {
               i--;
            } else {
               i++;
            }
         }
      }

      if (!stack.isEmpty()) {
         if (reverseDirection) {
            i = endIndex - 1;
         } else {
            i = startIndex;
         }

         while (reverseDirection ? i >= startIndex : i < endIndex) {
            Slot slotx = this.slots.get(i);
            ItemStack itemStackx = slotx.getItem();
            if (itemStackx.isEmpty() && slotx.mayPlace(stack)) {
               int j = slotx.getMaxStackSize(stack);
               slotx.setByPlayer(stack.split(Math.min(stack.getCount(), j)));
               slotx.setChanged();
               bl = true;
               break;
            }

            if (reverseDirection) {
               i--;
            } else {
               i++;
            }
         }
      }

      return bl;
   }

   public static int getQuickcraftType(int eventButton) {
      return eventButton >> 2 & 3;
   }

   public static int getQuickcraftHeader(int clickedButton) {
      return clickedButton & 3;
   }

   public static int getQuickcraftMask(int quickCraftingHeader, int quickCraftingType) {
      return quickCraftingHeader & 3 | (quickCraftingType & 3) << 2;
   }

   public static boolean isValidQuickcraftType(int dragMode, Player player) {
      if (dragMode == 0) {
         return true;
      } else {
         return dragMode == 1 ? true : dragMode == 2 && player.hasInfiniteMaterials();
      }
   }

   protected void resetQuickCraft() {
      this.quickcraftStatus = 0;
      this.quickcraftSlots.clear();
   }

   public static boolean canItemQuickReplace(@Nullable Slot slot, ItemStack stack, boolean stackSizeMatters) {
      boolean bl = slot == null || !slot.hasItem();
      return !bl && ItemStack.isSameItemSameComponents(stack, slot.getItem())
         ? slot.getItem().getCount() + (stackSizeMatters ? 0 : stack.getCount()) <= stack.getMaxStackSize()
         : bl;
   }

   public static int getQuickCraftPlaceCount(Set<Slot> slots, int type, ItemStack stack) {
      return switch (type) {
         case 0 -> Mth.floor((float)stack.getCount() / slots.size());
         case 1 -> 1;
         case 2 -> stack.getMaxStackSize();
         default -> stack.getCount();
      };
   }

   public boolean canDragTo(Slot slot) {
      return true;
   }

   public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity blockEntity) {
      return blockEntity instanceof Container ? getRedstoneSignalFromContainer((Container)blockEntity) : 0;
   }

   public static int getRedstoneSignalFromContainer(@Nullable Container container) {
      if (container == null) {
         return 0;
      } else {
         float f = 0.0F;

         for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            if (!itemStack.isEmpty()) {
               f += (float)itemStack.getCount() / container.getMaxStackSize(itemStack);
            }
         }

         f /= container.getContainerSize();
         return Mth.lerpDiscrete(f, 0, 15);
      }
   }

   public void setCarried(ItemStack stack) {
      this.carried = stack;
   }

   public ItemStack getCarried() {
      return this.carried;
   }

   public void suppressRemoteUpdates() {
      this.suppressRemoteUpdates = true;
   }

   public void resumeRemoteUpdates() {
      this.suppressRemoteUpdates = false;
   }

   public void transferState(AbstractContainerMenu menu) {
      Table<Container, Integer, Integer> table = HashBasedTable.create();

      for (int i = 0; i < menu.slots.size(); i++) {
         Slot slot = menu.slots.get(i);
         table.put(slot.container, slot.getContainerSlot(), i);
      }

      for (int i = 0; i < this.slots.size(); i++) {
         Slot slot = this.slots.get(i);
         Integer integer = (Integer)table.get(slot.container, slot.getContainerSlot());
         if (integer != null) {
            this.lastSlots.set(i, menu.lastSlots.get(integer));
            RemoteSlot remoteSlot = menu.remoteSlots.get(integer);
            RemoteSlot remoteSlot2 = this.remoteSlots.get(i);
            if (remoteSlot instanceof RemoteSlot.Synchronized synchronized_ && remoteSlot2 instanceof RemoteSlot.Synchronized synchronized2) {
               synchronized2.copyFrom(synchronized_);
            }
         }
      }
   }

   public OptionalInt findSlot(Container container, int slotIndex) {
      for (int i = 0; i < this.slots.size(); i++) {
         Slot slot = this.slots.get(i);
         if (slot.container == container && slotIndex == slot.getContainerSlot()) {
            return OptionalInt.of(i);
         }
      }

      return OptionalInt.empty();
   }

   public int getStateId() {
      return this.stateId;
   }

   public int incrementStateId() {
      this.stateId = this.stateId + 1 & 32767;
      return this.stateId;
   }
}
