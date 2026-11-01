package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceSphereConfiguration;

public class ReplaceBlobsFeature extends Feature<ReplaceSphereConfiguration> {
   public ReplaceBlobsFeature(Codec<ReplaceSphereConfiguration> p_66633_) {
      super(p_66633_);
   }

   @Override
   public boolean place(FeaturePlaceContext<ReplaceSphereConfiguration> p_160214_) {
      ReplaceSphereConfiguration $$1 = p_160214_.config();
      WorldGenLevel $$2 = p_160214_.level();
      RandomSource $$3 = p_160214_.random();
      Block $$4 = $$1.targetState.getBlock();
      BlockPos $$5 = findTarget($$2, p_160214_.origin().mutable().clamp(Direction.Axis.Y, $$2.getMinBuildHeight() + 1, $$2.getMaxBuildHeight() - 1), $$4);
      if ($$5 == null) {
         return false;
      } else {
         int $$6 = $$1.radius().sample($$3);
         int $$7 = $$1.radius().sample($$3);
         int $$8 = $$1.radius().sample($$3);
         int $$9 = Math.max($$6, Math.max($$7, $$8));
         boolean $$10 = false;

         for (BlockPos $$11 : BlockPos.withinManhattan($$5, $$6, $$7, $$8)) {
            if ($$11.distManhattan($$5) > $$9) {
               break;
            }

            BlockState $$12 = $$2.getBlockState($$11);
            if ($$12.is($$4)) {
               this.setBlock($$2, $$11, $$1.replaceState);
               $$10 = true;
            }
         }

         return $$10;
      }
   }

   @Nullable
   private static BlockPos findTarget(LevelAccessor p_66635_, BlockPos.MutableBlockPos p_66636_, Block p_66637_) {
      while (p_66636_.getY() > p_66635_.getMinBuildHeight() + 1) {
         BlockState $$3 = p_66635_.getBlockState(p_66636_);
         if ($$3.is(p_66637_)) {
            return p_66636_;
         }

         p_66636_.move(Direction.DOWN);
      }

      return null;
   }
}
