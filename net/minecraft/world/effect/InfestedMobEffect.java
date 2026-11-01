package net.minecraft.world.effect;

import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

class InfestedMobEffect extends MobEffect {
   private final float chanceToSpawn;
   private final ToIntFunction<RandomSource> spawnedCount;

   protected InfestedMobEffect(MobEffectCategory p_338776_, int p_338484_, float p_338378_, ToIntFunction<RandomSource> p_338750_) {
      super(p_338776_, p_338484_, ParticleTypes.INFESTED);
      this.chanceToSpawn = p_338378_;
      this.spawnedCount = p_338750_;
   }

   @Override
   public void onMobHurt(LivingEntity p_338481_, int p_338438_, DamageSource p_338894_, float p_338367_) {
      if (p_338481_.getRandom().nextFloat() <= this.chanceToSpawn) {
         int $$4 = this.spawnedCount.applyAsInt(p_338481_.getRandom());

         for (int $$5 = 0; $$5 < $$4; $$5++) {
            this.spawnSilverfish(p_338481_.level(), p_338481_, p_338481_.getX(), p_338481_.getY() + p_338481_.getBbHeight() / 2.0, p_338481_.getZ());
         }
      }
   }

   private void spawnSilverfish(Level p_338804_, LivingEntity p_341023_, double p_338557_, double p_338848_, double p_338562_) {
      Silverfish $$5 = EntityType.SILVERFISH.create(p_338804_);
      if ($$5 != null) {
         RandomSource $$6 = p_341023_.getRandom();
         float $$7 = (float) (Math.PI / 2);
         float $$8 = Mth.randomBetween($$6, (float) (-Math.PI / 2), (float) (Math.PI / 2));
         Vector3f $$9 = p_341023_.getLookAngle().toVector3f().mul(0.3F).mul(1.0F, 1.5F, 1.0F).rotateY($$8);
         $$5.moveTo(p_338557_, p_338848_, p_338562_, p_338804_.getRandom().nextFloat() * 360.0F, 0.0F);
         $$5.setDeltaMovement(new Vec3($$9));
         p_338804_.addFreshEntity($$5);
         $$5.playSound(SoundEvents.SILVERFISH_HURT);
      }
   }
}
