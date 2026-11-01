package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class DiskFeature extends Feature<DiskConfiguration> {
   public DiskFeature(Codec<DiskConfiguration> p_224992_) {
      super(p_224992_);
   }

   @Override
   public boolean place(FeaturePlaceContext<DiskConfiguration> p_224994_) {
      DiskConfiguration $$1 = p_224994_.config();
      BlockPos $$2 = p_224994_.origin();
      WorldGenLevel $$3 = p_224994_.level();
      RandomSource $$4 = p_224994_.random();
      boolean $$5 = false;
      int $$6 = $$2.getY();
      int $$7 = $$6 + $$1.halfHeight();
      int $$8 = $$6 - $$1.halfHeight() - 1;
      int $$9 = $$1.radius().sample($$4);
      BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();

      for (BlockPos $$11 : BlockPos.betweenClosed($$2.offset(-$$9, 0, -$$9), $$2.offset($$9, 0, $$9))) {
         int $$12 = $$11.getX() - $$2.getX();
         int $$13 = $$11.getZ() - $$2.getZ();
         if ($$12 * $$12 + $$13 * $$13 <= $$9 * $$9) {
            $$5 |= this.placeColumn($$1, $$3, $$4, $$7, $$8, $$10.set($$11));
         }
      }

      return $$5;
   }

   protected boolean placeColumn(
      DiskConfiguration p_224996_, WorldGenLevel p_224997_, RandomSource p_224998_, int p_224999_, int p_225000_, BlockPos.MutableBlockPos p_225001_
   ) {
      boolean $$6 = false;

      for (int $$7 = p_224999_; $$7 > p_225000_; $$7--) {
         p_225001_.setY($$7);
         if (p_224996_.target().test(p_224997_, p_225001_)) {
            BlockState $$8 = p_224996_.stateProvider().getState(p_224997_, p_224998_, p_225001_);
            p_224997_.setBlock(p_225001_, $$8, 2);
            this.markAboveForPostProcessing(p_224997_, p_225001_);
            $$6 = true;
         }
      }

      return $$6;
   }
}
