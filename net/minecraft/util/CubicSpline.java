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

public interface CubicSpline<C, I extends BoundedFloatFunction<C>> extends BoundedFloatFunction<C> {
   @VisibleForDebug
   String parityString();

   CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> visitor);

   static <C, I extends BoundedFloatFunction<C>> Codec<CubicSpline<C, I>> codec(Codec<I> valueCodec) {
      MutableObject<Codec<CubicSpline<C, I>>> mutableObject = new MutableObject();

      record Point<C, I extends BoundedFloatFunction<C>>(float location, CubicSpline<C, I> value, float derivative) {
      }

      Codec<Point<C, I>> codec = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.FLOAT.fieldOf("location").forGetter(Point::location),
               Codec.lazyInitialized(mutableObject).fieldOf("value").forGetter(Point::value),
               Codec.FLOAT.fieldOf("derivative").forGetter(Point::derivative)
            )
            .apply(instance, (f, cubicSpline, g) -> new Point(f, cubicSpline, g))
      );
      Codec<CubicSpline.Multipoint<C, I>> codec2 = RecordCodecBuilder.create(
         instance -> instance.group(
               valueCodec.fieldOf("coordinate").forGetter(CubicSpline.Multipoint::coordinate),
               ExtraCodecs.nonEmptyList(codec.listOf())
                  .fieldOf("points")
                  .forGetter(
                     multipoint -> IntStream.range(0, multipoint.locations.length)
                        .mapToObj(i -> new Point(multipoint.locations()[i], (CubicSpline<C, I>)multipoint.values().get(i), multipoint.derivatives()[i]))
                        .toList()
                  )
            )
            .apply(instance, (boundedFloatFunction, list) -> {
               float[] fs = new float[list.size()];
               com.google.common.collect.ImmutableList.Builder<CubicSpline<C, I>> builder = ImmutableList.builder();
               float[] gs = new float[list.size()];

               for (int i = 0; i < list.size(); i++) {
                  Point<C, I> lv = (Point<C, I>)list.get(i);
                  fs[i] = lv.location();
                  builder.add(lv.value());
                  gs[i] = lv.derivative();
               }

               return CubicSpline.Multipoint.create((I)boundedFloatFunction, fs, builder.build(), gs);
            })
      );
      mutableObject.setValue(
         Codec.either(Codec.FLOAT, codec2)
            .xmap(
               either -> (CubicSpline)either.map(CubicSpline.Constant::new, multipoint -> multipoint),
               cubicSpline -> cubicSpline instanceof CubicSpline.Constant<C, I> constant
                  ? Either.left(constant.value())
                  : Either.right((CubicSpline.Multipoint)cubicSpline)
            )
      );
      return (Codec<CubicSpline<C, I>>)mutableObject.get();
   }

   static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> constant(float value) {
      return new CubicSpline.Constant<>(value);
   }

   static <C, I extends BoundedFloatFunction<C>> CubicSpline.Builder<C, I> builder(I coordinate) {
      return new CubicSpline.Builder<>(coordinate);
   }

   static <C, I extends BoundedFloatFunction<C>> CubicSpline.Builder<C, I> builder(I coordinate, BoundedFloatFunction<Float> valueTransformer) {
      return new CubicSpline.Builder<>(coordinate, valueTransformer);
   }

   public static final class Builder<C, I extends BoundedFloatFunction<C>> {
      private final I coordinate;
      private final BoundedFloatFunction<Float> valueTransformer;
      private final FloatList locations = new FloatArrayList();
      private final List<CubicSpline<C, I>> values = Lists.newArrayList();
      private final FloatList derivatives = new FloatArrayList();

      protected Builder(I coordinate) {
         this(coordinate, BoundedFloatFunction.IDENTITY);
      }

      protected Builder(I coordinate, BoundedFloatFunction<Float> valueTransformer) {
         this.coordinate = coordinate;
         this.valueTransformer = valueTransformer;
      }

      public CubicSpline.Builder<C, I> addPoint(float location, float value) {
         return this.addPoint(location, new CubicSpline.Constant<>(this.valueTransformer.apply(value)), 0.0F);
      }

      public CubicSpline.Builder<C, I> addPoint(float location, float value, float derivative) {
         return this.addPoint(location, new CubicSpline.Constant<>(this.valueTransformer.apply(value)), derivative);
      }

      public CubicSpline.Builder<C, I> addPoint(float location, CubicSpline<C, I> value) {
         return this.addPoint(location, value, 0.0F);
      }

      private CubicSpline.Builder<C, I> addPoint(float location, CubicSpline<C, I> value, float derivative) {
         if (!this.locations.isEmpty() && location <= this.locations.getFloat(this.locations.size() - 1)) {
            throw new IllegalArgumentException("Please register points in ascending order");
         } else {
            this.locations.add(location);
            this.values.add(value);
            this.derivatives.add(derivative);
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
   public record Constant<C, I extends BoundedFloatFunction<C>>(float value) implements CubicSpline<C, I> {
      @Override
      public float apply(C input) {
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
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> visitor) {
         return this;
      }
   }

   public interface CoordinateVisitor<I> {
      I visit(I object);
   }

   @VisibleForDebug
   public record Multipoint<C, I extends BoundedFloatFunction<C>>(
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

      static <C, I extends BoundedFloatFunction<C>> CubicSpline.Multipoint<C, I> create(
         I coordinate, float[] locations, List<CubicSpline<C, I>> values, float[] derivatives
      ) {
         validateSizes(locations, values, derivatives);
         int i = locations.length - 1;
         float f = Float.POSITIVE_INFINITY;
         float g = Float.NEGATIVE_INFINITY;
         float h = coordinate.minValue();
         float j = coordinate.maxValue();
         if (h < locations[0]) {
            float k = linearExtend(h, locations, values.get(0).minValue(), derivatives, 0);
            float l = linearExtend(h, locations, values.get(0).maxValue(), derivatives, 0);
            f = Math.min(f, Math.min(k, l));
            g = Math.max(g, Math.max(k, l));
         }

         if (j > locations[i]) {
            float k = linearExtend(j, locations, values.get(i).minValue(), derivatives, i);
            float l = linearExtend(j, locations, values.get(i).maxValue(), derivatives, i);
            f = Math.min(f, Math.min(k, l));
            g = Math.max(g, Math.max(k, l));
         }

         for (CubicSpline<C, I> cubicSpline : values) {
            f = Math.min(f, cubicSpline.minValue());
            g = Math.max(g, cubicSpline.maxValue());
         }

         for (int m = 0; m < i; m++) {
            float l = locations[m];
            float n = locations[m + 1];
            float o = n - l;
            CubicSpline<C, I> cubicSpline2 = values.get(m);
            CubicSpline<C, I> cubicSpline3 = values.get(m + 1);
            float p = cubicSpline2.minValue();
            float q = cubicSpline2.maxValue();
            float r = cubicSpline3.minValue();
            float s = cubicSpline3.maxValue();
            float t = derivatives[m];
            float u = derivatives[m + 1];
            if (t != 0.0F || u != 0.0F) {
               float v = t * o;
               float w = u * o;
               float x = Math.min(p, r);
               float y = Math.max(q, s);
               float z = v - s + p;
               float aa = v - r + q;
               float ab = -w + r - q;
               float ac = -w + s - p;
               float ad = Math.min(z, ab);
               float ae = Math.max(aa, ac);
               f = Math.min(f, x + 0.25F * ad);
               g = Math.max(g, y + 0.25F * ae);
            }
         }

         return new CubicSpline.Multipoint<>(coordinate, locations, values, derivatives, f, g);
      }

      private static float linearExtend(float coordinate, float[] locations, float value, float[] derivatives, int index) {
         float f = derivatives[index];
         return f == 0.0F ? value : value + f * (coordinate - locations[index]);
      }

      private static <C, I extends BoundedFloatFunction<C>> void validateSizes(float[] locations, List<CubicSpline<C, I>> values, float[] derivatives) {
         if (locations.length != values.size() || locations.length != derivatives.length) {
            throw new IllegalArgumentException("All lengths must be equal, got: " + locations.length + " " + values.size() + " " + derivatives.length);
         } else if (locations.length == 0) {
            throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
         }
      }

      @Override
      public float apply(C input) {
         float f = this.coordinate.apply(input);
         int i = findIntervalStart(this.locations, f);
         int j = this.locations.length - 1;
         if (i < 0) {
            return linearExtend(f, this.locations, this.values.get(0).apply(input), this.derivatives, 0);
         } else if (i == j) {
            return linearExtend(f, this.locations, this.values.get(j).apply(input), this.derivatives, j);
         } else {
            float g = this.locations[i];
            float h = this.locations[i + 1];
            float k = (f - g) / (h - g);
            BoundedFloatFunction<C> boundedFloatFunction = (BoundedFloatFunction<C>)this.values.get(i);
            BoundedFloatFunction<C> boundedFloatFunction2 = (BoundedFloatFunction<C>)this.values.get(i + 1);
            float l = this.derivatives[i];
            float m = this.derivatives[i + 1];
            float n = boundedFloatFunction.apply(input);
            float o = boundedFloatFunction2.apply(input);
            float p = l * (h - g) - (o - n);
            float q = -m * (h - g) + (o - n);
            return Mth.lerp(k, n, o) + k * (1.0F - k) * Mth.lerp(k, p, q);
         }
      }

      private static int findIntervalStart(float[] locations, float start) {
         return Mth.binarySearch(0, locations.length, i -> start < locations[i]) - 1;
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

      private String toString(float[] locations) {
         return "["
            + IntStream.range(0, locations.length)
               .mapToDouble(i -> locations[i])
               .mapToObj(d -> String.format(Locale.ROOT, "%.3f", d))
               .collect(Collectors.joining(", "))
            + "]";
      }

      @Override
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> visitor) {
         return create(
            visitor.visit(this.coordinate), this.locations, this.values().stream().map(cubicSpline -> cubicSpline.mapAll(visitor)).toList(), this.derivatives
         );
      }
   }
}
