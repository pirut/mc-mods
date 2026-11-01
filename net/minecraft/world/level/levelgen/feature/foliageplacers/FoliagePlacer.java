package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products.P2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.material.Fluids;

public abstract class FoliagePlacer {
   public static final Codec<FoliagePlacer> CODEC = BuiltInRegistries.FOLIAGE_PLACER_TYPE.byNameCodec().dispatch(FoliagePlacer::type, FoliagePlacerType::codec);
   protected final IntProvider radius;
   protected final IntProvider offset;

   protected static <P extends FoliagePlacer> P2<Mu<P>, IntProvider, IntProvider> foliagePlacerParts(Instance<P> p_68574_) {
      return p_68574_.group(
         IntProvider.codec(0, 16).fieldOf("radius").forGetter(p_161449_ -> p_161449_.radius),
         IntProvider.codec(0, 16).fieldOf("offset").forGetter(p_161447_ -> p_161447_.offset)
      );
   }

   public FoliagePlacer(IntProvider p_161411_, IntProvider p_161412_) {
      this.radius = p_161411_;
      this.offset = p_161412_;
   }

   protected abstract FoliagePlacerType<?> type();

   public void createFoliage(
      LevelSimulatedReader p_273526_,
      FoliagePlacer.FoliageSetter p_273018_,
      RandomSource p_273425_,
      TreeConfiguration p_273138_,
      int p_273282_,
      FoliagePlacer.FoliageAttachment p_272944_,
      int p_272930_,
      int p_272727_
   ) {
      this.createFoliage(p_273526_, p_273018_, p_273425_, p_273138_, p_273282_, p_272944_, p_272930_, p_272727_, this.offset(p_273425_));
   }

   protected abstract void createFoliage(
      LevelSimulatedReader var1,
      FoliagePlacer.FoliageSetter var2,
      RandomSource var3,
      TreeConfiguration var4,
      int var5,
      FoliagePlacer.FoliageAttachment var6,
      int var7,
      int var8,
      int var9
   );

   public abstract int foliageHeight(RandomSource var1, int var2, TreeConfiguration var3);

   public int foliageRadius(RandomSource p_225593_, int p_225594_) {
      return this.radius.sample(p_225593_);
   }

   private int offset(RandomSource p_225592_) {
      return this.offset.sample(p_225592_);
   }

   protected abstract boolean shouldSkipLocation(RandomSource var1, int var2, int var3, int var4, int var5, boolean var6);

   protected boolean shouldSkipLocationSigned(RandomSource p_225639_, int p_225640_, int p_225641_, int p_225642_, int p_225643_, boolean p_225644_) {
      int $$6;
      int $$7;
      if (p_225644_) {
         $$6 = Math.min(Math.abs(p_225640_), Math.abs(p_225640_ - 1));
         $$7 = Math.min(Math.abs(p_225642_), Math.abs(p_225642_ - 1));
      } else {
         $$6 = Math.abs(p_225640_);
         $$7 = Math.abs(p_225642_);
      }

      return this.shouldSkipLocation(p_225639_, $$6, p_225641_, $$7, p_225643_, p_225644_);
   }

   protected void placeLeavesRow(
      LevelSimulatedReader p_225629_,
      FoliagePlacer.FoliageSetter p_272772_,
      RandomSource p_225631_,
      TreeConfiguration p_225632_,
      BlockPos p_225633_,
      int p_225634_,
      int p_225635_,
      boolean p_225636_
   ) {
      int $$8 = p_225636_ ? 1 : 0;
      BlockPos.MutableBlockPos $$9 = new BlockPos.MutableBlockPos();

      for (int $$10 = -p_225634_; $$10 <= p_225634_ + $$8; $$10++) {
         for (int $$11 = -p_225634_; $$11 <= p_225634_ + $$8; $$11++) {
            if (!this.shouldSkipLocationSigned(p_225631_, $$10, p_225635_, $$11, p_225634_, p_225636_)) {
               $$9.setWithOffset(p_225633_, $$10, p_225635_, $$11);
               tryPlaceLeaf(p_225629_, p_272772_, p_225631_, p_225632_, $$9);
            }
         }
      }
   }

