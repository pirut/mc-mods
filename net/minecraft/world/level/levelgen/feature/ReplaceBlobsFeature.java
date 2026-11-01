package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceSphereConfiguration;
import org.jspecify.annotations.Nullable;

public class ReplaceBlobsFeature extends Feature<ReplaceSphereConfiguration> {
   public ReplaceBlobsFeature(Codec<ReplaceSphereConfiguration> codec) {
      super(codec);
   }

   @Override
   public boolean place(FeaturePlaceContext<ReplaceSphereConfiguration> context) {
      ReplaceSphereConfiguration replaceSphereConfiguration = context.config();
      WorldGenLevel worldGenLevel = context.level();
      RandomSource randomSource = context.random();
      Block block = replaceSphereConfiguration.targetState.getBlock();
      BlockPos blockPos = findTarget(
         worldGenLevel, context.origin().mutable().clamp(Direction.Axis.Y, worldGenLevel.getMinY() + 1, worldGenLevel.getMaxY()), block
      );
      if (blockPos == null) {
         return false;
      } else {
         int i = replaceSphereConfiguration.radius().sample(randomSource);
         int j = replaceSphereConfiguration.radius().sample(randomSource);
         int k = replaceSphereConfiguration.radius().sample(randomSource);
         int l = Math.max(i, Math.max(j, k));
         boolean bl = false;

         for (BlockPos blockPos2 : BlockPos.withinManhattan(blockPos, i, j, k)) {
            if (blockPos2.distManhattan(blockPos) > l) {
               break;
            }

            BlockState blockState = worldGenLevel.getBlockState(blockPos2);
            if (blockState.is(block)) {
               this.setBlock(worldGenLevel, blockPos2, replaceSphereConfiguration.replaceState);
               bl = true;
            }
         }

         return bl;
      }
   }

   @Nullable
   private static BlockPos findTarget(LevelAccessor level, BlockPos.MutableBlockPos topPos, Block block) {
      while (topPos.getY() > level.getMinY() + 1) {
         BlockState blockState = level.getBlockState(topPos);
         if (blockState.is(block)) {
            return topPos;
         }

         topPos.move(Direction.DOWN);
      }

      return null;
   }
}
