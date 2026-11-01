package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
   protected static final int SLOT_INPUT = 0;
   protected static final int SLOT_FUEL = 1;
   protected static final int SLOT_RESULT = 2;
   public static final int DATA_LIT_TIME = 0;
   private static final int[] SLOTS_FOR_UP = new int[]{0};
   private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
   private static final int[] SLOTS_FOR_SIDES = new int[]{1};
   public static final int DATA_LIT_DURATION = 1;
   public static final int DATA_COOKING_PROGRESS = 2;
   public static final int DATA_COOKING_TOTAL_TIME = 3;
   public static final int NUM_DATA_VALUES = 4;
   public static final int BURN_TIME_STANDARD = 200;
   public static final int BURN_COOL_SPEED = 2;
   private static final Codec<Map<ResourceKey<Recipe<?>>, Integer>> RECIPES_USED_CODEC = Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);
   private static final short DEFAULT_COOKING_TIMER = 0;
   private static final short DEFAULT_COOKING_TOTAL_TIME = 0;
   private static final short DEFAULT_LIT_TIME_REMAINING = 0;
   private static final short DEFAULT_LIT_TOTAL_TIME = 0;
   protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
   int litTimeRemaining;
   int litTotalTime;
   int cookingTimer;
   int cookingTotalTime;
   protected final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int p_58431_) {
         switch (p_58431_) {
            case 0:
               if (AbstractFurnaceBlockEntity.this.litTotalTime > 32767) {
                  return Mth.floor((double)AbstractFurnaceBlockEntity.this.litTimeRemaining / AbstractFurnaceBlockEntity.this.litTotalTime * 32767.0);
               }

               return AbstractFurnaceBlockEntity.this.litTimeRemaining;
            case 1:
               return Math.min(AbstractFurnaceBlockEntity.this.litTotalTime, 32767);
            case 2:
               return AbstractFurnaceBlockEntity.this.cookingTimer;
            case 3:
               return AbstractFurnaceBlockEntity.this.cookingTotalTime;
            default:
               return 0;
         }
      }

      @Override
      public void set(int p_58433_, int p_58434_) {
         switch (p_58433_) {
            case 0:
               AbstractFurnaceBlockEntity.this.litTimeRemaining = p_58434_;
               break;
            case 1:
               AbstractFurnaceBlockEntity.this.litTotalTime = p_58434_;
               break;
            case 2:
               AbstractFurnaceBlockEntity.this.cookingTimer = p_58434_;
               break;
            case 3:
               AbstractFurnaceBlockEntity.this.cookingTotalTime = p_58434_;
         }
      }

      @Override
      public int getCount() {
         return 4;
      }
   };
   private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap();
   private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;
   private final RecipeType<? extends AbstractCookingRecipe> recipeType;
   private boolean needsCookingReset = false;
   private final SnapshotJournal<Boolean> cookingResetJournal = new SnapshotJournal<Boolean>() {
      protected Boolean createSnapshot() {
         return AbstractFurnaceBlockEntity.this.needsCookingReset;
      }

      protected void revertToSnapshot(Boolean snapshot) {
         AbstractFurnaceBlockEntity.this.needsCookingReset = snapshot;
      }

      protected void onRootCommit(Boolean originalState) {
         if (AbstractFurnaceBlockEntity.this.needsCookingReset) {
            if (AbstractFurnaceBlockEntity.this.level instanceof ServerLevel serverLevel) {
               AbstractFurnaceBlockEntity.this.cookingTotalTime = AbstractFurnaceBlockEntity.getTotalCookTime(serverLevel, AbstractFurnaceBlockEntity.this);
               AbstractFurnaceBlockEntity.this.cookingTimer = 0;
            }

            AbstractFurnaceBlockEntity.this.needsCookingReset = false;
         }
      }
   };

   protected AbstractFurnaceBlockEntity(
      BlockEntityType<?> p_154991_, BlockPos p_154992_, BlockState p_154993_, RecipeType<? extends AbstractCookingRecipe> p_154994_
   ) {
      super(p_154991_, p_154992_, p_154993_);
      this.quickCheck = RecipeManager.createCheck(p_154994_);
      this.recipeType = p_154994_;
   }

   private boolean isLit() {
      return this.litTimeRemaining > 0;
   }

   @Override
   protected void loadAdditional(ValueInput p_421736_) {
      super.loadAdditional(p_421736_);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(p_421736_, this.items);
      this.cookingTimer = p_421736_.getIntOr("cooking_time_spent", 0);
      this.cookingTotalTime = p_421736_.getIntOr("cooking_total_time", 0);
      this.litTimeRemaining = p_421736_.getIntOr("lit_time_remaining", 0);
      this.litTotalTime = p_421736_.getIntOr("lit_total_time", 0);
      this.recipesUsed.clear();
      this.recipesUsed.putAll(p_421736_.<Map>read("RecipesUsed", RECIPES_USED_CODEC).orElse(Map.of()));
   }

   @Override
   protected void saveAdditional(ValueOutput p_421785_) {
      super.saveAdditional(p_421785_);
      p_421785_.putInt("cooking_time_spent", this.cookingTimer);
      p_421785_.putInt("cooking_total_time", this.cookingTotalTime);
      p_421785_.putInt("lit_time_remaining", this.litTimeRemaining);
      p_421785_.putInt("lit_total_time", this.litTotalTime);
      ContainerHelper.saveAllItems(p_421785_, this.items);
      p_421785_.store("RecipesUsed", RECIPES_USED_CODEC, this.recipesUsed);
   }

   public static void serverTick(ServerLevel p_379747_, BlockPos p_155015_, BlockState p_155016_, AbstractFurnaceBlockEntity p_155017_) {
      boolean flag = p_155017_.isLit();
      boolean flag1 = false;
      if (p_155017_.isLit()) {
         p_155017_.litTimeRemaining--;
      }

      ItemStack itemstack = p_155017_.items.get(1);
      ItemStack itemstack1 = p_155017_.items.get(0);
      boolean flag2 = !itemstack1.isEmpty();
      boolean flag3 = !itemstack.isEmpty();
      if (p_155017_.isLit() || flag3 && flag2) {
         SingleRecipeInput singlerecipeinput = new SingleRecipeInput(itemstack1);
         RecipeHolder<? extends AbstractCookingRecipe> recipeholder;
         if (flag2) {
            recipeholder = p_155017_.quickCheck.getRecipeFor(singlerecipeinput, p_379747_).orElse(null);
         } else {
            recipeholder = null;
         }

         int i = p_155017_.getMaxStackSize();
         if (!p_155017_.isLit() && canBurn(p_379747_.registryAccess(), recipeholder, singlerecipeinput, p_155017_.items, i)) {
            p_155017_.litTimeRemaining = p_155017_.getBurnDuration(p_379747_.fuelValues(), itemstack);
            p_155017_.litTotalTime = p_155017_.litTimeRemaining;
            if (p_155017_.isLit()) {
               flag1 = true;
               ItemStack remainder = itemstack.getCraftingRemainder();
               if (!remainder.isEmpty()) {
                  p_155017_.items.set(1, remainder);
               } else if (flag3) {
                  Item item = itemstack.getItem();
                  itemstack.shrink(1);
                  if (itemstack.isEmpty()) {
                     p_155017_.items.set(1, item.getCraftingRemainder());
                  }
               }
            }
         }

         if (p_155017_.isLit() && canBurn(p_379747_.registryAccess(), recipeholder, singlerecipeinput, p_155017_.items, i)) {
            p_155017_.cookingTimer++;
            if (p_155017_.cookingTimer == p_155017_.cookingTotalTime) {
               p_155017_.cookingTimer = 0;
               p_155017_.cookingTotalTime = getTotalCookTime(p_379747_, p_155017_);
               if (burn(p_379747_.registryAccess(), recipeholder, singlerecipeinput, p_155017_.items, i)) {
                  p_155017_.setRecipeUsed(recipeholder);
               }

               flag1 = true;
            }
         } else {
            p_155017_.cookingTimer = 0;
         }
      } else if (!p_155017_.isLit() && p_155017_.cookingTimer > 0) {
         p_155017_.cookingTimer = Mth.clamp(p_155017_.cookingTimer - 2, 0, p_155017_.cookingTotalTime);
      }

      if (flag != p_155017_.isLit()) {
         flag1 = true;
         p_155016_ = p_155016_.setValue(AbstractFurnaceBlock.LIT, p_155017_.isLit());
         p_379747_.setBlock(p_155015_, p_155016_, 3);
      }

      if (flag1) {
         setChanged(p_379747_, p_155015_, p_155016_);
      }
   }

   private static boolean canBurn(
      RegistryAccess p_266924_,
      @Nullable RecipeHolder<? extends AbstractCookingRecipe> p_301107_,
      SingleRecipeInput p_380038_,
      NonNullList<ItemStack> p_155007_,
      int p_155008_
   ) {
      if (!p_155007_.get(0).isEmpty() && p_301107_ != null) {
         ItemStack itemstack = p_301107_.value().assemble(p_380038_, p_266924_);
         if (itemstack.isEmpty()) {
            return false;
         } else {
            ItemStack itemstack1 = p_155007_.get(2);
            if (itemstack1.isEmpty()) {
               return true;
            } else if (!ItemStack.isSameItemSameComponents(itemstack1, itemstack)) {
               return false;
            } else {
               return itemstack1.getCount() + itemstack.getCount() <= p_155008_ && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()
                  ? true
                  : itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize();
            }
         }
      } else {
         return false;
      }
   }

   private static boolean burn(
      RegistryAccess p_266740_,
      @Nullable RecipeHolder<? extends AbstractCookingRecipe> p_300910_,
      SingleRecipeInput p_380269_,
      NonNullList<ItemStack> p_267073_,
      int p_267157_
   ) {
      if (p_300910_ != null && canBurn(p_266740_, p_300910_, p_380269_, p_267073_, p_267157_)) {
         ItemStack itemstack = p_267073_.get(0);
         ItemStack itemstack1 = p_300910_.value().assemble(p_380269_, p_266740_);
         ItemStack itemstack2 = p_267073_.get(2);
         if (itemstack2.isEmpty()) {
            p_267073_.set(2, itemstack1.copy());
         } else if (ItemStack.isSameItemSameComponents(itemstack2, itemstack1)) {
            itemstack2.grow(itemstack1.getCount());
         }

         if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !p_267073_.get(1).isEmpty() && p_267073_.get(1).is(Items.BUCKET)) {
            p_267073_.set(1, new ItemStack(Items.WATER_BUCKET));
         }

         itemstack.shrink(1);
         return true;
      } else {
         return false;
      }
   }

   protected int getBurnDuration(FuelValues p_363501_, ItemStack p_58343_) {
      return p_58343_.getBurnTime(this.recipeType, p_363501_);
   }

   private static int getTotalCookTime(ServerLevel p_380169_, AbstractFurnaceBlockEntity p_222694_) {
      SingleRecipeInput singlerecipeinput = new SingleRecipeInput(p_222694_.getItem(0));
      return p_222694_.quickCheck.getRecipeFor(singlerecipeinput, p_380169_).map(p_379263_ -> p_379263_.value().cookingTime()).orElse(200);
   }

   @Override
   public int[] getSlotsForFace(Direction p_58363_) {
      if (p_58363_ == Direction.DOWN) {
         return SLOTS_FOR_DOWN;
      } else {
         return p_58363_ == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
      }
   }

   @Override
   public boolean canPlaceItemThroughFace(int p_58336_, ItemStack p_58337_, @Nullable Direction p_58338_) {
      return this.canPlaceItem(p_58336_, p_58337_);
   }

   @Override
   public boolean canTakeItemThroughFace(int p_58392_, ItemStack p_58393_, Direction p_58394_) {
      return p_58394_ == Direction.DOWN && p_58392_ == 1 ? p_58393_.is(Items.WATER_BUCKET) || p_58393_.is(Items.BUCKET) : true;
   }

   @Override
   public int getContainerSize() {
      return this.items.size();
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> p_332808_) {
      this.items = p_332808_;
   }

   @Override
   public void setItem(int p_58333_, ItemStack p_58334_) {
      this.setItem(p_58333_, p_58334_, false);
   }

   @Override
   public void setItem(int p_58333_, ItemStack p_58334_, boolean insideTransaction) {
      ItemStack itemstack = this.items.get(p_58333_);
      boolean flag = !p_58334_.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, p_58334_);
      this.items.set(p_58333_, p_58334_);
      p_58334_.limitSize(this.getMaxStackSize(p_58334_));
      if (p_58333_ == 0 && !flag && this.level instanceof ServerLevel serverlevel && !insideTransaction) {
         this.cookingTotalTime = getTotalCookTime(serverlevel, this);
         this.cookingTimer = 0;
         this.setChanged();
      }
   }

   @Override
   public void onTransfer(int slot, int amountChange, TransactionContext transaction) {
      if (slot == 0) {
         int currentAmount = this.getItem(slot).getCount();
         if (currentAmount == 0 || currentAmount == amountChange) {
            this.cookingResetJournal.updateSnapshots(transaction);
            this.needsCookingReset = true;
         }
      }
   }

   @Override
   public boolean canPlaceItem(int p_58389_, ItemStack p_58390_) {
      if (p_58389_ == 2) {
         return false;
      } else if (p_58389_ != 1) {
         return true;
      } else {
         ItemStack itemstack = this.items.get(1);
         return p_58390_.getBurnTime(this.recipeType, this.level.fuelValues()) > 0 || p_58390_.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
      }
   }

   @Override
   public void setRecipeUsed(@Nullable RecipeHolder<?> p_301245_) {
      if (p_301245_ != null) {
         ResourceKey<Recipe<?>> resourcekey = p_301245_.id();
         this.recipesUsed.addTo(resourcekey, 1);
      }
   }

   @Nullable
   @Override
   public RecipeHolder<?> getRecipeUsed() {
      return null;
   }

   @Override
   public void awardUsedRecipes(Player p_58396_, List<ItemStack> p_282202_) {
   }

   public void awardUsedRecipesAndPopExperience(ServerPlayer p_155004_) {
      List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(p_155004_.level(), p_155004_.position());
      p_155004_.awardRecipes(list);

      for (RecipeHolder<?> recipeholder : list) {
         p_155004_.triggerRecipeCrafted(recipeholder, this.items);
      }

      this.recipesUsed.clear();
   }

   public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel p_154996_, Vec3 p_154997_) {
      List<RecipeHolder<?>> list = Lists.newArrayList();
      ObjectIterator var4 = this.recipesUsed.reference2IntEntrySet().iterator();

      while (var4.hasNext()) {
         Entry<ResourceKey<Recipe<?>>> entry = (Entry<ResourceKey<Recipe<?>>>)var4.next();
         p_154996_.recipeAccess().byKey((ResourceKey<Recipe<?>>)entry.getKey()).ifPresent(p_379268_ -> {
            list.add((RecipeHolder<?>)p_379268_);
            createExperience(p_154996_, p_154997_, entry.getIntValue(), ((AbstractCookingRecipe)p_379268_.value()).experience());
         });
      }

      return list;
   }

   private static void createExperience(ServerLevel p_154999_, Vec3 p_155000_, int p_155001_, float p_155002_) {
      int i = Mth.floor(p_155001_ * p_155002_);
      float f = Mth.frac(p_155001_ * p_155002_);
      if (f != 0.0F && p_154999_.random.nextFloat() < f) {
         i++;
      }

      ExperienceOrb.award(p_154999_, p_155000_, i);
   }

   @Override
   public void fillStackedContents(StackedItemContents p_363281_) {
      for (ItemStack itemstack : this.items) {
         p_363281_.accountStack(itemstack);
      }
   }

   @Override
   public void preRemoveSideEffects(BlockPos p_393693_, BlockState p_393780_) {
      super.preRemoveSideEffects(p_393693_, p_393780_);
      if (this.level instanceof ServerLevel serverlevel) {
         this.getRecipesToAwardAndPopExperience(serverlevel, Vec3.atCenterOf(p_393693_));
      }
   }
}
