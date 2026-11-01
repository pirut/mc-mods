package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaInventoryCodeHooks;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class HopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {
   public static final int MOVE_ITEM_SPEED = 8;
   public static final int HOPPER_CONTAINER_SIZE = 5;
   private static final int[][] CACHED_SLOTS = new int[54][];
   private static final int NO_COOLDOWN_TIME = -1;
   private static final Component DEFAULT_NAME = Component.translatable("container.hopper");
   private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
   private int cooldownTime = -1;
   private long tickedGameTime;
   private Direction facing;
   private final SnapshotJournal<Integer> cooldownTimeJournal = new SnapshotJournal<Integer>() {
      protected Integer createSnapshot() {
         return HopperBlockEntity.this.cooldownTime;
      }

      protected void revertToSnapshot(Integer snapshot) {
         HopperBlockEntity.this.cooldownTime = snapshot;
      }
   };

   public HopperBlockEntity(BlockPos p_155550_, BlockState p_155551_) {
      super(BlockEntityType.HOPPER, p_155550_, p_155551_);
      this.facing = p_155551_.getValue(HopperBlock.FACING);
   }

   @Override
   protected void loadAdditional(ValueInput p_421495_) {
      super.loadAdditional(p_421495_);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(p_421495_)) {
         ContainerHelper.loadAllItems(p_421495_, this.items);
      }

      this.cooldownTime = p_421495_.getIntOr("TransferCooldown", -1);
   }

   @Override
   protected void saveAdditional(ValueOutput p_422620_) {
      super.saveAdditional(p_422620_);
      if (!this.trySaveLootTable(p_422620_)) {
         ContainerHelper.saveAllItems(p_422620_, this.items);
      }

      p_422620_.putInt("TransferCooldown", this.cooldownTime);
   }

   @Override
   public int getContainerSize() {
      return this.items.size();
   }

   @Override
   public ItemStack removeItem(int p_59309_, int p_59310_) {
      this.unpackLootTable(null);
      return ContainerHelper.removeItem(this.getItems(), p_59309_, p_59310_);
   }

   @Override
   public void setItem(int p_59315_, ItemStack p_59316_) {
      this.unpackLootTable(null);
      this.getItems().set(p_59315_, p_59316_);
      p_59316_.limitSize(this.getMaxStackSize(p_59316_));
   }

   private boolean wasEmpty(int slot, int amountChange) {
      if (this.items.get(slot).getCount() != amountChange) {
         return false;
      } else {
         for (int i = 0; i < this.items.size(); i++) {
            if (i != slot && !this.items.get(i).isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public void onTransfer(int slot, int amountChange, TransactionContext transaction) {
      if (amountChange > 0 && !this.isOnCustomCooldown() && this.wasEmpty(slot, amountChange)) {
         this.cooldownTimeJournal.updateSnapshots(transaction);
         this.setCooldown(8);
      }
   }

   @Override
   public void setBlockState(BlockState p_326468_) {
      super.setBlockState(p_326468_);
      this.facing = p_326468_.getValue(HopperBlock.FACING);
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   public static void pushItemsTick(Level p_155574_, BlockPos p_155575_, BlockState p_155576_, HopperBlockEntity p_155577_) {
      p_155577_.cooldownTime--;
      p_155577_.tickedGameTime = p_155574_.getGameTime();
      if (!p_155577_.isOnCooldown()) {
         p_155577_.setCooldown(0);
         tryMoveItems(p_155574_, p_155575_, p_155576_, p_155577_, () -> suckInItems(p_155574_, p_155577_));
      }
   }

   private static boolean tryMoveItems(Level p_155579_, BlockPos p_155580_, BlockState p_155581_, HopperBlockEntity p_155582_, BooleanSupplier p_155583_) {
      if (p_155579_.isClientSide()) {
         return false;
      } else {
         if (!p_155582_.isOnCooldown() && p_155581_.getValue(HopperBlock.ENABLED)) {
            boolean flag = false;
            if (!p_155582_.isEmpty()) {
               flag = ejectItems(p_155579_, p_155580_, p_155582_);
            }

            if (!p_155582_.inventoryFull()) {
               flag |= p_155583_.getAsBoolean();
            }

            if (flag) {
               p_155582_.setCooldown(8);
               setChanged(p_155579_, p_155580_, p_155581_);
               return true;
            }
         }

         return false;
      }
   }

   private boolean inventoryFull() {
      for (ItemStack itemstack : this.items) {
         if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
            return false;
         }
      }

      return true;
   }

   private static boolean ejectItems(Level p_155563_, BlockPos p_155564_, HopperBlockEntity p_326256_) {
      ContainerOrHandler containerOrHandler = getContainerOrHandlerAt(p_155563_, p_155564_.relative(p_326256_.facing), p_326256_.facing.getOpposite());
      if (containerOrHandler.isEmpty()) {
         return false;
      } else if (containerOrHandler.container() != null) {
         Container container = containerOrHandler.container();
         Direction direction = p_326256_.facing.getOpposite();
         if (isFullContainer(container, direction)) {
            return false;
         } else {
            for (int i = 0; i < p_326256_.getContainerSize(); i++) {
               ItemStack itemstack = p_326256_.getItem(i);
               if (!itemstack.isEmpty()) {
                  int j = itemstack.getCount();
                  ItemStack itemstack1 = addItem(p_326256_, container, p_326256_.removeItem(i, 1), direction);
                  if (itemstack1.isEmpty()) {
                     container.setChanged();
                     return true;
                  }

                  itemstack.setCount(j);
                  if (j == 1) {
                     p_326256_.setItem(i, itemstack);
                  }
               }
            }

            return false;
         }
      } else {
         return VanillaInventoryCodeHooks.insertHook(p_326256_, containerOrHandler.itemHandler());
      }
   }

   private static int[] getSlots(Container p_59340_, Direction p_59341_) {
      if (p_59340_ instanceof WorldlyContainer worldlycontainer) {
         return worldlycontainer.getSlotsForFace(p_59341_);
      } else {
         int i = p_59340_.getContainerSize();
         if (i < CACHED_SLOTS.length) {
            int[] aint = CACHED_SLOTS[i];
            if (aint != null) {
               return aint;
            } else {
               int[] aint1 = createFlatSlots(i);
               CACHED_SLOTS[i] = aint1;
               return aint1;
            }
         } else {
            return createFlatSlots(i);
         }
      }
   }

   private static int[] createFlatSlots(int p_326328_) {
      int[] aint = new int[p_326328_];
      int i = 0;

      while (i < aint.length) {
         aint[i] = i++;
      }

      return aint;
   }

   private static boolean isFullContainer(Container p_59386_, Direction p_59387_) {
      int[] aint = getSlots(p_59386_, p_59387_);

      for (int i : aint) {
         ItemStack itemstack = p_59386_.getItem(i);
         if (itemstack.getCount() < itemstack.getMaxStackSize()) {
            return false;
         }
      }

      return true;
   }

   public static boolean suckInItems(Level p_155553_, Hopper p_155554_) {
      BlockPos blockpos = BlockPos.containing(p_155554_.getLevelX(), p_155554_.getLevelY() + 1.0, p_155554_.getLevelZ());
      BlockState blockstate = p_155553_.getBlockState(blockpos);
      ContainerOrHandler containerOrHandler = getSourceContainerOrHandler(p_155553_, p_155554_, blockpos, blockstate);
      if (containerOrHandler.container() != null) {
         Container container = containerOrHandler.container();
         Direction direction = Direction.DOWN;

         for (int i : getSlots(container, direction)) {
            if (tryTakeInItemFromSlot(p_155554_, container, i, direction)) {
               return true;
            }
         }

         return false;
      } else if (containerOrHandler.itemHandler() != null) {
         return VanillaInventoryCodeHooks.extractHook(p_155554_, containerOrHandler.itemHandler());
      } else {
         boolean flag = p_155554_.isGridAligned()
            && blockstate.isCollisionShapeFullBlock(p_155553_, blockpos)
            && !blockstate.is(BlockTags.DOES_NOT_BLOCK_HOPPERS);
         if (!flag) {
            for (ItemEntity itementity : getItemsAtAndAbove(p_155553_, p_155554_)) {
               if (addItem(p_155554_, itementity)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static boolean tryTakeInItemFromSlot(Hopper p_59355_, Container p_59356_, int p_59357_, Direction p_59358_) {
      ItemStack itemstack = p_59356_.getItem(p_59357_);
      if (!itemstack.isEmpty() && canTakeItemFromContainer(p_59355_, p_59356_, itemstack, p_59357_, p_59358_)) {
         int i = itemstack.getCount();
         ItemStack itemstack1 = addItem(p_59356_, p_59355_, p_59356_.removeItem(p_59357_, 1), null);
         if (itemstack1.isEmpty()) {
            p_59356_.setChanged();
            return true;
         }

         itemstack.setCount(i);
         if (i == 1) {
            p_59356_.setItem(p_59357_, itemstack);
         }
      }

      return false;
   }

   public static boolean addItem(Container p_59332_, ItemEntity p_59333_) {
      boolean flag = false;
      ItemStack itemstack = p_59333_.getItem().copy();
      ItemStack itemstack1 = addItem(null, p_59332_, itemstack, null);
      if (itemstack1.isEmpty()) {
         flag = true;
         p_59333_.setItem(ItemStack.EMPTY);
         p_59333_.discard();
      } else {
         p_59333_.setItem(itemstack1);
      }

      return flag;
   }

   public static ItemStack addItem(@Nullable Container p_59327_, Container p_59328_, ItemStack p_59329_, @Nullable Direction p_59330_) {
      if (p_59328_ instanceof WorldlyContainer worldlycontainer && p_59330_ != null) {
         int[] aint = worldlycontainer.getSlotsForFace(p_59330_);

         for (int k = 0; k < aint.length && !p_59329_.isEmpty(); k++) {
            p_59329_ = tryMoveInItem(p_59327_, p_59328_, p_59329_, aint[k], p_59330_);
         }
      } else {
         int i = p_59328_.getContainerSize();

         for (int j = 0; j < i && !p_59329_.isEmpty(); j++) {
            p_59329_ = tryMoveInItem(p_59327_, p_59328_, p_59329_, j, p_59330_);
         }
      }

      return p_59329_;
   }

   private static boolean canPlaceItemInContainer(Container p_59335_, ItemStack p_59336_, int p_59337_, @Nullable Direction p_59338_) {
      return !p_59335_.canPlaceItem(p_59337_, p_59336_)
         ? false
         : !(p_59335_ instanceof WorldlyContainer worldlycontainer && !worldlycontainer.canPlaceItemThroughFace(p_59337_, p_59336_, p_59338_));
   }

   private static boolean canTakeItemFromContainer(Container p_273433_, Container p_273542_, ItemStack p_273400_, int p_273519_, Direction p_273088_) {
      return !p_273542_.canTakeItem(p_273433_, p_273519_, p_273400_)
         ? false
         : !(p_273542_ instanceof WorldlyContainer worldlycontainer && !worldlycontainer.canTakeItemThroughFace(p_273519_, p_273400_, p_273088_));
   }

   private static ItemStack tryMoveInItem(@Nullable Container p_59321_, Container p_59322_, ItemStack p_59323_, int p_59324_, @Nullable Direction p_59325_) {
      ItemStack itemstack = p_59322_.getItem(p_59324_);
      if (canPlaceItemInContainer(p_59322_, p_59323_, p_59324_, p_59325_)) {
         boolean flag = false;
         boolean flag1 = p_59322_.isEmpty();
         if (itemstack.isEmpty()) {
            p_59322_.setItem(p_59324_, p_59323_);
            p_59323_ = ItemStack.EMPTY;
            flag = true;
         } else if (canMergeItems(itemstack, p_59323_)) {
            int i = p_59323_.getMaxStackSize() - itemstack.getCount();
            int j = Math.min(p_59323_.getCount(), i);
            p_59323_.shrink(j);
            itemstack.grow(j);
            flag = j > 0;
         }

         if (flag) {
            if (flag1 && p_59322_ instanceof HopperBlockEntity hopperblockentity1 && !hopperblockentity1.isOnCustomCooldown()) {
               int k = 0;
               if (p_59321_ instanceof HopperBlockEntity hopperblockentity && hopperblockentity1.tickedGameTime >= hopperblockentity.tickedGameTime) {
                  k = 1;
               }

               hopperblockentity1.setCooldown(8 - k);
            }

            p_59322_.setChanged();
         }
      }

      return p_59323_;
   }

   @Nullable
   private static Container getAttachedContainer(Level p_155593_, BlockPos p_155594_, HopperBlockEntity p_326320_) {
      return getContainerAt(p_155593_, p_155594_.relative(p_326320_.facing));
   }

   @Nullable
   private static Container getSourceContainer(Level p_155597_, Hopper p_155598_, BlockPos p_326315_, BlockState p_326093_) {
      return getContainerAt(p_155597_, p_326315_, p_326093_, p_155598_.getLevelX(), p_155598_.getLevelY() + 1.0, p_155598_.getLevelZ());
   }

   public static List<ItemEntity> getItemsAtAndAbove(Level p_155590_, Hopper p_155591_) {
      AABB aabb = p_155591_.getSuckAabb().move(p_155591_.getLevelX() - 0.5, p_155591_.getLevelY() - 0.5, p_155591_.getLevelZ() - 0.5);
      return p_155590_.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
   }

   @Deprecated
   @Nullable
   public static Container getContainerAt(Level p_59391_, BlockPos p_59392_) {
      return getContainerAt(p_59391_, p_59392_, p_59391_.getBlockState(p_59392_), p_59392_.getX() + 0.5, p_59392_.getY() + 0.5, p_59392_.getZ() + 0.5);
   }

   @Nullable
   private static Container getContainerAt(Level p_59348_, BlockPos p_326114_, BlockState p_326445_, double p_59349_, double p_59350_, double p_59351_) {
      Container container = getBlockContainer(p_59348_, p_326114_, p_326445_);
      if (container == null) {
         container = getEntityContainer(p_59348_, p_59349_, p_59350_, p_59351_);
      }

      return container;
   }

   @Nullable
   private static Container getBlockContainer(Level p_326127_, BlockPos p_326017_, BlockState p_326108_) {
      Block block = p_326108_.getBlock();
      if (block instanceof WorldlyContainerHolder) {
         return ((WorldlyContainerHolder)block).getContainer(p_326108_, p_326127_, p_326017_);
      } else if (p_326108_.hasBlockEntity() && p_326127_.getBlockEntity(p_326017_) instanceof Container container) {
         if (container instanceof ChestBlockEntity && block instanceof ChestBlock) {
            container = ChestBlock.getContainer((ChestBlock)block, p_326108_, p_326127_, p_326017_, true);
         }

         return container;
      } else {
         return null;
      }
   }

   @Nullable
   private static Container getEntityContainer(Level p_326325_, double p_326012_, double p_326191_, double p_326289_) {
      List<Entity> list = p_326325_.getEntities(
         (Entity)null,
         new AABB(p_326012_ - 0.5, p_326191_ - 0.5, p_326289_ - 0.5, p_326012_ + 0.5, p_326191_ + 0.5, p_326289_ + 0.5),
         EntitySelector.CONTAINER_ENTITY_SELECTOR
      );
      return !list.isEmpty() ? (Container)list.get(p_326325_.random.nextInt(list.size())) : null;
   }

   private static ContainerOrHandler getSourceContainerOrHandler(Level p_155597_, Hopper p_155598_, BlockPos p_326315_, BlockState p_326093_) {
      return getContainerOrHandlerAt(p_155597_, p_326315_, p_326093_, p_155598_.getLevelX(), p_155598_.getLevelY() + 1.0, p_155598_.getLevelZ(), Direction.DOWN);
   }

   public static ContainerOrHandler getContainerOrHandlerAt(Level level, BlockPos pos, @Nullable Direction side) {
      return getContainerOrHandlerAt(level, pos, level.getBlockState(pos), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, side);
   }

   private static ContainerOrHandler getContainerOrHandlerAt(
      Level level, BlockPos pos, BlockState state, double x, double y, double z, @Nullable Direction side
   ) {
      Container container = getBlockContainer(level, pos, state);
      if (container != null) {
         return new ContainerOrHandler(container, null);
      } else {
         ResourceHandler<ItemResource> blockItemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, state, null, side);
         return blockItemHandler != null
            ? new ContainerOrHandler(null, blockItemHandler)
            : VanillaInventoryCodeHooks.getEntityContainerOrHandler(level, x, y, z, side);
      }
   }

   private static boolean canMergeItems(ItemStack p_59345_, ItemStack p_59346_) {
      return p_59345_.getCount() <= p_59345_.getMaxStackSize() && ItemStack.isSameItemSameComponents(p_59345_, p_59346_);
   }

   @Override
   public double getLevelX() {
      return this.worldPosition.getX() + 0.5;
   }

   @Override
   public double getLevelY() {
      return this.worldPosition.getY() + 0.5;
   }

   @Override
   public double getLevelZ() {
      return this.worldPosition.getZ() + 0.5;
   }

   @Override
   public boolean isGridAligned() {
      return true;
   }

   public void setCooldown(int p_59396_) {
      this.cooldownTime = p_59396_;
   }

   private boolean isOnCooldown() {
      return this.cooldownTime > 0;
   }

   public boolean isOnCustomCooldown() {
      return this.cooldownTime > 8;
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> p_59371_) {
      this.items = p_59371_;
   }

   public static void entityInside(Level p_155568_, BlockPos p_155569_, BlockState p_155570_, Entity p_155571_, HopperBlockEntity p_155572_) {
      if (p_155571_ instanceof ItemEntity itementity
         && !itementity.getItem().isEmpty()
         && p_155571_.getBoundingBox().move(-p_155569_.getX(), -p_155569_.getY(), -p_155569_.getZ()).intersects(p_155572_.getSuckAabb())) {
         tryMoveItems(p_155568_, p_155569_, p_155570_, p_155572_, () -> addItem(p_155572_, itementity));
      }
   }

   @Override
   protected AbstractContainerMenu createMenu(int p_59312_, Inventory p_59313_) {
      return new HopperMenu(p_59312_, p_59313_, this);
   }

   public long getLastUpdateTime() {
      return this.tickedGameTime;
   }
}
