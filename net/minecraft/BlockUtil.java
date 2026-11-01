package net.minecraft;

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
      BlockPos p_124335_, Direction.Axis p_124336_, int p_124337_, Direction.Axis p_124338_, int p_124339_, Predicate<BlockPos> p_124340_
   ) {
      BlockPos.MutableBlockPos $$6 = p_124335_.mutable();
      Direction $$7 = Direction.get(Direction.AxisDirection.NEGATIVE, p_124336_);
      Direction $$8 = $$7.getOpposite();
      Direction $$9 = Direction.get(Direction.AxisDirection.NEGATIVE, p_124338_);
      Direction $$10 = $$9.getOpposite();
      int $$11 = getLimit(p_124340_, $$6.set(p_124335_), $$7, p_124337_);
      int $$12 = getLimit(p_124340_, $$6.set(p_124335_), $$8, p_124337_);
      int $$13 = $$11;
      BlockUtil.IntBounds[] $$14 = new BlockUtil.IntBounds[$$11 + 1 + $$12];
      $$14[$$11] = new BlockUtil.IntBounds(getLimit(p_124340_, $$6.set(p_124335_), $$9, p_124339_), getLimit(p_124340_, $$6.set(p_124335_), $$10, p_124339_));
      int $$15 = $$14[$$11].min;

      for (int $$16 = 1; $$16 <= $$11; $$16++) {
         BlockUtil.IntBounds $$17 = $$14[$$13 - ($$16 - 1)];
         $$14[$$13 - $$16] = new BlockUtil.IntBounds(
            getLimit(p_124340_, $$6.set(p_124335_).move($$7, $$16), $$9, $$17.min), getLimit(p_124340_, $$6.set(p_124335_).move($$7, $$16), $$10, $$17.max)
         );
      }

      for (int $$18 = 1; $$18 <= $$12; $$18++) {
         BlockUtil.IntBounds $$19 = $$14[$$13 + $$18 - 1];
         $$14[$$13 + $$18] = new BlockUtil.IntBounds(
            getLimit(p_124340_, $$6.set(p_124335_).move($$8, $$18), $$9, $$19.min), getLimit(p_124340_, $$6.set(p_124335_).move($$8, $$18), $$10, $$19.max)
         );
      }

      int $$20 = 0;
      int $$21 = 0;
      int $$22 = 0;
      int $$23 = 0;
      int[] $$24 = new int[$$14.length];

      for (int $$25 = $$15; $$25 >= 0; $$25--) {
         for (int $$26 = 0; $$26 < $$14.length; $$26++) {
            BlockUtil.IntBounds $$27 = $$14[$$26];
            int $$28 = $$15 - $$27.min;
            int $$29 = $$15 + $$27.max;
            $$24[$$26] = $$25 >= $$28 && $$25 <= $$29 ? $$29 + 1 - $$25 : 0;
         }

         Pair<BlockUtil.IntBounds, Integer> $$30 = getMaxRectangleLocation($$24);
         BlockUtil.IntBounds $$31 = (BlockUtil.IntBounds)$$30.getFirst();
         int $$32 = 1 + $$31.max - $$31.min;
         int $$33 = (Integer)$$30.getSecond();
         if ($$32 * $$33 > $$22 * $$23) {
            $$20 = $$31.min;
            $$21 = $$25;
            $$22 = $$32;
            $$23 = $$33;
         }
      }

      return new BlockUtil.FoundRectangle(p_124335_.relative(p_124336_, $$20 - $$13).relative(p_124338_, $$21 - $$15), $$22, $$23);
   }

   private static int getLimit(Predicate<BlockPos> p_124342_, BlockPos.MutableBlockPos p_124343_, Direction p_124344_, int p_124345_) {
      int $$4 = 0;

      while ($$4 < p_124345_ && p_124342_.test(p_124343_.move(p_124344_))) {
         $$4++;
      }

      return $$4;
   }

   @VisibleForTesting
   static Pair<BlockUtil.IntBounds, Integer> getMaxRectangleLocation(int[] p_124347_) {
      int $$1 = 0;
      int $$2 = 0;
      int $$3 = 0;
      IntStack $$4 = new IntArrayList();
      $$4.push(0);

      for (int $$5 = 1; $$5 <= p_124347_.length; $$5++) {
         int $$6 = $$5 == p_124347_.length ? 0 : p_124347_[$$5];

         while (!$$4.isEmpty()) {
            int $$7 = p_124347_[$$4.topInt()];
            if ($$6 >= $$7) {
               $$4.push($$5);
               break;
            }

            $$4.popInt();
            int $$8 = $$4.isEmpty() ? 0 : $$4.topInt() + 1;
            if ($$7 * ($$5 - $$8) > $$3 * ($$2 - $$1)) {
               $$2 = $$5;
               $$1 = $$8;
               $$3 = $$7;
            }
         }

         if ($$4.isEmpty()) {
            $$4.push($$5);
         }
      }

      return new Pair(new BlockUtil.IntBounds($$1, $$2 - 1), $$3);
   }

   public static Optional<BlockPos> getTopConnectedBlock(BlockGetter p_177846_, BlockPos p_177847_, Block p_177848_, Direction p_177849_, Block p_177850_) {
      BlockPos.MutableBlockPos $$5 = p_177847_.mutable();

      BlockState $$6;
      do {
         $$5.move(p_177849_);
         $$6 = p_177846_.getBlockState($$5);
      } while ($$6.is(p_177848_));

      return $$6.is(p_177850_) ? Optional.of($$5) : Optional.empty();
   }

   public static class FoundRectangle {
      public final BlockPos minCorner;
      public final int axis1Size;
      public final int axis2Size;

      public FoundRectangle(BlockPos p_124352_, int p_124353_, int p_124354_) {
         this.minCorner = p_124352_;
         this.axis1Size = p_124353_;
         this.axis2Size = p_124354_;
      }
   }

   public static class IntBounds {
      public final int min;
      public final int max;

      public IntBounds(int p_124358_, int p_124359_) {
         this.min = p_124358_;
         this.max = p_124359_;
      }

      @Override
      public String toString() {
         return "IntBounds{min=" + this.min + ", max=" + this.max + "}";
      }
   }
}
