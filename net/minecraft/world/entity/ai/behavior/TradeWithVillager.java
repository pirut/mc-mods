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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
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

   protected boolean checkExtraStartConditions(ServerLevel p_24416_, Villager p_481200_) {
      return BehaviorUtils.targetIsValid(p_481200_.getBrain(), MemoryModuleType.INTERACTION_TARGET, EntityType.VILLAGER);
   }

   protected boolean canStillUse(ServerLevel p_24419_, Villager p_480922_, long p_24421_) {
      return this.checkExtraStartConditions(p_24419_, p_480922_);
   }

   protected void start(ServerLevel p_24437_, Villager p_478966_, long p_24439_) {
      Villager villager = (Villager)p_478966_.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(p_478966_, villager, 0.5F, 2);
      this.trades = figureOutWhatIAmWillingToTrade(p_478966_, villager);
   }

   protected void tick(ServerLevel p_24445_, Villager p_481190_, long p_24447_) {
      Villager villager = (Villager)p_481190_.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      if (!(p_481190_.distanceToSqr(villager) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(p_481190_, villager, 0.5F, 2);
         p_481190_.gossip(p_24445_, villager, p_24447_);
         boolean flag = p_481190_.getVillagerData().profession().is(VillagerProfession.FARMER);
         if (p_481190_.hasExcessFood() && (flag || villager.wantsMoreFood())) {
            throwHalfStack(p_481190_, Villager.FOOD_POINTS.keySet(), villager);
         }

         if (flag && p_481190_.getInventory().countItem(Items.WHEAT) > Items.WHEAT.getDefaultMaxStackSize() / 2) {
            throwHalfStack(p_481190_, ImmutableSet.of(Items.WHEAT), villager);
         }

         if (!this.trades.isEmpty() && p_481190_.getInventory().hasAnyOf(this.trades)) {
            throwHalfStack(p_481190_, this.trades, villager);
         }
      }
   }

   protected void stop(ServerLevel p_24453_, Villager p_478331_, long p_24455_) {
      p_478331_.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
   }

   private static Set<Item> figureOutWhatIAmWillingToTrade(Villager p_478982_, Villager p_480272_) {
      ImmutableSet<Item> immutableset = p_480272_.getVillagerData().profession().value().requestedItems();
      ImmutableSet<Item> immutableset1 = p_478982_.getVillagerData().profession().value().requestedItems();
      return immutableset.stream().filter(p_24431_ -> !immutableset1.contains(p_24431_)).collect(Collectors.toSet());
   }

   private static void throwHalfStack(Villager p_479452_, Set<Item> p_24427_, LivingEntity p_24428_) {
      SimpleContainer simplecontainer = p_479452_.getInventory();
      ItemStack itemstack = ItemStack.EMPTY;

      for (int i = 0; i < simplecontainer.getContainerSize(); i++) {
         ItemStack itemstack1 = simplecontainer.getItem(i);
         if (!itemstack1.isEmpty()) {
            Item item = itemstack1.getItem();
            if (p_24427_.contains(item)) {
               int j;
               if (itemstack1.getCount() > itemstack1.getMaxStackSize() / 2) {
                  j = itemstack1.getCount() / 2;
               } else {
                  if (itemstack1.getCount() <= 24) {
                     continue;
                  }

                  j = itemstack1.getCount() - 24;
               }

               itemstack1.shrink(j);
               itemstack = new ItemStack(item, j);
               break;
            }
         }
      }

      if (!itemstack.isEmpty()) {
         BehaviorUtils.throwItem(p_479452_, itemstack, p_24428_.position());
      }
   }
}
