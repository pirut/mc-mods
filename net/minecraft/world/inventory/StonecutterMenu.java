package net.minecraft.world.inventory;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
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
   private final DataSlot selectedRecipeIndex = DataSlot.standalone();
   private final Level level;
   private List<RecipeHolder<StonecutterRecipe>> recipes = Lists.newArrayList();
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

   public StonecutterMenu(int p_40294_, Inventory p_40295_) {
      this(p_40294_, p_40295_, ContainerLevelAccess.NULL);
   }

   public StonecutterMenu(int p_40297_, Inventory p_40298_, final ContainerLevelAccess p_40299_) {
      super(MenuType.STONECUTTER, p_40297_);
      this.access = p_40299_;
      this.level = p_40298_.player.level();
      this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
      this.resultSlot = this.addSlot(new Slot(this.resultContainer, 1, 143, 33) {
         @Override
         public boolean mayPlace(ItemStack p_40362_) {
            return false;
         }

         @Override
         public void onTake(Player p_150672_, ItemStack p_150673_) {
            p_150673_.onCraftedBy(p_150672_.level(), p_150672_, p_150673_.getCount());
            StonecutterMenu.this.resultContainer.awardUsedRecipes(p_150672_, this.getRelevantItems());
            ItemStack $$2 = StonecutterMenu.this.inputSlot.remove(1);
            if (!$$2.isEmpty()) {
               StonecutterMenu.this.setupResultSlot();
            }

            p_40299_.execute((p_40364_, p_40365_) -> {
               long $$2x = p_40364_.getGameTime();
               if (StonecutterMenu.this.lastSoundTime != $$2x) {
                  p_40364_.playSound(null, p_40365_, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  StonecutterMenu.this.lastSoundTime = $$2x;
               }
            });
            super.onTake(p_150672_, p_150673_);
         }

         private List<ItemStack> getRelevantItems() {
            return List.of(StonecutterMenu.this.inputSlot.getItem());
         }
      });

      for (int $$3 = 0; $$3 < 3; $$3++) {
         for (int $$4 = 0; $$4 < 9; $$4++) {
            this.addSlot(new Slot(p_40298_, $$4 + $$3 * 9 + 9, 8 + $$4 * 18, 84 + $$3 * 18));
         }
      }

      for (int $$5 = 0; $$5 < 9; $$5++) {
         this.addSlot(new Slot(p_40298_, $$5, 8 + $$5 * 18, 142));
      }

      this.addDataSlot(this.selectedRecipeIndex);
   }

   public int getSelectedRecipeIndex() {
      return this.selectedRecipeIndex.get();
   }

   public List<RecipeHolder<StonecutterRecipe>> getRecipes() {
      return this.recipes;
   }

   public int getNumRecipes() {
      return this.recipes.size();
   }

   public boolean hasInputItem() {
      return this.inputSlot.hasItem() && !this.recipes.isEmpty();
   }

   @Override
   public boolean stillValid(Player p_40307_) {
      return stillValid(this.access, p_40307_, Blocks.STONECUTTER);
   }

   @Override
   public boolean clickMenuButton(Player p_40309_, int p_40310_) {
      if (this.isValidRecipeIndex(p_40310_)) {
         this.selectedRecipeIndex.set(p_40310_);
         this.setupResultSlot();
      }

      return true;
   }

   private boolean isValidRecipeIndex(int p_40335_) {
      return p_40335_ >= 0 && p_40335_ < this.recipes.size();
   }

   @Override
   public void slotsChanged(Container p_40302_) {
      ItemStack $$1 = this.inputSlot.getItem();
      if (!$$1.is(this.input.getItem())) {
         this.input = $$1.copy();
         this.setupRecipeList(p_40302_, $$1);
      }
   }

   private static SingleRecipeInput createRecipeInput(Container p_346312_) {
      return new SingleRecipeInput(p_346312_.getItem(0));
   }

   private void setupRecipeList(Container p_40304_, ItemStack p_40305_) {
      this.recipes.clear();
      this.selectedRecipeIndex.set(-1);
      this.resultSlot.set(ItemStack.EMPTY);
      if (!p_40305_.isEmpty()) {
         this.recipes = this.level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, createRecipeInput(p_40304_), this.level);
      }
   }

   void setupResultSlot() {
      if (!this.recipes.isEmpty() && this.isValidRecipeIndex(this.selectedRecipeIndex.get())) {
         RecipeHolder<StonecutterRecipe> $$0 = this.recipes.get(this.selectedRecipeIndex.get());
         ItemStack $$1 = $$0.value().assemble(createRecipeInput(this.container), this.level.registryAccess());
         if ($$1.isItemEnabled(this.level.enabledFeatures())) {
            this.resultContainer.setRecipeUsed($$0);
            this.resultSlot.set($$1);
         } else {
            this.resultSlot.set(ItemStack.EMPTY);
         }
      } else {
         this.resultSlot.set(ItemStack.EMPTY);
      }

      this.broadcastChanges();
   }

   @Override
   public MenuType<?> getType() {
      return MenuType.STONECUTTER;
   }

   public void registerUpdateListener(Runnable p_40324_) {
      this.slotUpdateListener = p_40324_;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack p_40321_, Slot p_40322_) {
      return p_40322_.container != this.resultContainer && super.canTakeItemForPickAll(p_40321_, p_40322_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_40328_, int p_40329_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_40329_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         Item $$5 = $$4.getItem();
         $$2 = $$4.copy();
         if (p_40329_ == 1) {
            $$5.onCraftedBy($$4, p_40328_.level(), p_40328_);
            if (!this.moveItemStackTo($$4, 2, 38, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
         } else if (p_40329_ == 0) {
            if (!this.moveItemStackTo($$4, 2, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.level.getRecipeManager().getRecipeFor(RecipeType.STONECUTTING, new SingleRecipeInput($$4), this.level).isPresent()) {
            if (!this.moveItemStackTo($$4, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_40329_ >= 2 && p_40329_ < 29) {
            if (!this.moveItemStackTo($$4, 29, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_40329_ >= 29 && p_40329_ < 38 && !this.moveItemStackTo($$4, 2, 29, false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY);
         }

         $$3.setChanged();
         if ($$4.getCount() == $$2.getCount()) {
            return ItemStack.EMPTY;
         }

         $$3.onTake(p_40328_, $$4);
         this.broadcastChanges();
      }

      return $$2;
   }

   @Override
   public void removed(Player p_40326_) {
      super.removed(p_40326_);
      this.resultContainer.removeItemNoUpdate(1);
      this.access.execute((p_40313_, p_40314_) -> this.clearContainer(p_40326_, this.container));
   }
}
