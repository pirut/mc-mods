package net.minecraft.world.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface Explosion {
   static DamageSource getDefaultDamageSource(Level p_312716_, @Nullable Entity p_312608_) {
      return p_312716_.damageSources().explosion(p_312608_, getIndirectSourceEntity(p_312608_));
   }

   @Nullable
   static LivingEntity getIndirectSourceEntity(@Nullable Entity p_363745_) {
      return switch (p_363745_) {
         case PrimedTnt primedtnt -> primedtnt.getOwner();
         case LivingEntity livingentity -> livingentity;
         case Projectile projectile when projectile.getOwner() instanceof LivingEntity livingentity1 -> livingentity1;
         case null, default -> null;
      };
   }

   ServerLevel level();

   Explosion.BlockInteraction getBlockInteraction();

   @Nullable
   LivingEntity getIndirectSourceEntity();

   @Nullable
   Entity getDirectSourceEntity();

   float radius();

   Vec3 center();

   boolean canTriggerBlocks();

   boolean shouldAffectBlocklikeEntities();

   public static enum BlockInteraction {
      KEEP(false),
      DESTROY(true),
      DESTROY_WITH_DECAY(true),
      TRIGGER_BLOCK(false);

      private final boolean shouldAffectBlocklikeEntities;

      private BlockInteraction(boolean p_362438_) {
         this.shouldAffectBlocklikeEntities = p_362438_;
      }

      public boolean shouldAffectBlocklikeEntities() {
         return this.shouldAffectBlocklikeEntities;
      }
   }
}
