package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class VegetationPatchFeature extends Feature<VegetationPatchConfiguration> {
   public VegetationPatchFeature(Codec<VegetationPatchConfiguration> p_160588_) {
      super(p_160588_);
   }

   @Override
   public boolean place(FeaturePlaceContext<VegetationPatchConfiguration> p_160612_) {
      WorldGenLevel $$1 = p_160612_.level();
      VegetationPatchConfiguration $$2 = p_160612_.config();
      RandomSource $$3 = p_160612_.random();
      BlockPos $$4 = p_160612_.origin();
      Predicate<BlockState> $$5 = p_204782_ -> p_204782_.is($$2.replaceable);
      int $$6 = $$2.xzRadius.sample($$3) + 1;
      int $$7 = $$2.xzRadius.sample($$3) + 1;
      Set<BlockPos> $$8 = this.placeGroundPatch($$1, $$2, $$3, $$4, $$5, $$6, $$7);
      this.distributeVegetation(p_160612_, $$1, $$2, $$3, $$8, $$6, $$7);
      return !$$8.isEmpty();
   }

   protected Set<BlockPos> placeGroundPatch(
      WorldGenLevel p_225311_,
      VegetationPatchConfiguration p_225312_,
      RandomSource p_225313_,
      BlockPos p_225314_,
      Predicate<BlockState> p_225315_,
      int p_225316_,
      int p_225317_
   ) {
      BlockPos.MutableBlockPos $$7 = p_225314_.mutable();
      BlockPos.MutableBlockPos $$8 = $$7.mutable();
      Direction $$9 = p_225312_.surface.getDirection();
      Direction $$10 = $$9.getOpposite();
      Set<BlockPos> $$11 = new HashSet<>();

      for (int $$12 = -p_225316_; $$12 <= p_225316_; $$12++) {
         boolean $$13 = $$12 == -p_225316_ || $$12 == p_225316_;

         for (int $$14 = -p_225317_; $$14 <= p_225317_; $$14++) {
            boolean $$15 = $$14 == -p_225317_ || $$14 == p_225317_;
            boolean $$16 = $$13 || $$15;
            boolean $$17 = $$13 && $$15;
            boolean $$18 = $$16 && !$$17;
            if (!$$17 && (!$$18 || p_225312_.extraEdgeColumnChance != 0.0F && !(p_225313_.nextFloat() > p_225312_.extraEdgeColumnChance))) {
               $$7.setWithOffset(p_225314_, $$12, 0, $$14);

               for (int $$19 = 0; p_225311_.isStateAtPosition($$7, BlockBehaviour.BlockStateBase::isAir) && $$19 < p_225312_.verticalRange; $$19++) {
                  $$7.move($$9);
               }

               for (int var25 = 0; p_225311_.isStateAtPosition($$7, p_284926_ -> !p_284926_.isAir()) && var25 < p_225312_.verticalRange; var25++) {
                  $$7.move($$10);
               }

               $$8.setWithOffset($$7, p_225312_.surface.getDirection());
               BlockState $$20 = p_225311_.getBlockState($$8);
               if (p_225311_.isEmptyBlock($$7) && $$20.isFaceSturdy(p_225311_, $$8, p_225312_.surface.getDirection().getOpposite())) {
                  int $$21 = p_225312_.depth.sample(p_225313_)
                     + (p_225312_.extraBottomBlockChance > 0.0F && p_225313_.nextFloat() < p_225312_.extraBottomBlockChance ? 1 : 0);
                  BlockPos $$22 = $$8.immutable();
                  boolean $$23 = this.placeGround(p_225311_, p_225312_, p_225315_, p_225313_, $$8, $$21);
                  if ($$23) {
                     $$11.add($$22);
                  }
               }
            }
         }
      }

      return $$11;
   }

   protected void distributeVegetation(
      FeaturePlaceContext<VegetationPatchConfiguration> p_225331_,
      WorldGenLevel p_225332_,
      VegetationPatchConfiguration p_225333_,
      RandomSource p_225334_,
      Set<BlockPos> p_225335_,
      int p_225336_,
      int p_225337_
   ) {
      for (BlockPos $$7 : p_225335_) {
         if (p_225333_.vegetationChance > 0.0F && p_225334_.nextFloat() < p_225333_.vegetationChance) {
            this.placeVegetation(p_225332_, p_225333_, p_225331_.chunkGenerator(), p_225334_, $$7);
         }
      }
   }

   protected boolean placeVegetation(
      WorldGenLevel p_225318_, VegetationPatchConfiguration p_225319_, ChunkGenerator p_225320_, RandomSource p_225321_, BlockPos p_225322_
   ) {
      return p_225319_.vegetationFeature.value().place(p_225318_, p_225320_, p_225321_, p_225322_.relative(p_225319_.surface.getDirection().getOpposite()));
   }

   protected boolean placeGround(
      WorldGenLevel p_225324_,
      VegetationPatchConfiguration p_225325_,
      Predicate<BlockState> p_225326_,
      RandomSource p_225327_,
      BlockPos.MutableBlockPos p_225328_,
      int p_225329_
   ) {
      for (int $$6 = 0; $$6 < p_225329_; $$6++) {
         BlockState $$7 = p_225325_.groundState.getState(p_225327_, p_225328_);
         BlockState $$8 = p_225324_.getBlockState(p_225328_);
         if (!$$7.is($$8.getBlock())) {
            if (!p_225326_.test($$8)) {
               return $$6 != 0;
            }

            p_225324_.setBlock(p_225328_, $$7, 2);
            p_225328_.move(p_225325_.surface.getDirection());
         }
      }

      return true;
   }
}
