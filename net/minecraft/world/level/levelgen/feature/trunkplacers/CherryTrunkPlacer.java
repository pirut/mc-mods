package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class CherryTrunkPlacer extends TrunkPlacer {
   private static final Codec<UniformInt> BRANCH_START_CODEC = UniformInt.CODEC
      .codec()
      .validate(
         p_275181_ -> p_275181_.getMaxValue() - p_275181_.getMinValue() < 1
            ? DataResult.error(() -> "Need at least 2 blocks variation for the branch starts to fit both branches")
            : DataResult.success(p_275181_)
      );
   public static final MapCodec<CherryTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_338099_ -> trunkPlacerParts(p_338099_)
         .and(
            p_338099_.group(
               IntProvider.codec(1, 3).fieldOf("branch_count").forGetter(p_272644_ -> p_272644_.branchCount),
               IntProvider.codec(2, 16).fieldOf("branch_horizontal_length").forGetter(p_273612_ -> p_273612_.branchHorizontalLength),
               IntProvider.validateCodec(-16, 0, BRANCH_START_CODEC)
                  .fieldOf("branch_start_offset_from_top")
                  .forGetter(p_272705_ -> p_272705_.branchStartOffsetFromTop),
               IntProvider.codec(-16, 16).fieldOf("branch_end_offset_from_top").forGetter(p_273633_ -> p_273633_.branchEndOffsetFromTop)
            )
         )
         .apply(p_338099_, CherryTrunkPlacer::new)
   );
   private final IntProvider branchCount;
   private final IntProvider branchHorizontalLength;
   private final UniformInt branchStartOffsetFromTop;
   private final UniformInt secondBranchStartOffsetFromTop;
   private final IntProvider branchEndOffsetFromTop;

   public CherryTrunkPlacer(
      int p_273281_, int p_273327_, int p_272619_, IntProvider p_272873_, IntProvider p_272789_, UniformInt p_272917_, IntProvider p_272948_
   ) {
      super(p_273281_, p_273327_, p_272619_);
      this.branchCount = p_272873_;
      this.branchHorizontalLength = p_272789_;
      this.branchStartOffsetFromTop = p_272917_;
      this.secondBranchStartOffsetFromTop = UniformInt.of(p_272917_.getMinValue(), p_272917_.getMaxValue() - 1);
      this.branchEndOffsetFromTop = p_272948_;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.CHERRY_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_272827_,
      BiConsumer<BlockPos, BlockState> p_272650_,
      RandomSource p_272993_,
      int p_272990_,
      BlockPos p_273471_,
      TreeConfiguration p_273355_
   ) {
      setDirtAt(p_272827_, p_272650_, p_272993_, p_273471_.below(), p_273355_);
      int $$6 = Math.max(0, p_272990_ - 1 + this.branchStartOffsetFromTop.sample(p_272993_));
      int $$7 = Math.max(0, p_272990_ - 1 + this.secondBranchStartOffsetFromTop.sample(p_272993_));
      if ($$7 >= $$6) {
         $$7++;
      }

      int $$8 = this.branchCount.sample(p_272993_);
      boolean $$9 = $$8 == 3;
      boolean $$10 = $$8 >= 2;
      int $$11;
      if ($$9) {
         $$11 = p_272990_;
      } else if ($$10) {
         $$11 = Math.max($$6, $$7) + 1;
      } else {
         $$11 = $$6 + 1;
      }

      for (int $$14 = 0; $$14 < $$11; $$14++) {
         this.placeLog(p_272827_, p_272650_, p_272993_, p_273471_.above($$14), p_273355_);
      }

      List<FoliagePlacer.FoliageAttachment> $$15 = new ArrayList<>();
      if ($$9) {
         $$15.add(new FoliagePlacer.FoliageAttachment(p_273471_.above($$11), 0, false));
      }

      BlockPos.MutableBlockPos $$16 = new BlockPos.MutableBlockPos();
      Direction $$17 = Direction.Plane.HORIZONTAL.getRandomDirection(p_272993_);
      Function<BlockState, BlockState> $$18 = p_273382_ -> p_273382_.trySetValue(RotatedPillarBlock.AXIS, $$17.getAxis());
      $$15.add(this.generateBranch(p_272827_, p_272650_, p_272993_, p_272990_, p_273471_, p_273355_, $$18, $$17, $$6, $$6 < $$11 - 1, $$16));
      if ($$10) {
         $$15.add(this.generateBranch(p_272827_, p_272650_, p_272993_, p_272990_, p_273471_, p_273355_, $$18, $$17.getOpposite(), $$7, $$7 < $$11 - 1, $$16));
      }

      return $$15;
   }

   private FoliagePlacer.FoliageAttachment generateBranch(
      LevelSimulatedReader p_272736_,
      BiConsumer<BlockPos, BlockState> p_273092_,
      RandomSource p_273449_,
      int p_272659_,
      BlockPos p_273743_,
      TreeConfiguration p_273027_,
      Function<BlockState, BlockState> p_273558_,
      Direction p_273712_,
      int p_272980_,
      boolean p_272719_,
      BlockPos.MutableBlockPos p_273496_
   ) {
      p_273496_.set(p_273743_).move(Direction.UP, p_272980_);
      int $$11 = p_272659_ - 1 + this.branchEndOffsetFromTop.sample(p_273449_);
      boolean $$12 = p_272719_ || $$11 < p_272980_;
      int $$13 = this.branchHorizontalLength.sample(p_273449_) + ($$12 ? 1 : 0);
      BlockPos $$14 = p_273743_.relative(p_273712_, $$13).above($$11);
      int $$15 = $$12 ? 2 : 1;

      for (int $$16 = 0; $$16 < $$15; $$16++) {
         this.placeLog(p_272736_, p_273092_, p_273449_, p_273496_.move(p_273712_), p_273027_, p_273558_);
      }

      Direction $$17 = $$14.getY() > p_273496_.getY() ? Direction.UP : Direction.DOWN;

      while (true) {
         int $$18 = p_273496_.distManhattan($$14);
         if ($$18 == 0) {
            return new FoliagePlacer.FoliageAttachment($$14.above(), 0, false);
         }

         float $$19 = (float)Math.abs($$14.getY() - p_273496_.getY()) / $$18;
         boolean $$20 = p_273449_.nextFloat() < $$19;
         p_273496_.move($$20 ? $$17 : p_273712_);
         this.placeLog(p_272736_, p_273092_, p_273449_, p_273496_, p_273027_, $$20 ? Function.identity() : p_273558_);
      }
   }
}
