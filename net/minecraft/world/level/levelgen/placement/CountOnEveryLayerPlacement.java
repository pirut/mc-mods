package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

@Deprecated
public class CountOnEveryLayerPlacement extends PlacementModifier {
   public static final MapCodec<CountOnEveryLayerPlacement> CODEC = IntProvider.codec(0, 256)
      .fieldOf("count")
      .xmap(CountOnEveryLayerPlacement::new, p_191611_ -> p_191611_.count);
   private final IntProvider count;

   private CountOnEveryLayerPlacement(IntProvider p_191603_) {
      this.count = p_191603_;
   }

   public static CountOnEveryLayerPlacement of(IntProvider p_191607_) {
      return new CountOnEveryLayerPlacement(p_191607_);
   }

   public static CountOnEveryLayerPlacement of(int p_191605_) {
      return of(ConstantInt.of(p_191605_));
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext p_226329_, RandomSource p_226330_, BlockPos p_226331_) {
      Builder<BlockPos> $$3 = Stream.builder();
      int $$4 = 0;

      boolean $$5;
      do {
         $$5 = false;

         for (int $$6 = 0; $$6 < this.count.sample(p_226330_); $$6++) {
            int $$7 = p_226330_.nextInt(16) + p_226331_.getX();
            int $$8 = p_226330_.nextInt(16) + p_226331_.getZ();
            int $$9 = p_226329_.getHeight(Heightmap.Types.MOTION_BLOCKING, $$7, $$8);
            int $$10 = findOnGroundYPosition(p_226329_, $$7, $$9, $$8, $$4);
            if ($$10 != Integer.MAX_VALUE) {
               $$3.add(new BlockPos($$7, $$10, $$8));
               $$5 = true;
            }
         }

         $$4++;
      } while ($$5);

      return $$3.build();
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.COUNT_ON_EVERY_LAYER;
   }

   private static int findOnGroundYPosition(PlacementContext p_191613_, int p_191614_, int p_191615_, int p_191616_, int p_191617_) {
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos(p_191614_, p_191615_, p_191616_);
      int $$6 = 0;
      BlockState $$7 = p_191613_.getBlockState($$5);

      for (int $$8 = p_191615_; $$8 >= p_191613_.getMinBuildHeight() + 1; $$8--) {
         $$5.setY($$8 - 1);
         BlockState $$9 = p_191613_.getBlockState($$5);
         if (!isEmpty($$9) && isEmpty($$7) && !$$9.is(Blocks.BEDROCK)) {
            if ($$6 == p_191617_) {
               return $$5.getY() + 1;
            }

            $$6++;
         }

         $$7 = $$9;
      }

      return Integer.MAX_VALUE;
   }

   private static boolean isEmpty(BlockState p_191609_) {
      return p_191609_.isAir() || p_191609_.is(Blocks.WATER) || p_191609_.is(Blocks.LAVA);
   }
}
