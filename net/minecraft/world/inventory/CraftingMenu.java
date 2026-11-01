package net.minecraft.world.inventory;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CraftingMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
   public static final int RESULT_SLOT = 0;
   private static final int CRAFT_SLOT_START = 1;
   private static final int CRAFT_SLOT_END = 10;
   private static final int INV_SLOT_START = 10;
   private static final int INV_SLOT_END = 37;
   private static final int USE_ROW_SLOT_START = 37;
   private static final int USE_ROW_SLOT_END = 46;
   private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
   private final ResultContainer resultSlots = new ResultContainer();
   private final ContainerLevelAccess access;
   private final Player player;
   private boolean placingRecipe;

   public CraftingMenu(int p_39353_, Inventory p_39354_) {
      this(p_39353_, p_39354_, ContainerLevelAccess.NULL);
   }

   public CraftingMenu(int p_39356_, Inventory p_39357_, ContainerLevelAccess p_39358_) {
      super(MenuType.CRAFTING, p_39356_);
      this.access = p_39358_;
      this.player = p_39357_.player;
      this.addSlot(new ResultSlot(p_39357_.player, this.craftSlots, this.resultSlots, 0, 124, 35));

      for (int $$3 = 0; $$3 < 3; $$3++) {
         for (int $$4 = 0; $$4 < 3; $$4++) {
            this.addSlot(new Slot(this.craftSlots, $$4 + $$3 * 3, 30 + $$4 * 18, 17 + $$3 * 18));
         }
      }

      for (int $$5 = 0; $$5 < 3; $$5++) {
         for (int $$6 = 0; $$6 < 9; $$6++) {
            this.addSlot(new Slot(p_39357_, $$6 + $$5 * 9 + 9, 8 + $$6 * 18, 84 + $$5 * 18));
         }
      }

      for (int $$7 = 0; $$7 < 9; $$7++) {
         this.addSlot(new Slot(p_39357_, $$7, 8 + $$7 * 18, 142));
      }
   }

   protected static void slotChangedCraftingGrid(
      AbstractContainerMenu p_150547_,
      Level p_150548_,
      Player p_150549_,
      CraftingContainer p_150550_,
      ResultContainer p_150551_,
      @Nullable RecipeHolder<CraftingRecipe> p_345124_
   ) {
      if (!p_150548_.isClientSide) {
         CraftingInput $$6 = p_150550_.asCraftInput();
         ServerPlayer $$7 = (ServerPlayer)p_150549_;
         ItemStack $$8 = ItemStack.EMPTY;
         Optional<RecipeHolder<CraftingRecipe>> $$9 = p_150548_.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, $$6, p_150548_, p_345124_);
         if ($$9.isPresent()) {
            RecipeHolder<CraftingRecipe> $$10 = $$9.get();
            CraftingRecipe $$11 = $$10.value();
            if (p_150551_.setRecipeUsed(p_150548_, $$7, $$10)) {
               ItemStack $$12 = $$11.assemble($$6, p_150548_.registryAccess());
               if ($$12.isItemEnabled(p_150548_.enabledFeatures())) {
                  $$8 = $$12;
               }
            }
         }

         p_150551_.setItem(0, $$8);
         p_150547_.setRemoteSlot(0, $$8);
         $$7.connection.send(new ClientboundContainerSetSlotPacket(p_150547_.containerId, p_150547_.incrementStateId(), 0, $$8));
      }
   }

   @Override
   public void slotsChanged(Container p_39366_) {
      if (!this.placingRecipe) {
         this.access.execute((p_344363_, p_344364_) -> slotChangedCraftingGrid(this, p_344363_, this.player, this.craftSlots, this.resultSlots, null));
      }
   }

   @Override
   public void beginPlacingRecipe() {
      this.placingRecipe = true;
   }

   @Override
   public void finishPlacingRecipe(RecipeHolder<CraftingRecipe> p_345915_) {
      this.placingRecipe = false;
      this.access.execute((p_344361_, p_344362_) -> slotChangedCraftingGrid(this, p_344361_, this.player, this.craftSlots, this.resultSlots, p_345915_));
   }

   @Override
   public void fillCraftSlotsStackedContents(StackedContents p_39374_) {
      this.craftSlots.fillStackedContents(p_39374_);
   }

   @Override
   public void clearCraftingContent() {
      this.craftSlots.clearContent();
      this.resultSlots.clearContent();
   }

   @Override
   public boolean recipeMatches(RecipeHolder<CraftingRecipe> p_300858_) {
      return p_300858_.value().matches(this.craftSlots.asCraftInput(), this.player.level());
   }

   @Override
   public void removed(Player p_39389_) {
      super.removed(p_39389_);
      this.access.execute((p_39371_, p_39372_) -> this.clearContainer(p_39389_, this.craftSlots));
   }

   @Override
   public boolean stillValid(Player p_39368_) {
      return stillValid(this.access, p_39368_, Blocks.CRAFTING_TABLE);
   }

   @Override
   public ItemStack quickMoveStack(Player p_39391_, int p_39392_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_39392_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (p_39392_ == 0) {
            this.access.execute((p_39378_, p_39379_) -> $$4.getItem().onCraftedBy($$4, p_39378_, p_39391_));
            if (!this.moveItemStackTo($$4, 10, 46, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
         } else if (p_39392_ >= 10 && p_39392_ < 46) {
            if (!this.moveItemStackTo($$4, 1, 10, false)) {
               if (p_39392_ < 37) {
                  if (!this.moveItemStackTo($$4, 37, 46, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (!this.moveItemStackTo($$4, 10, 37, false)) {
                  return ItemStack.EMPTY;
               }
            }
         } else if (!this.moveItemStackTo($$4, 10, 46, false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY);
         } else {
            $$3.setChanged();
         }

         if ($$4.getCount() == $$2.getCount()) {
            return ItemStack.EMPTY;
         }

         $$3.onTake(p_39391_, $$4);
         if (p_39392_ == 0) {
            p_39391_.drop($$4, false);
         }
      }

      return $$2;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack p_39381_, Slot p_39382_) {
      return p_39382_.container != this.resultSlots && super.canTakeItemForPickAll(p_39381_, p_39382_);
   }

   @Override
   public int getResultSlotIndex() {
      return 0;
   }

   @Override
   public int getGridWidth() {
      return this.craftSlots.getWidth();
   }

   @Override
   public int getGridHeight() {
      return this.craftSlots.getHeight();
   }

   @Override
   public int getSize() {
      return 10;
   }

   @Override
   public RecipeBookType getRecipeBookType() {
      return RecipeBookType.CRAFTING;
   }

   @Override
   public boolean shouldMoveToInventory(int p_150553_) {
      return p_150553_ != this.getResultSlotIndex();
   }
}
