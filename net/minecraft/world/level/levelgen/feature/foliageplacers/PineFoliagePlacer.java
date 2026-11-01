package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class PineFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<PineFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68698_ -> foliagePlacerParts(p_68698_)
         .and(IntProvider.codec(0, 24).fieldOf("height").forGetter(p_161500_ -> p_161500_.height))
         .apply(p_68698_, PineFoliagePlacer::new)
   );
   private final IntProvider height;

   public PineFoliagePlacer(IntProvider p_161486_, IntProvider p_161487_, IntProvider p_161488_) {
      super(p_161486_, p_161487_);
      this.height = p_161488_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.PINE_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_225702_,
      FoliagePlacer.FoliageSetter p_272791_,
      RandomSource p_225704_,
      TreeConfiguration p_225705_,
      int p_225706_,
      FoliagePlacer.FoliageAttachment p_225707_,
      int p_225708_,
      int p_225709_,
      int p_225710_
   ) {
      int $$9 = 0;

      for (int $$10 = p_225710_; $$10 >= p_225710_ - p_225708_; $$10--) {
         this.placeLeavesRow(p_225702_, p_272791_, p_225704_, p_225705_, p_225707_.pos(), $$9, $$10, p_225707_.doubleTrunk());
         if ($$9 >= 1 && $$10 == p_225710_ - p_225708_ + 1) {
            $$9--;
         } else if ($$9 < p_225709_ + p_225707_.radiusOffset()) {
            $$9++;
         }
      }
   }

   @Override
   public int foliageRadius(RandomSource p_225688_, int p_225689_) {
      return super.foliageRadius(p_225688_, p_225689_) + p_225688_.nextInt(Math.max(p_225689_ + 1, 1));
   }

   @Override
   public int foliageHeight(RandomSource p_225698_, int p_225699_, TreeConfiguration p_225700_) {
      return this.height.sample(p_225698_);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_225691_, int p_225692_, int p_225693_, int p_225694_, int p_225695_, boolean p_225696_) {
      return p_225692_ == p_225695_ && p_225694_ == p_225695_ && p_225695_ > 0;
   }
}
