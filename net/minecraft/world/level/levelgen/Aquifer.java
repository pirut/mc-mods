package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.mutable.MutableDouble;

public interface Aquifer {
   static Aquifer create(
      NoiseChunk p_223881_,
      ChunkPos p_223882_,
      NoiseRouter p_223883_,
      PositionalRandomFactory p_223884_,
      int p_223885_,
      int p_223886_,
      Aquifer.FluidPicker p_223887_
   ) {
      return new Aquifer.NoiseBasedAquifer(p_223881_, p_223882_, p_223883_, p_223884_, p_223885_, p_223886_, p_223887_);
   }

   static Aquifer createDisabled(final Aquifer.FluidPicker p_188375_) {
      return new Aquifer() {
         @Nullable
         @Override
         public BlockState computeSubstance(DensityFunction.FunctionContext p_208172_, double p_208173_) {
            return p_208173_ > 0.0 ? null : p_188375_.computeFluid(p_208172_.blockX(), p_208172_.blockY(), p_208172_.blockZ()).at(p_208172_.blockY());
         }

         @Override
         public boolean shouldScheduleFluidUpdate() {
            return false;
         }
      };
   }

   @Nullable
   BlockState computeSubstance(DensityFunction.FunctionContext var1, double var2);

   boolean shouldScheduleFluidUpdate();

   public interface FluidPicker {
      Aquifer.FluidStatus computeFluid(int var1, int var2, int var3);
   }

   public static final class FluidStatus {
      final int fluidLevel;
      final BlockState fluidType;

      public FluidStatus(int p_188403_, BlockState p_188404_) {
         this.fluidLevel = p_188403_;
         this.fluidType = p_188404_;
      }

      public BlockState at(int p_188406_) {
         return p_188406_ < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
      }
   }

