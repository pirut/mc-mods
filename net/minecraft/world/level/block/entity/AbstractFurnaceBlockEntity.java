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
      public int get(int index) {
         switch (index) {
            case 0:
               return AbstractFurnaceBlockEntity.this.litTimeRemaining;
            case 1:
               return AbstractFurnaceBlockEntity.this.litTotalTime;
            case 2:
               return AbstractFurnaceBlockEntity.this.cookingTimer;
            case 3:
               return AbstractFurnaceBlockEntity.this.cookingTotalTime;
            default:
               return 0;
         }
      }

      @Override
      public void set(int index, int value) {
         switch (index) {
            case 0:
               AbstractFurnaceBlockEntity.this.litTimeRemaining = value;
               break;
            case 1:
               AbstractFurnaceBlockEntity.this.litTotalTime = value;
               break;
            case 2:
               AbstractFurnaceBlockEntity.this.cookingTimer = value;
               break;
            case 3:
               AbstractFurnaceBlockEntity.this.cookingTotalTime = value;
         }
      }

      @Override
      public int getCount() {
         return 4;
      }
   };
   private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap();
   private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

   protected AbstractFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, RecipeType<? extends AbstractCookingRecipe> recipeType) {
      super(type, pos, blockState);
      this.quickCheck = RecipeManager.createCheck(recipeType);
   }

   private boolean isLit() {
      return this.litTimeRemaining > 0;
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(input, this.items);
      this.cookingTimer = input.getShortOr("cooking_time_spent", (short)0);
      this.cookingTotalTime = input.getShortOr("cooking_total_time", (short)0);
      this.litTimeRemaining = input.getShortOr("lit_time_remaining", (short)0);
      this.litTotalTime = input.getShortOr("lit_total_time", (short)0);
      this.recipesUsed.clear();
      this.recipesUsed.putAll(input.<Map>read("RecipesUsed", RECIPES_USED_CODEC).orElse(Map.of()));
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putShort("cooking_time_spent", (short)this.cookingTimer);
      output.putShort("cooking_total_time", (short)this.cookingTotalTime);
      output.putShort("lit_time_remaining", (short)this.litTimeRemaining);
      output.putShort("lit_total_time", (short)this.litTotalTime);
      ContainerHelper.saveAllItems(output, this.items);
      output.store("RecipesUsed", RECIPES_USED_CODEC, this.recipesUsed);
   }

   public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace) {
      boolean bl = furnace.isLit();
      boolean bl2 = false;
      if (furnace.isLit()) {
         furnace.litTimeRemaining--;
      }

      ItemStack itemStack = furnace.items.get(1);
      ItemStack itemStack2 = furnace.items.get(0);
      boolean bl3 = !itemStack2.isEmpty();
      boolean bl4 = !itemStack.isEmpty();
      if (furnace.isLit() || bl4 && bl3) {
         SingleRecipeInput singleRecipeInput = new SingleRecipeInput(itemStack2);
         RecipeHolder<? extends AbstractCookingRecipe> recipeHolder;
         if (bl3) {
            recipeHolder = furnace.quickCheck.getRecipeFor(singleRecipeInput, level).orElse(null);
         } else {
            recipeHolder = null;
         }

         int i = furnace.getMaxStackSize();
         if (!furnace.isLit() && canBurn(level.registryAccess(), recipeHolder, singleRecipeInput, furnace.items, i)) {
            furnace.litTimeRemaining = furnace.getBurnDuration(level.fuelValues(), itemStack);
            furnace.litTotalTime = furnace.litTimeRemaining;
            if (furnace.isLit()) {
               bl2 = true;
               if (bl4) {
                  Item item = itemStack.getItem();
                  itemStack.shrink(1);
                  if (itemStack.isEmpty()) {
                     furnace.items.set(1, item.getCraftingRemainder());
                  }
               }
            }
         }

         if (furnace.isLit() && canBurn(level.registryAccess(), recipeHolder, singleRecipeInput, furnace.items, i)) {
            furnace.cookingTimer++;
            if (furnace.cookingTimer == furnace.cookingTotalTime) {
               furnace.cookingTimer = 0;
               furnace.cookingTotalTime = getTotalCookTime(level, furnace);
               if (burn(level.registryAccess(), recipeHolder, singleRecipeInput, furnace.items, i)) {
                  furnace.setRecipeUsed(recipeHolder);
               }

               bl2 = true;
            }
         } else {
            furnace.cookingTimer = 0;
         }
      } else if (!furnace.isLit() && furnace.cookingTimer > 0) {
         furnace.cookingTimer = Mth.clamp(furnace.cookingTimer - 2, 0, furnace.cookingTotalTime);
      }

      if (bl != furnace.isLit()) {
         bl2 = true;
         state = state.setValue(AbstractFurnaceBlock.LIT, furnace.isLit());
         level.setBlock(pos, state, 3);
      }

      if (bl2) {
         setChanged(level, pos, state);
      }
   }

   private static boolean canBurn(
      RegistryAccess registryAccess,
      @Nullable RecipeHolder<? extends AbstractCookingRecipe> recipe,
      SingleRecipeInput recipeInput,
      NonNullList<ItemStack> items,
      int maxStackSize
   ) {
      if (!items.get(0).isEmpty() && recipe != null) {
         ItemStack itemStack = recipe.value().assemble(recipeInput, registryAccess);
         if (itemStack.isEmpty()) {
            return false;
         } else {
            ItemStack itemStack2 = items.get(2);
            if (itemStack2.isEmpty()) {
               return true;
            } else if (!ItemStack.isSameItemSameComponents(itemStack2, itemStack)) {
               return false;
            } else {
               return itemStack2.getCount() < maxStackSize && itemStack2.getCount() < itemStack2.getMaxStackSize()
                  ? true
                  : itemStack2.getCount() < itemStack.getMaxStackSize();
            }
         }
      } else {
         return false;
      }
   }

   private static boolean burn(
      RegistryAccess registryAccess,
      @Nullable RecipeHolder<? extends AbstractCookingRecipe> recipe,
      SingleRecipeInput recipeInput,
      NonNullList<ItemStack> items,
      int maxStackSize
   ) {
      if (recipe != null && canBurn(registryAccess, recipe, recipeInput, items, maxStackSize)) {
         ItemStack itemStack = items.get(0);
         ItemStack itemStack2 = recipe.value().assemble(recipeInput, registryAccess);
         ItemStack itemStack3 = items.get(2);
         if (itemStack3.isEmpty()) {
            items.set(2, itemStack2.copy());
         } else if (ItemStack.isSameItemSameComponents(itemStack3, itemStack2)) {
            itemStack3.grow(1);
         }

         if (itemStack.is(Blocks.WET_SPONGE.asItem()) && !items.get(1).isEmpty() && items.get(1).is(Items.BUCKET)) {
            items.set(1, new ItemStack(Items.WATER_BUCKET));
         }

         itemStack.shrink(1);
         return true;
      } else {
         return false;
      }
   }

   protected int getBurnDuration(FuelValues fuelValues, ItemStack stack) {
      return fuelValues.burnDuration(stack);
   }

   private static int getTotalCookTime(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
      SingleRecipeInput singleRecipeInput = new SingleRecipeInput(furnace.getItem(0));
      return furnace.quickCheck.getRecipeFor(singleRecipeInput, level).map(recipeHolder -> recipeHolder.value().cookingTime()).orElse(200);
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      if (side == Direction.DOWN) {
         return SLOTS_FOR_DOWN;
      } else {
         return side == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
      }
   }

   @Override
   public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
      return this.canPlaceItem(index, itemStack);
   }

   @Override
   public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
      return direction == Direction.DOWN && index == 1 ? stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET) : true;
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
   protected void setItems(NonNullList<ItemStack> items) {
      this.items = items;
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      ItemStack itemStack = this.items.get(slot);
      boolean bl = !stack.isEmpty() && ItemStack.isSameItemSameComponents(itemStack, stack);
      this.items.set(slot, stack);
      stack.limitSize(this.getMaxStackSize(stack));
      if (slot == 0 && !bl && this.level instanceof ServerLevel serverLevel) {
         this.cookingTotalTime = getTotalCookTime(serverLevel, this);
         this.cookingTimer = 0;
         this.setChanged();
      }
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      if (slot == 2) {
         return false;
      } else if (slot != 1) {
         return true;
      } else {
         ItemStack itemStack = this.items.get(1);
         return this.level.fuelValues().isFuel(stack) || stack.is(Items.BUCKET) && !itemStack.is(Items.BUCKET);
      }
   }

   @Override
   public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
      if (recipe != null) {
         ResourceKey<Recipe<?>> resourceKey = recipe.id();
         this.recipesUsed.addTo(resourceKey, 1);
      }
   }

   @Nullable
   @Override
   public RecipeHolder<?> getRecipeUsed() {
      return null;
   }

   @Override
   public void awardUsedRecipes(Player player, List<ItemStack> items) {
   }

   public void awardUsedRecipesAndPopExperience(ServerPlayer player) {
      List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(player.level(), player.position());
      player.awardRecipes(list);

      for (RecipeHolder<?> recipeHolder : list) {
         player.triggerRecipeCrafted(recipeHolder, this.items);
      }

      this.recipesUsed.clear();
   }

   public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel level, Vec3 popVec) {
      List<RecipeHolder<?>> list = Lists.newArrayList();
      ObjectIterator var4 = this.recipesUsed.reference2IntEntrySet().iterator();

      while (var4.hasNext()) {
         Entry<ResourceKey<Recipe<?>>> entry = (Entry<ResourceKey<Recipe<?>>>)var4.next();
         level.recipeAccess().byKey((ResourceKey<Recipe<?>>)entry.getKey()).ifPresent(recipeHolder -> {
            list.add((RecipeHolder<?>)recipeHolder);
            createExperience(level, popVec, entry.getIntValue(), ((AbstractCookingRecipe)recipeHolder.value()).experience());
         });
      }

      return list;
   }

   private static void createExperience(ServerLevel level, Vec3 popVec, int recipeIndex, float experience) {
      int i = Mth.floor(recipeIndex * experience);
      float f = Mth.frac(recipeIndex * experience);
      if (f != 0.0F && level.random.nextFloat() < f) {
         i++;
      }

      ExperienceOrb.award(level, popVec, i);
   }

   @Override
   public void fillStackedContents(StackedItemContents stackedItemContents) {
      for (ItemStack itemStack : this.items) {
         stackedItemContents.accountStack(itemStack);
      }
   }

   @Override
   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      super.preRemoveSideEffects(pos, state);
      if (this.level instanceof ServerLevel serverLevel) {
         this.getRecipesToAwardAndPopExperience(serverLevel, Vec3.atCenterOf(pos));
      }
   }
}
