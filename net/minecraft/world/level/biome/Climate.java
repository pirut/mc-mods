package net.minecraft.world.level.biome;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public class Climate {
   private static final boolean DEBUG_SLOW_BIOME_SEARCH = false;
   private static final float QUANTIZATION_FACTOR = 10000.0F;
   @VisibleForTesting
   protected static final int PARAMETER_COUNT = 7;

   public static Climate.TargetPoint target(float p_186782_, float p_186783_, float p_186784_, float p_186785_, float p_186786_, float p_186787_) {
      return new Climate.TargetPoint(
         quantizeCoord(p_186782_),
         quantizeCoord(p_186783_),
         quantizeCoord(p_186784_),
         quantizeCoord(p_186785_),
         quantizeCoord(p_186786_),
         quantizeCoord(p_186787_)
      );
   }

   public static Climate.ParameterPoint parameters(
      float p_186789_, float p_186790_, float p_186791_, float p_186792_, float p_186793_, float p_186794_, float p_186795_
   ) {
      return new Climate.ParameterPoint(
         Climate.Parameter.point(p_186789_),
         Climate.Parameter.point(p_186790_),
         Climate.Parameter.point(p_186791_),
         Climate.Parameter.point(p_186792_),
         Climate.Parameter.point(p_186793_),
         Climate.Parameter.point(p_186794_),
         quantizeCoord(p_186795_)
      );
   }

   public static Climate.ParameterPoint parameters(
      Climate.Parameter p_186799_,
      Climate.Parameter p_186800_,
      Climate.Parameter p_186801_,
      Climate.Parameter p_186802_,
      Climate.Parameter p_186803_,
      Climate.Parameter p_186804_,
      float p_186805_
   ) {
      return new Climate.ParameterPoint(p_186799_, p_186800_, p_186801_, p_186802_, p_186803_, p_186804_, quantizeCoord(p_186805_));
   }

   public static long quantizeCoord(float p_186780_) {
      return (long)(p_186780_ * 10000.0F);
   }

   public static float unquantizeCoord(long p_186797_) {
      return (float)p_186797_ / 10000.0F;
   }

   public static Climate.Sampler empty() {
      DensityFunction $$0 = DensityFunctions.zero();
      return new Climate.Sampler($$0, $$0, $$0, $$0, $$0, $$0, List.of());
   }

   public static BlockPos findSpawnPosition(List<Climate.ParameterPoint> p_207843_, Climate.Sampler p_207844_) {
      return (new Climate.SpawnFinder(p_207843_, p_207844_)).result.location();
   }

   interface DistanceMetric<T> {
      long distance(Climate.RTree.Node<T> var1, long[] var2);
   }

   public record Parameter(long min, long max) {
      public static final Codec<Climate.Parameter> CODEC = ExtraCodecs.intervalCodec(
         Codec.floatRange(-2.0F, 2.0F),
         "min",
         "max",
         (p_275164_, p_275165_) -> p_275164_.compareTo(p_275165_) > 0
            ? DataResult.error(() -> "Cannon construct interval, min > max (" + p_275164_ + " > " + p_275165_ + ")")
            : DataResult.success(new Climate.Parameter(Climate.quantizeCoord(p_275164_), Climate.quantizeCoord(p_275165_))),
         p_186841_ -> Climate.unquantizeCoord(p_186841_.min()),
         p_186839_ -> Climate.unquantizeCoord(p_186839_.max())
      );

      public static Climate.Parameter point(float p_186821_) {
         return span(p_186821_, p_186821_);
      }

      public static Climate.Parameter span(float p_186823_, float p_186824_) {
         if (p_186823_ > p_186824_) {
            throw new IllegalArgumentException("min > max: " + p_186823_ + " " + p_186824_);
         } else {
            return new Climate.Parameter(Climate.quantizeCoord(p_186823_), Climate.quantizeCoord(p_186824_));
         }
      }

      public static Climate.Parameter span(Climate.Parameter p_186830_, Climate.Parameter p_186831_) {
         if (p_186830_.min() > p_186831_.max()) {
            throw new IllegalArgumentException("min > max: " + p_186830_ + " " + p_186831_);
         } else {
            return new Climate.Parameter(p_186830_.min(), p_186831_.max());
         }
      }

      @Override
      public String toString() {
         return this.min == this.max ? String.format(Locale.ROOT, "%d", this.min) : String.format(Locale.ROOT, "[%d-%d]", this.min, this.max);
      }

      public long distance(long p_186826_) {
         long $$1 = p_186826_ - this.max;
         long $$2 = this.min - p_186826_;
         return $$1 > 0L ? $$1 : Math.max($$2, 0L);
      }

      public long distance(Climate.Parameter p_186828_) {
         long $$1 = p_186828_.min() - this.max;
         long $$2 = this.min - p_186828_.max();
         return $$1 > 0L ? $$1 : Math.max($$2, 0L);
      }

      public Climate.Parameter span(@Nullable Climate.Parameter p_186837_) {
         return p_186837_ == null ? this : new Climate.Parameter(Math.min(this.min, p_186837_.min()), Math.max(this.max, p_186837_.max()));
      }
   }

   public static class ParameterList<T> {
      private final List<Pair<Climate.ParameterPoint, T>> values;
      private final Climate.RTree<T> index;

      public static <T> Codec<Climate.ParameterList<T>> codec(MapCodec<T> p_275523_) {
         return ExtraCodecs.nonEmptyList(
               RecordCodecBuilder.create(
                     p_275233_ -> p_275233_.group(
                           Climate.ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), p_275523_.forGetter(Pair::getSecond)
                        )
                        .apply(p_275233_, Pair::of)
                  )
                  .listOf()
            )
            .xmap(Climate.ParameterList::new, Climate.ParameterList::values);
      }

      public ParameterList(List<Pair<Climate.ParameterPoint, T>> p_186849_) {
         this.values = p_186849_;
         this.index = Climate.RTree.create(p_186849_);
      }

      public List<Pair<Climate.ParameterPoint, T>> values() {
         return this.values;
      }

      public T findValue(Climate.TargetPoint p_204253_) {
         return this.findValueIndex(p_204253_);
      }

      @VisibleForTesting
      public T findValueBruteForce(Climate.TargetPoint p_204255_) {
         Iterator<Pair<Climate.ParameterPoint, T>> $$1 = this.values().iterator();
         Pair<Climate.ParameterPoint, T> $$2 = $$1.next();
         long $$3 = ((Climate.ParameterPoint)$$2.getFirst()).fitness(p_204255_);
         T $$4 = (T)$$2.getSecond();

         while ($$1.hasNext()) {
            Pair<Climate.ParameterPoint, T> $$5 = $$1.next();
            long $$6 = ((Climate.ParameterPoint)$$5.getFirst()).fitness(p_204255_);
            if ($$6 < $$3) {
               $$3 = $$6;
               $$4 = (T)$$5.getSecond();
            }
         }

         return $$4;
      }

      public T findValueIndex(Climate.TargetPoint p_186852_) {
         return this.findValueIndex(p_186852_, Climate.RTree.Node::distance);
      }

      protected T findValueIndex(Climate.TargetPoint p_186854_, Climate.DistanceMetric<T> p_186855_) {
         return this.index.search(p_186854_, p_186855_);
      }
   }

   public record ParameterPoint(
      Climate.Parameter temperature,
      Climate.Parameter humidity,
      Climate.Parameter continentalness,
      Climate.Parameter erosion,
      Climate.Parameter depth,
      Climate.Parameter weirdness,
      long offset
   ) {
      public static final Codec<Climate.ParameterPoint> CODEC = RecordCodecBuilder.create(
         p_186885_ -> p_186885_.group(
               Climate.Parameter.CODEC.fieldOf("temperature").forGetter(p_186905_ -> p_186905_.temperature),
               Climate.Parameter.CODEC.fieldOf("humidity").forGetter(p_186902_ -> p_186902_.humidity),
               Climate.Parameter.CODEC.fieldOf("continentalness").forGetter(p_186897_ -> p_186897_.continentalness),
               Climate.Parameter.CODEC.fieldOf("erosion").forGetter(p_186894_ -> p_186894_.erosion),
               Climate.Parameter.CODEC.fieldOf("depth").forGetter(p_186891_ -> p_186891_.depth),
               Climate.Parameter.CODEC.fieldOf("weirdness").forGetter(p_186888_ -> p_186888_.weirdness),
               Codec.floatRange(0.0F, 1.0F).fieldOf("offset").xmap(Climate::quantizeCoord, Climate::unquantizeCoord).forGetter(p_186881_ -> p_186881_.offset)
            )
            .apply(p_186885_, Climate.ParameterPoint::new)
      );

      long fitness(Climate.TargetPoint p_186883_) {
         return Mth.square(this.temperature.distance(p_186883_.temperature))
            + Mth.square(this.humidity.distance(p_186883_.humidity))
            + Mth.square(this.continentalness.distance(p_186883_.continentalness))
            + Mth.square(this.erosion.distance(p_186883_.erosion))
            + Mth.square(this.depth.distance(p_186883_.depth))
            + Mth.square(this.weirdness.distance(p_186883_.weirdness))
            + Mth.square(this.offset);
      }

      protected List<Climate.Parameter> parameterSpace() {
         return ImmutableList.of(
            this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, new Climate.Parameter(this.offset, this.offset)
         );
      }
   }

   protected static final class RTree<T> {
      private static final int CHILDREN_PER_NODE = 6;
      private final Climate.RTree.Node<T> root;
      private final ThreadLocal<Climate.RTree.Leaf<T>> lastResult = new ThreadLocal<>();

      private RTree(Climate.RTree.Node<T> p_186913_) {
         this.root = p_186913_;
      }

      public static <T> Climate.RTree<T> create(List<Pair<Climate.ParameterPoint, T>> p_186936_) {
         if (p_186936_.isEmpty()) {
            throw new IllegalArgumentException("Need at least one value to build the search tree.");
         } else {
            int $$1 = ((Climate.ParameterPoint)p_186936_.get(0).getFirst()).parameterSpace().size();
            if ($$1 != 7) {
               throw new IllegalStateException("Expecting parameter space to be 7, got " + $$1);
            } else {
               List<Climate.RTree.Leaf<T>> $$2 = p_186936_.stream()
                  .map(p_186934_ -> new Climate.RTree.Leaf<>((Climate.ParameterPoint)p_186934_.getFirst(), p_186934_.getSecond()))
                  .collect(Collectors.toCollection(ArrayList::new));
               return new Climate.RTree<>(build($$1, $$2));
            }
         }
      }

      private static <T> Climate.RTree.Node<T> build(int p_186921_, List<? extends Climate.RTree.Node<T>> p_186922_) {
         if (p_186922_.isEmpty()) {
            throw new IllegalStateException("Need at least one child to build a node");
         } else if (p_186922_.size() == 1) {
            return (Climate.RTree.Node<T>)p_186922_.get(0);
         } else if (p_186922_.size() <= 6) {
            p_186922_.sort(Comparator.comparingLong(p_186916_ -> {
               long $$2x = 0L;

               for (int $$3x = 0; $$3x < p_186921_; $$3x++) {
                  Climate.Parameter $$4x = p_186916_.parameterSpace[$$3x];
                  $$2x += Math.abs(($$4x.min() + $$4x.max()) / 2L);
               }

               return $$2x;
            }));
            return new Climate.RTree.SubTree<>(p_186922_);
         } else {
            long $$2 = Long.MAX_VALUE;
            int $$3 = -1;
            List<Climate.RTree.SubTree<T>> $$4 = null;

            for (int $$5 = 0; $$5 < p_186921_; $$5++) {
               sort(p_186922_, p_186921_, $$5, false);
               List<Climate.RTree.SubTree<T>> $$6 = bucketize(p_186922_);
               long $$7 = 0L;

               for (Climate.RTree.SubTree<T> $$8 : $$6) {
                  $$7 += cost($$8.parameterSpace);
               }

               if ($$2 > $$7) {
                  $$2 = $$7;
                  $$3 = $$5;
                  $$4 = $$6;
               }
            }

            sort($$4, p_186921_, $$3, true);
            return new Climate.RTree.SubTree<>($$4.stream().map(p_186919_ -> build(p_186921_, Arrays.asList(p_186919_.children))).collect(Collectors.toList()));
         }
      }

      private static <T> void sort(List<? extends Climate.RTree.Node<T>> p_186938_, int p_186939_, int p_186940_, boolean p_186941_) {
         Comparator<Climate.RTree.Node<T>> $$4 = comparator(p_186940_, p_186941_);

         for (int $$5 = 1; $$5 < p_186939_; $$5++) {
            $$4 = $$4.thenComparing(comparator((p_186940_ + $$5) % p_186939_, p_186941_));
         }

         p_186938_.sort($$4);
      }

      private static <T> Comparator<Climate.RTree.Node<T>> comparator(int p_186924_, boolean p_186925_) {
         return Comparator.comparingLong(p_186929_ -> {
            Climate.Parameter $$3 = p_186929_.parameterSpace[p_186924_];
            long $$4 = ($$3.min() + $$3.max()) / 2L;
            return p_186925_ ? Math.abs($$4) : $$4;
         });
      }

      private static <T> List<Climate.RTree.SubTree<T>> bucketize(List<? extends Climate.RTree.Node<T>> p_186945_) {
         List<Climate.RTree.SubTree<T>> $$1 = Lists.newArrayList();
         List<Climate.RTree.Node<T>> $$2 = Lists.newArrayList();
         int $$3 = (int)Math.pow(6.0, Math.floor(Math.log(p_186945_.size() - 0.01) / Math.log(6.0)));

         for (Climate.RTree.Node<T> $$4 : p_186945_) {
            $$2.add($$4);
            if ($$2.size() >= $$3) {
               $$1.add(new Climate.RTree.SubTree<>($$2));
               $$2 = Lists.newArrayList();
            }
         }

         if (!$$2.isEmpty()) {
            $$1.add(new Climate.RTree.SubTree<>($$2));
         }

         return $$1;
      }

      private static long cost(Climate.Parameter[] p_186943_) {
         long $$1 = 0L;

         for (Climate.Parameter $$2 : p_186943_) {
            $$1 += Math.abs($$2.max() - $$2.min());
         }

         return $$1;
      }

      static <T> List<Climate.Parameter> buildParameterSpace(List<? extends Climate.RTree.Node<T>> p_186947_) {
         if (p_186947_.isEmpty()) {
            throw new IllegalArgumentException("SubTree needs at least one child");
         } else {
            int $$1 = 7;
            List<Climate.Parameter> $$2 = Lists.newArrayList();

            for (int $$3 = 0; $$3 < 7; $$3++) {
               $$2.add(null);
            }

            for (Climate.RTree.Node<T> $$4 : p_186947_) {
               for (int $$5 = 0; $$5 < 7; $$5++) {
                  $$2.set($$5, $$4.parameterSpace[$$5].span($$2.get($$5)));
               }
            }

            return $$2;
         }
      }

      public T search(Climate.TargetPoint p_186931_, Climate.DistanceMetric<T> p_186932_) {
         long[] $$2 = p_186931_.toParameterArray();
         Climate.RTree.Leaf<T> $$3 = this.root.search($$2, this.lastResult.get(), p_186932_);
         this.lastResult.set($$3);
         return $$3.value;
      }

      static final class Leaf<T> extends Climate.RTree.Node<T> {
         final T value;

         Leaf(Climate.ParameterPoint p_186950_, T p_186951_) {
            super(p_186950_.parameterSpace());
            this.value = p_186951_;
         }

         @Override
         protected Climate.RTree.Leaf<T> search(long[] p_186953_, @Nullable Climate.RTree.Leaf<T> p_186954_, Climate.DistanceMetric<T> p_186955_) {
            return this;
         }
      }

      abstract static class Node<T> {
         protected final Climate.Parameter[] parameterSpace;

         protected Node(List<Climate.Parameter> p_186958_) {
            this.parameterSpace = p_186958_.toArray(new Climate.Parameter[0]);
         }

         protected abstract Climate.RTree.Leaf<T> search(long[] var1, @Nullable Climate.RTree.Leaf<T> var2, Climate.DistanceMetric<T> var3);

         protected long distance(long[] p_186960_) {
            long $$1 = 0L;

            for (int $$2 = 0; $$2 < 7; $$2++) {
               $$1 += Mth.square(this.parameterSpace[$$2].distance(p_186960_[$$2]));
            }

            return $$1;
         }

         @Override
         public String toString() {
            return Arrays.toString((Object[])this.parameterSpace);
         }
      }

      static final class SubTree<T> extends Climate.RTree.Node<T> {
         final Climate.RTree.Node<T>[] children;

         protected SubTree(List<? extends Climate.RTree.Node<T>> p_186967_) {
            this(Climate.RTree.buildParameterSpace(p_186967_), p_186967_);
         }

         protected SubTree(List<Climate.Parameter> p_186969_, List<? extends Climate.RTree.Node<T>> p_186970_) {
            super(p_186969_);
            this.children = p_186970_.toArray(new Climate.RTree.Node[0]);
         }

         @Override
         protected Climate.RTree.Leaf<T> search(long[] p_186972_, @Nullable Climate.RTree.Leaf<T> p_186973_, Climate.DistanceMetric<T> p_186974_) {
            long $$3 = p_186973_ == null ? Long.MAX_VALUE : p_186974_.distance(p_186973_, p_186972_);
            Climate.RTree.Leaf<T> $$4 = p_186973_;

            for (Climate.RTree.Node<T> $$5 : this.children) {
               long $$6 = p_186974_.distance($$5, p_186972_);
               if ($$3 > $$6) {
                  Climate.RTree.Leaf<T> $$7 = $$5.search(p_186972_, $$4, p_186974_);
                  long $$8 = $$5 == $$7 ? $$6 : p_186974_.distance($$7, p_186972_);
                  if ($$3 > $$8) {
                     $$3 = $$8;
                     $$4 = $$7;
                  }
               }
            }

            return $$4;
         }
      }
   }

   public record Sampler(
      DensityFunction temperature,
      DensityFunction humidity,
      DensityFunction continentalness,
      DensityFunction erosion,
      DensityFunction depth,
      DensityFunction weirdness,
      List<Climate.ParameterPoint> spawnTarget
   ) {
      public Climate.TargetPoint sample(int p_186975_, int p_186976_, int p_186977_) {
         int $$3 = QuartPos.toBlock(p_186975_);
         int $$4 = QuartPos.toBlock(p_186976_);
         int $$5 = QuartPos.toBlock(p_186977_);
         DensityFunction.SinglePointContext $$6 = new DensityFunction.SinglePointContext($$3, $$4, $$5);
         return Climate.target(
            (float)this.temperature.compute($$6),
            (float)this.humidity.compute($$6),
            (float)this.continentalness.compute($$6),
            (float)this.erosion.compute($$6),
            (float)this.depth.compute($$6),
            (float)this.weirdness.compute($$6)
         );
      }

      public BlockPos findSpawnPosition() {
         return this.spawnTarget.isEmpty() ? BlockPos.ZERO : Climate.findSpawnPosition(this.spawnTarget, this);
      }
   }

   static class SpawnFinder {
      Climate.SpawnFinder.Result result;

      SpawnFinder(List<Climate.ParameterPoint> p_207872_, Climate.Sampler p_207873_) {
         this.result = getSpawnPositionAndFitness(p_207872_, p_207873_, 0, 0);
         this.radialSearch(p_207872_, p_207873_, 2048.0F, 512.0F);
         this.radialSearch(p_207872_, p_207873_, 512.0F, 32.0F);
      }

      private void radialSearch(List<Climate.ParameterPoint> p_207875_, Climate.Sampler p_207876_, float p_207877_, float p_207878_) {
         float $$4 = 0.0F;
         float $$5 = p_207878_;
         BlockPos $$6 = this.result.location();

         while ($$5 <= p_207877_) {
            int $$7 = $$6.getX() + (int)(Math.sin($$4) * $$5);
            int $$8 = $$6.getZ() + (int)(Math.cos($$4) * $$5);
            Climate.SpawnFinder.Result $$9 = getSpawnPositionAndFitness(p_207875_, p_207876_, $$7, $$8);
            if ($$9.fitness() < this.result.fitness()) {
               this.result = $$9;
            }

            $$4 += p_207878_ / $$5;
            if ($$4 > Math.PI * 2) {
               $$4 = 0.0F;
               $$5 += p_207878_;
            }
         }
      }

      private static Climate.SpawnFinder.Result getSpawnPositionAndFitness(
         List<Climate.ParameterPoint> p_207880_, Climate.Sampler p_207881_, int p_207882_, int p_207883_
      ) {
         double $$4 = Mth.square(2500.0);
         int $$5 = 2;
         long $$6 = (long)(Mth.square(10000.0F) * Math.pow((Mth.square((long)p_207882_) + Mth.square((long)p_207883_)) / $$4, 2.0));
         Climate.TargetPoint $$7 = p_207881_.sample(QuartPos.fromBlock(p_207882_), 0, QuartPos.fromBlock(p_207883_));
         Climate.TargetPoint $$8 = new Climate.TargetPoint($$7.temperature(), $$7.humidity(), $$7.continentalness(), $$7.erosion(), 0L, $$7.weirdness());
         long $$9 = Long.MAX_VALUE;

         for (Climate.ParameterPoint $$10 : p_207880_) {
            $$9 = Math.min($$9, $$10.fitness($$8));
         }

         return new Climate.SpawnFinder.Result(new BlockPos(p_207882_, 0, p_207883_), $$6 + $$9);
      }

      record Result(BlockPos location, long fitness) {
      }
   }

   public record TargetPoint(long temperature, long humidity, long continentalness, long erosion, long depth, long weirdness) {
      @VisibleForTesting
      protected long[] toParameterArray() {
         return new long[]{this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, 0L};
      }
   }
}
