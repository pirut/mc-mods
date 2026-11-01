package net.neoforged.neoforge.event.level;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

public class ExplosionKnockbackEvent extends ExplosionEvent {
   private final Entity entity;
   private final List<BlockPos> blockList;
   private Vec3 knockbackVelocity;

   public ExplosionKnockbackEvent(Level level, ServerExplosion explosion, Entity entity, Vec3 knockbackVelocity, List<BlockPos> blockList) {
      super(level, explosion);
      this.entity = entity;
      this.blockList = blockList;
      this.knockbackVelocity = knockbackVelocity;
   }

   public List<BlockPos> getAffectedBlocks() {
      return this.blockList;
   }

   public Entity getAffectedEntity() {
      return this.entity;
   }

   public Vec3 getKnockbackVelocity() {
      return this.knockbackVelocity;
   }

   public void setKnockbackVelocity(Vec3 newKnockbackVelocity) {
      this.knockbackVelocity = newKnockbackVelocity;
   }
}