   public static class NoiseBasedAquifer implements Aquifer {
      private static final int X_RANGE = 10;
      private static final int Y_RANGE = 9;
      private static final int Z_RANGE = 10;
      private static final int X_SEPARATION = 6;
      private static final int Y_SEPARATION = 3;
      private static final int Z_SEPARATION = 6;
      private static final int X_SPACING = 16;
      private static final int Y_SPACING = 12;
      private static final int Z_SPACING = 16;
      private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
      private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
      private final NoiseChunk noiseChunk;
      protected final DensityFunction barrierNoise;
      private final DensityFunction fluidLevelFloodednessNoise;
      private final DensityFunction fluidLevelSpreadNoise;
      protected final DensityFunction lavaNoise;
      private final PositionalRandomFactory positionalRandomFactory;
      protected final Aquifer.FluidStatus[] aquiferCache;
      protected final long[] aquiferLocationCache;
      private final Aquifer.FluidPicker globalFluidPicker;
      private final DensityFunction erosion;
      private final DensityFunction depth;
      protected boolean shouldScheduleFluidUpdate;
      protected final int minGridX;
      protected final int minGridY;
      protected final int minGridZ;
      protected final int gridSizeX;
      protected final int gridSizeZ;
      private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{
         {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
      };

      NoiseBasedAquifer(
         NoiseChunk p_223891_,
         ChunkPos p_223892_,
         NoiseRouter p_223893_,
         PositionalRandomFactory p_223894_,
         int p_223895_,
         int p_223896_,
         Aquifer.FluidPicker p_223897_
      ) {
         this.noiseChunk = p_223891_;
         this.barrierNoise = p_223893_.barrierNoise();
         this.fluidLevelFloodednessNoise = p_223893_.fluidLevelFloodednessNoise();
         this.fluidLevelSpreadNoise = p_223893_.fluidLevelSpreadNoise();
         this.lavaNoise = p_223893_.lavaNoise();
         this.erosion = p_223893_.erosion();
         this.depth = p_223893_.depth();
         this.positionalRandomFactory = p_223894_;
         this.minGridX = this.gridX(p_223892_.getMinBlockX()) - 1;
         this.globalFluidPicker = p_223897_;
         int $$7 = this.gridX(p_223892_.getMaxBlockX()) + 1;
         this.gridSizeX = $$7 - this.minGridX + 1;
         this.minGridY = this.gridY(p_223895_) - 1;
         int $$8 = this.gridY(p_223895_ + p_223896_) + 1;
         int $$9 = $$8 - this.minGridY + 1;
         this.minGridZ = this.gridZ(p_223892_.getMinBlockZ()) - 1;
         int $$10 = this.gridZ(p_223892_.getMaxBlockZ()) + 1;
         this.gridSizeZ = $$10 - this.minGridZ + 1;
         int $$11 = this.gridSizeX * $$9 * this.gridSizeZ;
         this.aquiferCache = new Aquifer.FluidStatus[$$11];
         this.aquiferLocationCache = new long[$$11];
         Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
      }

      protected int getIndex(int p_158028_, int p_158029_, int p_158030_) {
         int $$3 = p_158028_ - this.minGridX;
         int $$4 = p_158029_ - this.minGridY;
         int $$5 = p_158030_ - this.minGridZ;
         return ($$4 * this.gridSizeZ + $$5) * this.gridSizeX + $$3;
      }

      @Nullable
      @Override
      public BlockState computeSubstance(DensityFunction.FunctionContext p_208186_, double p_208187_) {
         int $$2 = p_208186_.blockX();
         int $$3 = p_208186_.blockY();
         int $$4 = p_208186_.blockZ();
         if (p_208187_ > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return null;
         } else {
            Aquifer.FluidStatus $$5 = this.globalFluidPicker.computeFluid($$2, $$3, $$4);
            if ($$5.at($$3).is(Blocks.LAVA)) {
               this.shouldScheduleFluidUpdate = false;
               return Blocks.LAVA.defaultBlockState();
            } else {
               int $$6 = Math.floorDiv($$2 - 5, 16);
               int $$7 = Math.floorDiv($$3 + 1, 12);
               int $$8 = Math.floorDiv($$4 - 5, 16);
               int $$9 = Integer.MAX_VALUE;
               int $$10 = Integer.MAX_VALUE;
               int $$11 = Integer.MAX_VALUE;
               long $$12 = 0L;
               long $$13 = 0L;
               long $$14 = 0L;

               for (int $$15 = 0; $$15 <= 1; $$15++) {
                  for (int $$16 = -1; $$16 <= 1; $$16++) {
                     for (int $$17 = 0; $$17 <= 1; $$17++) {
                        int $$18 = $$6 + $$15;
                        int $$19 = $$7 + $$16;
                        int $$20 = $$8 + $$17;
                        int $$21 = this.getIndex($$18, $$19, $$20);
                        long $$22 = this.aquiferLocationCache[$$21];
                        long $$23;
                        if ($$22 != Long.MAX_VALUE) {
                           $$23 = $$22;
                        } else {
                           RandomSource $$24 = this.positionalRandomFactory.at($$18, $$19, $$20);
                           $$23 = BlockPos.asLong($$18 * 16 + $$24.nextInt(10), $$19 * 12 + $$24.nextInt(9), $$20 * 16 + $$24.nextInt(10));
                           this.aquiferLocationCache[$$21] = $$23;
                        }

                        int $$26 = BlockPos.getX($$23) - $$2;
                        int $$27 = BlockPos.getY($$23) - $$3;
                        int $$28 = BlockPos.getZ($$23) - $$4;
                        int $$29 = $$26 * $$26 + $$27 * $$27 + $$28 * $$28;
                        if ($$9 >= $$29) {
                           $$14 = $$13;
                           $$13 = $$12;
                           $$12 = $$23;
                           $$11 = $$10;
                           $$10 = $$9;
                           $$9 = $$29;
                        } else if ($$10 >= $$29) {
                           $$14 = $$13;
                           $$13 = $$23;
                           $$11 = $$10;
                           $$10 = $$29;
                        } else if ($$11 >= $$29) {
                           $$14 = $$23;
                           $$11 = $$29;
                        }
                     }
                  }
               }

               Aquifer.FluidStatus $$30 = this.getAquiferStatus($$12);
               double $$31 = similarity($$9, $$10);
               BlockState $$32 = $$30.at($$3);
               if ($$31 <= 0.0) {
                  this.shouldScheduleFluidUpdate = $$31 >= FLOWING_UPDATE_SIMULARITY;
                  return $$32;
               } else if ($$32.is(Blocks.WATER) && this.globalFluidPicker.computeFluid($$2, $$3 - 1, $$4).at($$3 - 1).is(Blocks.LAVA)) {
                  this.shouldScheduleFluidUpdate = true;
                  return $$32;
               } else {
                  MutableDouble $$34 = new MutableDouble(Double.NaN);
                  Aquifer.FluidStatus $$35 = this.getAquiferStatus($$13);
                  double $$36 = $$31 * this.calculatePressure(p_208186_, $$34, $$30, $$35);
                  if (p_208187_ + $$36 > 0.0) {
                     this.shouldScheduleFluidUpdate = false;
                     return null;
                  } else {
                     Aquifer.FluidStatus $$37 = this.getAquiferStatus($$14);
                     double $$38 = similarity($$9, $$11);
                     if ($$38 > 0.0) {
                        double $$39 = $$31 * $$38 * this.calculatePressure(p_208186_, $$34, $$30, $$37);
                        if (p_208187_ + $$39 > 0.0) {
                           this.shouldScheduleFluidUpdate = false;
                           return null;
                        }
                     }

                     double $$40 = similarity($$10, $$11);
                     if ($$40 > 0.0) {
                        double $$41 = $$31 * $$40 * this.calculatePressure(p_208186_, $$34, $$35, $$37);
                        if (p_208187_ + $$41 > 0.0) {
                           this.shouldScheduleFluidUpdate = false;
                           return null;
                        }
                     }

                     this.shouldScheduleFluidUpdate = true;
                     return $$32;
                  }
               }
            }
         }
      }

      @Override
      public boolean shouldScheduleFluidUpdate() {
         return this.shouldScheduleFluidUpdate;
      }

      protected static double similarity(int p_158025_, int p_158026_) {
         double $$2 = 25.0;
         return 1.0 - Math.abs(p_158026_ - p_158025_) / 25.0;
      }

      private double calculatePressure(
         DensityFunction.FunctionContext p_208189_, MutableDouble p_208190_, Aquifer.FluidStatus p_208191_, Aquifer.FluidStatus p_208192_
      ) {
         int $$4 = p_208189_.blockY();
         BlockState $$5 = p_208191_.at($$4);
         BlockState $$6 = p_208192_.at($$4);
         if ((!$$5.is(Blocks.LAVA) || !$$6.is(Blocks.WATER)) && (!$$5.is(Blocks.WATER) || !$$6.is(Blocks.LAVA))) {
            int $$7 = Math.abs(p_208191_.fluidLevel - p_208192_.fluidLevel);
            if ($$7 == 0) {
               return 0.0;
            } else {
               double $$8 = 0.5 * (p_208191_.fluidLevel + p_208192_.fluidLevel);
               double $$9 = $$4 + 0.5 - $$8;
               double $$10 = $$7 / 2.0;
               double $$11 = 0.0;
               double $$12 = 2.5;
               double $$13 = 1.5;
               double $$14 = 3.0;
               double $$15 = 10.0;
               double $$16 = 3.0;
               double $$17 = $$10 - Math.abs($$9);
               double $$19;
               if ($$9 > 0.0) {
                  double $$18 = 0.0 + $$17;
                  if ($$18 > 0.0) {
                     $$19 = $$18 / 1.5;
                  } else {
                     $$19 = $$18 / 2.5;
                  }
               } else {
                  double $$21 = 3.0 + $$17;
                  if ($$21 > 0.0) {
                     $$19 = $$21 / 3.0;
                  } else {
                     $$19 = $$21 / 10.0;
                  }
               }

               double $$24 = 2.0;
               double $$28;
               if (!($$19 < -2.0) && !($$19 > 2.0)) {
                  double $$26 = p_208190_.getValue();
                  if (Double.isNaN($$26)) {
                     double $$27 = this.barrierNoise.compute(p_208189_);
                     p_208190_.setValue($$27);
                     $$28 = $$27;
                  } else {
                     $$28 = $$26;
                  }
               } else {
                  $$28 = 0.0;
               }

               return 2.0 * ($$28 + $$19);
            }
         } else {
            return 2.0;
         }
      }

      protected int gridX(int p_158040_) {
         return Math.floorDiv(p_158040_, 16);
      }

      protected int gridY(int p_158046_) {
         return Math.floorDiv(p_158046_, 12);
      }

      protected int gridZ(int p_158048_) {
         return Math.floorDiv(p_158048_, 16);
      }

      private Aquifer.FluidStatus getAquiferStatus(long p_188446_) {
         int $$1 = BlockPos.getX(p_188446_);
         int $$2 = BlockPos.getY(p_188446_);
         int $$3 = BlockPos.getZ(p_188446_);
         int $$4 = this.gridX($$1);
         int $$5 = this.gridY($$2);
         int $$6 = this.gridZ($$3);
         int $$7 = this.getIndex($$4, $$5, $$6);
         Aquifer.FluidStatus $$8 = this.aquiferCache[$$7];
         if ($$8 != null) {
            return $$8;
         } else {
            Aquifer.FluidStatus $$9 = this.computeFluid($$1, $$2, $$3);
            this.aquiferCache[$$7] = $$9;
            return $$9;
         }
      }

      private Aquifer.FluidStatus computeFluid(int p_188448_, int p_188449_, int p_188450_) {
         Aquifer.FluidStatus $$3 = this.globalFluidPicker.computeFluid(p_188448_, p_188449_, p_188450_);
         int $$4 = Integer.MAX_VALUE;
         int $$5 = p_188449_ + 12;
         int $$6 = p_188449_ - 12;
         boolean $$7 = false;

         for (int[] $$8 : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int $$9 = p_188448_ + SectionPos.sectionToBlockCoord($$8[0]);
            int $$10 = p_188450_ + SectionPos.sectionToBlockCoord($$8[1]);
            int $$11 = this.noiseChunk.preliminarySurfaceLevel($$9, $$10);
            int $$12 = $$11 + 8;
            boolean $$13 = $$8[0] == 0 && $$8[1] == 0;
            if ($$13 && $$6 > $$12) {
               return $$3;
            }

            boolean $$14 = $$5 > $$12;
            if ($$14 || $$13) {
               Aquifer.FluidStatus $$15 = this.globalFluidPicker.computeFluid($$9, $$12, $$10);
               if (!$$15.at($$12).isAir()) {
                  if ($$13) {
                     $$7 = true;
                  }

                  if ($$14) {
                     return $$15;
                  }
               }
            }

            $$4 = Math.min($$4, $$11);
         }

         int $$16 = this.computeSurfaceLevel(p_188448_, p_188449_, p_188450_, $$3, $$4, $$7);
         return new Aquifer.FluidStatus($$16, this.computeFluidType(p_188448_, p_188449_, p_188450_, $$3, $$16));
      }

      private int computeSurfaceLevel(int p_223910_, int p_223911_, int p_223912_, Aquifer.FluidStatus p_223913_, int p_223914_, boolean p_223915_) {
         DensityFunction.SinglePointContext $$6 = new DensityFunction.SinglePointContext(p_223910_, p_223911_, p_223912_);
         double $$7;
         double $$8;
         if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, $$6)) {
            $$7 = -1.0;
            $$8 = -1.0;
         } else {
            int $$9 = p_223914_ + 8 - p_223911_;
            int $$10 = 64;
            double $$11 = p_223915_ ? Mth.clampedMap((double)$$9, 0.0, 64.0, 1.0, 0.0) : 0.0;
            double $$12 = Mth.clamp(this.fluidLevelFloodednessNoise.compute($$6), -1.0, 1.0);
            double $$13 = Mth.map($$11, 1.0, 0.0, -0.3, 0.8);
            double $$14 = Mth.map($$11, 1.0, 0.0, -0.8, 0.4);
            $$7 = $$12 - $$14;
            $$8 = $$12 - $$13;
         }

