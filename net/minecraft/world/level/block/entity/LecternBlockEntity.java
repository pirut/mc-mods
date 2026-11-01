package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class LecternBlockEntity extends BlockEntity implements Clearable, MenuProvider {
   public static final int DATA_PAGE = 0;
   public static final int NUM_DATA = 1;
   public static final int SLOT_BOOK = 0;
   public static final int NUM_SLOTS = 1;
   private final Container bookAccess = new Container() {
      @Override
      public int getContainerSize() {
         return 1;
      }

      @Override
      public boolean isEmpty() {
         return LecternBlockEntity.this.book.isEmpty();
      }

      @Override
      public ItemStack getItem(int p_59580_) {
         return p_59580_ == 0 ? LecternBlockEntity.this.book : ItemStack.EMPTY;
      }

      @Override
      public ItemStack removeItem(int p_59582_, int p_59583_) {
         if (p_59582_ == 0) {
            ItemStack $$2 = LecternBlockEntity.this.book.split(p_59583_);
            if (LecternBlockEntity.this.book.isEmpty()) {
               LecternBlockEntity.this.onBookItemRemove();
            }

            return $$2;
         } else {
            return ItemStack.EMPTY;
         }
      }

      @Override
      public ItemStack removeItemNoUpdate(int p_59590_) {
         if (p_59590_ == 0) {
            ItemStack $$1 = LecternBlockEntity.this.book;
            LecternBlockEntity.this.book = ItemStack.EMPTY;
            LecternBlockEntity.this.onBookItemRemove();
            return $$1;
         } else {
            return ItemStack.EMPTY;
         }
      }

      @Override
      public void setItem(int p_59585_, ItemStack p_59586_) {
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public void setChanged() {
         LecternBlockEntity.this.setChanged();
      }

      @Override
      public boolean stillValid(Player p_59588_) {
         return Container.stillValidBlockEntity(LecternBlockEntity.this, p_59588_) && LecternBlockEntity.this.hasBook();
      }

      @Override
      public boolean canPlaceItem(int p_59592_, ItemStack p_59593_) {
         return false;
      }

      @Override
      public void clearContent() {
      }
   };
   private final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int p_59600_) {
         return p_59600_ == 0 ? LecternBlockEntity.this.page : 0;
      }

      @Override
      public void set(int p_59602_, int p_59603_) {
         if (p_59602_ == 0) {
            LecternBlockEntity.this.setPage(p_59603_);
         }
      }

      @Override
      public int getCount() {
         return 1;
      }
   };
   ItemStack book = ItemStack.EMPTY;
   int page;
   private int pageCount;

   public LecternBlockEntity(BlockPos p_155622_, BlockState p_155623_) {
      super(BlockEntityType.LECTERN, p_155622_, p_155623_);
   }

   public ItemStack getBook() {
      return this.book;
   }

   public boolean hasBook() {
      return this.book.is(Items.WRITABLE_BOOK) || this.book.is(Items.WRITTEN_BOOK);
   }

   public void setBook(ItemStack p_59537_) {
      this.setBook(p_59537_, null);
   }

   void onBookItemRemove() {
      this.page = 0;
      this.pageCount = 0;
      LecternBlock.resetBookState(null, this.getLevel(), this.getBlockPos(), this.getBlockState(), false);
   }

   public void setBook(ItemStack p_59539_, @Nullable Player p_59540_) {
      this.book = this.resolveBook(p_59539_, p_59540_);
      this.page = 0;
      this.pageCount = getPageCount(this.book);
      this.setChanged();
   }

   void setPage(int p_59533_) {
      int $$1 = Mth.clamp(p_59533_, 0, this.pageCount - 1);
      if ($$1 != this.page) {
         this.page = $$1;
         this.setChanged();
         LecternBlock.signalPageChange(this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   public int getPage() {
      return this.page;
   }

   public int getRedstoneSignal() {
      float $$0 = this.pageCount > 1 ? this.getPage() / (this.pageCount - 1.0F) : 1.0F;
      return Mth.floor($$0 * 14.0F) + (this.hasBook() ? 1 : 0);
   }

   private ItemStack resolveBook(ItemStack p_59555_, @Nullable Player p_59556_) {
      if (this.level instanceof ServerLevel && p_59555_.is(Items.WRITTEN_BOOK)) {
         WrittenBookItem.resolveBookComponents(p_59555_, this.createCommandSourceStack(p_59556_), p_59556_);
      }

      return p_59555_;
   }

   private CommandSourceStack createCommandSourceStack(@Nullable Player p_59535_) {
      String $$1;
      Component $$2;
      if (p_59535_ == null) {
         $$1 = "Lectern";
         $$2 = Component.literal("Lectern");
      } else {
         $$1 = p_59535_.getName().getString();
         $$2 = p_59535_.getDisplayName();
      }

      Vec3 $$5 = Vec3.atCenterOf(this.worldPosition);
      return new CommandSourceStack(CommandSource.NULL, $$5, Vec2.ZERO, (ServerLevel)this.level, 2, $$1, $$2, this.level.getServer(), p_59535_);
   }

   @Override
   public boolean onlyOpCanSetNbt() {
      return true;
   }

   @Override
   protected void loadAdditional(CompoundTag p_155625_, HolderLookup.Provider p_324025_) {
      super.loadAdditional(p_155625_, p_324025_);
      if (p_155625_.contains("Book", 10)) {
         this.book = this.resolveBook(ItemStack.parse(p_324025_, p_155625_.getCompound("Book")).orElse(ItemStack.EMPTY), null);
      } else {
         this.book = ItemStack.EMPTY;
      }

      this.pageCount = getPageCount(this.book);
      this.page = Mth.clamp(p_155625_.getInt("Page"), 0, this.pageCount - 1);
   }

   @Override
   protected void saveAdditional(CompoundTag p_187509_, HolderLookup.Provider p_323474_) {
      super.saveAdditional(p_187509_, p_323474_);
      if (!this.getBook().isEmpty()) {
         p_187509_.put("Book", this.getBook().save(p_323474_));
         p_187509_.putInt("Page", this.page);
      }
   }

   @Override
   public void clearContent() {
      this.setBook(ItemStack.EMPTY);
   }

   @Override
   public AbstractContainerMenu createMenu(int p_59562_, Inventory p_59563_, Player p_59564_) {
      return new LecternMenu(p_59562_, this.bookAccess, this.dataAccess);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("container.lectern");
   }

   private static int getPageCount(ItemStack p_331229_) {
      WrittenBookContent $$1 = p_331229_.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if ($$1 != null) {
         return $$1.pages().size();
      } else {
         WritableBookContent $$2 = p_331229_.get(DataComponents.WRITABLE_BOOK_CONTENT);
         return $$2 != null ? $$2.pages().size() : 0;
      }
   }
}
