package net.neoforged.neoforge.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public interface IRenderableSection {
   BlockPos getRenderOrigin();

   AABB getBoundingBox();

   boolean isEmpty();
}
