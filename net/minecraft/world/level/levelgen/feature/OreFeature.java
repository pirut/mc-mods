package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class OreFeature extends Feature<OreConfiguration> {
   public OreFeature(Codec<OreConfiguration> p_66531_) {
      super(p_66531_);
   }

   @Override
   public boolean place(FeaturePlaceContext<OreConfiguration> p_160177_) {
      RandomSource $$1 = p_160177_.random();
      BlockPos $$2 = p_160177_.origin();
      WorldGenLevel $$3 = p_160177_.level();
      OreConfiguration $$4 = p_160177_.config();
      float $$5 = $$1.nextFloat() * (float) Math.PI;
      float $$6 = $$4.size / 8.0F;
      int $$7 = Mth.ceil(($$4.size / 16.0F * 2.0F + 1.0F) / 2.0F);
      double $$8 = $$2.getX() + Math.sin($$5) * $$6;
      double $$9 = $$2.getX() - Math.sin($$5) * $$6;
      double $$10 = $$2.getZ() + Math.cos($$5) * $$6;
      double $$11 = $$2.getZ() - Math.cos($$5) * $$6;
      int $$12 = 2;
      double $$13 = $$2.getY() + $$1.nextInt(3) - 2;
      double $$14 = $$2.getY() + $$1.nextInt(3) - 2;
      int $$15 = $$2.getX() - Mth.ceil($$6) - $$7;
      int $$16 = $$2.getY() - 2 - $$7;
      int $$17 = $$2.getZ() - Mth.ceil($$6) - $$7;
      int $$18 = 2 * (Mth.ceil($$6) + $$7);
      int $$19 = 2 * (2 + $$7);

      for (int $$20 = $$15; $$20 <= $$15 + $$18; $$20++) {
         for (int $$21 = $$17; $$21 <= $$17 + $$18; $$21++) {
            if ($$16 <= $$3.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, $$20, $$21)) {
               return this.doPlace($$3, $$1, $$4, $$8, $$9, $$10, $$11, $$13, $$14, $$15, $$16, $$17, $$18, $$19);
            }
         }
      }

      return false;
   }

   protected boolean doPlace(
      WorldGenLevel p_225172_,
      RandomSource p_225173_,
      OreConfiguration p_225174_,
      double p_225175_,
      double p_225176_,
      double p_225177_,
      double p_225178_,
      double p_225179_,
      double p_225180_,
      int p_225181_,
      int p_225182_,
      int p_225183_,
      int p_225184_,
      int p_225185_
   ) {
      int $$14 = 0;
      BitSet $$15 = new BitSet(p_225184_ * p_225185_ * p_225184_);
      BlockPos.MutableBlockPos $$16 = new BlockPos.MutableBlockPos();
      int $$17 = p_225174_.size;
      double[] $$18 = new double[$$17 * 4];

      for (int $$19 = 0; $$19 < $$17; $$19++) {
         float $$20 = (float)$$19 / $$17;
         double $$21 = Mth.lerp((double)$$20, p_225175_, p_225176_);
         double $$22 = Mth.lerp((double)$$20, p_225179_, p_225180_);
         double $$23 = Mth.lerp((double)$$20, p_225177_, p_225178_);
         double $$24 = p_225173_.nextDouble() * $$17 / 16.0;
         double $$25 = ((Mth.sin((float) Math.PI * $$20) + 1.0F) * $$24 + 1.0) / 2.0;
         $$18[$$19 * 4 + 0] = $$21;
         $$18[$$19 * 4 + 1] = $$22;
         $$18[$$19 * 4 + 2] = $$23;
         $$18[$$19 * 4 + 3] = $$25;
      }

      for (int $$26 = 0; $$26 < $$17 - 1; $$26++) {
         if (!($$18[$$26 * 4 + 3] <= 0.0)) {
            for (int $$27 = $$26 + 1; $$27 < $$17; $$27++) {
               if (!($$18[$$27 * 4 + 3] <= 0.0)) {
                  double $$28 = $$18[$$26 * 4 + 0] - $$18[$$27 * 4 + 0];
                  double $$29 = $$18[$$26 * 4 + 1] - $$18[$$27 * 4 + 1];
                  double $$30 = $$18[$$26 * 4 + 2] - $$18[$$27 * 4 + 2];
                  double $$31 = $$18[$$26 * 4 + 3] - $$18[$$27 * 4 + 3];
                  if ($$31 * $$31 > $$28 * $$28 + $$29 * $$29 + $$30 * $$30) {
                     if ($$31 > 0.0) {
                        $$18[$$27 * 4 + 3] = -1.0;
                     } else {
                        $$18[$$26 * 4 + 3] = -1.0;
                     }
                  }
               }
            }
         }
      }

      try (BulkSectionAccess $$32 = new BulkSectionAccess(p_225172_)) {
         for (int $$33 = 0; $$33 < $$17; $$33++) {
            double $$34 = $$18[$$33 * 4 + 3];
            if (!($$34 < 0.0)) {
               double $$35 = $$18[$$33 * 4 + 0];
               double $$36 = $$18[$$33 * 4 + 1];
               double $$37 = $$18[$$33 * 4 + 2];
               int $$38 = Math.max(Mth.floor($$35 - $$34), p_225181_);
               int $$39 = Math.max(Mth.floor($$36 - $$34), p_225182_);
               int $$40 = Math.max(Mth.floor($$37 - $$34), p_225183_);
               int $$41 = Math.max(Mth.floor($$35 + $$34), $$38);
               int $$42 = Math.max(Mth.floor($$36 + $$34), $$39);
               int $$43 = Math.max(Mth.floor($$37 + $$34), $$40);

               for (int $$44 = $$38; $$44 <= $$41; $$44++) {
                  double $$45 = ($$44 + 0.5 - $$35) / $$34;
                  if ($$45 * $$45 < 1.0) {
                     for (int $$46 = $$39; $$46 <= $$42; $$46++) {
                        double $$47 = ($$46 + 0.5 - $$36) / $$34;
                        if ($$45 * $$45 + $$47 * $$47 < 1.0) {
                           for (int $$48 = $$40; $$48 <= $$43; $$48++) {
                              double $$49 = ($$48 + 0.5 - $$37) / $$34;
                              if ($$45 * $$45 + $$47 * $$47 + $$49 * $$49 < 1.0 && !p_225172_.isOutsideBuildHeight($$46)) {
                                 int $$50 = $$44 - p_225181_ + ($$46 - p_225182_) * p_225184_ + ($$48 - p_225183_) * p_225184_ * p_225185_;
                                 if (!$$15.get($$50)) {
                                    $$15.set($$50);
                                    $$16.set($$44, $$46, $$48);
                                    if (p_225172_.ensureCanWrite($$16)) {
                                       LevelChunkSection $$51 = $$32.getSection($$16);
                                       if ($$51 != null) {
                                          int $$52 = SectionPos.sectionRelative($$44);
                                          int $$53 = SectionPos.sectionRelative($$46);
                                          int $$54 = SectionPos.sectionRelative($$48);
                                          BlockState $$55 = $$51.getBlockState($$52, $$53, $$54);

                                          for (OreConfiguration.TargetBlockState $$56 : p_225174_.targetStates) {
                                             if (canPlaceOre($$55, $$32::getBlockState, p_225173_, p_225174_, $$56, $$16)) {
                                                $$51.setBlockState($$52, $$53, $$54, $$56.state, false);
                                                $$14++;
                                                break;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return $$14 > 0;
   }

   public static boolean canPlaceOre(
      BlockState p_225187_,
      Function<BlockPos, BlockState> p_225188_,
      RandomSource p_225189_,
      OreConfiguration p_225190_,
      OreConfiguration.TargetBlockState p_225191_,
      BlockPos.MutableBlockPos p_225192_
   ) {
      if (!p_225191_.target.test(p_225187_, p_225189_)) {
         return false;
      } else {
         return shouldSkipAirCheck(p_225189_, p_225190_.discardChanceOnAirExposure) ? true : !isAdjacentToAir(p_225188_, p_225192_);
      }
   }

   protected static boolean shouldSkipAirCheck(RandomSource p_225169_, float p_225170_) {
      if (p_225170_ <= 0.0F) {
         return true;
      } else {
         return p_225170_ >= 1.0F ? false : p_225169_.nextFloat() >= p_225170_;
      }
   }
}
