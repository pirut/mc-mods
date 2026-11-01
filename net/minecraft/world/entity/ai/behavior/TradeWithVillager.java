package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TradeWithVillager extends Behavior<Villager> {
   private Set<Item> trades = ImmutableSet.of();

   public TradeWithVillager() {
      super(
         ImmutableMap.of(
            MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
         )
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_24416_, Villager p_24417_) {
      return BehaviorUtils.targetIsValid(p_24417_.getBrain(), MemoryModuleType.INTERACTION_TARGET, EntityType.VILLAGER);
   }

   protected boolean canStillUse(ServerLevel p_24419_, Villager p_24420_, long p_24421_) {
      return this.checkExtraStartConditions(p_24419_, p_24420_);
   }

   protected void start(ServerLevel p_24437_, Villager p_24438_, long p_24439_) {
      Villager $$3 = (Villager)p_24438_.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(p_24438_, $$3, 0.5F, 2);
      this.trades = figureOutWhatIAmWillingToTrade(p_24438_, $$3);
   }

   protected void tick(ServerLevel p_24445_, Villager p_24446_, long p_24447_) {
      Villager $$3 = (Villager)p_24446_.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      if (!(p_24446_.distanceToSqr($$3) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(p_24446_, $$3, 0.5F, 2);
         p_24446_.gossip(p_24445_, $$3, p_24447_);
         if (p_24446_.hasExcessFood() && (p_24446_.getVillagerData().getProfession() == VillagerProfession.FARMER || $$3.wantsMoreFood())) {
            throwHalfStack(p_24446_, Villager.FOOD_POINTS.keySet(), $$3);
         }

         if ($$3.getVillagerData().getProfession() == VillagerProfession.FARMER
            && p_24446_.getInventory().countItem(Items.WHEAT) > Items.WHEAT.getDefaultMaxStackSize() / 2) {
            throwHalfStack(p_24446_, ImmutableSet.of(Items.WHEAT), $$3);
         }

         if (!this.trades.isEmpty() && p_24446_.getInventory().hasAnyOf(this.trades)) {
            throwHalfStack(p_24446_, this.trades, $$3);
         }
      }
   }

   protected void stop(ServerLevel p_24453_, Villager p_24454_, long p_24455_) {
      p_24454_.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
   }

   private static Set<Item> figureOutWhatIAmWillingToTrade(Villager p_24423_, Villager p_24424_) {
      ImmutableSet<Item> $$2 = p_24424_.getVillagerData().getProfession().requestedItems();
      ImmutableSet<Item> $$3 = p_24423_.getVillagerData().getProfession().requestedItems();
      return $$2.stream().filter(p_24431_ -> !$$3.contains(p_24431_)).collect(Collectors.toSet());
   }

   private static void throwHalfStack(Villager p_24426_, Set<Item> p_24427_, LivingEntity p_24428_) {
      SimpleContainer $$3 = p_24426_.getInventory();
      ItemStack $$4 = ItemStack.EMPTY;
      int $$5 = 0;

      while ($$5 < $$3.getContainerSize()) {
         ItemStack $$6;
         Item $$7;
         int $$8;
         label28: {
            $$6 = $$3.getItem($$5);
            if (!$$6.isEmpty()) {
               $$7 = $$6.getItem();
               if (p_24427_.contains($$7)) {
                  if ($$6.getCount() > $$6.getMaxStackSize() / 2) {
                     $$8 = $$6.getCount() / 2;
                     break label28;
                  }

                  if ($$6.getCount() > 24) {
                     $$8 = $$6.getCount() - 24;
                     break label28;
                  }
               }
            }

            $$5++;
            continue;
         }

         $$6.shrink($$8);
         $$4 = new ItemStack($$7, $$8);
         break;
      }

      if (!$$4.isEmpty()) {
         BehaviorUtils.throwItem(p_24426_, $$4, p_24428_.position());
      }
   }
}
