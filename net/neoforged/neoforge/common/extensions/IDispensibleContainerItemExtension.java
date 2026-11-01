package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public interface IDispensibleContainerItemExtension {
   private DispensibleContainerItem self() {
      return (DispensibleContainerItem)this;
   }

   default boolean emptyContents(@Nullable LivingEntity entity, Level level, BlockPos pos, @Nullable BlockHitResult hitResult, @Nullable ItemStack container) {
      return this.self().emptyContents(entity, level, pos, hitResult);
   }
}
