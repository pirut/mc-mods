package net.minecraft.world.entity.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Inventory implements Container, Nameable {
   public static final int POP_TIME_DURATION = 5;
   public static final int INVENTORY_SIZE = 36;
   public static final int SELECTION_SIZE = 9;
   public static final int SLOT_OFFHAND = 40;
   public static final int SLOT_BODY_ARMOR = 41;
   public static final int SLOT_SADDLE = 42;
   public static final int NOT_FOUND_INDEX = -1;
   public static final Int2ObjectMap<EquipmentSlot> EQUIPMENT_SLOT_MAPPING = new Int2ObjectArrayMap(
      Map.of(
         EquipmentSlot.FEET.getIndex(36),
         EquipmentSlot.FEET,
         EquipmentSlot.LEGS.getIndex(36),
         EquipmentSlot.LEGS,
         EquipmentSlot.CHEST.getIndex(36),
         EquipmentSlot.CHEST,
         EquipmentSlot.HEAD.getIndex(36),
         EquipmentSlot.HEAD,
         40,
         EquipmentSlot.OFFHAND,
         41,
         EquipmentSlot.BODY,
         42,
         EquipmentSlot.SADDLE
      )
   );
   private static final Component DEFAULT_NAME = Component.translatable("container.inventory");
   private final NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
   private int selected;
   public final Player player;
   private final EntityEquipment equipment;
   private int timesChanged;

   public Inventory(Player player, EntityEquipment equipment) {
      this.player = player;
      this.equipment = equipment;
   }

   public int getSelectedSlot() {
      return this.selected;
   }

   public void setSelectedSlot(int slot) {
      if (!isHotbarSlot(slot)) {
         throw new IllegalArgumentException("Invalid selected slot");
      } else {
         this.selected = slot;
      }
   }

   public ItemStack getSelectedItem() {
      return this.items.get(this.selected);
   }

   public ItemStack setSelectedItem(ItemStack stack) {
      return this.items.set(this.selected, stack);
   }

   public static int getSelectionSize() {
      return 9;
   }

   public NonNullList<ItemStack> getNonEquipmentItems() {
      return this.items;
   }

   private boolean hasRemainingSpaceForItem(ItemStack destination, ItemStack origin) {
      return !destination.isEmpty()
         && ItemStack.isSameItemSameComponents(destination, origin)
         && destination.isStackable()
         && destination.getCount() < this.getMaxStackSize(destination);
   }

   public int getFreeSlot() {
      for (int i = 0; i < this.items.size(); i++) {
         if (this.items.get(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public void addAndPickItem(ItemStack stack) {
      this.setSelectedSlot(this.getSuitableHotbarSlot());
      if (!this.items.get(this.selected).isEmpty()) {
         int i = this.getFreeSlot();
         if (i != -1) {
            this.items.set(i, this.items.get(this.selected));
         }
      }

      this.items.set(this.selected, stack);
   }

   public void pickSlot(int index) {
      this.setSelectedSlot(this.getSuitableHotbarSlot());
      ItemStack itemStack = this.items.get(this.selected);
      this.items.set(this.selected, this.items.get(index));
      this.items.set(index, itemStack);
   }

   public static boolean isHotbarSlot(int index) {
      return index >= 0 && index < 9;
   }

   public int findSlotMatchingItem(ItemStack stack) {
      for (int i = 0; i < this.items.size(); i++) {
         if (!this.items.get(i).isEmpty() && ItemStack.isSameItemSameComponents(stack, this.items.get(i))) {
            return i;
         }
      }

      return -1;
   }

   public static boolean isUsableForCrafting(ItemStack stack) {
      return !stack.isDamaged() && !stack.isEnchanted() && !stack.has(DataComponents.CUSTOM_NAME);
   }

   public int findSlotMatchingCraftingIngredient(Holder<Item> item, ItemStack stack) {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemStack = this.items.get(i);
         if (!itemStack.isEmpty()
            && itemStack.is(item)
            && isUsableForCrafting(itemStack)
            && (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, itemStack))) {
            return i;
         }
      }

      return -1;
   }

   public int getSuitableHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         int j = (this.selected + i) % 9;
         if (this.items.get(j).isEmpty()) {
            return j;
         }
      }

      for (int ix = 0; ix < 9; ix++) {
         int j = (this.selected + ix) % 9;
         if (!this.items.get(j).isEnchanted()) {
            return j;
         }
      }

      return this.selected;
   }

   public int clearOrCountMatchingItems(Predicate<ItemStack> stackPredicate, int maxCount, Container inventory) {
      int i = 0;
      boolean bl = maxCount == 0;
      i += ContainerHelper.clearOrCountMatchingItems(this, stackPredicate, maxCount - i, bl);
      i += ContainerHelper.clearOrCountMatchingItems(inventory, stackPredicate, maxCount - i, bl);
      ItemStack itemStack = this.player.containerMenu.getCarried();
      i += ContainerHelper.clearOrCountMatchingItems(itemStack, stackPredicate, maxCount - i, bl);
      if (itemStack.isEmpty()) {
         this.player.containerMenu.setCarried(ItemStack.EMPTY);
      }

      return i;
   }

   private int addResource(ItemStack stack) {
      int i = this.getSlotWithRemainingSpace(stack);
      if (i == -1) {
         i = this.getFreeSlot();
      }

      return i == -1 ? stack.getCount() : this.addResource(i, stack);
   }

   private int addResource(int slot, ItemStack stack) {
      int i = stack.getCount();
      ItemStack itemStack = this.getItem(slot);
      if (itemStack.isEmpty()) {
         itemStack = stack.copyWithCount(0);
         this.setItem(slot, itemStack);
      }

      int j = this.getMaxStackSize(itemStack) - itemStack.getCount();
      int k = Math.min(i, j);
      if (k == 0) {
         return i;
      } else {
         i -= k;
         itemStack.grow(k);
         itemStack.setPopTime(5);
         return i;
      }
   }

   public int getSlotWithRemainingSpace(ItemStack stack) {
      if (this.hasRemainingSpaceForItem(this.getItem(this.selected), stack)) {
         return this.selected;
      } else if (this.hasRemainingSpaceForItem(this.getItem(40), stack)) {
         return 40;
      } else {
         for (int i = 0; i < this.items.size(); i++) {
            if (this.hasRemainingSpaceForItem(this.items.get(i), stack)) {
               return i;
            }
         }

         return -1;
      }
   }

   public void tick() {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemStack = this.getItem(i);
         if (!itemStack.isEmpty()) {
            itemStack.inventoryTick(this.player.level(), this.player, i == this.selected ? EquipmentSlot.MAINHAND : null);
         }
      }
   }

   public boolean add(ItemStack stack) {
      return this.add(-1, stack);
   }

   public boolean add(int slot, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         try {
            if (stack.isDamaged()) {
               if (slot == -1) {
                  slot = this.getFreeSlot();
               }

               if (slot >= 0) {
                  this.items.set(slot, stack.copyAndClear());
                  this.items.get(slot).setPopTime(5);
                  return true;
               } else if (this.player.hasInfiniteMaterials()) {
                  stack.setCount(0);
                  return true;
               } else {
                  return false;
               }
            } else {
               int i;
               do {
                  i = stack.getCount();
                  if (slot == -1) {
                     stack.setCount(this.addResource(stack));
                  } else {
                     stack.setCount(this.addResource(slot, stack));
                  }
               } while (!stack.isEmpty() && stack.getCount() < i);

               if (stack.getCount() == i && this.player.hasInfiniteMaterials()) {
                  stack.setCount(0);
                  return true;
               } else {
                  return stack.getCount() < i;
               }
            }
         } catch (Throwable var6) {
            CrashReport crashReport = CrashReport.forThrowable(var6, "Adding item to inventory");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Item being added");
            crashReportCategory.setDetail("Item ID", Item.getId(stack.getItem()));
            crashReportCategory.setDetail("Item data", stack.getDamageValue());
            crashReportCategory.setDetail("Item name", () -> stack.getHoverName().getString());
            throw new ReportedException(crashReport);
         }
      }
   }

   public void placeItemBackInInventory(ItemStack stack) {
      this.placeItemBackInInventory(stack, true);
   }

   public void placeItemBackInInventory(ItemStack stack, boolean sendPacket) {
      while (!stack.isEmpty()) {
         int i = this.getSlotWithRemainingSpace(stack);
         if (i == -1) {
            i = this.getFreeSlot();
         }

         if (i == -1) {
            this.player.drop(stack, false);
            break;
         }

         int j = stack.getMaxStackSize() - this.getItem(i).getCount();
         if (this.add(i, stack.split(j)) && sendPacket && this.player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(this.createInventoryUpdatePacket(i));
         }
      }
   }

   public ClientboundSetPlayerInventoryPacket createInventoryUpdatePacket(int slot) {
      return new ClientboundSetPlayerInventoryPacket(slot, this.getItem(slot).copy());
   }

   @Override
   public ItemStack removeItem(int slot, int amount) {
      if (slot < this.items.size()) {
         return ContainerHelper.removeItem(this.items, slot, amount);
      } else {
         EquipmentSlot equipmentSlot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(slot);
         if (equipmentSlot != null) {
            ItemStack itemStack = this.equipment.get(equipmentSlot);
            if (!itemStack.isEmpty()) {
               return itemStack.split(amount);
            }
         }

         return ItemStack.EMPTY;
      }
   }

   public void removeItem(ItemStack stack) {
      for (int i = 0; i < this.items.size(); i++) {
         if (this.items.get(i) == stack) {
            this.items.set(i, ItemStack.EMPTY);
            return;
         }
      }

      ObjectIterator var5 = EQUIPMENT_SLOT_MAPPING.values().iterator();

      while (var5.hasNext()) {
         EquipmentSlot equipmentSlot = (EquipmentSlot)var5.next();
         ItemStack itemStack = this.equipment.get(equipmentSlot);
         if (itemStack == stack) {
            this.equipment.set(equipmentSlot, ItemStack.EMPTY);
            return;
         }
      }
   }

   @Override
   public ItemStack removeItemNoUpdate(int slot) {
      if (slot < this.items.size()) {
         ItemStack itemStack = this.items.get(slot);
         this.items.set(slot, ItemStack.EMPTY);
         return itemStack;
      } else {
         EquipmentSlot equipmentSlot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(slot);
         return equipmentSlot != null ? this.equipment.set(equipmentSlot, ItemStack.EMPTY) : ItemStack.EMPTY;
      }
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      if (slot < this.items.size()) {
         this.items.set(slot, stack);
      }

      EquipmentSlot equipmentSlot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(slot);
      if (equipmentSlot != null) {
         this.equipment.set(equipmentSlot, stack);
      }
   }

   public void save(ValueOutput.TypedOutputList<ItemStackWithSlot> output) {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemStack = this.items.get(i);
         if (!itemStack.isEmpty()) {
            output.add(new ItemStackWithSlot(i, itemStack));
         }
      }
   }

   public void load(ValueInput.TypedInputList<ItemStackWithSlot> input) {
      this.items.clear();

      for (ItemStackWithSlot itemStackWithSlot : input) {
         if (itemStackWithSlot.isValidInContainer(this.items.size())) {
            this.setItem(itemStackWithSlot.slot(), itemStackWithSlot.stack());
         }
      }
   }

   @Override
   public int getContainerSize() {
      return this.items.size() + EQUIPMENT_SLOT_MAPPING.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack itemStack : this.items) {
         if (!itemStack.isEmpty()) {
            return false;
         }
      }

      ObjectIterator var3 = EQUIPMENT_SLOT_MAPPING.values().iterator();

      while (var3.hasNext()) {
         EquipmentSlot equipmentSlot = (EquipmentSlot)var3.next();
         if (!this.equipment.get(equipmentSlot).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int slot) {
      if (slot < this.items.size()) {
         return this.items.get(slot);
      } else {
         EquipmentSlot equipmentSlot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(slot);
         return equipmentSlot != null ? this.equipment.get(equipmentSlot) : ItemStack.EMPTY;
      }
   }

   @Override
   public Component getName() {
      return DEFAULT_NAME;
   }

   public void dropAll() {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemStack = this.items.get(i);
         if (!itemStack.isEmpty()) {
            this.player.drop(itemStack, true, false);
            this.items.set(i, ItemStack.EMPTY);
         }
      }

      this.equipment.dropAll(this.player);
   }

   @Override
   public void setChanged() {
      this.timesChanged++;
   }

   public int getTimesChanged() {
      return this.timesChanged;
   }

   @Override
   public boolean stillValid(Player player) {
      return true;
   }

   public boolean contains(ItemStack stack) {
      for (ItemStack itemStack : this) {
         if (!itemStack.isEmpty() && ItemStack.isSameItemSameComponents(itemStack, stack)) {
            return true;
         }
      }

      return false;
   }

   public boolean contains(TagKey<Item> tag) {
      for (ItemStack itemStack : this) {
         if (!itemStack.isEmpty() && itemStack.is(tag)) {
            return true;
         }
      }

      return false;
   }

   public boolean contains(Predicate<ItemStack> predicate) {
      for (ItemStack itemStack : this) {
         if (predicate.test(itemStack)) {
            return true;
         }
      }

      return false;
   }

   public void replaceWith(Inventory playerInventory) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         this.setItem(i, playerInventory.getItem(i));
      }

      this.setSelectedSlot(playerInventory.getSelectedSlot());
   }

   @Override
   public void clearContent() {
      this.items.clear();
      this.equipment.clear();
   }

   public void fillStackedContents(StackedItemContents contents) {
      for (ItemStack itemStack : this.items) {
         contents.accountSimpleStack(itemStack);
      }
   }

   public ItemStack removeFromSelected(boolean removeStack) {
      ItemStack itemStack = this.getSelectedItem();
      return itemStack.isEmpty() ? ItemStack.EMPTY : this.removeItem(this.selected, removeStack ? itemStack.getCount() : 1);
   }
}
