package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUtil {
   public static BlockUtil.FoundRectangle getLargestRectangleAround(
      BlockPos p_468960_, Direction.Axis p_469851_, int p_468868_, Direction.Axis p_467470_, int p_468713_, Predicate<BlockPos> p_469358_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_468960_.mutable();
      Direction direction = Direction.get(Direction.AxisDirection.NEGATIVE, p_469851_);
      Direction direction1 = direction.getOpposite();
      Direction direction2 = Direction.get(Direction.AxisDirection.NEGATIVE, p_467470_);
      Direction direction3 = direction2.getOpposite();
      int i = getLimit(p_469358_, blockpos$mutableblockpos.set(p_468960_), direction, p_468868_);
      int j = getLimit(p_469358_, blockpos$mutableblockpos.set(p_468960_), direction1, p_468868_);
      int k = i;
      BlockUtil.IntBounds[] ablockutil$intbounds = new BlockUtil.IntBounds[i + 1 + j];
      ablockutil$intbounds[i] = new BlockUtil.IntBounds(
         getLimit(p_469358_, blockpos$mutableblockpos.set(p_468960_), direction2, p_468713_),
         getLimit(p_469358_, blockpos$mutableblockpos.set(p_468960_), direction3, p_468713_)
      );
      int l = ablockutil$intbounds[i].min;

      for (int i1 = 1; i1 <= i; i1++) {
         BlockUtil.IntBounds blockutil$intbounds = ablockutil$intbounds[k - (i1 - 1)];
         ablockutil$intbounds[k - i1] = new BlockUtil.IntBounds(
            getLimit(p_469358_, blockpos$mutableblockpos.set(p_468960_).move(direction, i1), direction2, blockutil$intbounds.min),
            getLimit(p_469358_, blockpos$mutableblockpos.set(p_468960_).move(direction, i1), direction3, blockutil$intbounds.max)
         );
      }

      for (int l2 = 1; l2 <= j; l2++) {
         BlockUtil.IntBounds blockutil$intbounds2 = ablockutil$intbounds[k + l2 - 1];
         ablockutil$intbounds[k + l2] = new BlockUtil.IntBounds(
            getLimit(p_469358_, blockpos$mutableblockpos.set(p_468960_).move(direction1, l2), direction2, blockutil$intbounds2.min),
            getLimit(p_469358_, blockpos$mutableblockpos.set(p_468960_).move(direction1, l2), direction3, blockutil$intbounds2.max)
         );
      }

      int i3 = 0;
      int j3 = 0;
      int j1 = 0;
      int k1 = 0;
      int[] aint = new int[ablockutil$intbounds.length];

      for (int l1 = l; l1 >= 0; l1--) {
         for (int i2 = 0; i2 < ablockutil$intbounds.length; i2++) {
            BlockUtil.IntBounds blockutil$intbounds1 = ablockutil$intbounds[i2];
            int j2 = l - blockutil$intbounds1.min;
            int k2 = l + blockutil$intbounds1.max;
            aint[i2] = l1 >= j2 && l1 <= k2 ? k2 + 1 - l1 : 0;
         }

         Pair<BlockUtil.IntBounds, Integer> pair = getMaxRectangleLocation(aint);
         BlockUtil.IntBounds blockutil$intbounds3 = (BlockUtil.IntBounds)pair.getFirst();
         int k3 = 1 + blockutil$intbounds3.max - blockutil$intbounds3.min;
         int l3 = (Integer)pair.getSecond();
         if (k3 * l3 > j1 * k1) {
            i3 = blockutil$intbounds3.min;
            j3 = l1;
            j1 = k3;
            k1 = l3;
         }
      }

      return new BlockUtil.FoundRectangle(p_468960_.relative(p_469851_, i3 - k).relative(p_467470_, j3 - l), j1, k1);
   }

   private static int getLimit(Predicate<BlockPos> p_469584_, BlockPos.MutableBlockPos p_469092_, Direction p_469650_, int p_466898_) {
      int i = 0;

      while (i < p_466898_ && p_469584_.test(p_469092_.move(p_469650_))) {
         i++;
      }

      return i;
   }

   @VisibleForTesting
   static Pair<BlockUtil.IntBounds, Integer> getMaxRectangleLocation(int[] p_469802_) {
      int i = 0;
      int j = 0;
      int k = 0;
      IntStack intstack = new IntArrayList();
      intstack.push(0);

      for (int l = 1; l <= p_469802_.length; l++) {
         int i1 = l == p_469802_.length ? 0 : p_469802_[l];

         while (!intstack.isEmpty()) {
            int j1 = p_469802_[intstack.topInt()];
            if (i1 >= j1) {
               intstack.push(l);
               break;
            }

            intstack.popInt();
            int k1 = intstack.isEmpty() ? 0 : intstack.topInt() + 1;
            if (j1 * (l - k1) > k * (j - i)) {
               j = l;
               i = k1;
               k = j1;
            }
         }

         if (intstack.isEmpty()) {
            intstack.push(l);
         }
      }

      return new Pair(new BlockUtil.IntBounds(i, j - 1), k);
   }

   public static Optional<BlockPos> getTopConnectedBlock(BlockGetter p_469684_, BlockPos p_468202_, Block p_469985_, Direction p_468104_, Block p_467900_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_468202_.mutable();

      BlockState blockstate;
      do {
         blockpos$mutableblockpos.move(p_468104_);
         blockstate = p_469684_.getBlockState(blockpos$mutableblockpos);
      } while (blockstate.is(p_469985_));

      return blockstate.is(p_467900_) ? Optional.of(blockpos$mutableblockpos) : Optional.empty();
   }

   public static class FoundRectangle {
      public final BlockPos minCorner;
      public final int axis1Size;
      public final int axis2Size;

      public FoundRectangle(BlockPos p_468602_, int p_467805_, int p_467280_) {
         this.minCorner = p_468602_;
         this.axis1Size = p_467805_;
         this.axis2Size = p_467280_;
      }
   }

   public static class IntBounds {
      public final int min;
      public final int max;

      public IntBounds(int p_469256_, int p_468821_) {
         this.min = p_469256_;
         this.max = p_468821_;
      }

      @Override
      public String toString() {
         return "IntBounds{min=" + this.min + ", max=" + this.max + "}";
      }
   }
}
