package net.neoforged.neoforge.event.entity;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.ICancellableEvent;

public class ProjectileImpactEvent extends EntityEvent implements ICancellableEvent {
   private final HitResult ray;
   private final Projectile projectile;

   public ProjectileImpactEvent(Projectile projectile, HitResult ray) {
      super(projectile);
      this.ray = ray;
      this.projectile = projectile;
   }

   public HitResult getRayTraceResult() {
      return this.ray;
   }

   public Projectile getProjectile() {
      return this.projectile;
   }
}
