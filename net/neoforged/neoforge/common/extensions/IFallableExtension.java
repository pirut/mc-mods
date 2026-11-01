package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

public interface IFallableExtension {
   default void fallingTick(Level level, BlockPos currentPosition, FallingBlockEntity entity) {
   }
}
