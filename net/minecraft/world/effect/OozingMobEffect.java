package net.minecraft.world.effect;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

class OozingMobEffect extends MobEffect {
   private static final int RADIUS_TO_CHECK_SLIMES = 2;
   public static final int SLIME_SIZE = 2;
   private final ToIntFunction<RandomSource> spawnedCount;

   protected OozingMobEffect(MobEffectCategory p_338567_, int p_338409_, ToIntFunction<RandomSource> p_338888_) {
      super(p_338567_, p_338409_, ParticleTypes.ITEM_SLIME);
      this.spawnedCount = p_338888_;
   }

   @VisibleForTesting
   protected static int numberOfSlimesToSpawn(int p_341016_, OozingMobEffect.NearbySlimes p_348591_, int p_341398_) {
      return p_341016_ < 1 ? p_341398_ : Mth.clamp(0, p_341016_ - p_348591_.count(p_341016_), p_341398_);
   }

   @Override
   public void onMobRemoved(LivingEntity p_338339_, int p_338421_, Entity.RemovalReason p_338677_) {
      if (p_338677_ == Entity.RemovalReason.KILLED) {
         int $$3 = this.spawnedCount.applyAsInt(p_338339_.getRandom());
         Level $$4 = p_338339_.level();
         int $$5 = $$4.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
         int $$6 = numberOfSlimesToSpawn($$5, OozingMobEffect.NearbySlimes.closeTo(p_338339_), $$3);

         for (int $$7 = 0; $$7 < $$6; $$7++) {
            this.spawnSlimeOffspring(p_338339_.level(), p_338339_.getX(), p_338339_.getY() + 0.5, p_338339_.getZ());
         }
      }
   }

   private void spawnSlimeOffspring(Level p_338724_, double p_338555_, double p_338811_, double p_338192_) {
      Slime $$4 = EntityType.SLIME.create(p_338724_);
      if ($$4 != null) {
         $$4.setSize(2, true);
         $$4.moveTo(p_338555_, p_338811_, p_338192_, p_338724_.getRandom().nextFloat() * 360.0F, 0.0F);
         p_338724_.addFreshEntity($$4);
      }
   }

   @FunctionalInterface
   protected interface NearbySlimes {
      int count(int var1);

      static OozingMobEffect.NearbySlimes closeTo(LivingEntity p_348471_) {
         return p_352702_ -> {
            List<Slime> $$2 = new ArrayList<>();
            p_348471_.level().getEntities(EntityType.SLIME, p_348471_.getBoundingBox().inflate(2.0), p_348647_ -> p_348647_ != p_348471_, $$2, p_352702_);
            return $$2.size();
         };
      }
   }
}
