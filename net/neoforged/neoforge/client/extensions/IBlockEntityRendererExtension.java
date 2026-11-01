package net.neoforged.neoforge.client.extensions;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public interface IBlockEntityRendererExtension<T extends BlockEntity> {
   default AABB getRenderBoundingBox(T blockEntity) {
      return new AABB(blockEntity.getBlockPos());
   }
}
