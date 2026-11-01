package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.extensions.IDispensibleContainerItemExtension;

public interface DispensibleContainerItem extends IDispensibleContainerItemExtension {
   default void checkExtraContent(@Nullable Player p_150817_, Level p_150818_, ItemStack p_150819_, BlockPos p_150820_) {
   }

   @Deprecated
   boolean emptyContents(@Nullable Player var1, Level var2, BlockPos var3, @Nullable BlockHitResult var4);
}
