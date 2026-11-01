package net.neoforged.neoforge.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface IBlockCapabilityProvider<T, C> {
   @Nullable
   T getCapability(Level var1, BlockPos var2, BlockState var3, @Nullable BlockEntity var4, C var5);
}
