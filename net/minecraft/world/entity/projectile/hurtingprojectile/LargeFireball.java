package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class LargeFireball extends Fireball {
   private static final byte DEFAULT_EXPLOSION_POWER = 1;
   private int explosionPower = 1;

   public LargeFireball(EntityType<? extends LargeFireball> p_479336_, Level p_481109_) {
      super(p_479336_, p_481109_);
   }

   public LargeFireball(Level p_482104_, LivingEntity p_481713_, Vec3 p_479389_, int p_480142_) {
      super(EntityType.FIREBALL, p_481713_, p_479389_, p_482104_);
      this.explosionPower = p_480142_;
   }

   @Override
   protected void onHit(HitResult p_479612_) {
      super.onHit(p_479612_);
      if (this.level() instanceof ServerLevel serverlevel) {
         boolean flag = EventHooks.canEntityGrief(serverlevel, this.getOwner());
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionPower, flag, Level.ExplosionInteraction.MOB);
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult p_480445_) {
      super.onHitEntity(p_480445_);
      if (this.level() instanceof ServerLevel serverlevel) {
         Entity entity1 = p_480445_.getEntity();
         Entity $$4 = this.getOwner();
         DamageSource $$5 = this.damageSources().fireball(this, $$4);
         entity1.hurtServer(serverlevel, $$5, 6.0F);
         EnchantmentHelper.doPostAttackEffects(serverlevel, entity1, $$5);
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_481953_) {
      super.addAdditionalSaveData(p_481953_);
      p_481953_.putByte("ExplosionPower", (byte)this.explosionPower);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_479076_) {
      super.readAdditionalSaveData(p_479076_);
      this.explosionPower = p_479076_.getByteOr("ExplosionPower", (byte)1);
   }
}
