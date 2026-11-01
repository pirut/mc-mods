package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class RandomPos {
   private static final int RANDOM_POS_ATTEMPTS = 10;

   public static BlockPos generateRandomDirection(RandomSource p_217852_, int p_217853_, int p_217854_) {
      int $$3 = p_217852_.nextInt(2 * p_217853_ + 1) - p_217853_;
      int $$4 = p_217852_.nextInt(2 * p_217854_ + 1) - p_217854_;
      int $$5 = p_217852_.nextInt(2 * p_217853_ + 1) - p_217853_;
      return new BlockPos($$3, $$4, $$5);
   }

   @Nullable
   public static BlockPos generateRandomDirectionWithinRadians(
      RandomSource p_217856_, int p_217857_, int p_217858_, int p_217859_, double p_217860_, double p_217861_, double p_217862_
   ) {
      double $$7 = Mth.atan2(p_217861_, p_217860_) - (float) (Math.PI / 2);
      double $$8 = $$7 + (2.0F * p_217856_.nextFloat() - 1.0F) * p_217862_;
      double $$9 = Math.sqrt(p_217856_.nextDouble()) * Mth.SQRT_OF_TWO * p_217857_;
      double $$10 = -$$9 * Math.sin($$8);
      double $$11 = $$9 * Math.cos($$8);
      if (!(Math.abs($$10) > p_217857_) && !(Math.abs($$11) > p_217857_)) {
         int $$12 = p_217856_.nextInt(2 * p_217858_ + 1) - p_217858_ + p_217859_;
         return BlockPos.containing($$10, $$12, $$11);
      } else {
         return null;
      }
   }

   @VisibleForTesting
   public static BlockPos moveUpOutOfSolid(BlockPos p_148546_, int p_148547_, Predicate<BlockPos> p_148548_) {
      if (!p_148548_.test(p_148546_)) {
         return p_148546_;
      } else {
         BlockPos $$3 = p_148546_.above();

         while ($$3.getY() < p_148547_ && p_148548_.test($$3)) {
            $$3 = $$3.above();
         }

         return $$3;
      }
   }

   @VisibleForTesting
   public static BlockPos moveUpToAboveSolid(BlockPos p_26948_, int p_26949_, int p_26950_, Predicate<BlockPos> p_26951_) {
      if (p_26949_ < 0) {
         throw new IllegalArgumentException("aboveSolidAmount was " + p_26949_ + ", expected >= 0");
      } else if (!p_26951_.test(p_26948_)) {
         return p_26948_;
      } else {
         BlockPos $$4 = p_26948_.above();

         while ($$4.getY() < p_26950_ && p_26951_.test($$4)) {
            $$4 = $$4.above();
         }

         BlockPos $$5 = $$4;

         while ($$5.getY() < p_26950_ && $$5.getY() - $$4.getY() < p_26949_) {
            BlockPos $$6 = $$5.above();
            if (p_26951_.test($$6)) {
               break;
            }

            $$5 = $$6;
         }

         return $$5;
      }
   }

   @Nullable
   public static Vec3 generateRandomPos(PathfinderMob p_148543_, Supplier<BlockPos> p_148544_) {
      return generateRandomPos(p_148544_, p_148543_::getWalkTargetValue);
   }

   @Nullable
   public static Vec3 generateRandomPos(Supplier<BlockPos> p_148562_, ToDoubleFunction<BlockPos> p_148563_) {
      double $$2 = Double.NEGATIVE_INFINITY;
      BlockPos $$3 = null;

      for (int $$4 = 0; $$4 < 10; $$4++) {
         BlockPos $$5 = p_148562_.get();
         if ($$5 != null) {
            double $$6 = p_148563_.applyAsDouble($$5);
            if ($$6 > $$2) {
               $$2 = $$6;
               $$3 = $$5;
            }
         }
      }

      return $$3 != null ? Vec3.atBottomCenterOf($$3) : null;
   }

   public static BlockPos generateRandomPosTowardDirection(PathfinderMob p_217864_, int p_217865_, RandomSource p_217866_, BlockPos p_217867_) {
      int $$4 = p_217867_.getX();
      int $$5 = p_217867_.getZ();
      if (p_217864_.hasRestriction() && p_217865_ > 1) {
         BlockPos $$6 = p_217864_.getRestrictCenter();
         if (p_217864_.getX() > $$6.getX()) {
            $$4 -= p_217866_.nextInt(p_217865_ / 2);
         } else {
            $$4 += p_217866_.nextInt(p_217865_ / 2);
         }

         if (p_217864_.getZ() > $$6.getZ()) {
            $$5 -= p_217866_.nextInt(p_217865_ / 2);
         } else {
            $$5 += p_217866_.nextInt(p_217865_ / 2);
         }
      }

      return BlockPos.containing($$4 + p_217864_.getX(), p_217867_.getY() + p_217864_.getY(), $$5 + p_217864_.getZ());
   }
}