         int $$17;
         if ($$8 > 0.0) {
            $$17 = p_223913_.fluidLevel;
         } else if ($$7 > 0.0) {
            $$17 = this.computeRandomizedFluidSurfaceLevel(p_223910_, p_223911_, p_223912_, p_223914_);
         } else {
            $$17 = DimensionType.WAY_BELOW_MIN_Y;
         }

         return $$17;
      }

      private int computeRandomizedFluidSurfaceLevel(int p_223899_, int p_223900_, int p_223901_, int p_223902_) {
         int $$4 = 16;
         int $$5 = 40;
         int $$6 = Math.floorDiv(p_223899_, 16);
         int $$7 = Math.floorDiv(p_223900_, 40);
         int $$8 = Math.floorDiv(p_223901_, 16);
         int $$9 = $$7 * 40 + 20;
         int $$10 = 10;
         double $$11 = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext($$6, $$7, $$8)) * 10.0;
         int $$12 = Mth.quantize($$11, 3);
         int $$13 = $$9 + $$12;
         return Math.min(p_223902_, $$13);
      }

      private BlockState computeFluidType(int p_223904_, int p_223905_, int p_223906_, Aquifer.FluidStatus p_223907_, int p_223908_) {
         BlockState $$5 = p_223907_.fluidType;
         if (p_223908_ <= -10 && p_223908_ != DimensionType.WAY_BELOW_MIN_Y && p_223907_.fluidType != Blocks.LAVA.defaultBlockState()) {
            int $$6 = 64;
            int $$7 = 40;
            int $$8 = Math.floorDiv(p_223904_, 64);
            int $$9 = Math.floorDiv(p_223905_, 40);
            int $$10 = Math.floorDiv(p_223906_, 64);
            double $$11 = this.lavaNoise.compute(new DensityFunction.SinglePointContext($$8, $$9, $$10));
            if (Math.abs($$11) > 0.3) {
               $$5 = Blocks.LAVA.defaultBlockState();
            }
         }

         return $$5;
      }
   }
}