   protected final void placeLeavesRowWithHangingLeavesBelow(
      LevelSimulatedReader p_273087_,
      FoliagePlacer.FoliageSetter p_273225_,
      RandomSource p_272629_,
      TreeConfiguration p_272885_,
      BlockPos p_273412_,
      int p_272712_,
      int p_272656_,
      boolean p_272689_,
      float p_273464_,
      float p_273068_
   ) {
      this.placeLeavesRow(p_273087_, p_273225_, p_272629_, p_272885_, p_273412_, p_272712_, p_272656_, p_272689_);
      int $$10 = p_272689_ ? 1 : 0;
      BlockPos $$11 = p_273412_.below();
      BlockPos.MutableBlockPos $$12 = new BlockPos.MutableBlockPos();

      for (Direction $$13 : Direction.Plane.HORIZONTAL) {
         Direction $$14 = $$13.getClockWise();
         int $$15 = $$14.getAxisDirection() == Direction.AxisDirection.POSITIVE ? p_272712_ + $$10 : p_272712_;
         $$12.setWithOffset(p_273412_, 0, p_272656_ - 1, 0).move($$14, $$15).move($$13, -p_272712_);
         int $$16 = -p_272712_;

         while ($$16 < p_272712_ + $$10) {
            boolean $$17 = p_273225_.isSet($$12.move(Direction.UP));
            $$12.move(Direction.DOWN);
            if ($$17 && tryPlaceExtension(p_273087_, p_273225_, p_272629_, p_272885_, p_273464_, $$11, $$12)) {
               $$12.move(Direction.DOWN);
               tryPlaceExtension(p_273087_, p_273225_, p_272629_, p_272885_, p_273068_, $$11, $$12);
               $$12.move(Direction.UP);
            }

            $$16++;
            $$12.move($$13);
         }
      }
   }

   private static boolean tryPlaceExtension(
      LevelSimulatedReader p_277577_,
      FoliagePlacer.FoliageSetter p_277449_,
      RandomSource p_277966_,
      TreeConfiguration p_277897_,
      float p_277979_,
      BlockPos p_277833_,
      BlockPos.MutableBlockPos p_277567_
   ) {
      if (p_277567_.distManhattan(p_277833_) >= 7) {
         return false;
      } else {
         return p_277966_.nextFloat() > p_277979_ ? false : tryPlaceLeaf(p_277577_, p_277449_, p_277966_, p_277897_, p_277567_);
      }
   }

   protected static boolean tryPlaceLeaf(
      LevelSimulatedReader p_273596_, FoliagePlacer.FoliageSetter p_273054_, RandomSource p_272977_, TreeConfiguration p_273040_, BlockPos p_273406_
   ) {
      if (!TreeFeature.validTreePos(p_273596_, p_273406_)) {
         return false;
      } else {
         BlockState $$5 = p_273040_.foliageProvider.getState(p_272977_, p_273406_);
         if ($$5.hasProperty(BlockStateProperties.WATERLOGGED)) {
            $$5 = $$5.setValue(BlockStateProperties.WATERLOGGED, p_273596_.isFluidAtPosition(p_273406_, p_225638_ -> p_225638_.isSourceOfType(Fluids.WATER)));
         }

         p_273054_.set(p_273406_, $$5);
         return true;
      }
   }

   public static final class FoliageAttachment {
      private final BlockPos pos;
      private final int radiusOffset;
      private final boolean doubleTrunk;

      public FoliageAttachment(BlockPos p_68585_, int p_68586_, boolean p_68587_) {
         this.pos = p_68585_;
         this.radiusOffset = p_68586_;
         this.doubleTrunk = p_68587_;
      }

      public BlockPos pos() {
         return this.pos;
      }

      public int radiusOffset() {
         return this.radiusOffset;
      }

      public boolean doubleTrunk() {
         return this.doubleTrunk;
      }
   }

   public interface FoliageSetter {
      void set(BlockPos var1, BlockState var2);

      boolean isSet(BlockPos var1);
   }
}
