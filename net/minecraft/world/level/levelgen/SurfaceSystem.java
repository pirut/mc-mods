package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class SurfaceSystem {
   private static final BlockState WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.defaultBlockState();
   private static final BlockState ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
   private static final BlockState TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
   private static final BlockState YELLOW_TERRACOTTA = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
   private static final BlockState BROWN_TERRACOTTA = Blocks.BROWN_TERRACOTTA.defaultBlockState();
   private static final BlockState RED_TERRACOTTA = Blocks.RED_TERRACOTTA.defaultBlockState();
   private static final BlockState LIGHT_GRAY_TERRACOTTA = Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
   private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
   private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
   private final BlockState defaultBlock;
   private final int seaLevel;
   private final BlockState[] clayBands;
   private final NormalNoise clayBandsOffsetNoise;
   private final NormalNoise badlandsPillarNoise;
   private final NormalNoise badlandsPillarRoofNoise;
   private final NormalNoise badlandsSurfaceNoise;
   private final NormalNoise icebergPillarNoise;
   private final NormalNoise icebergPillarRoofNoise;
   private final NormalNoise icebergSurfaceNoise;
   private final PositionalRandomFactory noiseRandom;
   private final NormalNoise surfaceNoise;
   private final NormalNoise surfaceSecondaryNoise;

   public SurfaceSystem(RandomState p_224637_, BlockState p_224638_, int p_224639_, PositionalRandomFactory p_224640_) {
      this.defaultBlock = p_224638_;
      this.seaLevel = p_224639_;
      this.noiseRandom = p_224640_;
      this.clayBandsOffsetNoise = p_224637_.getOrCreateNoise(Noises.CLAY_BANDS_OFFSET);
      this.clayBands = generateBands(p_224640_.fromHashOf(ResourceLocation.withDefaultNamespace("clay_bands")));
      this.surfaceNoise = p_224637_.getOrCreateNoise(Noises.SURFACE);
      this.surfaceSecondaryNoise = p_224637_.getOrCreateNoise(Noises.SURFACE_SECONDARY);
      this.badlandsPillarNoise = p_224637_.getOrCreateNoise(Noises.BADLANDS_PILLAR);
      this.badlandsPillarRoofNoise = p_224637_.getOrCreateNoise(Noises.BADLANDS_PILLAR_ROOF);
      this.badlandsSurfaceNoise = p_224637_.getOrCreateNoise(Noises.BADLANDS_SURFACE);
      this.icebergPillarNoise = p_224637_.getOrCreateNoise(Noises.ICEBERG_PILLAR);
      this.icebergPillarRoofNoise = p_224637_.getOrCreateNoise(Noises.ICEBERG_PILLAR_ROOF);
      this.icebergSurfaceNoise = p_224637_.getOrCreateNoise(Noises.ICEBERG_SURFACE);
   }

   public void buildSurface(
      RandomState p_224649_,
      BiomeManager p_224650_,
      Registry<Biome> p_224651_,
      boolean p_224652_,
      WorldGenerationContext p_224653_,
      final ChunkAccess p_224654_,
      NoiseChunk p_224655_,
      SurfaceRules.RuleSource p_224656_
   ) {
      final BlockPos.MutableBlockPos $$8 = new BlockPos.MutableBlockPos();
      final ChunkPos $$9 = p_224654_.getPos();
      int $$10 = $$9.getMinBlockX();
      int $$11 = $$9.getMinBlockZ();
      BlockColumn $$12 = new BlockColumn() {
         @Override
         public BlockState getBlock(int p_190006_) {
            return p_224654_.getBlockState($$8.setY(p_190006_));
         }

         @Override
         public void setBlock(int p_190008_, BlockState p_190009_) {
            LevelHeightAccessor $$2 = p_224654_.getHeightAccessorForGeneration();
            if (p_190008_ >= $$2.getMinBuildHeight() && p_190008_ < $$2.getMaxBuildHeight()) {
               p_224654_.setBlockState($$8.setY(p_190008_), p_190009_, false);
               if (!p_190009_.getFluidState().isEmpty()) {
                  p_224654_.markPosForPostprocessing($$8);
               }
            }
         }

         @Override
         public String toString() {
            return "ChunkBlockColumn " + $$9;
         }
      };
      SurfaceRules.Context $$13 = new SurfaceRules.Context(this, p_224649_, p_224654_, p_224655_, p_224650_::getBiome, p_224651_, p_224653_);
      SurfaceRules.SurfaceRule $$14 = p_224656_.apply($$13);
      BlockPos.MutableBlockPos $$15 = new BlockPos.MutableBlockPos();

      for (int $$16 = 0; $$16 < 16; $$16++) {
         for (int $$17 = 0; $$17 < 16; $$17++) {
            int $$18 = $$10 + $$16;
            int $$19 = $$11 + $$17;
            int $$20 = p_224654_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, $$16, $$17) + 1;
            $$8.setX($$18).setZ($$19);
            Holder<Biome> $$21 = p_224650_.getBiome($$15.set($$18, p_224652_ ? 0 : $$20, $$19));
            if ($$21.is(Biomes.ERODED_BADLANDS)) {
               this.erodedBadlandsExtension($$12, $$18, $$19, $$20, p_224654_);
            }

            int $$22 = p_224654_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, $$16, $$17) + 1;
            $$13.updateXZ($$18, $$19);
            int $$23 = 0;
            int $$24 = Integer.MIN_VALUE;
            int $$25 = Integer.MAX_VALUE;
            int $$26 = p_224654_.getMinBuildHeight();

            for (int $$27 = $$22; $$27 >= $$26; $$27--) {
               BlockState $$28 = $$12.getBlock($$27);
               if ($$28.isAir()) {
                  $$23 = 0;
                  $$24 = Integer.MIN_VALUE;
               } else if (!$$28.getFluidState().isEmpty()) {
                  if ($$24 == Integer.MIN_VALUE) {
                     $$24 = $$27 + 1;
                  }
               } else {
                  if ($$25 >= $$27) {
                     $$25 = DimensionType.WAY_BELOW_MIN_Y;

                     for (int $$29 = $$27 - 1; $$29 >= $$26 - 1; $$29--) {
                        BlockState $$30 = $$12.getBlock($$29);
                        if (!this.isStone($$30)) {
                           $$25 = $$29 + 1;
                           break;
                        }
                     }
                  }

                  $$23++;
                  int $$31 = $$27 - $$25 + 1;
                  $$13.updateY($$23, $$31, $$24, $$18, $$27, $$19);
                  if ($$28 == this.defaultBlock) {
                     BlockState $$32 = $$14.tryApply($$18, $$27, $$19);
                     if ($$32 != null) {
                        $$12.setBlock($$27, $$32);
                     }
                  }
               }
            }

            if ($$21.is(Biomes.FROZEN_OCEAN) || $$21.is(Biomes.DEEP_FROZEN_OCEAN)) {
               this.frozenOceanExtension($$13.getMinSurfaceLevel(), $$21.value(), $$12, $$15, $$18, $$19, $$20);
            }
         }
      }
   }

   protected int getSurfaceDepth(int p_189928_, int p_189929_) {
      double $$2 = this.surfaceNoise.getValue(p_189928_, 0.0, p_189929_);
      return (int)($$2 * 2.75 + 3.0 + this.noiseRandom.at(p_189928_, 0, p_189929_).nextDouble() * 0.25);
   }

   protected double getSurfaceSecondary(int p_202190_, int p_202191_) {
      return this.surfaceSecondaryNoise.getValue(p_202190_, 0.0, p_202191_);
   }

   private boolean isStone(BlockState p_189953_) {
      return !p_189953_.isAir() && p_189953_.getFluidState().isEmpty();
   }

   @Deprecated
   public Optional<BlockState> topMaterial(
      SurfaceRules.RuleSource p_189972_,
      CarvingContext p_189973_,
      Function<BlockPos, Holder<Biome>> p_189974_,
      ChunkAccess p_189975_,
      NoiseChunk p_189976_,
      BlockPos p_189977_,
      boolean p_189978_
   ) {
      SurfaceRules.Context $$7 = new SurfaceRules.Context(
         this, p_189973_.randomState(), p_189975_, p_189976_, p_189974_, p_189973_.registryAccess().registryOrThrow(Registries.BIOME), p_189973_
      );
      SurfaceRules.SurfaceRule $$8 = p_189972_.apply($$7);
      int $$9 = p_189977_.getX();
      int $$10 = p_189977_.getY();
      int $$11 = p_189977_.getZ();
      $$7.updateXZ($$9, $$11);
      $$7.updateY(1, 1, p_189978_ ? $$10 + 1 : Integer.MIN_VALUE, $$9, $$10, $$11);
      BlockState $$12 = $$8.tryApply($$9, $$10, $$11);
      return Optional.ofNullable($$12);
   }

   private void erodedBadlandsExtension(BlockColumn p_189955_, int p_189956_, int p_189957_, int p_189958_, LevelHeightAccessor p_189959_) {
      double $$5 = 0.2;
      double $$6 = Math.min(
         Math.abs(this.badlandsSurfaceNoise.getValue(p_189956_, 0.0, p_189957_) * 8.25),
         this.badlandsPillarNoise.getValue(p_189956_ * 0.2, 0.0, p_189957_ * 0.2) * 15.0
      );
      if (!($$6 <= 0.0)) {
         double $$7 = 0.75;
         double $$8 = 1.5;
         double $$9 = Math.abs(this.badlandsPillarRoofNoise.getValue(p_189956_ * 0.75, 0.0, p_189957_ * 0.75) * 1.5);
         double $$10 = 64.0 + Math.min($$6 * $$6 * 2.5, Math.ceil($$9 * 50.0) + 24.0);
         int $$11 = Mth.floor($$10);
         if (p_189958_ <= $$11) {
            for (int $$12 = $$11; $$12 >= p_189959_.getMinBuildHeight(); $$12--) {
               BlockState $$13 = p_189955_.getBlock($$12);
               if ($$13.is(this.defaultBlock.getBlock())) {
                  break;
               }

               if ($$13.is(Blocks.WATER)) {
                  return;
               }
            }

            for (int $$14 = $$11; $$14 >= p_189959_.getMinBuildHeight() && p_189955_.getBlock($$14).isAir(); $$14--) {
               p_189955_.setBlock($$14, this.defaultBlock);
            }
         }
      }
   }

   private void frozenOceanExtension(
      int p_189935_, Biome p_189936_, BlockColumn p_189937_, BlockPos.MutableBlockPos p_189938_, int p_189939_, int p_189940_, int p_189941_
   ) {
      double $$7 = 1.28;
      double $$8 = Math.min(
         Math.abs(this.icebergSurfaceNoise.getValue(p_189939_, 0.0, p_189940_) * 8.25),
         this.icebergPillarNoise.getValue(p_189939_ * 1.28, 0.0, p_189940_ * 1.28) * 15.0
      );
      if (!($$8 <= 1.8)) {
         double $$9 = 1.17;
         double $$10 = 1.5;
         double $$11 = Math.abs(this.icebergPillarRoofNoise.getValue(p_189939_ * 1.17, 0.0, p_189940_ * 1.17) * 1.5);
         double $$12 = Math.min($$8 * $$8 * 1.2, Math.ceil($$11 * 40.0) + 14.0);
         if (p_189936_.shouldMeltFrozenOceanIcebergSlightly(p_189938_.set(p_189939_, 63, p_189940_))) {
            $$12 -= 2.0;
         }

         double $$13;
         if ($$12 > 2.0) {
            $$13 = this.seaLevel - $$12 - 7.0;
            $$12 += this.seaLevel;
         } else {
            $$12 = 0.0;
            $$13 = 0.0;
         }

         double $$15 = $$12;
         RandomSource $$16 = this.noiseRandom.at(p_189939_, 0, p_189940_);
         int $$17 = 2 + $$16.nextInt(4);
         int $$18 = this.seaLevel + 18 + $$16.nextInt(10);
         int $$19 = 0;

         for (int $$20 = Math.max(p_189941_, (int)$$12 + 1); $$20 >= p_189935_; $$20--) {
            if (p_189937_.getBlock($$20).isAir() && $$20 < (int)$$15 && $$16.nextDouble() > 0.01
               || p_189937_.getBlock($$20).is(Blocks.WATER) && $$20 > (int)$$13 && $$20 < this.seaLevel && $$13 != 0.0 && $$16.nextDouble() > 0.15) {
               if ($$19 <= $$17 && $$20 > $$18) {
                  p_189937_.setBlock($$20, SNOW_BLOCK);
                  $$19++;
               } else {
                  p_189937_.setBlock($$20, PACKED_ICE);
               }
            }
         }
      }
   }

   private static BlockState[] generateBands(RandomSource p_224642_) {
      BlockState[] $$1 = new BlockState[192];
      Arrays.fill($$1, TERRACOTTA);

      for (int $$2 = 0; $$2 < $$1.length; $$2++) {
         $$2 += p_224642_.nextInt(5) + 1;
         if ($$2 < $$1.length) {
            $$1[$$2] = ORANGE_TERRACOTTA;
         }
      }

      makeBands(p_224642_, $$1, 1, YELLOW_TERRACOTTA);
      makeBands(p_224642_, $$1, 2, BROWN_TERRACOTTA);
      makeBands(p_224642_, $$1, 1, RED_TERRACOTTA);
      int $$3 = p_224642_.nextIntBetweenInclusive(9, 15);
      int $$4 = 0;

      for (int $$5 = 0; $$4 < $$3 && $$5 < $$1.length; $$5 += p_224642_.nextInt(16) + 4) {
         $$1[$$5] = WHITE_TERRACOTTA;
         if ($$5 - 1 > 0 && p_224642_.nextBoolean()) {
            $$1[$$5 - 1] = LIGHT_GRAY_TERRACOTTA;
         }

         if ($$5 + 1 < $$1.length && p_224642_.nextBoolean()) {
            $$1[$$5 + 1] = LIGHT_GRAY_TERRACOTTA;
         }

         $$4++;
      }

      return $$1;
   }

   private static void makeBands(RandomSource p_224644_, BlockState[] p_224645_, int p_224646_, BlockState p_224647_) {
      int $$4 = p_224644_.nextIntBetweenInclusive(6, 15);

      for (int $$5 = 0; $$5 < $$4; $$5++) {
         int $$6 = p_224646_ + p_224644_.nextInt(3);
         int $$7 = p_224644_.nextInt(p_224645_.length);

         for (int $$8 = 0; $$7 + $$8 < p_224645_.length && $$8 < $$6; $$8++) {
            p_224645_[$$7 + $$8] = p_224647_;
         }
      }
   }

   protected BlockState getBand(int p_189931_, int p_189932_, int p_189933_) {
      int $$3 = (int)Math.round(this.clayBandsOffsetNoise.getValue(p_189931_, 0.0, p_189933_) * 4.0);
      return this.clayBands[(p_189932_ + $$3 + this.clayBands.length) % this.clayBands.length];
   }
}
