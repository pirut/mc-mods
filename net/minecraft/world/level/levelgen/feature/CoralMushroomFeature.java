package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralMushroomFeature extends CoralFeature {
   public CoralMushroomFeature(Codec<NoneFeatureConfiguration> p_65452_) {
      super(p_65452_);
   }

   @Override
   protected boolean placeFeature(LevelAccessor p_224982_, RandomSource p_224983_, BlockPos p_224984_, BlockState p_224985_) {
      int $$4 = p_224983_.nextInt(3) + 3;
      int $$5 = p_224983_.nextInt(3) + 3;
      int $$6 = p_224983_.nextInt(3) + 3;
      int $$7 = p_224983_.nextInt(3) + 1;
      BlockPos.MutableBlockPos $$8 = p_224984_.mutable();

      for (int $$9 = 0; $$9 <= $$5; $$9++) {
         for (int $$10 = 0; $$10 <= $$4; $$10++) {
            for (int $$11 = 0; $$11 <= $$6; $$11++) {
               $$8.set($$9 + p_224984_.getX(), $$10 + p_224984_.getY(), $$11 + p_224984_.getZ());
               $$8.move(Direction.DOWN, $$7);
               if (($$9 != 0 && $$9 != $$5 || $$10 != 0 && $$10 != $$4)
                  && ($$11 != 0 && $$11 != $$6 || $$10 != 0 && $$10 != $$4)
                  && ($$9 != 0 && $$9 != $$5 || $$11 != 0 && $$11 != $$6)
                  && ($$9 == 0 || $$9 == $$5 || $$10 == 0 || $$10 == $$4 || $$11 == 0 || $$11 == $$6)
                  && !(p_224983_.nextFloat() < 0.1F)
                  && !this.placeCoralBlock(p_224982_, p_224983_, $$8, p_224985_)) {
               }
            }
         }
      }

      return true;
   }
}
