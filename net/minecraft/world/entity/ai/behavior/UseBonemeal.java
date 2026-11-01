package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class UseBonemeal extends Behavior<Villager> {
   private static final int BONEMEALING_DURATION = 80;
   private long nextWorkCycleTime;
   private long lastBonemealingSession;
   private int timeWorkedSoFar;
   private Optional<BlockPos> cropPos = Optional.empty();

   public UseBonemeal() {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
   }

   protected boolean checkExtraStartConditions(ServerLevel p_24474_, Villager p_24475_) {
      if (p_24475_.tickCount % 10 == 0 && (this.lastBonemealingSession == 0L || this.lastBonemealingSession + 160L <= p_24475_.tickCount)) {
         if (p_24475_.getInventory().countItem(Items.BONE_MEAL) <= 0) {
            return false;
         } else {
            this.cropPos = this.pickNextTarget(p_24474_, p_24475_);
            return this.cropPos.isPresent();
         }
      } else {
         return false;
      }
   }

   protected boolean canStillUse(ServerLevel p_24477_, Villager p_24478_, long p_24479_) {
      return this.timeWorkedSoFar < 80 && this.cropPos.isPresent();
   }

   private Optional<BlockPos> pickNextTarget(ServerLevel p_24493_, Villager p_24494_) {
      BlockPos.MutableBlockPos $$2 = new BlockPos.MutableBlockPos();
      Optional<BlockPos> $$3 = Optional.empty();
      int $$4 = 0;

      for (int $$5 = -1; $$5 <= 1; $$5++) {
         for (int $$6 = -1; $$6 <= 1; $$6++) {
            for (int $$7 = -1; $$7 <= 1; $$7++) {
               $$2.setWithOffset(p_24494_.blockPosition(), $$5, $$6, $$7);
               if (this.validPos($$2, p_24493_)) {
                  if (p_24493_.random.nextInt(++$$4) == 0) {
                     $$3 = Optional.of($$2.immutable());
                  }
               }
            }
         }
      }

      return $$3;
   }

   private boolean validPos(BlockPos p_24486_, ServerLevel p_24487_) {
      BlockState $$2 = p_24487_.getBlockState(p_24486_);
      Block $$3 = $$2.getBlock();
      return $$3 instanceof CropBlock && !((CropBlock)$$3).isMaxAge($$2);
   }

   protected void start(ServerLevel p_24496_, Villager p_24497_, long p_24498_) {
      this.setCurrentCropAsTarget(p_24497_);
      p_24497_.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE_MEAL));
      this.nextWorkCycleTime = p_24498_;
      this.timeWorkedSoFar = 0;
   }

   private void setCurrentCropAsTarget(Villager p_24481_) {
      this.cropPos.ifPresent(p_24484_ -> {
         BlockPosTracker $$2 = new BlockPosTracker(p_24484_);
         p_24481_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, $$2);
         p_24481_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget($$2, 0.5F, 1));
      });
   }

   protected void stop(ServerLevel p_24504_, Villager p_24505_, long p_24506_) {
      p_24505_.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      this.lastBonemealingSession = p_24505_.tickCount;
   }

   protected void tick(ServerLevel p_24512_, Villager p_24513_, long p_24514_) {
      BlockPos $$3 = this.cropPos.get();
      if (p_24514_ >= this.nextWorkCycleTime && $$3.closerToCenterThan(p_24513_.position(), 1.0)) {
         ItemStack $$4 = ItemStack.EMPTY;
         SimpleContainer $$5 = p_24513_.getInventory();
         int $$6 = $$5.getContainerSize();

         for (int $$7 = 0; $$7 < $$6; $$7++) {
            ItemStack $$8 = $$5.getItem($$7);
            if ($$8.is(Items.BONE_MEAL)) {
               $$4 = $$8;
               break;
            }
         }

         if (!$$4.isEmpty() && BoneMealItem.growCrop($$4, p_24512_, $$3)) {
            p_24512_.levelEvent(1505, $$3, 15);
            this.cropPos = this.pickNextTarget(p_24512_, p_24513_);
            this.setCurrentCropAsTarget(p_24513_);
            this.nextWorkCycleTime = p_24514_ + 40L;
         }

         this.timeWorkedSoFar++;
      }
   }
}
