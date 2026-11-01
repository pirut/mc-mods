package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CampfireBlockEntity extends BlockEntity implements Clearable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int BURN_COOL_SPEED = 2;
   private static final int NUM_SLOTS = 4;
   private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
   private final int[] cookingProgress = new int[4];
   private final int[] cookingTime = new int[4];

   public CampfireBlockEntity(BlockPos pos, BlockState blockState) {
      super(BlockEntityType.CAMPFIRE, pos, blockState);
   }

   public static void cookTick(
      ServerLevel level,
      BlockPos pos,
      BlockState state,
      CampfireBlockEntity campfire,
      RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> check
   ) {
      boolean bl = false;

      for (int i = 0; i < campfire.items.size(); i++) {
         ItemStack itemStack = campfire.items.get(i);
         if (!itemStack.isEmpty()) {
            bl = true;
            campfire.cookingProgress[i]++;
            if (campfire.cookingProgress[i] >= campfire.cookingTime[i]) {
               SingleRecipeInput singleRecipeInput = new SingleRecipeInput(itemStack);
               ItemStack itemStack2 = check.getRecipeFor(singleRecipeInput, level)
                  .map(recipeHolder -> recipeHolder.value().assemble(singleRecipeInput, level.registryAccess()))
                  .orElse(itemStack);
               if (itemStack2.isItemEnabled(level.enabledFeatures())) {
                  Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack2);
                  campfire.items.set(i, ItemStack.EMPTY);
                  level.sendBlockUpdated(pos, state, state, 3);
                  level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
               }
            }
         }
      }

      if (bl) {
         setChanged(level, pos, state);
      }
   }

   public static void cooldownTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity) {
      boolean bl = false;

      for (int i = 0; i < blockEntity.items.size(); i++) {
         if (blockEntity.cookingProgress[i] > 0) {
            bl = true;
            blockEntity.cookingProgress[i] = Mth.clamp(blockEntity.cookingProgress[i] - 2, 0, blockEntity.cookingTime[i]);
         }
      }

      if (bl) {
         setChanged(level, pos, state);
      }
   }

   public static void particleTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity) {
      RandomSource randomSource = level.random;
      if (randomSource.nextFloat() < 0.11F) {
         for (int i = 0; i < randomSource.nextInt(2) + 2; i++) {
            CampfireBlock.makeParticles(level, pos, state.getValue(CampfireBlock.SIGNAL_FIRE), false);
         }
      }

      int i = state.getValue(CampfireBlock.FACING).get2DDataValue();

      for (int j = 0; j < blockEntity.items.size(); j++) {
         if (!blockEntity.items.get(j).isEmpty() && randomSource.nextFloat() < 0.2F) {
            Direction direction = Direction.from2DDataValue(Math.floorMod(j + i, 4));
            float f = 0.3125F;
            double d = pos.getX() + 0.5 - direction.getStepX() * 0.3125F + direction.getClockWise().getStepX() * 0.3125F;
            double e = pos.getY() + 0.5;
            double g = pos.getZ() + 0.5 - direction.getStepZ() * 0.3125F + direction.getClockWise().getStepZ() * 0.3125F;

            for (int k = 0; k < 4; k++) {
               level.addParticle(ParticleTypes.SMOKE, d, e, g, 0.0, 5.0E-4, 0.0);
            }
         }
      }
   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.items.clear();
      ContainerHelper.loadAllItems(input, this.items);
      input.getIntArray("CookingTimes")
         .ifPresentOrElse(
            is -> System.arraycopy(is, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, is.length)), () -> Arrays.fill(this.cookingProgress, 0)
         );
      input.getIntArray("CookingTotalTimes")
         .ifPresentOrElse(
            is -> System.arraycopy(is, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, is.length)), () -> Arrays.fill(this.cookingTime, 0)
         );
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, this.items, true);
      output.putIntArray("CookingTimes", this.cookingProgress);
      output.putIntArray("CookingTotalTimes", this.cookingTime);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      CompoundTag var4;
      try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector, registries);
         ContainerHelper.saveAllItems(tagValueOutput, this.items, true);
         var4 = tagValueOutput.buildResult();
      }

      return var4;
   }

   public boolean placeFood(ServerLevel level, @Nullable LivingEntity entity, ItemStack stack) {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemStack = this.items.get(i);
         if (itemStack.isEmpty()) {
            Optional<RecipeHolder<CampfireCookingRecipe>> optional = level.recipeAccess()
               .getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack), level);
            if (optional.isEmpty()) {
               return false;
            }

            this.cookingTime[i] = optional.get().value().cookingTime();
            this.cookingProgress[i] = 0;
            this.items.set(i, stack.consumeAndReturn(1, entity));
            level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(entity, this.getBlockState()));
            this.markUpdated();
            return true;
         }
      }

      return false;
   }

   private void markUpdated() {
      this.setChanged();
      this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
   }

   @Override
   public void clearContent() {
      this.items.clear();
   }

   @Override
   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      if (this.level != null) {
         Containers.dropContents(this.level, pos, this.getItems());
      }
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter componentGetter) {
      super.applyImplicitComponents(componentGetter);
      componentGetter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder components) {
      super.collectImplicitComponents(components);
      components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
   }

   @Override
   public void removeComponentsFromTag(ValueOutput output) {
      output.discard("Items");
   }
}
