package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class SmallFireball extends Fireball {
   public SmallFireball(EntityType<? extends SmallFireball> p_480456_, Level p_481000_) {
      super(p_480456_, p_481000_);
   }

   public SmallFireball(Level p_480097_, LivingEntity p_479209_, Vec3 p_480455_) {
      super(EntityType.SMALL_FIREBALL, p_479209_, p_480455_, p_480097_);
   }

   public SmallFireball(Level p_480264_, double p_478814_, double p_481547_, double p_479775_, Vec3 p_480553_) {
      super(EntityType.SMALL_FIREBALL, p_478814_, p_481547_, p_479775_, p_480553_, p_480264_);
   }

   @Override
   protected void onHitEntity(EntityHitResult p_480875_) {
      super.onHitEntity(p_480875_);
      if (this.level() instanceof ServerLevel serverlevel) {
         Entity entity1 = p_480875_.getEntity();
         Entity $$4 = this.getOwner();
         int $$5 = entity1.getRemainingFireTicks();
         entity1.igniteForSeconds(5.0F);
         DamageSource $$6 = this.damageSources().fireball(this, $$4);
         if (!entity1.hurtServer(serverlevel, $$6, 5.0F)) {
            entity1.setRemainingFireTicks($$5);
         } else {
            EnchantmentHelper.doPostAttackEffects(serverlevel, entity1, $$6);
         }
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult p_479052_) {
      super.onHitBlock(p_479052_);
      if (this.level() instanceof ServerLevel serverlevel) {
         Entity entity = this.getOwner();
         if (!(entity instanceof Mob) || EventHooks.canEntityGrief(serverlevel, entity)) {
            BlockPos blockpos = p_479052_.getBlockPos().relative(p_479052_.getDirection());
            if (this.level().isEmptyBlock(blockpos)) {
               this.level().setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level(), blockpos));
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult p_481180_) {
      super.onHit(p_481180_);
      if (!this.level().isClientSide()) {
         this.discard();
      }
   }
}
