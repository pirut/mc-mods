package net.neoforged.neoforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

public interface SpecialPlantable {
   boolean canPlacePlantAtPosition(ItemStack var1, LevelReader var2, BlockPos var3, @Nullable Direction var4);

   void spawnPlantAtPosition(ItemStack var1, LevelAccessor var2, BlockPos var3, @Nullable Direction var4);

   default boolean villagerCanPlantItem(Villager villager) {
      return false;
   }
}
