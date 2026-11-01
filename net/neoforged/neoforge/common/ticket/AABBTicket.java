package net.neoforged.neoforge.common.ticket;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AABBTicket extends SimpleTicket<Vec3> {
   public final AABB axisAlignedBB;

   public AABBTicket(AABB axisAlignedBB) {
      this.axisAlignedBB = axisAlignedBB;
   }

   public boolean matches(Vec3 toMatch) {
      return this.axisAlignedBB.contains(toMatch);
   }
}
