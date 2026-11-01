package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class DarkOakFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<DarkOakFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      instance -> foliagePlacerParts(instance).apply(instance, DarkOakFoliagePlacer::new)
   );

   public DarkOakFoliagePlacer(IntProvider intProvider, IntProvider intProvider2) {
      super(intProvider, intProvider2);
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.DARK_OAK_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader level,
      FoliagePlacer.FoliageSetter blockSetter,
      RandomSource random,
      TreeConfiguration config,
      int maxFreeTreeHeight,
      FoliagePlacer.FoliageAttachment attachment,
      int foliageHeight,
      int foliageRadius,
      int offset
   ) {
      BlockPos blockPos = attachment.pos().above(offset);
      boolean bl = attachment.doubleTrunk();
      if (bl) {
         this.placeLeavesRow(level, blockSetter, random, config, blockPos, foliageRadius + 2, -1, bl);
         this.placeLeavesRow(level, blockSetter, random, config, blockPos, foliageRadius + 3, 0, bl);
         this.placeLeavesRow(level, blockSetter, random, config, blockPos, foliageRadius + 2, 1, bl);
         if (random.nextBoolean()) {
            this.placeLeavesRow(level, blockSetter, random, config, blockPos, foliageRadius, 2, bl);
         }
      } else {
         this.placeLeavesRow(level, blockSetter, random, config, blockPos, foliageRadius + 2, -1, bl);
         this.placeLeavesRow(level, blockSetter, random, config, blockPos, foliageRadius + 1, 0, bl);
      }
   }

   @Override
   public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
      return 4;
   }

   @Override
   protected boolean shouldSkipLocationSigned(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
      return localY != 0 || !large || localX != -range && localX < range || localZ != -range && localZ < range
         ? super.shouldSkipLocationSigned(random, localX, localY, localZ, range, large)
         : true;
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
      if (localY == -1 && !large) {
         return localX == range && localZ == range;
      } else {
         return localY == 1 ? localX + localZ > range * 2 - 2 : false;
      }
   }
}
