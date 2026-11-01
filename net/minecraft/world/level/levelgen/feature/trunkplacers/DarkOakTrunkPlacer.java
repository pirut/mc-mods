package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class DarkOakTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<DarkOakTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70090_ -> trunkPlacerParts(p_70090_).apply(p_70090_, DarkOakTrunkPlacer::new)
   );

   public DarkOakTrunkPlacer(int p_70077_, int p_70078_, int p_70079_) {
      super(p_70077_, p_70078_, p_70079_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.DARK_OAK_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_226086_,
      BiConsumer<BlockPos, BlockState> p_226087_,
      RandomSource p_226088_,
      int p_226089_,
      BlockPos p_226090_,
      TreeConfiguration p_226091_
   ) {
      List<FoliagePlacer.FoliageAttachment> $$6 = Lists.newArrayList();
      BlockPos $$7 = p_226090_.below();
      setDirtAt(p_226086_, p_226087_, p_226088_, $$7, p_226091_);
      setDirtAt(p_226086_, p_226087_, p_226088_, $$7.east(), p_226091_);
      setDirtAt(p_226086_, p_226087_, p_226088_, $$7.south(), p_226091_);
      setDirtAt(p_226086_, p_226087_, p_226088_, $$7.south().east(), p_226091_);
      Direction $$8 = Direction.Plane.HORIZONTAL.getRandomDirection(p_226088_);
      int $$9 = p_226089_ - p_226088_.nextInt(4);
      int $$10 = 2 - p_226088_.nextInt(3);
      int $$11 = p_226090_.getX();
      int $$12 = p_226090_.getY();
      int $$13 = p_226090_.getZ();
      int $$14 = $$11;
      int $$15 = $$13;
      int $$16 = $$12 + p_226089_ - 1;

      for (int $$17 = 0; $$17 < p_226089_; $$17++) {
         if ($$17 >= $$9 && $$10 > 0) {
            $$14 += $$8.getStepX();
            $$15 += $$8.getStepZ();
            $$10--;
         }

         int $$18 = $$12 + $$17;
         BlockPos $$19 = new BlockPos($$14, $$18, $$15);
         if (TreeFeature.isAirOrLeaves(p_226086_, $$19)) {
            this.placeLog(p_226086_, p_226087_, p_226088_, $$19, p_226091_);
            this.placeLog(p_226086_, p_226087_, p_226088_, $$19.east(), p_226091_);
            this.placeLog(p_226086_, p_226087_, p_226088_, $$19.south(), p_226091_);
            this.placeLog(p_226086_, p_226087_, p_226088_, $$19.east().south(), p_226091_);
         }
      }

      $$6.add(new FoliagePlacer.FoliageAttachment(new BlockPos($$14, $$16, $$15), 0, true));

      for (int $$20 = -1; $$20 <= 2; $$20++) {
         for (int $$21 = -1; $$21 <= 2; $$21++) {
            if (($$20 < 0 || $$20 > 1 || $$21 < 0 || $$21 > 1) && p_226088_.nextInt(3) <= 0) {
               int $$22 = p_226088_.nextInt(3) + 2;

               for (int $$23 = 0; $$23 < $$22; $$23++) {
                  this.placeLog(p_226086_, p_226087_, p_226088_, new BlockPos($$11 + $$20, $$16 - $$23 - 1, $$13 + $$21), p_226091_);
               }

               $$6.add(new FoliagePlacer.FoliageAttachment(new BlockPos($$14 + $$20, $$16, $$15 + $$21), 0, false));
            }
         }
      }

      return $$6;
   }
}
