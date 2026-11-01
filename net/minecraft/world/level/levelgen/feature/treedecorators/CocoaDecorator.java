package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;

public class CocoaDecorator extends TreeDecorator {
   public static final MapCodec<CocoaDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(CocoaDecorator::new, p_69989_ -> p_69989_.probability);
   private final float probability;

   public CocoaDecorator(float p_69976_) {
      this.probability = p_69976_;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.COCOA;
   }

   @Override
   public void place(TreeDecorator.Context p_226028_) {
      RandomSource $$1 = p_226028_.random();
      if (!($$1.nextFloat() >= this.probability)) {
         List<BlockPos> $$2 = p_226028_.logs();
         int $$3 = $$2.get(0).getY();
         $$2.stream().filter(p_69980_ -> p_69980_.getY() - $$3 <= 2).forEach(p_226026_ -> {
            for (Direction $$3x : Direction.Plane.HORIZONTAL) {
               if ($$1.nextFloat() <= 0.25F) {
                  Direction $$4 = $$3x.getOpposite();
                  BlockPos $$5 = p_226026_.offset($$4.getStepX(), 0, $$4.getStepZ());
                  if (p_226028_.isAir($$5)) {
                     p_226028_.setBlock($$5, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, $$1.nextInt(3)).setValue(CocoaBlock.FACING, $$3x));
                  }
               }
            }
         });
      }
   }
}
