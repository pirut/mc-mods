package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class SpruceFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<SpruceFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68735_ -> foliagePlacerParts(p_68735_)
         .and(IntProvider.codec(0, 24).fieldOf("trunk_height").forGetter(p_161553_ -> p_161553_.trunkHeight))
         .apply(p_68735_, SpruceFoliagePlacer::new)
   );
   private final IntProvider trunkHeight;

   public SpruceFoliagePlacer(IntProvider p_161539_, IntProvider p_161540_, IntProvider p_161541_) {
      super(p_161539_, p_161540_);
      this.trunkHeight = p_161541_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.SPRUCE_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_225744_,
      FoliagePlacer.FoliageSetter p_273256_,
      RandomSource p_225746_,
      TreeConfiguration p_225747_,
      int p_225748_,
      FoliagePlacer.FoliageAttachment p_225749_,
      int p_225750_,
      int p_225751_,
      int p_225752_
   ) {
      BlockPos $$9 = p_225749_.pos();
      int $$10 = p_225746_.nextInt(2);
      int $$11 = 1;
      int $$12 = 0;

      for (int $$13 = p_225752_; $$13 >= -p_225750_; $$13--) {
         this.placeLeavesRow(p_225744_, p_273256_, p_225746_, p_225747_, $$9, $$10, $$13, p_225749_.doubleTrunk());
         if ($$10 >= $$11) {
            $$10 = $$12;
            $$12 = 1;
            $$11 = Math.min($$11 + 1, p_225751_ + p_225749_.radiusOffset());
         } else {
            $$10++;
         }
      }
   }

   @Override
   public int foliageHeight(RandomSource p_225740_, int p_225741_, TreeConfiguration p_225742_) {
      return Math.max(4, p_225741_ - this.trunkHeight.sample(p_225740_));
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_225733_, int p_225734_, int p_225735_, int p_225736_, int p_225737_, boolean p_225738_) {
      return p_225734_ == p_225737_ && p_225736_ == p_225737_ && p_225737_ > 0;
   }
}
