package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;

public class NoiseChunk implements DensityFunction.ContextProvider, DensityFunction.FunctionContext {
   private final NoiseSettings noiseSettings;
   final int cellCountXZ;
   final int cellCountY;
   final int cellNoiseMinY;
   private final int firstCellX;
   private final int firstCellZ;
   final int firstNoiseX;
   final int firstNoiseZ;
   final List<NoiseChunk.NoiseInterpolator> interpolators;
   final List<NoiseChunk.CacheAllInCell> cellCaches;
   private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();
   private final Long2IntMap preliminarySurfaceLevel = new Long2IntOpenHashMap();
   private final Aquifer aquifer;
   private final DensityFunction initialDensityNoJaggedness;
   private final NoiseChunk.BlockStateFiller blockStateRule;
   private final Blender blender;
   private final NoiseChunk.FlatCache blendAlpha;
   private final NoiseChunk.FlatCache blendOffset;
   private final DensityFunctions.BeardifierOrMarker beardifier;
   private long lastBlendingDataPos = ChunkPos.INVALID_CHUNK_POS;
   private Blender.BlendingOutput lastBlendingOutput = new Blender.BlendingOutput(1.0, 0.0);
   final int noiseSizeXZ;
   final int cellWidth;
   final int cellHeight;
   boolean interpolating;
   boolean fillingCell;
   private int cellStartBlockX;
   int cellStartBlockY;
   private int cellStartBlockZ;
   int inCellX;
   int inCellY;
   int inCellZ;
   long interpolationCounter;
   long arrayInterpolationCounter;
   int arrayIndex;
   private final DensityFunction.ContextProvider sliceFillingContextProvider = new DensityFunction.ContextProvider() {
      @Override
      public DensityFunction.FunctionContext forIndex(int p_209253_) {
         NoiseChunk.this.cellStartBlockY = (p_209253_ + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
         NoiseChunk.this.interpolationCounter++;
         NoiseChunk.this.inCellY = 0;
         NoiseChunk.this.arrayIndex = p_209253_;
         return NoiseChunk.this;
      }

      @Override
      public void fillAllDirectly(double[] p_209255_, DensityFunction p_209256_) {
         for (int $$2 = 0; $$2 < NoiseChunk.this.cellCountY + 1; $$2++) {
            NoiseChunk.this.cellStartBlockY = ($$2 + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
            NoiseChunk.this.interpolationCounter++;
            NoiseChunk.this.inCellY = 0;
            NoiseChunk.this.arrayIndex = $$2;
            p_209255_[$$2] = p_209256_.compute(NoiseChunk.this);
         }
      }
   };

   public static NoiseChunk forChunk(
      ChunkAccess p_224353_,
      RandomState p_224354_,
      DensityFunctions.BeardifierOrMarker p_224355_,
      NoiseGeneratorSettings p_224356_,
      Aquifer.FluidPicker p_224357_,
      Blender p_224358_
   ) {
      NoiseSettings $$6 = p_224356_.noiseSettings().clampToHeightAccessor(p_224353_);
      ChunkPos $$7 = p_224353_.getPos();
      int $$8 = 16 / $$6.getCellWidth();
      return new NoiseChunk($$8, p_224354_, $$7.getMinBlockX(), $$7.getMinBlockZ(), $$6, p_224355_, p_224356_, p_224357_, p_224358_);
   }

   public NoiseChunk(
      int p_224343_,
      RandomState p_224344_,
      int p_224345_,
      int p_224346_,
      NoiseSettings p_224347_,
      DensityFunctions.BeardifierOrMarker p_224348_,
      NoiseGeneratorSettings p_224349_,
      Aquifer.FluidPicker p_224350_,
      Blender p_224351_
   ) {
      this.noiseSettings = p_224347_;
      this.cellWidth = p_224347_.getCellWidth();
      this.cellHeight = p_224347_.getCellHeight();
      this.cellCountXZ = p_224343_;
      this.cellCountY = Mth.floorDiv(p_224347_.height(), this.cellHeight);
      this.cellNoiseMinY = Mth.floorDiv(p_224347_.minY(), this.cellHeight);
      this.firstCellX = Math.floorDiv(p_224345_, this.cellWidth);
      this.firstCellZ = Math.floorDiv(p_224346_, this.cellWidth);
      this.interpolators = Lists.newArrayList();
      this.cellCaches = Lists.newArrayList();
      this.firstNoiseX = QuartPos.fromBlock(p_224345_);
      this.firstNoiseZ = QuartPos.fromBlock(p_224346_);
      this.noiseSizeXZ = QuartPos.fromBlock(p_224343_ * this.cellWidth);
      this.blender = p_224351_;
      this.beardifier = p_224348_;
      this.blendAlpha = new NoiseChunk.FlatCache(new NoiseChunk.BlendAlpha(), false);
      this.blendOffset = new NoiseChunk.FlatCache(new NoiseChunk.BlendOffset(), false);

      for (int $$9 = 0; $$9 <= this.noiseSizeXZ; $$9++) {
         int $$10 = this.firstNoiseX + $$9;
         int $$11 = QuartPos.toBlock($$10);

         for (int $$12 = 0; $$12 <= this.noiseSizeXZ; $$12++) {
            int $$13 = this.firstNoiseZ + $$12;
            int $$14 = QuartPos.toBlock($$13);
            Blender.BlendingOutput $$15 = p_224351_.blendOffsetAndFactor($$11, $$14);
            this.blendAlpha.values[$$9][$$12] = $$15.alpha();
            this.blendOffset.values[$$9][$$12] = $$15.blendingOffset();
         }
      }

      NoiseRouter $$16 = p_224344_.router();
      NoiseRouter $$17 = $$16.mapAll(this::wrap);
      if (!p_224349_.isAquifersEnabled()) {
         this.aquifer = Aquifer.createDisabled(p_224350_);
      } else {
         int $$18 = SectionPos.blockToSectionCoord(p_224345_);
         int $$19 = SectionPos.blockToSectionCoord(p_224346_);
         this.aquifer = Aquifer.create(this, new ChunkPos($$18, $$19), $$17, p_224344_.aquiferRandom(), p_224347_.minY(), p_224347_.height(), p_224350_);
      }

      Builder<NoiseChunk.BlockStateFiller> $$20 = ImmutableList.builder();
      DensityFunction $$21 = DensityFunctions.cacheAllInCell(DensityFunctions.add($$17.finalDensity(), DensityFunctions.BeardifierMarker.INSTANCE))
         .mapAll(this::wrap);
      $$20.add((NoiseChunk.BlockStateFiller)p_209217_ -> this.aquifer.computeSubstance(p_209217_, $$21.compute(p_209217_)));
      if (p_224349_.oreVeinsEnabled()) {
         $$20.add(OreVeinifier.create($$17.veinToggle(), $$17.veinRidged(), $$17.veinGap(), p_224344_.oreRandom()));
      }

      this.blockStateRule = new MaterialRuleList($$20.build());
      this.initialDensityNoJaggedness = $$17.initialDensityWithoutJaggedness();
   }

   protected Climate.Sampler cachedClimateSampler(NoiseRouter p_224360_, List<Climate.ParameterPoint> p_224361_) {
      return new Climate.Sampler(
         p_224360_.temperature().mapAll(this::wrap),
         p_224360_.vegetation().mapAll(this::wrap),
         p_224360_.continents().mapAll(this::wrap),
         p_224360_.erosion().mapAll(this::wrap),
         p_224360_.depth().mapAll(this::wrap),
         p_224360_.ridges().mapAll(this::wrap),
         p_224361_
      );
   }

   @Nullable
   protected BlockState getInterpolatedState() {
      return this.blockStateRule.calculate(this);
   }

   @Override
   public int blockX() {
      return this.cellStartBlockX + this.inCellX;
   }

   @Override
   public int blockY() {
      return this.cellStartBlockY + this.inCellY;
   }

   @Override
   public int blockZ() {
      return this.cellStartBlockZ + this.inCellZ;
   }

   public int preliminarySurfaceLevel(int p_198257_, int p_198258_) {
      int $$2 = QuartPos.toBlock(QuartPos.fromBlock(p_198257_));
      int $$3 = QuartPos.toBlock(QuartPos.fromBlock(p_198258_));
      return this.preliminarySurfaceLevel.computeIfAbsent(ColumnPos.asLong($$2, $$3), this::computePreliminarySurfaceLevel);
   }

   private int computePreliminarySurfaceLevel(long p_198250_) {
      int $$1 = ColumnPos.getX(p_198250_);
      int $$2 = ColumnPos.getZ(p_198250_);
      int $$3 = this.noiseSettings.minY();

      for (int $$4 = $$3 + this.noiseSettings.height(); $$4 >= $$3; $$4 -= this.cellHeight) {
         if (this.initialDensityNoJaggedness.compute(new DensityFunction.SinglePointContext($$1, $$4, $$2)) > 0.390625) {
            return $$4;
         }
      }

      return Integer.MAX_VALUE;
   }

   @Override
   public Blender getBlender() {
      return this.blender;
   }

   private void fillSlice(boolean p_209221_, int p_209222_) {
      this.cellStartBlockX = p_209222_ * this.cellWidth;
      this.inCellX = 0;

      for (int $$2 = 0; $$2 < this.cellCountXZ + 1; $$2++) {
         int $$3 = this.firstCellZ + $$2;
         this.cellStartBlockZ = $$3 * this.cellWidth;
         this.inCellZ = 0;
         this.arrayInterpolationCounter++;

         for (NoiseChunk.NoiseInterpolator $$4 : this.interpolators) {
            double[] $$5 = (p_209221_ ? $$4.slice0 : $$4.slice1)[$$2];
            $$4.fillArray($$5, this.sliceFillingContextProvider);
         }
      }

      this.arrayInterpolationCounter++;
   }

   public void initializeForFirstCellX() {
      if (this.interpolating) {
         throw new IllegalStateException("Staring interpolation twice");
      } else {
         this.interpolating = true;
         this.interpolationCounter = 0L;
         this.fillSlice(true, this.firstCellX);
      }
   }

   public void advanceCellX(int p_188750_) {
      this.fillSlice(false, this.firstCellX + p_188750_ + 1);
      this.cellStartBlockX = (this.firstCellX + p_188750_) * this.cellWidth;
   }

   public NoiseChunk forIndex(int p_209240_) {
      int $$1 = Math.floorMod(p_209240_, this.cellWidth);
      int $$2 = Math.floorDiv(p_209240_, this.cellWidth);
      int $$3 = Math.floorMod($$2, this.cellWidth);
      int $$4 = this.cellHeight - 1 - Math.floorDiv($$2, this.cellWidth);
      this.inCellX = $$3;
      this.inCellY = $$4;
      this.inCellZ = $$1;
      this.arrayIndex = p_209240_;
      return this;
   }

   @Override
   public void fillAllDirectly(double[] p_209224_, DensityFunction p_209225_) {
      this.arrayIndex = 0;

      for (int $$2 = this.cellHeight - 1; $$2 >= 0; $$2--) {
         this.inCellY = $$2;

         for (int $$3 = 0; $$3 < this.cellWidth; $$3++) {
            this.inCellX = $$3;

            for (int $$4 = 0; $$4 < this.cellWidth; $$4++) {
               this.inCellZ = $$4;
               p_209224_[this.arrayIndex++] = p_209225_.compute(this);
            }
         }
      }
   }

   public void selectCellYZ(int p_188811_, int p_188812_) {
      this.interpolators.forEach(p_209205_ -> p_209205_.selectCellYZ(p_188811_, p_188812_));
      this.fillingCell = true;
      this.cellStartBlockY = (p_188811_ + this.cellNoiseMinY) * this.cellHeight;
      this.cellStartBlockZ = (this.firstCellZ + p_188812_) * this.cellWidth;
      this.arrayInterpolationCounter++;

      for (NoiseChunk.CacheAllInCell $$2 : this.cellCaches) {
         $$2.noiseFiller.fillArray($$2.values, this);
      }

      this.arrayInterpolationCounter++;
      this.fillingCell = false;
   }

   public void updateForY(int p_209192_, double p_209193_) {
      this.inCellY = p_209192_ - this.cellStartBlockY;
      this.interpolators.forEach(p_209238_ -> p_209238_.updateForY(p_209193_));
   }

   public void updateForX(int p_209231_, double p_209232_) {
      this.inCellX = p_209231_ - this.cellStartBlockX;
      this.interpolators.forEach(p_209229_ -> p_209229_.updateForX(p_209232_));
   }

   public void updateForZ(int p_209242_, double p_209243_) {
      this.inCellZ = p_209242_ - this.cellStartBlockZ;
      this.interpolationCounter++;
      this.interpolators.forEach(p_209188_ -> p_209188_.updateForZ(p_209243_));
   }

   public void stopInterpolation() {
      if (!this.interpolating) {
         throw new IllegalStateException("Staring interpolation twice");
      } else {
         this.interpolating = false;
      }
   }

   public void swapSlices() {
      this.interpolators.forEach(NoiseChunk.NoiseInterpolator::swapSlices);
   }

   public Aquifer aquifer() {
      return this.aquifer;
   }

   protected int cellWidth() {
      return this.cellWidth;
   }

   protected int cellHeight() {
      return this.cellHeight;
   }

   Blender.BlendingOutput getOrComputeBlendingOutput(int p_209245_, int p_209246_) {
      long $$2 = ChunkPos.asLong(p_209245_, p_209246_);
      if (this.lastBlendingDataPos == $$2) {
         return this.lastBlendingOutput;
      } else {
         this.lastBlendingDataPos = $$2;
         Blender.BlendingOutput $$3 = this.blender.blendOffsetAndFactor(p_209245_, p_209246_);
         this.lastBlendingOutput = $$3;
         return $$3;
      }
   }

   protected DensityFunction wrap(DensityFunction p_209214_) {
      return this.wrapped.computeIfAbsent(p_209214_, this::wrapNew);
   }

   private DensityFunction wrapNew(DensityFunction p_209234_) {
      if (p_209234_ instanceof DensityFunctions.Marker $$1) {
         return (DensityFunction)(switch ($$1.type()) {
            case Interpolated -> new NoiseChunk.NoiseInterpolator($$1.wrapped());
            case FlatCache -> new NoiseChunk.FlatCache($$1.wrapped(), true);
            case Cache2D -> new NoiseChunk.Cache2D($$1.wrapped());
            case CacheOnce -> new NoiseChunk.CacheOnce($$1.wrapped());
            case CacheAllInCell -> new NoiseChunk.CacheAllInCell($$1.wrapped());
         });
      } else {
         if (this.blender != Blender.empty()) {
            if (p_209234_ == DensityFunctions.BlendAlpha.INSTANCE) {
               return this.blendAlpha;
            }

            if (p_209234_ == DensityFunctions.BlendOffset.INSTANCE) {
               return this.blendOffset;
            }
         }

         if (p_209234_ == DensityFunctions.BeardifierMarker.INSTANCE) {
            return this.beardifier;
         } else {
            return p_209234_ instanceof DensityFunctions.HolderHolder $$2 ? $$2.function().value() : p_209234_;
         }
      }
   }

   class BlendAlpha implements NoiseChunk.NoiseChunkDensityFunction {
      @Override
      public DensityFunction wrapped() {
         return DensityFunctions.BlendAlpha.INSTANCE;
      }

      @Override
      public DensityFunction mapAll(DensityFunction.Visitor p_224365_) {
         return this.wrapped().mapAll(p_224365_);
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209264_) {
         return NoiseChunk.this.getOrComputeBlendingOutput(p_209264_.blockX(), p_209264_.blockZ()).alpha();
      }

      @Override
      public void fillArray(double[] p_209266_, DensityFunction.ContextProvider p_209267_) {
         p_209267_.fillAllDirectly(p_209266_, this);
      }

      @Override
      public double minValue() {
         return 0.0;
      }

      @Override
      public double maxValue() {
         return 1.0;
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return DensityFunctions.BlendAlpha.CODEC;
      }
   }

   class BlendOffset implements NoiseChunk.NoiseChunkDensityFunction {
      @Override
      public DensityFunction wrapped() {
         return DensityFunctions.BlendOffset.INSTANCE;
      }

      @Override
      public DensityFunction mapAll(DensityFunction.Visitor p_224368_) {
         return this.wrapped().mapAll(p_224368_);
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209276_) {
         return NoiseChunk.this.getOrComputeBlendingOutput(p_209276_.blockX(), p_209276_.blockZ()).blendingOffset();
      }

      @Override
      public void fillArray(double[] p_209278_, DensityFunction.ContextProvider p_209279_) {
         p_209279_.fillAllDirectly(p_209278_, this);
      }

      @Override
      public double minValue() {
         return Double.NEGATIVE_INFINITY;
      }

      @Override
      public double maxValue() {
         return Double.POSITIVE_INFINITY;
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return DensityFunctions.BlendOffset.CODEC;
      }
   }

   @FunctionalInterface
   public interface BlockStateFiller {
      @Nullable
      BlockState calculate(DensityFunction.FunctionContext var1);
   }

   static class Cache2D implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction function;
      private long lastPos2D = ChunkPos.INVALID_CHUNK_POS;
      private double lastValue;

      Cache2D(DensityFunction p_209288_) {
         this.function = p_209288_;
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209290_) {
         int $$1 = p_209290_.blockX();
         int $$2 = p_209290_.blockZ();
         long $$3 = ChunkPos.asLong($$1, $$2);
         if (this.lastPos2D == $$3) {
            return this.lastValue;
         } else {
            this.lastPos2D = $$3;
            double $$4 = this.function.compute(p_209290_);
            this.lastValue = $$4;
            return $$4;
         }
      }

      @Override
      public void fillArray(double[] p_209292_, DensityFunction.ContextProvider p_209293_) {
         this.function.fillArray(p_209292_, p_209293_);
      }

      @Override
      public DensityFunction wrapped() {
         return this.function;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.Cache2D;
      }
   }

   class CacheAllInCell implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      final DensityFunction noiseFiller;
      final double[] values;

      CacheAllInCell(DensityFunction p_209301_) {
         this.noiseFiller = p_209301_;
         this.values = new double[NoiseChunk.this.cellWidth * NoiseChunk.this.cellWidth * NoiseChunk.this.cellHeight];
         NoiseChunk.this.cellCaches.add(this);
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209303_) {
         if (p_209303_ != NoiseChunk.this) {
            return this.noiseFiller.compute(p_209303_);
         } else if (!NoiseChunk.this.interpolating) {
            throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
         } else {
            int $$1 = NoiseChunk.this.inCellX;
            int $$2 = NoiseChunk.this.inCellY;
            int $$3 = NoiseChunk.this.inCellZ;
            return $$1 >= 0 && $$2 >= 0 && $$3 >= 0 && $$1 < NoiseChunk.this.cellWidth && $$2 < NoiseChunk.this.cellHeight && $$3 < NoiseChunk.this.cellWidth
               ? this.values[((NoiseChunk.this.cellHeight - 1 - $$2) * NoiseChunk.this.cellWidth + $$1) * NoiseChunk.this.cellWidth + $$3]
               : this.noiseFiller.compute(p_209303_);
         }
      }

      @Override
      public void fillArray(double[] p_209305_, DensityFunction.ContextProvider p_209306_) {
         p_209306_.fillAllDirectly(p_209305_, this);
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.CacheAllInCell;
      }
   }

