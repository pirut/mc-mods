package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.UnderwaterMagmaConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class UnderwaterMagmaFeature extends Feature<UnderwaterMagmaConfiguration> {
   public UnderwaterMagmaFeature(Codec<UnderwaterMagmaConfiguration> p_160560_) {
      super(p_160560_);
   }

   @Override
   public boolean place(FeaturePlaceContext<UnderwaterMagmaConfiguration> p_160569_) {
      WorldGenLevel $$1 = p_160569_.level();
      BlockPos $$2 = p_160569_.origin();
      UnderwaterMagmaConfiguration $$3 = p_160569_.config();
      RandomSource $$4 = p_160569_.random();
      OptionalInt $$5 = getFloorY($$1, $$2, $$3);
      if ($$5.isEmpty()) {
         return false;
      } else {
         BlockPos $$6 = $$2.atY($$5.getAsInt());
         Vec3i $$7 = new Vec3i($$3.placementRadiusAroundFloor, $$3.placementRadiusAroundFloor, $$3.placementRadiusAroundFloor);
         BoundingBox $$8 = BoundingBox.fromCorners($$6.subtract($$7), $$6.offset($$7));
         return BlockPos.betweenClosedStream($$8)
               .filter(p_225310_ -> $$4.nextFloat() < $$3.placementProbabilityPerValidPosition)
               .filter(p_160584_ -> this.isValidPlacement($$1, p_160584_))
               .mapToInt(p_160579_ -> {
                  $$1.setBlock(p_160579_, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                  return 1;
               })
               .sum()
            > 0;
      }
   }

   private static OptionalInt getFloorY(WorldGenLevel p_160565_, BlockPos p_160566_, UnderwaterMagmaConfiguration p_160567_) {
      Predicate<BlockState> $$3 = p_160586_ -> p_160586_.is(Blocks.WATER);
      Predicate<BlockState> $$4 = p_160581_ -> !p_160581_.is(Blocks.WATER);
      Optional<Column> $$5 = Column.scan(p_160565_, p_160566_, p_160567_.floorSearchRange, $$3, $$4);
      return $$5.map(Column::getFloor).orElseGet(OptionalInt::empty);
   }

   private boolean isValidPlacement(WorldGenLevel p_160575_, BlockPos p_160576_) {
      if (!this.isWaterOrAir(p_160575_, p_160576_) && !this.isWaterOrAir(p_160575_, p_160576_.below())) {
         for (Direction $$2 : Direction.Plane.HORIZONTAL) {
            if (this.isWaterOrAir(p_160575_, p_160576_.relative($$2))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isWaterOrAir(LevelAccessor p_160562_, BlockPos p_160563_) {
      BlockState $$2 = p_160562_.getBlockState(p_160563_);
      return $$2.is(Blocks.WATER) || $$2.isAir();
   }
}
