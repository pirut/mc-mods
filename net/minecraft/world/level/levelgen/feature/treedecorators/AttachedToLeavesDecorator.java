package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AttachedToLeavesDecorator extends TreeDecorator {
   public static final MapCodec<AttachedToLeavesDecorator> CODEC = RecordCodecBuilder.mapCodec(
      p_225996_ -> p_225996_.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(p_226014_ -> p_226014_.probability),
            Codec.intRange(0, 16).fieldOf("exclusion_radius_xz").forGetter(p_226012_ -> p_226012_.exclusionRadiusXZ),
            Codec.intRange(0, 16).fieldOf("exclusion_radius_y").forGetter(p_226010_ -> p_226010_.exclusionRadiusY),
            BlockStateProvider.CODEC.fieldOf("block_provider").forGetter(p_226008_ -> p_226008_.blockProvider),
            Codec.intRange(1, 16).fieldOf("required_empty_blocks").forGetter(p_226006_ -> p_226006_.requiredEmptyBlocks),
            ExtraCodecs.nonEmptyList(Direction.CODEC.listOf()).fieldOf("directions").forGetter(p_225998_ -> p_225998_.directions)
         )
         .apply(p_225996_, AttachedToLeavesDecorator::new)
   );
   protected final float probability;
   protected final int exclusionRadiusXZ;
   protected final int exclusionRadiusY;
   protected final BlockStateProvider blockProvider;
   protected final int requiredEmptyBlocks;
   protected final List<Direction> directions;

   public AttachedToLeavesDecorator(float p_225988_, int p_225989_, int p_225990_, BlockStateProvider p_225991_, int p_225992_, List<Direction> p_225993_) {
      this.probability = p_225988_;
      this.exclusionRadiusXZ = p_225989_;
      this.exclusionRadiusY = p_225990_;
      this.blockProvider = p_225991_;
      this.requiredEmptyBlocks = p_225992_;
      this.directions = p_225993_;
   }

   @Override
   public void place(TreeDecorator.Context p_226000_) {
      Set<BlockPos> $$1 = new HashSet<>();
      RandomSource $$2 = p_226000_.random();

      for (BlockPos $$3 : Util.shuffledCopy(p_226000_.leaves(), $$2)) {
         Direction $$4 = Util.getRandom(this.directions, $$2);
         BlockPos $$5 = $$3.relative($$4);
         if (!$$1.contains($$5) && $$2.nextFloat() < this.probability && this.hasRequiredEmptyBlocks(p_226000_, $$3, $$4)) {
            BlockPos $$6 = $$5.offset(-this.exclusionRadiusXZ, -this.exclusionRadiusY, -this.exclusionRadiusXZ);
            BlockPos $$7 = $$5.offset(this.exclusionRadiusXZ, this.exclusionRadiusY, this.exclusionRadiusXZ);

            for (BlockPos $$8 : BlockPos.betweenClosed($$6, $$7)) {
               $$1.add($$8.immutable());
            }

            p_226000_.setBlock($$5, this.blockProvider.getState($$2, $$5));
         }
      }
   }

   private boolean hasRequiredEmptyBlocks(TreeDecorator.Context p_226002_, BlockPos p_226003_, Direction p_226004_) {
      for (int $$3 = 1; $$3 <= this.requiredEmptyBlocks; $$3++) {
         BlockPos $$4 = p_226003_.relative(p_226004_, $$3);
         if (!p_226002_.isAir($$4)) {
            return false;
         }
      }

      return true;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.ATTACHED_TO_LEAVES;
   }
}
