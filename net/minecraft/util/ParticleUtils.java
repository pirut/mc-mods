package net.minecraft.util;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ParticleUtils {
   public static void spawnParticlesOnBlockFaces(Level p_216314_, BlockPos p_216315_, ParticleOptions p_216316_, IntProvider p_216317_) {
      for (Direction $$4 : Direction.values()) {
         spawnParticlesOnBlockFace(p_216314_, p_216315_, p_216316_, p_216317_, $$4, () -> getRandomSpeedRanges(p_216314_.random), 0.55);
      }
   }

   public static void spawnParticlesOnBlockFace(
      Level p_216319_, BlockPos p_216320_, ParticleOptions p_216321_, IntProvider p_216322_, Direction p_216323_, Supplier<Vec3> p_216324_, double p_216325_
   ) {
      int $$7 = p_216322_.sample(p_216319_.random);

      for (int $$8 = 0; $$8 < $$7; $$8++) {
         spawnParticleOnFace(p_216319_, p_216320_, p_216323_, p_216321_, p_216324_.get(), p_216325_);
      }
   }

   private static Vec3 getRandomSpeedRanges(RandomSource p_216303_) {
      return new Vec3(Mth.nextDouble(p_216303_, -0.5, 0.5), Mth.nextDouble(p_216303_, -0.5, 0.5), Mth.nextDouble(p_216303_, -0.5, 0.5));
   }

   public static void spawnParticlesAlongAxis(
      Direction.Axis p_144968_, Level p_144969_, BlockPos p_144970_, double p_144971_, ParticleOptions p_144972_, UniformInt p_144973_
   ) {
      Vec3 $$6 = Vec3.atCenterOf(p_144970_);
      boolean $$7 = p_144968_ == Direction.Axis.X;
      boolean $$8 = p_144968_ == Direction.Axis.Y;
      boolean $$9 = p_144968_ == Direction.Axis.Z;
      int $$10 = p_144973_.sample(p_144969_.random);

      for (int $$11 = 0; $$11 < $$10; $$11++) {
         double $$12 = $$6.x + Mth.nextDouble(p_144969_.random, -1.0, 1.0) * ($$7 ? 0.5 : p_144971_);
         double $$13 = $$6.y + Mth.nextDouble(p_144969_.random, -1.0, 1.0) * ($$8 ? 0.5 : p_144971_);
         double $$14 = $$6.z + Mth.nextDouble(p_144969_.random, -1.0, 1.0) * ($$9 ? 0.5 : p_144971_);
         double $$15 = $$7 ? Mth.nextDouble(p_144969_.random, -1.0, 1.0) : 0.0;
         double $$16 = $$8 ? Mth.nextDouble(p_144969_.random, -1.0, 1.0) : 0.0;
         double $$17 = $$9 ? Mth.nextDouble(p_144969_.random, -1.0, 1.0) : 0.0;
         p_144969_.addParticle(p_144972_, $$12, $$13, $$14, $$15, $$16, $$17);
      }
   }

   public static void spawnParticleOnFace(Level p_216307_, BlockPos p_216308_, Direction p_216309_, ParticleOptions p_216310_, Vec3 p_216311_, double p_216312_) {
      Vec3 $$6 = Vec3.atCenterOf(p_216308_);
      int $$7 = p_216309_.getStepX();
      int $$8 = p_216309_.getStepY();
      int $$9 = p_216309_.getStepZ();
      double $$10 = $$6.x + ($$7 == 0 ? Mth.nextDouble(p_216307_.random, -0.5, 0.5) : $$7 * p_216312_);
      double $$11 = $$6.y + ($$8 == 0 ? Mth.nextDouble(p_216307_.random, -0.5, 0.5) : $$8 * p_216312_);
      double $$12 = $$6.z + ($$9 == 0 ? Mth.nextDouble(p_216307_.random, -0.5, 0.5) : $$9 * p_216312_);
      double $$13 = $$7 == 0 ? p_216311_.x() : 0.0;
      double $$14 = $$8 == 0 ? p_216311_.y() : 0.0;
      double $$15 = $$9 == 0 ? p_216311_.z() : 0.0;
      p_216307_.addParticle(p_216310_, $$10, $$11, $$12, $$13, $$14, $$15);
   }

   public static void spawnParticleBelow(Level p_273159_, BlockPos p_273452_, RandomSource p_273538_, ParticleOptions p_273419_) {
      double $$4 = p_273452_.getX() + p_273538_.nextDouble();
      double $$5 = p_273452_.getY() - 0.05;
      double $$6 = p_273452_.getZ() + p_273538_.nextDouble();
      p_273159_.addParticle(p_273419_, $$4, $$5, $$6, 0.0, 0.0, 0.0);
   }

   public static void spawnParticleInBlock(LevelAccessor p_316553_, BlockPos p_316882_, int p_316412_, ParticleOptions p_316308_) {
      double $$4 = 0.5;
      BlockState $$5 = p_316553_.getBlockState(p_316882_);
      double $$6 = $$5.isAir() ? 1.0 : $$5.getShape(p_316553_, p_316882_).max(Direction.Axis.Y);
      spawnParticles(p_316553_, p_316882_, p_316412_, 0.5, $$6, true, p_316308_);
   }

   public static void spawnParticles(
      LevelAccessor p_316549_, BlockPos p_316335_, int p_316207_, double p_316351_, double p_316421_, boolean p_316563_, ParticleOptions p_316241_
   ) {
      RandomSource $$7 = p_316549_.getRandom();

      for (int $$8 = 0; $$8 < p_316207_; $$8++) {
         double $$9 = $$7.nextGaussian() * 0.02;
         double $$10 = $$7.nextGaussian() * 0.02;
         double $$11 = $$7.nextGaussian() * 0.02;
         double $$12 = 0.5 - p_316351_;
         double $$13 = p_316335_.getX() + $$12 + $$7.nextDouble() * p_316351_ * 2.0;
         double $$14 = p_316335_.getY() + $$7.nextDouble() * p_316421_;
         double $$15 = p_316335_.getZ() + $$12 + $$7.nextDouble() * p_316351_ * 2.0;
         if (p_316563_ || !p_316549_.getBlockState(BlockPos.containing($$13, $$14, $$15).below()).isAir()) {
            p_316549_.addParticle(p_316241_, $$13, $$14, $$15, $$9, $$10, $$11);
         }
      }
   }

   public static void spawnSmashAttackParticles(LevelAccessor p_338535_, BlockPos p_338284_, int p_338684_) {
      Vec3 $$3 = p_338284_.getCenter().add(0.0, 0.5, 0.0);
      BlockParticleOption $$4 = new BlockParticleOption(ParticleTypes.DUST_PILLAR, p_338535_.getBlockState(p_338284_));

      for (int $$5 = 0; $$5 < p_338684_ / 3.0F; $$5++) {
         double $$6 = $$3.x + p_338535_.getRandom().nextGaussian() / 2.0;
         double $$7 = $$3.y;
         double $$8 = $$3.z + p_338535_.getRandom().nextGaussian() / 2.0;
         double $$9 = p_338535_.getRandom().nextGaussian() * 0.2F;
         double $$10 = p_338535_.getRandom().nextGaussian() * 0.2F;
         double $$11 = p_338535_.getRandom().nextGaussian() * 0.2F;
         p_338535_.addParticle($$4, $$6, $$7, $$8, $$9, $$10, $$11);
      }

      for (int $$12 = 0; $$12 < p_338684_ / 1.5F; $$12++) {
         double $$13 = $$3.x + 3.5 * Math.cos($$12) + p_338535_.getRandom().nextGaussian() / 2.0;
         double $$14 = $$3.y;
         double $$15 = $$3.z + 3.5 * Math.sin($$12) + p_338535_.getRandom().nextGaussian() / 2.0;
         double $$16 = p_338535_.getRandom().nextGaussian() * 0.05F;
         double $$17 = p_338535_.getRandom().nextGaussian() * 0.05F;
         double $$18 = p_338535_.getRandom().nextGaussian() * 0.05F;
         p_338535_.addParticle($$4, $$13, $$14, $$15, $$16, $$17, $$18);
      }
   }
}
