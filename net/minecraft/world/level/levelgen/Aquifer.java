package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import net.minecraft.SharedConstants;
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
import org.jspecify.annotations.Nullable;

public interface Aquifer {
   static Aquifer create(
      NoiseChunk chunk,
      ChunkPos chunkPos,
      NoiseRouter noiseRouter,
      PositionalRandomFactory positionalRandomFactory,
      int minY,
      int height,
      Aquifer.FluidPicker globalFluidPicker
   ) {
      return new Aquifer.NoiseBasedAquifer(chunk, chunkPos, noiseRouter, positionalRandomFactory, minY, height, globalFluidPicker);
   }

   static Aquifer createDisabled(Aquifer.FluidPicker defaultFluid) {
      return new Aquifer() {
         @Nullable
         @Override
         public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
            return substance > 0.0 ? null : defaultFluid.computeFluid(context.blockX(), context.blockY(), context.blockZ()).at(context.blockY());
         }

         @Override
         public boolean shouldScheduleFluidUpdate() {
            return false;
         }
      };
   }

   @Nullable
   BlockState computeSubstance(DensityFunction.FunctionContext context, double substance);

   boolean shouldScheduleFluidUpdate();

   public interface FluidPicker {
      Aquifer.FluidStatus computeFluid(int i, int j, int k);
   }

   public record FluidStatus(int fluidLevel, BlockState fluidType) {

      public BlockState at(int y) {
         return y < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
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
      private static final int X_SPACING_SHIFT = 4;
      private static final int Z_SPACING_SHIFT = 4;
      private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
      private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
      private static final int SAMPLE_OFFSET_X = -5;
      private static final int SAMPLE_OFFSET_Y = 1;
      private static final int SAMPLE_OFFSET_Z = -5;
      private static final int MIN_CELL_SAMPLE_X = 0;
      private static final int MIN_CELL_SAMPLE_Y = -1;
      private static final int MIN_CELL_SAMPLE_Z = 0;
      private static final int MAX_CELL_SAMPLE_X = 1;
      private static final int MAX_CELL_SAMPLE_Y = 1;
      private static final int MAX_CELL_SAMPLE_Z = 1;
      private final NoiseChunk noiseChunk;
      private final DensityFunction barrierNoise;
      private final DensityFunction fluidLevelFloodednessNoise;
      private final DensityFunction fluidLevelSpreadNoise;
      private final DensityFunction lavaNoise;
      private final PositionalRandomFactory positionalRandomFactory;
      private final Aquifer.FluidStatus[] aquiferCache;
      private final long[] aquiferLocationCache;
      private final Aquifer.FluidPicker globalFluidPicker;
      private final DensityFunction erosion;
      private final DensityFunction depth;
      private boolean shouldScheduleFluidUpdate;
      private final int skipSamplingAboveY;
      private final int minGridX;
      private final int minGridY;
      private final int minGridZ;
      private final int gridSizeX;
      private final int gridSizeZ;
      private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{
         {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
      };

      NoiseBasedAquifer(
         NoiseChunk noiseChunk,
         ChunkPos chunkPos,
         NoiseRouter noiseRouter,
         PositionalRandomFactory positionalRandomFactory,
         int minY,
         int height,
         Aquifer.FluidPicker globalFluidPicker
      ) {
         this.noiseChunk = noiseChunk;
         this.barrierNoise = noiseRouter.barrierNoise();
         this.fluidLevelFloodednessNoise = noiseRouter.fluidLevelFloodednessNoise();
         this.fluidLevelSpreadNoise = noiseRouter.fluidLevelSpreadNoise();
         this.lavaNoise = noiseRouter.lavaNoise();
         this.erosion = noiseRouter.erosion();
         this.depth = noiseRouter.depth();
         this.positionalRandomFactory = positionalRandomFactory;
         this.minGridX = gridX(chunkPos.getMinBlockX() + -5) + 0;
         this.globalFluidPicker = globalFluidPicker;
         int i = gridX(chunkPos.getMaxBlockX() + -5) + 1;
         this.gridSizeX = i - this.minGridX + 1;
         this.minGridY = gridY(minY + 1) + -1;
         int j = gridY(minY + height + 1) + 1;
         int k = j - this.minGridY + 1;
         this.minGridZ = gridZ(chunkPos.getMinBlockZ() + -5) + 0;
         int l = gridZ(chunkPos.getMaxBlockZ() + -5) + 1;
         this.gridSizeZ = l - this.minGridZ + 1;
         int m = this.gridSizeX * k * this.gridSizeZ;
         this.aquiferCache = new Aquifer.FluidStatus[m];
         this.aquiferLocationCache = new long[m];
         Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
         int n = this.adjustSurfaceLevel(
            noiseChunk.maxPreliminarySurfaceLevel(fromGridX(this.minGridX, 0), fromGridZ(this.minGridZ, 0), fromGridX(i, 9), fromGridZ(l, 9))
         );
         int o = gridY(n + 12) - -1;
         this.skipSamplingAboveY = fromGridY(o, 11) - 1;
      }

      private int getIndex(int gridX, int gridY, int gridZ) {
         int i = gridX - this.minGridX;
         int j = gridY - this.minGridY;
         int k = gridZ - this.minGridZ;
         return (j * this.gridSizeZ + k) * this.gridSizeX + i;
      }

      @Nullable
      @Override
      public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
         if (substance > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return null;
         } else {
            int i = context.blockX();
            int j = context.blockY();
            int k = context.blockZ();
            Aquifer.FluidStatus fluidStatus = this.globalFluidPicker.computeFluid(i, j, k);
            if (j > this.skipSamplingAboveY) {
               this.shouldScheduleFluidUpdate = false;
               return fluidStatus.at(j);
            } else if (fluidStatus.at(j).is(Blocks.LAVA)) {
               this.shouldScheduleFluidUpdate = false;
               return SharedConstants.DEBUG_DISABLE_FLUID_GENERATION ? Blocks.AIR.defaultBlockState() : Blocks.LAVA.defaultBlockState();
            } else {
               int l = gridX(i + -5);
               int m = gridY(j + 1);
               int n = gridZ(k + -5);
               int o = Integer.MAX_VALUE;
               int p = Integer.MAX_VALUE;
               int q = Integer.MAX_VALUE;
               int r = Integer.MAX_VALUE;
               int s = 0;
               int t = 0;
               int u = 0;
               int v = 0;

               for (int w = 0; w <= 1; w++) {
                  for (int x = -1; x <= 1; x++) {
                     for (int y = 0; y <= 1; y++) {
                        int z = l + w;
                        int aa = m + x;
                        int ab = n + y;
                        int ac = this.getIndex(z, aa, ab);
                        long ad = this.aquiferLocationCache[ac];
                        long ae;
                        if (ad != Long.MAX_VALUE) {
                           ae = ad;
                        } else {
                           RandomSource randomSource = this.positionalRandomFactory.at(z, aa, ab);
                           ae = BlockPos.asLong(
                              fromGridX(z, randomSource.nextInt(10)), fromGridY(aa, randomSource.nextInt(9)), fromGridZ(ab, randomSource.nextInt(10))
                           );
                           this.aquiferLocationCache[ac] = ae;
                        }

                        int af = BlockPos.getX(ae) - i;
                        int ag = BlockPos.getY(ae) - j;
                        int ah = BlockPos.getZ(ae) - k;
                        int ai = af * af + ag * ag + ah * ah;
                        if (o >= ai) {
                           v = u;
                           u = t;
                           t = s;
                           s = ac;
                           r = q;
                           q = p;
                           p = o;
                           o = ai;
                        } else if (p >= ai) {
                           v = u;
                           u = t;
                           t = ac;
                           r = q;
                           q = p;
                           p = ai;
                        } else if (q >= ai) {
                           v = u;
                           u = ac;
                           r = q;
                           q = ai;
                        } else if (r >= ai) {
                           v = ac;
                           r = ai;
                        }
                     }
                  }
               }

               Aquifer.FluidStatus fluidStatus2 = this.getAquiferStatus(s);
               double d = similarity(o, p);
               BlockState blockState = fluidStatus2.at(j);
               BlockState blockState2 = SharedConstants.DEBUG_DISABLE_FLUID_GENERATION ? Blocks.AIR.defaultBlockState() : blockState;
               if (d <= 0.0) {
                  if (d >= FLOWING_UPDATE_SIMULARITY) {
                     Aquifer.FluidStatus fluidStatus3 = this.getAquiferStatus(t);
                     this.shouldScheduleFluidUpdate = !fluidStatus2.equals(fluidStatus3);
                  } else {
                     this.shouldScheduleFluidUpdate = false;
                  }

                  return blockState2;
               } else if (blockState.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(i, j - 1, k).at(j - 1).is(Blocks.LAVA)) {
                  this.shouldScheduleFluidUpdate = true;
                  return blockState2;
               } else {
                  MutableDouble mutableDouble = new MutableDouble(Double.NaN);
                  Aquifer.FluidStatus fluidStatus4 = this.getAquiferStatus(t);
                  double e = d * this.calculatePressure(context, mutableDouble, fluidStatus2, fluidStatus4);
                  if (substance + e > 0.0) {
                     this.shouldScheduleFluidUpdate = false;
                     return null;
                  } else {
                     Aquifer.FluidStatus fluidStatus5 = this.getAquiferStatus(u);
                     double f = similarity(o, q);
                     if (f > 0.0) {
                        double g = d * f * this.calculatePressure(context, mutableDouble, fluidStatus2, fluidStatus5);
                        if (substance + g > 0.0) {
                           this.shouldScheduleFluidUpdate = false;
                           return null;
                        }
                     }

                     double g = similarity(p, q);
                     if (g > 0.0) {
                        double h = d * g * this.calculatePressure(context, mutableDouble, fluidStatus4, fluidStatus5);
                        if (substance + h > 0.0) {
                           this.shouldScheduleFluidUpdate = false;
                           return null;
                        }
                     }

                     boolean bl = !fluidStatus2.equals(fluidStatus4);
                     boolean bl2 = g >= FLOWING_UPDATE_SIMULARITY && !fluidStatus4.equals(fluidStatus5);
                     boolean bl3 = f >= FLOWING_UPDATE_SIMULARITY && !fluidStatus2.equals(fluidStatus5);
                     if (!bl && !bl2 && !bl3) {
                        this.shouldScheduleFluidUpdate = f >= FLOWING_UPDATE_SIMULARITY
                           && similarity(o, r) >= FLOWING_UPDATE_SIMULARITY
                           && !fluidStatus2.equals(this.getAquiferStatus(v));
                     } else {
                        this.shouldScheduleFluidUpdate = true;
                     }

                     return blockState2;
                  }
               }
            }
         }
      }

      @Override
      public boolean shouldScheduleFluidUpdate() {
         return this.shouldScheduleFluidUpdate;
      }

      private static double similarity(int firstDistance, int secondDistance) {
         double d = 25.0;
         return 1.0 - (secondDistance - firstDistance) / 25.0;
      }

      private double calculatePressure(
         DensityFunction.FunctionContext context, MutableDouble substance, Aquifer.FluidStatus firstFluid, Aquifer.FluidStatus secondFluid
      ) {
         int i = context.blockY();
         BlockState blockState = firstFluid.at(i);
         BlockState blockState2 = secondFluid.at(i);
         if ((!blockState.is(Blocks.LAVA) || !blockState2.is(Blocks.WATER)) && (!blockState.is(Blocks.WATER) || !blockState2.is(Blocks.LAVA))) {
            int j = Math.abs(firstFluid.fluidLevel - secondFluid.fluidLevel);
            if (j == 0) {
               return 0.0;
            } else {
               double d = 0.5 * (firstFluid.fluidLevel + secondFluid.fluidLevel);
               double e = i + 0.5 - d;
               double f = j / 2.0;
               double g = 0.0;
               double h = 2.5;
               double k = 1.5;
               double l = 3.0;
               double m = 10.0;
               double n = 3.0;
               double o = f - Math.abs(e);
               double q;
               if (e > 0.0) {
                  double p = 0.0 + o;
                  if (p > 0.0) {
                     q = p / 1.5;
                  } else {
                     q = p / 2.5;
                  }
               } else {
                  double p = 3.0 + o;
                  if (p > 0.0) {
                     q = p / 3.0;
                  } else {
                     q = p / 10.0;
                  }
               }

               double px = 2.0;
               double r;
               if (!(q < -2.0) && !(q > 2.0)) {
                  double s = substance.doubleValue();
                  if (Double.isNaN(s)) {
                     double t = this.barrierNoise.compute(context);
                     substance.setValue(t);
                     r = t;
                  } else {
                     r = s;
                  }
               } else {
                  r = 0.0;
               }

               return 2.0 * (r + q);
            }
         } else {
            return 2.0;
         }
      }

      private static int gridX(int x) {
         return x >> 4;
      }

      private static int fromGridX(int gridX, int offset) {
         return (gridX << 4) + offset;
      }

      private static int gridY(int y) {
         return Math.floorDiv(y, 12);
      }

      private static int fromGridY(int gridY, int offset) {
         return gridY * 12 + offset;
      }

      private static int gridZ(int z) {
         return z >> 4;
      }

      private static int fromGridZ(int gridZ, int offset) {
         return (gridZ << 4) + offset;
      }

      private Aquifer.FluidStatus getAquiferStatus(int index) {
         Aquifer.FluidStatus fluidStatus = this.aquiferCache[index];
         if (fluidStatus != null) {
            return fluidStatus;
         } else {
            long l = this.aquiferLocationCache[index];
            Aquifer.FluidStatus fluidStatus2 = this.computeFluid(BlockPos.getX(l), BlockPos.getY(l), BlockPos.getZ(l));
            this.aquiferCache[index] = fluidStatus2;
            return fluidStatus2;
         }
      }

      private Aquifer.FluidStatus computeFluid(int x, int y, int z) {
         Aquifer.FluidStatus fluidStatus = this.globalFluidPicker.computeFluid(x, y, z);
         int i = Integer.MAX_VALUE;
         int j = y + 12;
         int k = y - 12;
         boolean bl = false;

         for (int[] is : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int l = x + SectionPos.sectionToBlockCoord(is[0]);
            int m = z + SectionPos.sectionToBlockCoord(is[1]);
            int n = this.noiseChunk.preliminarySurfaceLevel(l, m);
            int o = this.adjustSurfaceLevel(n);
            boolean bl2 = is[0] == 0 && is[1] == 0;
            if (bl2 && k > o) {
               return fluidStatus;
            }

            boolean bl3 = j > o;
            if (bl3 || bl2) {
               Aquifer.FluidStatus fluidStatus2 = this.globalFluidPicker.computeFluid(l, o, m);
               if (!fluidStatus2.at(o).isAir()) {
                  if (bl2) {
                     bl = true;
                  }

                  if (bl3) {
                     return fluidStatus2;
                  }
               }
            }

            i = Math.min(i, n);
         }

         int p = this.computeSurfaceLevel(x, y, z, fluidStatus, i, bl);
         return new Aquifer.FluidStatus(p, this.computeFluidType(x, y, z, fluidStatus, p));
      }

      private int adjustSurfaceLevel(int level) {
         return level + 8;
      }

      private int computeSurfaceLevel(int x, int y, int z, Aquifer.FluidStatus fluidStatus, int maxSurfaceLevel, boolean fluidPresent) {
         DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(x, y, z);
         double d;
         double e;
         if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, singlePointContext)) {
            d = -1.0;
            e = -1.0;
         } else {
            int i = maxSurfaceLevel + 8 - y;
            int j = 64;
            double f = fluidPresent ? Mth.clampedMap((double)i, 0.0, 64.0, 1.0, 0.0) : 0.0;
            double g = Mth.clamp(this.fluidLevelFloodednessNoise.compute(singlePointContext), -1.0, 1.0);
            double h = Mth.map(f, 1.0, 0.0, -0.3, 0.8);
            double k = Mth.map(f, 1.0, 0.0, -0.8, 0.4);
            d = g - k;
            e = g - h;
         }

         int i;
         if (e > 0.0) {
            i = fluidStatus.fluidLevel;
         } else if (d > 0.0) {
            i = this.computeRandomizedFluidSurfaceLevel(x, y, z, maxSurfaceLevel);
         } else {
            i = DimensionType.WAY_BELOW_MIN_Y;
         }

         return i;
      }

      private int computeRandomizedFluidSurfaceLevel(int x, int y, int z, int maxSurfaceLevel) {
         int i = 16;
         int j = 40;
         int k = Math.floorDiv(x, 16);
         int l = Math.floorDiv(y, 40);
         int m = Math.floorDiv(z, 16);
         int n = l * 40 + 20;
         int o = 10;
         double d = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext(k, l, m)) * 10.0;
         int p = Mth.quantize(d, 3);
         int q = n + p;
         return Math.min(maxSurfaceLevel, q);
      }

      private BlockState computeFluidType(int x, int y, int z, Aquifer.FluidStatus fluidStatus, int surfaceLevel) {
         BlockState blockState = fluidStatus.fluidType;
         if (surfaceLevel <= -10 && surfaceLevel != DimensionType.WAY_BELOW_MIN_Y && fluidStatus.fluidType != Blocks.LAVA.defaultBlockState()) {
            int i = 64;
            int j = 40;
            int k = Math.floorDiv(x, 64);
            int l = Math.floorDiv(y, 40);
            int m = Math.floorDiv(z, 64);
            double d = this.lavaNoise.compute(new DensityFunction.SinglePointContext(k, l, m));
            if (Math.abs(d) > 0.3) {
               blockState = Blocks.LAVA.defaultBlockState();
            }
         }

         return blockState;
      }
   }
}
