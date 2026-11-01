package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;

public abstract class BiomeSource implements BiomeResolver {
   public static final Codec<BiomeSource> CODEC = BuiltInRegistries.BIOME_SOURCE.byNameCodec().dispatchStable(BiomeSource::codec, Function.identity());
   private final Supplier<Set<Holder<Biome>>> possibleBiomes = Suppliers.memoize(
      () -> this.collectPossibleBiomes().distinct().collect(ImmutableSet.toImmutableSet())
   );

   protected BiomeSource() {
   }

   protected abstract MapCodec<? extends BiomeSource> codec();

   protected abstract Stream<Holder<Biome>> collectPossibleBiomes();

   public Set<Holder<Biome>> possibleBiomes() {
      return this.possibleBiomes.get();
   }

   public Set<Holder<Biome>> getBiomesWithin(int p_186705_, int p_186706_, int p_186707_, int p_186708_, Climate.Sampler p_186709_) {
      int $$5 = QuartPos.fromBlock(p_186705_ - p_186708_);
      int $$6 = QuartPos.fromBlock(p_186706_ - p_186708_);
      int $$7 = QuartPos.fromBlock(p_186707_ - p_186708_);
      int $$8 = QuartPos.fromBlock(p_186705_ + p_186708_);
      int $$9 = QuartPos.fromBlock(p_186706_ + p_186708_);
      int $$10 = QuartPos.fromBlock(p_186707_ + p_186708_);
      int $$11 = $$8 - $$5 + 1;
      int $$12 = $$9 - $$6 + 1;
      int $$13 = $$10 - $$7 + 1;
      Set<Holder<Biome>> $$14 = Sets.newHashSet();

      for (int $$15 = 0; $$15 < $$13; $$15++) {
         for (int $$16 = 0; $$16 < $$11; $$16++) {
            for (int $$17 = 0; $$17 < $$12; $$17++) {
               int $$18 = $$5 + $$16;
               int $$19 = $$6 + $$17;
               int $$20 = $$7 + $$15;
               $$14.add(this.getNoiseBiome($$18, $$19, $$20, p_186709_));
            }
         }
      }

      return $$14;
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(
      int p_220571_, int p_220572_, int p_220573_, int p_220574_, Predicate<Holder<Biome>> p_220575_, RandomSource p_220576_, Climate.Sampler p_220577_
   ) {
      return this.findBiomeHorizontal(p_220571_, p_220572_, p_220573_, p_220574_, 1, p_220575_, p_220576_, false, p_220577_);
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(
      BlockPos p_220578_, int p_220579_, int p_220580_, int p_220581_, Predicate<Holder<Biome>> p_220582_, Climate.Sampler p_220583_, LevelReader p_220584_
   ) {
      Set<Holder<Biome>> $$7 = this.possibleBiomes().stream().filter(p_220582_).collect(Collectors.toUnmodifiableSet());
      if ($$7.isEmpty()) {
         return null;
      } else {
         int $$8 = Math.floorDiv(p_220579_, p_220580_);
         int[] $$9 = Mth.outFromOrigin(p_220578_.getY(), p_220584_.getMinBuildHeight() + 1, p_220584_.getMaxBuildHeight(), p_220581_).toArray();

         for (BlockPos.MutableBlockPos $$10 : BlockPos.spiralAround(BlockPos.ZERO, $$8, Direction.EAST, Direction.SOUTH)) {
            int $$11 = p_220578_.getX() + $$10.getX() * p_220580_;
            int $$12 = p_220578_.getZ() + $$10.getZ() * p_220580_;
            int $$13 = QuartPos.fromBlock($$11);
            int $$14 = QuartPos.fromBlock($$12);

            for (int $$15 : $$9) {
               int $$16 = QuartPos.fromBlock($$15);
               Holder<Biome> $$17 = this.getNoiseBiome($$13, $$16, $$14, p_220583_);
               if ($$7.contains($$17)) {
                  return Pair.of(new BlockPos($$11, $$15, $$12), $$17);
               }
            }
         }

         return null;
      }
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(
      int p_220561_,
      int p_220562_,
      int p_220563_,
      int p_220564_,
      int p_220565_,
      Predicate<Holder<Biome>> p_220566_,
      RandomSource p_220567_,
      boolean p_220568_,
      Climate.Sampler p_220569_
   ) {
      int $$9 = QuartPos.fromBlock(p_220561_);
      int $$10 = QuartPos.fromBlock(p_220563_);
      int $$11 = QuartPos.fromBlock(p_220564_);
      int $$12 = QuartPos.fromBlock(p_220562_);
      Pair<BlockPos, Holder<Biome>> $$13 = null;
      int $$14 = 0;
      int $$15 = p_220568_ ? 0 : $$11;
      int $$16 = $$15;

      while ($$16 <= $$11) {
         for (int $$17 = SharedConstants.debugGenerateSquareTerrainWithoutNoise ? 0 : -$$16; $$17 <= $$16; $$17 += p_220565_) {
            boolean $$18 = Math.abs($$17) == $$16;

            for (int $$19 = -$$16; $$19 <= $$16; $$19 += p_220565_) {
               if (p_220568_) {
                  boolean $$20 = Math.abs($$19) == $$16;
                  if (!$$20 && !$$18) {
                     continue;
                  }
               }

               int $$21 = $$9 + $$19;
               int $$22 = $$10 + $$17;
               Holder<Biome> $$23 = this.getNoiseBiome($$21, $$12, $$22, p_220569_);
               if (p_220566_.test($$23)) {
                  if ($$13 == null || p_220567_.nextInt($$14 + 1) == 0) {
                     BlockPos $$24 = new BlockPos(QuartPos.toBlock($$21), p_220562_, QuartPos.toBlock($$22));
                     if (p_220568_) {
                        return Pair.of($$24, $$23);
                     }

                     $$13 = Pair.of($$24, $$23);
                  }

                  $$14++;
               }
            }
         }

         $$16 += p_220565_;
      }

      return $$13;
   }

   @Override
   public abstract Holder<Biome> getNoiseBiome(int var1, int var2, int var3, Climate.Sampler var4);

   public void addDebugInfo(List<String> p_207837_, BlockPos p_207838_, Climate.Sampler p_207839_) {
   }
}
