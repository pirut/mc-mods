package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.mutable.MutableObject;

public interface CubicSpline<C, I extends ToFloatFunction<C>> extends ToFloatFunction<C> {
   @VisibleForDebug
   String parityString();

   CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> var1);

   static <C, I extends ToFloatFunction<C>> Codec<CubicSpline<C, I>> codec(Codec<I> p_184263_) {
      MutableObject<Codec<CubicSpline<C, I>>> $$1 = new MutableObject();

      record Point<C, I extends ToFloatFunction<C>>(float location, CubicSpline<C, I> value, float derivative) {
      }

      Codec<Point<C, I>> $$2 = RecordCodecBuilder.create(
         p_337574_ -> p_337574_.group(
               Codec.FLOAT.fieldOf("location").forGetter(Point::location),
               Codec.lazyInitialized($$1::getValue).fieldOf("value").forGetter(Point::value),
               Codec.FLOAT.fieldOf("derivative").forGetter(Point::derivative)
            )
            .apply(p_337574_, (p_184242_, p_184243_, p_184244_) -> new Point(p_184242_, p_184243_, p_184244_))
      );
      Codec<CubicSpline.Multipoint<C, I>> $$3 = RecordCodecBuilder.create(
         p_184267_ -> p_184267_.group(
               p_184263_.fieldOf("coordinate").forGetter(CubicSpline.Multipoint::coordinate),
               ExtraCodecs.nonEmptyList($$2.listOf())
                  .fieldOf("points")
                  .forGetter(
                     p_184272_ -> IntStream.range(0, p_184272_.locations.length)
                        .mapToObj(
                           p_184249_ -> new Point(
                              p_184272_.locations()[p_184249_], (CubicSpline<C, I>)p_184272_.values().get(p_184249_), p_184272_.derivatives()[p_184249_]
                           )
                        )
                        .toList()
                  )
            )
            .apply(p_184267_, (p_184258_, p_184259_) -> {
               float[] $$2x = new float[p_184259_.size()];
               com.google.common.collect.ImmutableList.Builder<CubicSpline<C, I>> $$3x = ImmutableList.builder();
               float[] $$4 = new float[p_184259_.size()];

               for (int $$5 = 0; $$5 < p_184259_.size(); $$5++) {
                  Point<C, I> $$6 = (Point<C, I>)p_184259_.get($$5);
                  $$2x[$$5] = $$6.location();
                  $$3x.add($$6.value());
                  $$4[$$5] = $$6.derivative();
               }

               return CubicSpline.Multipoint.create((I)p_184258_, $$2x, $$3x.build(), $$4);
            })
      );
      $$1.setValue(
         Codec.either(Codec.FLOAT, $$3)
            .xmap(
               p_184261_ -> (CubicSpline)p_184261_.map(CubicSpline.Constant::new, p_184246_ -> p_184246_),
               p_184251_ -> p_184251_ instanceof CubicSpline.Constant<C, I> $$1x ? Either.left($$1x.value()) : Either.right((CubicSpline.Multipoint)p_184251_)
            )
      );
      return (Codec<CubicSpline<C, I>>)$$1.getValue();
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> constant(float p_184240_) {
      return new CubicSpline.Constant<>(p_184240_);
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline.Builder<C, I> builder(I p_184253_) {
      return new CubicSpline.Builder<>(p_184253_);
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline.Builder<C, I> builder(I p_184255_, ToFloatFunction<Float> p_184256_) {
      return new CubicSpline.Builder<>(p_184255_, p_184256_);
   }

   public static final class Builder<C, I extends ToFloatFunction<C>> {
      private final I coordinate;
      private final ToFloatFunction<Float> valueTransformer;
      private final FloatList locations = new FloatArrayList();
      private final List<CubicSpline<C, I>> values = Lists.newArrayList();
      private final FloatList derivatives = new FloatArrayList();

      protected Builder(I p_184293_) {
         this(p_184293_, ToFloatFunction.IDENTITY);
      }

      protected Builder(I p_184295_, ToFloatFunction<Float> p_184296_) {
         this.coordinate = p_184295_;
         this.valueTransformer = p_184296_;
      }

      public CubicSpline.Builder<C, I> addPoint(float p_216115_, float p_216116_) {
         return this.addPoint(p_216115_, new CubicSpline.Constant<>(this.valueTransformer.apply(p_216116_)), 0.0F);
      }

      public CubicSpline.Builder<C, I> addPoint(float p_184299_, float p_184300_, float p_184301_) {
         return this.addPoint(p_184299_, new CubicSpline.Constant<>(this.valueTransformer.apply(p_184300_)), p_184301_);
      }

      public CubicSpline.Builder<C, I> addPoint(float p_216118_, CubicSpline<C, I> p_216119_) {
         return this.addPoint(p_216118_, p_216119_, 0.0F);
      }

      private CubicSpline.Builder<C, I> addPoint(float p_184303_, CubicSpline<C, I> p_184304_, float p_184305_) {
         if (!this.locations.isEmpty() && p_184303_ <= this.locations.getFloat(this.locations.size() - 1)) {
            throw new IllegalArgumentException("Please register points in ascending order");
         } else {
            this.locations.add(p_184303_);
            this.values.add(p_184304_);
            this.derivatives.add(p_184305_);
            return this;
         }
      }

      public CubicSpline<C, I> build() {
         if (this.locations.isEmpty()) {
            throw new IllegalStateException("No elements added");
         } else {
            return CubicSpline.Multipoint.create(
               this.coordinate, this.locations.toFloatArray(), ImmutableList.copyOf(this.values), this.derivatives.toFloatArray()
            );
         }
      }
   }

   @VisibleForDebug
   public record Constant<C, I extends ToFloatFunction<C>>(float value) implements CubicSpline<C, I> {
      @Override
      public float apply(C p_184313_) {
         return this.value;
      }

      @Override
      public String parityString() {
         return String.format(Locale.ROOT, "k=%.3f", this.value);
      }

      @Override
      public float minValue() {
         return this.value;
      }

      @Override
      public float maxValue() {
         return this.value;
      }

      @Override
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> p_211581_) {
         return this;
      }
   }

   public interface CoordinateVisitor<I> {
      I visit(I var1);
   }

   @VisibleForDebug
   public record Multipoint<C, I extends ToFloatFunction<C>>(
      I coordinate, float[] locations, List<CubicSpline<C, I>> values, float[] derivatives, float minValue, float maxValue
   ) implements CubicSpline<C, I> {
      public Multipoint(I coordinate, float[] locations, List<CubicSpline<C, I>> values, float[] derivatives, float minValue, float maxValue) {
         validateSizes(locations, values, derivatives);
         this.coordinate = coordinate;
         this.locations = locations;
         this.values = values;
         this.derivatives = derivatives;
         this.minValue = minValue;
         this.maxValue = maxValue;
      }

      static <C, I extends ToFloatFunction<C>> CubicSpline.Multipoint<C, I> create(
         I p_216144_, float[] p_216145_, List<CubicSpline<C, I>> p_216146_, float[] p_216147_
      ) {
         validateSizes(p_216145_, p_216146_, p_216147_);
         int $$4 = p_216145_.length - 1;
         float $$5 = Float.POSITIVE_INFINITY;
         float $$6 = Float.NEGATIVE_INFINITY;
         float $$7 = p_216144_.minValue();
         float $$8 = p_216144_.maxValue();
         if ($$7 < p_216145_[0]) {
            float $$9 = linearExtend($$7, p_216145_, p_216146_.get(0).minValue(), p_216147_, 0);
            float $$10 = linearExtend($$7, p_216145_, p_216146_.get(0).maxValue(), p_216147_, 0);
            $$5 = Math.min($$5, Math.min($$9, $$10));
            $$6 = Math.max($$6, Math.max($$9, $$10));
         }

         if ($$8 > p_216145_[$$4]) {
            float $$11 = linearExtend($$8, p_216145_, p_216146_.get($$4).minValue(), p_216147_, $$4);
            float $$12 = linearExtend($$8, p_216145_, p_216146_.get($$4).maxValue(), p_216147_, $$4);
            $$5 = Math.min($$5, Math.min($$11, $$12));
            $$6 = Math.max($$6, Math.max($$11, $$12));
         }

         for (CubicSpline<C, I> $$13 : p_216146_) {
            $$5 = Math.min($$5, $$13.minValue());
            $$6 = Math.max($$6, $$13.maxValue());
         }

         for (int $$14 = 0; $$14 < $$4; $$14++) {
            float $$15 = p_216145_[$$14];
            float $$16 = p_216145_[$$14 + 1];
            float $$17 = $$16 - $$15;
            CubicSpline<C, I> $$18 = p_216146_.get($$14);
            CubicSpline<C, I> $$19 = p_216146_.get($$14 + 1);
            float $$20 = $$18.minValue();
            float $$21 = $$18.maxValue();
            float $$22 = $$19.minValue();
            float $$23 = $$19.maxValue();
            float $$24 = p_216147_[$$14];
            float $$25 = p_216147_[$$14 + 1];
            if ($$24 != 0.0F || $$25 != 0.0F) {
               float $$26 = $$24 * $$17;
               float $$27 = $$25 * $$17;
               float $$28 = Math.min($$20, $$22);
               float $$29 = Math.max($$21, $$23);
               float $$30 = $$26 - $$23 + $$20;
               float $$31 = $$26 - $$22 + $$21;
               float $$32 = -$$27 + $$22 - $$21;
               float $$33 = -$$27 + $$23 - $$20;
               float $$34 = Math.min($$30, $$32);
               float $$35 = Math.max($$31, $$33);
               $$5 = Math.min($$5, $$28 + 0.25F * $$34);
               $$6 = Math.max($$6, $$29 + 0.25F * $$35);
            }
         }

         return new CubicSpline.Multipoint<>(p_216144_, p_216145_, p_216146_, p_216147_, $$5, $$6);
      }

      private static float linearExtend(float p_216134_, float[] p_216135_, float p_216136_, float[] p_216137_, int p_216138_) {
         float $$5 = p_216137_[p_216138_];
         return $$5 == 0.0F ? p_216136_ : p_216136_ + $$5 * (p_216134_ - p_216135_[p_216138_]);
      }

      private static <C, I extends ToFloatFunction<C>> void validateSizes(float[] p_216152_, List<CubicSpline<C, I>> p_216153_, float[] p_216154_) {
         if (p_216152_.length != p_216153_.size() || p_216152_.length != p_216154_.length) {
            throw new IllegalArgumentException("All lengths must be equal, got: " + p_216152_.length + " " + p_216153_.size() + " " + p_216154_.length);
         } else if (p_216152_.length == 0) {
            throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
         }
      }

      @Override
      public float apply(C p_184340_) {
         float $$1 = this.coordinate.apply(p_184340_);
         int $$2 = findIntervalStart(this.locations, $$1);
         int $$3 = this.locations.length - 1;
         if ($$2 < 0) {
            return linearExtend($$1, this.locations, this.values.get(0).apply(p_184340_), this.derivatives, 0);
         } else if ($$2 == $$3) {
            return linearExtend($$1, this.locations, this.values.get($$3).apply(p_184340_), this.derivatives, $$3);
         } else {
            float $$4 = this.locations[$$2];
            float $$5 = this.locations[$$2 + 1];
            float $$6 = ($$1 - $$4) / ($$5 - $$4);
            ToFloatFunction<C> $$7 = (ToFloatFunction<C>)this.values.get($$2);
            ToFloatFunction<C> $$8 = (ToFloatFunction<C>)this.values.get($$2 + 1);
            float $$9 = this.derivatives[$$2];
            float $$10 = this.derivatives[$$2 + 1];
            float $$11 = $$7.apply(p_184340_);
            float $$12 = $$8.apply(p_184340_);
            float $$13 = $$9 * ($$5 - $$4) - ($$12 - $$11);
            float $$14 = -$$10 * ($$5 - $$4) + ($$12 - $$11);
            return Mth.lerp($$6, $$11, $$12) + $$6 * (1.0F - $$6) * Mth.lerp($$6, $$13, $$14);
         }
      }

      private static int findIntervalStart(float[] p_216149_, float p_216150_) {
         return Mth.binarySearch(0, p_216149_.length, p_216142_ -> p_216150_ < p_216149_[p_216142_]) - 1;
      }

      @VisibleForTesting
      @Override
      public String parityString() {
         return "Spline{coordinate="
            + this.coordinate
            + ", locations="
            + this.toString(this.locations)
            + ", derivatives="
            + this.toString(this.derivatives)
            + ", values="
            + this.values.stream().map(CubicSpline::parityString).collect(Collectors.joining(", ", "[", "]"))
            + "}";
      }

      private String toString(float[] p_184335_) {
         return "["
            + IntStream.range(0, p_184335_.length)
               .mapToDouble(p_184338_ -> p_184335_[p_184338_])
               .mapToObj(p_184330_ -> String.format(Locale.ROOT, "%.3f", p_184330_))
               .collect(Collectors.joining(", "))
            + "]";
      }

      @Override
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> p_211585_) {
         return create(
            p_211585_.visit(this.coordinate), this.locations, this.values().stream().map(p_211588_ -> p_211588_.mapAll(p_211585_)).toList(), this.derivatives
         );
      }
   }
}
