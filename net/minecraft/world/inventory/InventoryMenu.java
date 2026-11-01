package net.minecraft.world.inventory;

import com.mojang.datafixers.util.Pair;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class InventoryMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
   public static final int CONTAINER_ID = 0;
   public static final int RESULT_SLOT = 0;
   public static final int CRAFT_SLOT_START = 1;
   public static final int CRAFT_SLOT_COUNT = 4;
   public static final int CRAFT_SLOT_END = 5;
   public static final int ARMOR_SLOT_START = 5;
   public static final int ARMOR_SLOT_COUNT = 4;
   public static final int ARMOR_SLOT_END = 9;
   public static final int INV_SLOT_START = 9;
   public static final int INV_SLOT_END = 36;
   public static final int USE_ROW_SLOT_START = 36;
   public static final int USE_ROW_SLOT_END = 45;
   public static final int SHIELD_SLOT = 45;
   public static final ResourceLocation BLOCK_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
   public static final ResourceLocation EMPTY_ARMOR_SLOT_HELMET = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet");
   public static final ResourceLocation EMPTY_ARMOR_SLOT_CHESTPLATE = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate");
   public static final ResourceLocation EMPTY_ARMOR_SLOT_LEGGINGS = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings");
   public static final ResourceLocation EMPTY_ARMOR_SLOT_BOOTS = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots");
   public static final ResourceLocation EMPTY_ARMOR_SLOT_SHIELD = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_shield");
   private static final Map<EquipmentSlot, ResourceLocation> TEXTURE_EMPTY_SLOTS = Map.of(
      EquipmentSlot.FEET,
      EMPTY_ARMOR_SLOT_BOOTS,
      EquipmentSlot.LEGS,
      EMPTY_ARMOR_SLOT_LEGGINGS,
      EquipmentSlot.CHEST,
      EMPTY_ARMOR_SLOT_CHESTPLATE,
      EquipmentSlot.HEAD,
      EMPTY_ARMOR_SLOT_HELMET
   );
   private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
   private final ResultContainer resultSlots = new ResultContainer();
   public final boolean active;
   private final Player owner;

   public InventoryMenu(Inventory p_39706_, boolean p_39707_, final Player p_39708_) {
      super(null, 0);
      this.active = p_39707_;
      this.owner = p_39708_;
      this.addSlot(new ResultSlot(p_39706_.player, this.craftSlots, this.resultSlots, 0, 154, 28));

      for (int $$3 = 0; $$3 < 2; $$3++) {
         for (int $$4 = 0; $$4 < 2; $$4++) {
            this.addSlot(new Slot(this.craftSlots, $$4 + $$3 * 2, 98 + $$4 * 18, 18 + $$3 * 18));
         }
      }

      for (int $$5 = 0; $$5 < 4; $$5++) {
         EquipmentSlot $$6 = SLOT_IDS[$$5];
         ResourceLocation $$7 = TEXTURE_EMPTY_SLOTS.get($$6);
         this.addSlot(new ArmorSlot(p_39706_, p_39708_, $$6, 39 - $$5, 8, 8 + $$5 * 18, $$7));
      }

      for (int $$8 = 0; $$8 < 3; $$8++) {
         for (int $$9 = 0; $$9 < 9; $$9++) {
            this.addSlot(new Slot(p_39706_, $$9 + ($$8 + 1) * 9, 8 + $$9 * 18, 84 + $$8 * 18));
         }
      }

      for (int $$10 = 0; $$10 < 9; $$10++) {
         this.addSlot(new Slot(p_39706_, $$10, 8 + $$10 * 18, 142));
      }

      this.addSlot(new Slot(p_39706_, 40, 77, 62) {
         @Override
         public void setByPlayer(ItemStack p_270969_, ItemStack p_299918_) {
            p_39708_.onEquipItem(EquipmentSlot.OFFHAND, p_299918_, p_270969_);
            super.setByPlayer(p_270969_, p_299918_);
         }

         @Override
         public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
         }
      });
   }

   public static boolean isHotbarSlot(int p_150593_) {
      return p_150593_ >= 36 && p_150593_ < 45 || p_150593_ == 45;
   }

   @Override
   public void fillCraftSlotsStackedContents(StackedContents p_39714_) {
      this.craftSlots.fillStackedContents(p_39714_);
   }

   @Override
   public void clearCraftingContent() {
      this.resultSlots.clearContent();
      this.craftSlots.clearContent();
   }

   @Override
   public boolean recipeMatches(RecipeHolder<CraftingRecipe> p_300901_) {
      return p_300901_.value().matches(this.craftSlots.asCraftInput(), this.owner.level());
   }

   @Override
   public void slotsChanged(Container p_39710_) {
      CraftingMenu.slotChangedCraftingGrid(this, this.owner.level(), this.owner, this.craftSlots, this.resultSlots, null);
   }

   @Override
   public void removed(Player p_39721_) {
      super.removed(p_39721_);
      this.resultSlots.clearContent();
      if (!p_39721_.level().isClientSide) {
         this.clearContainer(p_39721_, this.craftSlots);
      }
   }

   @Override
   public boolean stillValid(Player p_39712_) {
      return true;
   }

   @Override
   public ItemStack quickMoveStack(Player p_39723_, int p_39724_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_39724_);
      if ($$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         EquipmentSlot $$5 = p_39723_.getEquipmentSlotForItem($$2);
         if (p_39724_ == 0) {
            if (!this.moveItemStackTo($$4, 9, 45, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
         } else if (p_39724_ >= 1 && p_39724_ < 5) {
            if (!this.moveItemStackTo($$4, 9, 45, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39724_ >= 5 && p_39724_ < 9) {
            if (!this.moveItemStackTo($$4, 9, 45, false)) {
               return ItemStack.EMPTY;
            }
         } else if ($$5.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !this.slots.get(8 - $$5.getIndex()).hasItem()) {
            int $$6 = 8 - $$5.getIndex();
            if (!this.moveItemStackTo($$4, $$6, $$6 + 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if ($$5 == EquipmentSlot.OFFHAND && !this.slots.get(45).hasItem()) {
            if (!this.moveItemStackTo($$4, 45, 46, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39724_ >= 9 && p_39724_ < 36) {
            if (!this.moveItemStackTo($$4, 36, 45, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39724_ >= 36 && p_39724_ < 45) {
            if (!this.moveItemStackTo($$4, 9, 36, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 9, 45, false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY, $$2);
         } else {
            $$3.setChanged();
         }

         if ($$4.getCount() == $$2.getCount()) {
            return ItemStack.EMPTY;
         }

         $$3.onTake(p_39723_, $$4);
         if (p_39724_ == 0) {
            p_39723_.drop($$4, false);
         }
      }

      return $$2;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack p_39716_, Slot p_39717_) {
      return p_39717_.container != this.resultSlots && super.canTakeItemForPickAll(p_39716_, p_39717_);
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
      return 5;
   }

   public CraftingContainer getCraftSlots() {
      return this.craftSlots;
   }

   @Override
   public RecipeBookType getRecipeBookType() {
      return RecipeBookType.CRAFTING;
   }

   @Override
   public boolean shouldMoveToInventory(int p_150591_) {
      return p_150591_ != this.getResultSlotIndex();
   }
}
