package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class ForkingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<ForkingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70161_ -> trunkPlacerParts(p_70161_).apply(p_70161_, ForkingTrunkPlacer::new)
   );

   public ForkingTrunkPlacer(int p_70148_, int p_70149_, int p_70150_) {
      super(p_70148_, p_70149_, p_70150_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.FORKING_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_226116_,
      BiConsumer<BlockPos, BlockState> p_226117_,
      RandomSource p_226118_,
      int p_226119_,
      BlockPos p_226120_,
      TreeConfiguration p_226121_
   ) {
      setDirtAt(p_226116_, p_226117_, p_226118_, p_226120_.below(), p_226121_);
      List<FoliagePlacer.FoliageAttachment> $$6 = Lists.newArrayList();
      Direction $$7 = Direction.Plane.HORIZONTAL.getRandomDirection(p_226118_);
      int $$8 = p_226119_ - p_226118_.nextInt(4) - 1;
      int $$9 = 3 - p_226118_.nextInt(3);
      BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();
      int $$11 = p_226120_.getX();
      int $$12 = p_226120_.getZ();
      OptionalInt $$13 = OptionalInt.empty();

      for (int $$14 = 0; $$14 < p_226119_; $$14++) {
         int $$15 = p_226120_.getY() + $$14;
         if ($$14 >= $$8 && $$9 > 0) {
            $$11 += $$7.getStepX();
            $$12 += $$7.getStepZ();
            $$9--;
         }

         if (this.placeLog(p_226116_, p_226117_, p_226118_, $$10.set($$11, $$15, $$12), p_226121_)) {
            $$13 = OptionalInt.of($$15 + 1);
         }
      }

      if ($$13.isPresent()) {
         $$6.add(new FoliagePlacer.FoliageAttachment(new BlockPos($$11, $$13.getAsInt(), $$12), 1, false));
      }

      $$11 = p_226120_.getX();
      $$12 = p_226120_.getZ();
      Direction $$16 = Direction.Plane.HORIZONTAL.getRandomDirection(p_226118_);
      if ($$16 != $$7) {
         int $$17 = $$8 - p_226118_.nextInt(2) - 1;
         int $$18 = 1 + p_226118_.nextInt(3);
         $$13 = OptionalInt.empty();

         for (int $$19 = $$17; $$19 < p_226119_ && $$18 > 0; $$18--) {
            if ($$19 >= 1) {
               int $$20 = p_226120_.getY() + $$19;
               $$11 += $$16.getStepX();
               $$12 += $$16.getStepZ();
               if (this.placeLog(p_226116_, p_226117_, p_226118_, $$10.set($$11, $$20, $$12), p_226121_)) {
                  $$13 = OptionalInt.of($$20 + 1);
               }
            }

            $$19++;
         }

         if ($$13.isPresent()) {
            $$6.add(new FoliagePlacer.FoliageAttachment(new BlockPos($$11, $$13.getAsInt(), $$12), 0, false));
         }
      }

      return $$6;
   }
}
