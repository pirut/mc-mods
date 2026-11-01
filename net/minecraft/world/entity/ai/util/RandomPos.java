package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RandomPos {
   private static final int RANDOM_POS_ATTEMPTS = 10;

   public static BlockPos generateRandomDirection(RandomSource random, int horizontalDistance, int verticalDistance) {
      int i = random.nextInt(2 * horizontalDistance + 1) - horizontalDistance;
      int j = random.nextInt(2 * verticalDistance + 1) - verticalDistance;
      int k = random.nextInt(2 * horizontalDistance + 1) - horizontalDistance;
      return new BlockPos(i, j, k);
   }

   @Nullable
   public static BlockPos generateRandomDirectionWithinRadians(
      RandomSource random, double minDistance, double maxDistance, int verticalDistance, int y, double x, double z, double radians
   ) {
      double d = Mth.atan2(z, x) - (float) (Math.PI / 2);
      double e = d + (2.0F * random.nextFloat() - 1.0F) * radians;
      double f = Mth.lerp(Math.sqrt(random.nextDouble()), minDistance, maxDistance) * Mth.SQRT_OF_TWO;
      double g = -f * Math.sin(e);
      double h = f * Math.cos(e);
      if (!(Math.abs(g) > maxDistance) && !(Math.abs(h) > maxDistance)) {
         int i = random.nextInt(2 * verticalDistance + 1) - verticalDistance + y;
         return BlockPos.containing(g, i, h);
      } else {
         return null;
      }
   }

   @VisibleForTesting
   public static BlockPos moveUpOutOfSolid(BlockPos pos, int maxY, Predicate<BlockPos> posPredicate) {
      if (!posPredicate.test(pos)) {
         return pos;
      } else {
         BlockPos.MutableBlockPos mutableBlockPos = pos.mutable().move(Direction.UP);

         while (mutableBlockPos.getY() <= maxY && posPredicate.test(mutableBlockPos)) {
            mutableBlockPos.move(Direction.UP);
         }

         return mutableBlockPos.immutable();
      }
   }

   @VisibleForTesting
   public static BlockPos moveUpToAboveSolid(BlockPos pos, int aboveSolidAmount, int maxY, Predicate<BlockPos> posPredicate) {
      if (aboveSolidAmount < 0) {
         throw new IllegalArgumentException("aboveSolidAmount was " + aboveSolidAmount + ", expected >= 0");
      } else if (!posPredicate.test(pos)) {
         return pos;
      } else {
         BlockPos.MutableBlockPos mutableBlockPos = pos.mutable().move(Direction.UP);

         while (mutableBlockPos.getY() <= maxY && posPredicate.test(mutableBlockPos)) {
            mutableBlockPos.move(Direction.UP);
         }

         int i = mutableBlockPos.getY();

         while (mutableBlockPos.getY() <= maxY && mutableBlockPos.getY() - i < aboveSolidAmount) {
            mutableBlockPos.move(Direction.UP);
            if (posPredicate.test(mutableBlockPos)) {
               mutableBlockPos.move(Direction.DOWN);
               break;
            }
         }

         return mutableBlockPos.immutable();
      }
   }

   @Nullable
   public static Vec3 generateRandomPos(PathfinderMob mob, Supplier<BlockPos> posSupplier) {
      return generateRandomPos(posSupplier, mob::getWalkTargetValue);
   }

   @Nullable
   public static Vec3 generateRandomPos(Supplier<BlockPos> posSupplier, ToDoubleFunction<BlockPos> toDoubleFunction) {
      double d = Double.NEGATIVE_INFINITY;
      BlockPos blockPos = null;

      for (int i = 0; i < 10; i++) {
         BlockPos blockPos2 = posSupplier.get();
         if (blockPos2 != null) {
            double e = toDoubleFunction.applyAsDouble(blockPos2);
            if (e > d) {
               d = e;
               blockPos = blockPos2;
            }
         }
      }

      return blockPos != null ? Vec3.atBottomCenterOf(blockPos) : null;
   }

   public static BlockPos generateRandomPosTowardDirection(PathfinderMob mob, double radius, RandomSource random, BlockPos pos) {
      double d = pos.getX();
      double e = pos.getZ();
      if (mob.hasHome() && radius > 1.0) {
         BlockPos blockPos = mob.getHomePosition();
         if (mob.getX() > blockPos.getX()) {
            d -= random.nextDouble() * radius / 2.0;
         } else {
            d += random.nextDouble() * radius / 2.0;
         }

         if (mob.getZ() > blockPos.getZ()) {
            e -= random.nextDouble() * radius / 2.0;
         } else {
            e += random.nextDouble() * radius / 2.0;
         }
      }

      return BlockPos.containing(d + mob.getX(), pos.getY() + mob.getY(), e + mob.getZ());
   }
}
