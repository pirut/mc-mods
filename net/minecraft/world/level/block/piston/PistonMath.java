package net.minecraft.world.level.block.piston;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class PistonMath {
   public static AABB getMovementArea(AABB p_60329_, Direction p_60330_, double p_60331_) {
      double $$3 = p_60331_ * p_60330_.getAxisDirection().getStep();
      double $$4 = Math.min($$3, 0.0);
      double $$5 = Math.max($$3, 0.0);
      switch (p_60330_) {
         case WEST:
            return new AABB(p_60329_.minX + $$4, p_60329_.minY, p_60329_.minZ, p_60329_.minX + $$5, p_60329_.maxY, p_60329_.maxZ);
         case EAST:
            return new AABB(p_60329_.maxX + $$4, p_60329_.minY, p_60329_.minZ, p_60329_.maxX + $$5, p_60329_.maxY, p_60329_.maxZ);
         case DOWN:
            return new AABB(p_60329_.minX, p_60329_.minY + $$4, p_60329_.minZ, p_60329_.maxX, p_60329_.minY + $$5, p_60329_.maxZ);
         case UP:
         default:
            return new AABB(p_60329_.minX, p_60329_.maxY + $$4, p_60329_.minZ, p_60329_.maxX, p_60329_.maxY + $$5, p_60329_.maxZ);
         case NORTH:
            return new AABB(p_60329_.minX, p_60329_.minY, p_60329_.minZ + $$4, p_60329_.maxX, p_60329_.maxY, p_60329_.minZ + $$5);
         case SOUTH:
            return new AABB(p_60329_.minX, p_60329_.minY, p_60329_.maxZ + $$4, p_60329_.maxX, p_60329_.maxY, p_60329_.maxZ + $$5);
      }
   }
}
