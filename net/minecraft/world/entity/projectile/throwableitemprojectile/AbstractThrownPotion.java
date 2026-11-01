package net.minecraft.world.entity.projectile.throwableitemprojectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public abstract class AbstractThrownPotion extends ThrowableItemProjectile {
   public static final double SPLASH_RANGE = 4.0;
   protected static final double SPLASH_RANGE_SQ = 16.0;
   public static final Predicate<LivingEntity> WATER_SENSITIVE_OR_ON_FIRE = p_479103_ -> p_479103_.isSensitiveToWater() || p_479103_.isOnFire();

   public AbstractThrownPotion(EntityType<? extends AbstractThrownPotion> p_478303_, Level p_478669_) {
      super(p_478303_, p_478669_);
   }

   public AbstractThrownPotion(EntityType<? extends AbstractThrownPotion> p_481795_, Level p_481703_, LivingEntity p_481903_, ItemStack p_480761_) {
      super(p_481795_, p_481903_, p_481703_, p_480761_);
   }

   public AbstractThrownPotion(
      EntityType<? extends AbstractThrownPotion> p_479873_, Level p_482036_, double p_477914_, double p_478995_, double p_482136_, ItemStack p_477930_
   ) {
      super(p_479873_, p_477914_, p_478995_, p_482136_, p_482036_, p_477930_);
   }

   @Override
   protected double getDefaultGravity() {
      return 0.05;
   }

   @Override
   protected void onHitBlock(BlockHitResult p_480715_) {
      super.onHitBlock(p_480715_);
      if (!this.level().isClientSide()) {
         ItemStack itemstack = this.getItem();
         Direction direction = p_480715_.getDirection();
         BlockPos blockpos = p_480715_.getBlockPos();
         BlockPos blockpos1 = blockpos.relative(direction);
         PotionContents potioncontents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if (potioncontents.is(Potions.WATER)) {
            this.dowseFire(blockpos1);
            this.dowseFire(blockpos1.relative(direction.getOpposite()));

            for (Direction direction1 : Direction.Plane.HORIZONTAL) {
               this.dowseFire(blockpos1.relative(direction1));
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult p_478492_) {
      super.onHit(p_478492_);
      if (this.level() instanceof ServerLevel serverlevel) {
         ItemStack itemstack = this.getItem();
         PotionContents potioncontents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if (potioncontents.is(Potions.WATER)) {
            this.onHitAsWater(serverlevel);
         } else if (potioncontents.hasEffects()) {
            this.onHitAsPotion(serverlevel, itemstack, p_478492_);
         }

         int i = potioncontents.potion().isPresent() && potioncontents.potion().get().value().hasInstantEffects() ? 2007 : 2002;
         serverlevel.levelEvent(i, this.blockPosition(), potioncontents.getColor());
         this.discard();
      }
   }

   private void onHitAsWater(ServerLevel p_478684_) {
      AABB aabb = this.getBoundingBox().inflate(4.0, 2.0, 4.0);

      for (LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, aabb, WATER_SENSITIVE_OR_ON_FIRE)) {
         double d0 = this.distanceToSqr(livingentity);
         if (d0 < 16.0) {
            if (livingentity.isSensitiveToWater()) {
               livingentity.hurtServer(p_478684_, this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
            }

            if (livingentity.isOnFire() && livingentity.isAlive()) {
               livingentity.extinguishFire();
            }
         }
      }

      for (Axolotl axolotl : this.level().getEntitiesOfClass(Axolotl.class, aabb)) {
         axolotl.rehydrate();
      }
   }

   protected abstract void onHitAsPotion(ServerLevel var1, ItemStack var2, HitResult var3);

   private void dowseFire(BlockPos p_480233_) {
      BlockState blockstate = this.level().getBlockState(p_480233_);
      if (blockstate.is(BlockTags.FIRE)) {
         this.level().destroyBlock(p_480233_, false, this);
      } else if (AbstractCandleBlock.isLit(blockstate)) {
         AbstractCandleBlock.extinguish(null, blockstate, this.level(), p_480233_);
      } else if (CampfireBlock.isLitCampfire(blockstate)) {
         this.level().levelEvent(null, 1009, p_480233_, 0);
         CampfireBlock.dowse(this.getOwner(), this.level(), p_480233_, blockstate);
         this.level().setBlockAndUpdate(p_480233_, blockstate.setValue(CampfireBlock.LIT, false));
      }
   }

   @Override
   public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity p_481495_, DamageSource p_478829_) {
      double d0 = p_481495_.position().x - this.position().x;
      double d1 = p_481495_.position().z - this.position().z;
      return DoubleDoubleImmutablePair.of(d0, d1);
   }
}