   class CacheOnce implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction function;
      private long lastCounter;
      private long lastArrayCounter;
      private double lastValue;
      @Nullable
      private double[] lastArray;

      CacheOnce(DensityFunction p_209317_) {
         this.function = p_209317_;
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209319_) {
         if (p_209319_ != NoiseChunk.this) {
            return this.function.compute(p_209319_);
         } else if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
            return this.lastArray[NoiseChunk.this.arrayIndex];
         } else if (this.lastCounter == NoiseChunk.this.interpolationCounter) {
            return this.lastValue;
         } else {
            this.lastCounter = NoiseChunk.this.interpolationCounter;
            double $$1 = this.function.compute(p_209319_);
            this.lastValue = $$1;
            return $$1;
         }
      }

      @Override
      public void fillArray(double[] p_209321_, DensityFunction.ContextProvider p_209322_) {
         if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
            System.arraycopy(this.lastArray, 0, p_209321_, 0, p_209321_.length);
         } else {
            this.wrapped().fillArray(p_209321_, p_209322_);
            if (this.lastArray != null && this.lastArray.length == p_209321_.length) {
               System.arraycopy(p_209321_, 0, this.lastArray, 0, p_209321_.length);
            } else {
               this.lastArray = (double[])p_209321_.clone();
            }

            this.lastArrayCounter = NoiseChunk.this.arrayInterpolationCounter;
         }
      }

      @Override
      public DensityFunction wrapped() {
         return this.function;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.CacheOnce;
      }
   }

   class FlatCache implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction noiseFiller;
      final double[][] values;

      FlatCache(DensityFunction p_209330_, boolean p_209331_) {
         this.noiseFiller = p_209330_;
         this.values = new double[NoiseChunk.this.noiseSizeXZ + 1][NoiseChunk.this.noiseSizeXZ + 1];
         if (p_209331_) {
            for (int $$2 = 0; $$2 <= NoiseChunk.this.noiseSizeXZ; $$2++) {
               int $$3 = NoiseChunk.this.firstNoiseX + $$2;
               int $$4 = QuartPos.toBlock($$3);

               for (int $$5 = 0; $$5 <= NoiseChunk.this.noiseSizeXZ; $$5++) {
                  int $$6 = NoiseChunk.this.firstNoiseZ + $$5;
                  int $$7 = QuartPos.toBlock($$6);
                  this.values[$$2][$$5] = p_209330_.compute(new DensityFunction.SinglePointContext($$4, 0, $$7));
               }
            }
         }
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209333_) {
         int $$1 = QuartPos.fromBlock(p_209333_.blockX());
         int $$2 = QuartPos.fromBlock(p_209333_.blockZ());
         int $$3 = $$1 - NoiseChunk.this.firstNoiseX;
         int $$4 = $$2 - NoiseChunk.this.firstNoiseZ;
         int $$5 = this.values.length;
         return $$3 >= 0 && $$4 >= 0 && $$3 < $$5 && $$4 < $$5 ? this.values[$$3][$$4] : this.noiseFiller.compute(p_209333_);
      }

      @Override
      public void fillArray(double[] p_209335_, DensityFunction.ContextProvider p_209336_) {
         p_209336_.fillAllDirectly(p_209335_, this);
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.FlatCache;
      }
   }

   interface NoiseChunkDensityFunction extends DensityFunction {
      DensityFunction wrapped();

      @Override
      default double minValue() {
         return this.wrapped().minValue();
      }

      @Override
      default double maxValue() {
         return this.wrapped().maxValue();
      }
   }

   public class NoiseInterpolator implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      double[][] slice0;
      double[][] slice1;
      private final DensityFunction noiseFiller;
      private double noise000;
      private double noise001;
      private double noise100;
      private double noise101;
      private double noise010;
      private double noise011;
      private double noise110;
      private double noise111;
      private double valueXZ00;
      private double valueXZ10;
      private double valueXZ01;
      private double valueXZ11;
      private double valueZ0;
      private double valueZ1;
      private double value;

      NoiseInterpolator(DensityFunction p_209345_) {
         this.noiseFiller = p_209345_;
         this.slice0 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
         this.slice1 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
         NoiseChunk.this.interpolators.add(this);
      }

      private double[][] allocateSlice(int p_188855_, int p_188856_) {
         int $$2 = p_188856_ + 1;
         int $$3 = p_188855_ + 1;
         double[][] $$4 = new double[$$2][$$3];

         for (int $$5 = 0; $$5 < $$2; $$5++) {
            $$4[$$5] = new double[$$3];
         }

         return $$4;
      }

      void selectCellYZ(int p_188864_, int p_188865_) {
         this.noise000 = this.slice0[p_188865_][p_188864_];
         this.noise001 = this.slice0[p_188865_ + 1][p_188864_];
         this.noise100 = this.slice1[p_188865_][p_188864_];
         this.noise101 = this.slice1[p_188865_ + 1][p_188864_];
         this.noise010 = this.slice0[p_188865_][p_188864_ + 1];
         this.noise011 = this.slice0[p_188865_ + 1][p_188864_ + 1];
         this.noise110 = this.slice1[p_188865_][p_188864_ + 1];
         this.noise111 = this.slice1[p_188865_ + 1][p_188864_ + 1];
      }

      void updateForY(double p_188851_) {
         this.valueXZ00 = Mth.lerp(p_188851_, this.noise000, this.noise010);
         this.valueXZ10 = Mth.lerp(p_188851_, this.noise100, this.noise110);
         this.valueXZ01 = Mth.lerp(p_188851_, this.noise001, this.noise011);
         this.valueXZ11 = Mth.lerp(p_188851_, this.noise101, this.noise111);
      }

      void updateForX(double p_188862_) {
         this.valueZ0 = Mth.lerp(p_188862_, this.valueXZ00, this.valueXZ10);
         this.valueZ1 = Mth.lerp(p_188862_, this.valueXZ01, this.valueXZ11);
      }

      void updateForZ(double p_188867_) {
         this.value = Mth.lerp(p_188867_, this.valueZ0, this.valueZ1);
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209347_) {
         if (p_209347_ != NoiseChunk.this) {
            return this.noiseFiller.compute(p_209347_);
         } else if (!NoiseChunk.this.interpolating) {
            throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
         } else {
            return NoiseChunk.this.fillingCell
               ? Mth.lerp3(
                  (double)NoiseChunk.this.inCellX / NoiseChunk.this.cellWidth,
                  (double)NoiseChunk.this.inCellY / NoiseChunk.this.cellHeight,
                  (double)NoiseChunk.this.inCellZ / NoiseChunk.this.cellWidth,
                  this.noise000,
                  this.noise100,
                  this.noise010,
                  this.noise110,
                  this.noise001,
                  this.noise101,
                  this.noise011,
                  this.noise111
               )
               : this.value;
         }
      }

      @Override
      public void fillArray(double[] p_209349_, DensityFunction.ContextProvider p_209350_) {
         if (NoiseChunk.this.fillingCell) {
            p_209350_.fillAllDirectly(p_209349_, this);
         } else {
            this.wrapped().fillArray(p_209349_, p_209350_);
         }
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      private void swapSlices() {
         double[][] $$0 = this.slice0;
         this.slice0 = this.slice1;
         this.slice1 = $$0;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.Interpolated;
      }
   }
}
