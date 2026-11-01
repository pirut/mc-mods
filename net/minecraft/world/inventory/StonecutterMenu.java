package net.minecraft.world.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class StonecutterMenu extends AbstractContainerMenu {
   public static final int INPUT_SLOT = 0;
   public static final int RESULT_SLOT = 1;
   private static final int INV_SLOT_START = 2;
   private static final int INV_SLOT_END = 29;
   private static final int USE_ROW_SLOT_START = 29;
   private static final int USE_ROW_SLOT_END = 38;
   private final ContainerLevelAccess access;
   final DataSlot selectedRecipeIndex = DataSlot.standalone();
   private final Level level;
   private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipesForInput = SelectableRecipe.SingleInputSet.empty();
   private ItemStack input = ItemStack.EMPTY;
   long lastSoundTime;
   final Slot inputSlot;
   final Slot resultSlot;
   Runnable slotUpdateListener = () -> {};
   public final Container container = new SimpleContainer(1) {
      @Override
      public void setChanged() {
         super.setChanged();
         StonecutterMenu.this.slotsChanged(this);
         StonecutterMenu.this.slotUpdateListener.run();
      }
   };
   final ResultContainer resultContainer = new ResultContainer();

   public StonecutterMenu(int containerId, Inventory playerInventory) {
      this(containerId, playerInventory, ContainerLevelAccess.NULL);
   }

   public StonecutterMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
      super(MenuType.STONECUTTER, containerId);
      this.access = access;
      this.level = playerInventory.player.level();
      this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
      this.resultSlot = this.addSlot(new Slot(this.resultContainer, 1, 143, 33) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return false;
         }

         @Override
         public void onTake(Player player, ItemStack stack) {
            stack.onCraftedBy(player, stack.getCount());
            StonecutterMenu.this.resultContainer.awardUsedRecipes(player, this.getRelevantItems());
            ItemStack itemStack = StonecutterMenu.this.inputSlot.remove(1);
            if (!itemStack.isEmpty()) {
               StonecutterMenu.this.setupResultSlot(StonecutterMenu.this.selectedRecipeIndex.get());
            }

            access.execute((level, blockPos) -> {
               long l = level.getGameTime();
               if (StonecutterMenu.this.lastSoundTime != l) {
                  level.playSound(null, blockPos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  StonecutterMenu.this.lastSoundTime = l;
               }
            });
            super.onTake(player, stack);
         }

         private List<ItemStack> getRelevantItems() {
            return List.of(StonecutterMenu.this.inputSlot.getItem());
         }
      });
      this.addStandardInventorySlots(playerInventory, 8, 84);
      this.addDataSlot(this.selectedRecipeIndex);
   }

   public int getSelectedRecipeIndex() {
      return this.selectedRecipeIndex.get();
   }

   public SelectableRecipe.SingleInputSet<StonecutterRecipe> getVisibleRecipes() {
      return this.recipesForInput;
   }

   public int getNumberOfVisibleRecipes() {
      return this.recipesForInput.size();
   }

   public boolean hasInputItem() {
      return this.inputSlot.hasItem() && !this.recipesForInput.isEmpty();
   }

   @Override
   public boolean stillValid(Player player) {
      return stillValid(this.access, player, Blocks.STONECUTTER);
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      if (this.selectedRecipeIndex.get() == id) {
         return false;
      } else {
         if (this.isValidRecipeIndex(id)) {
            this.selectedRecipeIndex.set(id);
            this.setupResultSlot(id);
         }

         return true;
      }
   }

   private boolean isValidRecipeIndex(int recipeIndex) {
      return recipeIndex >= 0 && recipeIndex < this.recipesForInput.size();
   }

   @Override
   public void slotsChanged(Container container) {
      ItemStack itemStack = this.inputSlot.getItem();
      if (!itemStack.is(this.input.getItem())) {
         this.input = itemStack.copy();
         this.setupRecipeList(itemStack);
      }
   }

   private void setupRecipeList(ItemStack stack) {
      this.selectedRecipeIndex.set(-1);
      this.resultSlot.set(ItemStack.EMPTY);
      if (!stack.isEmpty()) {
         this.recipesForInput = this.level.recipeAccess().stonecutterRecipes().selectByInput(stack);
      } else {
         this.recipesForInput = SelectableRecipe.SingleInputSet.empty();
      }
   }

   void setupResultSlot(int id) {
      Optional<RecipeHolder<StonecutterRecipe>> optional;
      if (!this.recipesForInput.isEmpty() && this.isValidRecipeIndex(id)) {
         SelectableRecipe.SingleInputEntry<StonecutterRecipe> singleInputEntry = this.recipesForInput.entries().get(id);
         optional = singleInputEntry.recipe().recipe();
      } else {
         optional = Optional.empty();
      }

      optional.ifPresentOrElse(recipeHolder -> {
         this.resultContainer.setRecipeUsed((RecipeHolder<?>)recipeHolder);
         this.resultSlot.set(recipeHolder.value().assemble(new SingleRecipeInput(this.container.getItem(0)), this.level.registryAccess()));
      }, () -> {
         this.resultSlot.set(ItemStack.EMPTY);
         this.resultContainer.setRecipeUsed(null);
      });
      this.broadcastChanges();
   }

   @Override
   public MenuType<?> getType() {
      return MenuType.STONECUTTER;
   }

   public void registerUpdateListener(Runnable listener) {
      this.slotUpdateListener = listener;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
      return slot.container != this.resultContainer && super.canTakeItemForPickAll(stack, slot);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemStack = ItemStack.EMPTY;
      Slot slot = this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack itemStack2 = slot.getItem();
         Item item = itemStack2.getItem();
         itemStack = itemStack2.copy();
         if (index == 1) {
            item.onCraftedBy(itemStack2, player);
            if (!this.moveItemStackTo(itemStack2, 2, 38, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemStack2, itemStack);
         } else if (index == 0) {
            if (!this.moveItemStackTo(itemStack2, 2, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.level.recipeAccess().stonecutterRecipes().acceptsInput(itemStack2)) {
            if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (index >= 2 && index < 29) {
            if (!this.moveItemStackTo(itemStack2, 29, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (index >= 29 && index < 38 && !this.moveItemStackTo(itemStack2, 2, 29, false)) {
            return ItemStack.EMPTY;
         }

         if (itemStack2.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         }

         slot.setChanged();
         if (itemStack2.getCount() == itemStack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, itemStack2);
         if (index == 1) {
            player.drop(itemStack2, false);
         }

         this.broadcastChanges();
      }

      return itemStack;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.resultContainer.removeItemNoUpdate(1);
      this.access.execute((level, blockPos) -> this.clearContainer(player, this.container));
   }
}
