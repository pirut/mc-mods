package net.minecraft.world.level.block.entity;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
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

public class CampfireBlockEntity extends BlockEntity implements Clearable {
   private static final int BURN_COOL_SPEED = 2;
   private static final int NUM_SLOTS = 4;
   private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
   private final int[] cookingProgress = new int[4];
   private final int[] cookingTime = new int[4];
   private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> quickCheck = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);

   public CampfireBlockEntity(BlockPos p_155301_, BlockState p_155302_) {
      super(BlockEntityType.CAMPFIRE, p_155301_, p_155302_);
   }

   public static void cookTick(Level p_155307_, BlockPos p_155308_, BlockState p_155309_, CampfireBlockEntity p_155310_) {
      boolean $$4 = false;

      for (int $$5 = 0; $$5 < p_155310_.items.size(); $$5++) {
         ItemStack $$6 = p_155310_.items.get($$5);
         if (!$$6.isEmpty()) {
            $$4 = true;
            p_155310_.cookingProgress[$$5]++;
            if (p_155310_.cookingProgress[$$5] >= p_155310_.cookingTime[$$5]) {
               SingleRecipeInput $$7 = new SingleRecipeInput($$6);
               ItemStack $$8 = p_155310_.quickCheck
                  .getRecipeFor($$7, p_155307_)
                  .map(p_344662_ -> p_344662_.value().assemble($$7, p_155307_.registryAccess()))
                  .orElse($$6);
               if ($$8.isItemEnabled(p_155307_.enabledFeatures())) {
                  Containers.dropItemStack(p_155307_, p_155308_.getX(), p_155308_.getY(), p_155308_.getZ(), $$8);
                  p_155310_.items.set($$5, ItemStack.EMPTY);
                  p_155307_.sendBlockUpdated(p_155308_, p_155309_, p_155309_, 3);
                  p_155307_.gameEvent(GameEvent.BLOCK_CHANGE, p_155308_, GameEvent.Context.of(p_155309_));
               }
            }
         }
      }

      if ($$4) {
         setChanged(p_155307_, p_155308_, p_155309_);
      }
   }

   public static void cooldownTick(Level p_155314_, BlockPos p_155315_, BlockState p_155316_, CampfireBlockEntity p_155317_) {
      boolean $$4 = false;

      for (int $$5 = 0; $$5 < p_155317_.items.size(); $$5++) {
         if (p_155317_.cookingProgress[$$5] > 0) {
            $$4 = true;
            p_155317_.cookingProgress[$$5] = Mth.clamp(p_155317_.cookingProgress[$$5] - 2, 0, p_155317_.cookingTime[$$5]);
         }
      }

      if ($$4) {
         setChanged(p_155314_, p_155315_, p_155316_);
      }
   }

   public static void particleTick(Level p_155319_, BlockPos p_155320_, BlockState p_155321_, CampfireBlockEntity p_155322_) {
      RandomSource $$4 = p_155319_.random;
      if ($$4.nextFloat() < 0.11F) {
         for (int $$5 = 0; $$5 < $$4.nextInt(2) + 2; $$5++) {
            CampfireBlock.makeParticles(p_155319_, p_155320_, p_155321_.getValue(CampfireBlock.SIGNAL_FIRE), false);
         }
      }

      int $$6 = p_155321_.getValue(CampfireBlock.FACING).get2DDataValue();

      for (int $$7 = 0; $$7 < p_155322_.items.size(); $$7++) {
         if (!p_155322_.items.get($$7).isEmpty() && $$4.nextFloat() < 0.2F) {
            Direction $$8 = Direction.from2DDataValue(Math.floorMod($$7 + $$6, 4));
            float $$9 = 0.3125F;
            double $$10 = p_155320_.getX() + 0.5 - $$8.getStepX() * 0.3125F + $$8.getClockWise().getStepX() * 0.3125F;
            double $$11 = p_155320_.getY() + 0.5;
            double $$12 = p_155320_.getZ() + 0.5 - $$8.getStepZ() * 0.3125F + $$8.getClockWise().getStepZ() * 0.3125F;

            for (int $$13 = 0; $$13 < 4; $$13++) {
               p_155319_.addParticle(ParticleTypes.SMOKE, $$10, $$11, $$12, 0.0, 5.0E-4, 0.0);
            }
         }
      }
   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void loadAdditional(CompoundTag p_155312_, HolderLookup.Provider p_323804_) {
      super.loadAdditional(p_155312_, p_323804_);
      this.items.clear();
      ContainerHelper.loadAllItems(p_155312_, this.items, p_323804_);
      if (p_155312_.contains("CookingTimes", 11)) {
         int[] $$2 = p_155312_.getIntArray("CookingTimes");
         System.arraycopy($$2, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, $$2.length));
      }

      if (p_155312_.contains("CookingTotalTimes", 11)) {
         int[] $$3 = p_155312_.getIntArray("CookingTotalTimes");
         System.arraycopy($$3, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, $$3.length));
      }
   }

   @Override
   protected void saveAdditional(CompoundTag p_187486_, HolderLookup.Provider p_324005_) {
      super.saveAdditional(p_187486_, p_324005_);
      ContainerHelper.saveAllItems(p_187486_, this.items, true, p_324005_);
      p_187486_.putIntArray("CookingTimes", this.cookingProgress);
      p_187486_.putIntArray("CookingTotalTimes", this.cookingTime);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_324612_) {
      CompoundTag $$1 = new CompoundTag();
      ContainerHelper.saveAllItems($$1, this.items, true, p_324612_);
      return $$1;
   }

   public Optional<RecipeHolder<CampfireCookingRecipe>> getCookableRecipe(ItemStack p_59052_) {
      return this.items.stream().noneMatch(ItemStack::isEmpty) ? Optional.empty() : this.quickCheck.getRecipeFor(new SingleRecipeInput(p_59052_), this.level);
   }

   public boolean placeFood(@Nullable LivingEntity p_347582_, ItemStack p_238286_, int p_238287_) {
      for (int $$3 = 0; $$3 < this.items.size(); $$3++) {
         ItemStack $$4 = this.items.get($$3);
         if ($$4.isEmpty()) {
            this.cookingTime[$$3] = p_238287_;
            this.cookingProgress[$$3] = 0;
            this.items.set($$3, p_238286_.consumeAndReturn(1, p_347582_));
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(p_347582_, this.getBlockState()));
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

   public void dowse() {
      if (this.level != null) {
         this.markUpdated();
      }
   }

   @Override
   protected void applyImplicitComponents(BlockEntity.DataComponentInput p_338534_) {
      super.applyImplicitComponents(p_338534_);
      p_338534_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_338620_) {
      super.collectImplicitComponents(p_338620_);
      p_338620_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
   }

   @Override
   public void removeComponentsFromTag(CompoundTag p_332690_) {
      p_332690_.remove("Items");
   }
}
