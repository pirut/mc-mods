package net.minecraft.world.entity.projectile.arrow;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SpectralArrow extends AbstractArrow {
   private static final int DEFAULT_DURATION = 200;
   private int duration = 200;

   public SpectralArrow(EntityType<? extends SpectralArrow> p_481388_, Level p_481664_) {
      super(p_481388_, p_481664_);
   }

   public SpectralArrow(Level p_480218_, LivingEntity p_481846_, ItemStack p_481746_, @Nullable ItemStack p_482111_) {
      super(EntityType.SPECTRAL_ARROW, p_481846_, p_480218_, p_481746_, p_482111_);
   }

   public SpectralArrow(Level p_478695_, double p_482090_, double p_481116_, double p_477923_, ItemStack p_479163_, @Nullable ItemStack p_478832_) {
      super(EntityType.SPECTRAL_ARROW, p_482090_, p_481116_, p_477923_, p_478695_, p_479163_, p_478832_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide() && !this.isInGround()) {
         this.level().addParticle(SpellParticleOption.create(ParticleTypes.EFFECT, -1, 1.0F), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void doPostHurtEffects(LivingEntity p_482015_) {
      super.doPostHurtEffects(p_482015_);
      MobEffectInstance mobeffectinstance = new MobEffectInstance(MobEffects.GLOWING, this.duration, 0);
      p_482015_.addEffect(mobeffectinstance, this.getEffectSource());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_478652_) {
      super.readAdditionalSaveData(p_478652_);
      this.duration = p_478652_.getIntOr("Duration", 200);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_481701_) {
      super.addAdditionalSaveData(p_481701_);
      p_481701_.putInt("Duration", this.duration);
   }

   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(Items.SPECTRAL_ARROW);
   }
}
