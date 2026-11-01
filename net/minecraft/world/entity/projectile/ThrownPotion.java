package net.minecraft.world.entity.projectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownPotion extends ThrowableItemProjectile implements ItemSupplier {
   public static final double SPLASH_RANGE = 4.0;
   private static final double SPLASH_RANGE_SQ = 16.0;
   public static final Predicate<LivingEntity> WATER_SENSITIVE_OR_ON_FIRE = p_350140_ -> p_350140_.isSensitiveToWater() || p_350140_.isOnFire();

   public ThrownPotion(EntityType<? extends ThrownPotion> p_37527_, Level p_37528_) {
      super(p_37527_, p_37528_);
   }

   public ThrownPotion(Level p_37535_, LivingEntity p_37536_) {
      super(EntityType.POTION, p_37536_, p_37535_);
   }

   public ThrownPotion(Level p_37530_, double p_37531_, double p_37532_, double p_37533_) {
      super(EntityType.POTION, p_37531_, p_37532_, p_37533_, p_37530_);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SPLASH_POTION;
   }

   @Override
   protected double getDefaultGravity() {
      return 0.05;
   }

   @Override
   protected void onHitBlock(BlockHitResult p_37541_) {
      super.onHitBlock(p_37541_);
      if (!this.level().isClientSide) {
         ItemStack $$1 = this.getItem();
         Direction $$2 = p_37541_.getDirection();
         BlockPos $$3 = p_37541_.getBlockPos();
         BlockPos $$4 = $$3.relative($$2);
         PotionContents $$5 = $$1.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if ($$5.is(Potions.WATER)) {
            this.dowseFire($$4);
            this.dowseFire($$4.relative($$2.getOpposite()));

            for (Direction $$6 : Direction.Plane.HORIZONTAL) {
               this.dowseFire($$4.relative($$6));
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult p_37543_) {
      super.onHit(p_37543_);
      if (!this.level().isClientSide) {
         ItemStack $$1 = this.getItem();
         PotionContents $$2 = $$1.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if ($$2.is(Potions.WATER)) {
            this.applyWater();
         } else if ($$2.hasEffects()) {
            if (this.isLingering()) {
               this.makeAreaOfEffectCloud($$2);
            } else {
               this.applySplash($$2.getAllEffects(), p_37543_.getType() == HitResult.Type.ENTITY ? ((EntityHitResult)p_37543_).getEntity() : null);
            }
         }

         int $$3 = $$2.potion().isPresent() && $$2.potion().get().value().hasInstantEffects() ? 2007 : 2002;
         this.level().levelEvent($$3, this.blockPosition(), $$2.getColor());
         this.discard();
      }
   }

   private void applyWater() {
      AABB $$0 = this.getBoundingBox().inflate(4.0, 2.0, 4.0);

      for (LivingEntity $$2 : this.level().getEntitiesOfClass(LivingEntity.class, $$0, WATER_SENSITIVE_OR_ON_FIRE)) {
         double $$3 = this.distanceToSqr($$2);
         if ($$3 < 16.0) {
            if ($$2.isSensitiveToWater()) {
               $$2.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
            }

            if ($$2.isOnFire() && $$2.isAlive()) {
               $$2.extinguishFire();
            }
         }
      }

      for (Axolotl $$5 : this.level().getEntitiesOfClass(Axolotl.class, $$0)) {
         $$5.rehydrate();
      }
   }

   private void applySplash(Iterable<MobEffectInstance> p_330815_, @Nullable Entity p_37549_) {
      AABB $$2 = this.getBoundingBox().inflate(4.0, 2.0, 4.0);
      List<LivingEntity> $$3 = this.level().getEntitiesOfClass(LivingEntity.class, $$2);
      if (!$$3.isEmpty()) {
         Entity $$4 = this.getEffectSource();

         for (LivingEntity $$5 : $$3) {
            if ($$5.isAffectedByPotions()) {
               double $$6 = this.distanceToSqr($$5);
               if ($$6 < 16.0) {
                  double $$7;
                  if ($$5 == p_37549_) {
                     $$7 = 1.0;
                  } else {
                     $$7 = 1.0 - Math.sqrt($$6) / 4.0;
                  }

                  for (MobEffectInstance $$9 : p_330815_) {
                     Holder<MobEffect> $$10 = $$9.getEffect();
                     if ($$10.value().isInstantenous()) {
                        $$10.value().applyInstantenousEffect(this, this.getOwner(), $$5, $$9.getAmplifier(), $$7);
                     } else {
                        int $$11 = $$9.mapDuration(p_267930_ -> (int)($$7 * p_267930_ + 0.5));
                        MobEffectInstance $$12 = new MobEffectInstance($$10, $$11, $$9.getAmplifier(), $$9.isAmbient(), $$9.isVisible());
                        if (!$$12.endsWithin(20)) {
                           $$5.addEffect($$12, $$4);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void makeAreaOfEffectCloud(PotionContents p_332124_) {
      AreaEffectCloud $$1 = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
      if (this.getOwner() instanceof LivingEntity $$2) {
         $$1.setOwner($$2);
      }

      $$1.setRadius(3.0F);
      $$1.setRadiusOnUse(-0.5F);
      $$1.setWaitTime(10);
      $$1.setRadiusPerTick(-$$1.getRadius() / $$1.getDuration());
      $$1.setPotionContents(p_332124_);
      this.level().addFreshEntity($$1);
   }

   private boolean isLingering() {
      return this.getItem().is(Items.LINGERING_POTION);
   }

   private void dowseFire(BlockPos p_150193_) {
      BlockState $$1 = this.level().getBlockState(p_150193_);
      if ($$1.is(BlockTags.FIRE)) {
         this.level().destroyBlock(p_150193_, false, this);
      } else if (AbstractCandleBlock.isLit($$1)) {
         AbstractCandleBlock.extinguish(null, $$1, this.level(), p_150193_);
      } else if (CampfireBlock.isLitCampfire($$1)) {
         this.level().levelEvent(null, 1009, p_150193_, 0);
         CampfireBlock.dowse(this.getOwner(), this.level(), p_150193_, $$1);
         this.level().setBlockAndUpdate(p_150193_, $$1.setValue(CampfireBlock.LIT, false));
      }
   }

   @Override
   public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity p_345103_, DamageSource p_345887_) {
      double $$2 = p_345103_.position().x - this.position().x;
      double $$3 = p_345103_.position().z - this.position().z;
      return DoubleDoubleImmutablePair.of($$2, $$3);
   }
}
