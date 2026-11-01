package net.minecraft.advancements.criterion;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public interface MinMaxBounds<T extends Number & Comparable<T>> {
   SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(Component.translatable("argument.range.empty"));
   SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType(Component.translatable("argument.range.swapped"));

   MinMaxBounds.Bounds<T> bounds();

   default Optional<T> min() {
      return this.bounds().min;
   }

   default Optional<T> max() {
      return this.bounds().max;
   }

   default boolean isAny() {
      return this.bounds().isAny();
   }

   public record Bounds<T extends Number & Comparable<T>>(Optional<T> min, Optional<T> max) {

      public boolean isAny() {
         return this.min().isEmpty() && this.max().isEmpty();
      }

      public DataResult<MinMaxBounds.Bounds<T>> validateSwappedBoundsInCodec() {
         return this.areSwapped()
            ? DataResult.error(() -> "Swapped bounds in range: " + this.min() + " is higher than " + this.max())
            : DataResult.success(this);
      }

      public boolean areSwapped() {
         return this.min.isPresent() && this.max.isPresent() && this.min.get().compareTo(this.max.get()) > 0;
      }

      public Optional<T> asPoint() {
         Optional<T> optional = this.min();
         Optional<T> optional2 = this.max();
         return optional.equals(optional2) ? optional : Optional.empty();
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> any() {
         return new MinMaxBounds.Bounds<>(Optional.empty(), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> exactly(T value) {
         Optional<T> optional = Optional.of(value);
         return new MinMaxBounds.Bounds<>(optional, optional);
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> between(T min, T max) {
         return new MinMaxBounds.Bounds<>(Optional.of(min), Optional.of(max));
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> atLeast(T min) {
         return new MinMaxBounds.Bounds<>(Optional.of(min), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> atMost(T max) {
         return new MinMaxBounds.Bounds<>(Optional.empty(), Optional.of(max));
      }

      public <U extends Number & Comparable<U>> MinMaxBounds.Bounds<U> map(Function<T, U> mapper) {
         return new MinMaxBounds.Bounds<>(this.min.map(mapper), this.max.map(mapper));
      }

      static <T extends Number & Comparable<T>> Codec<MinMaxBounds.Bounds<T>> createCodec(Codec<T> valueCodec) {
         Codec<MinMaxBounds.Bounds<T>> codec = RecordCodecBuilder.create(
            instance -> instance.group(
                  valueCodec.optionalFieldOf("min").forGetter(MinMaxBounds.Bounds::min), valueCodec.optionalFieldOf("max").forGetter(MinMaxBounds.Bounds::max)
               )
               .apply(instance, MinMaxBounds.Bounds::new)
         );
         return Codec.either(codec, valueCodec).xmap(either -> (MinMaxBounds.Bounds)either.map(bounds -> bounds, object -> exactly((T)object)), bounds -> {
            Optional<T> optional = bounds.asPoint();
            return optional.isPresent() ? Either.right(optional.get()) : Either.left(bounds);
         });
      }

      static <B extends ByteBuf, T extends Number & Comparable<T>> StreamCodec<B, MinMaxBounds.Bounds<T>> createStreamCodec(StreamCodec<B, T> valueCodec) {
         return new StreamCodec<B, MinMaxBounds.Bounds<T>>() {
            private static final int MIN_FLAG = 1;
            private static final int MAX_FLAG = 2;

            public MinMaxBounds.Bounds<T> decode(B byteBuf) {
               byte b = byteBuf.readByte();
               Optional<T> optional = (b & 1) != 0 ? Optional.of(valueCodec.decode(byteBuf)) : Optional.empty();
               Optional<T> optional2 = (b & 2) != 0 ? Optional.of(valueCodec.decode(byteBuf)) : Optional.empty();
               return new MinMaxBounds.Bounds<>(optional, optional2);
            }

            public void encode(B byteBuf, MinMaxBounds.Bounds<T> bounds) {
               Optional<T> optional = bounds.min();
               Optional<T> optional2 = bounds.max();
               byteBuf.writeByte((optional.isPresent() ? 1 : 0) | (optional2.isPresent() ? 2 : 0));
               optional.ifPresent(number -> valueCodec.encode(byteBuf, (T)number));
               optional2.ifPresent(number -> valueCodec.encode(byteBuf, (T)number));
            }
         };
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> fromReader(
         StringReader reader, Function<String, T> parser, Supplier<DynamicCommandExceptionType> exceptionSupplier
      ) throws CommandSyntaxException {
         if (!reader.canRead()) {
            throw MinMaxBounds.ERROR_EMPTY.createWithContext(reader);
         } else {
            int i = reader.getCursor();

            try {
               Optional<T> optional = readNumber(reader, parser, exceptionSupplier);
               Optional<T> optional2;
               if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
                  reader.skip();
                  reader.skip();
                  optional2 = readNumber(reader, parser, exceptionSupplier);
               } else {
                  optional2 = optional;
               }

               if (optional.isEmpty() && optional2.isEmpty()) {
                  throw MinMaxBounds.ERROR_EMPTY.createWithContext(reader);
               } else {
                  return new MinMaxBounds.Bounds<>(optional, optional2);
               }
            } catch (CommandSyntaxException var6) {
               reader.setCursor(i);
               throw new CommandSyntaxException(var6.getType(), var6.getRawMessage(), var6.getInput(), i);
            }
         }
      }

      private static <T extends Number> Optional<T> readNumber(
         StringReader reader, Function<String, T> parser, Supplier<DynamicCommandExceptionType> exceptionSupplier
      ) throws CommandSyntaxException {
         int i = reader.getCursor();

         while (reader.canRead() && isAllowedInputChar(reader)) {
            reader.skip();
         }

         String string = reader.getString().substring(i, reader.getCursor());
         if (string.isEmpty()) {
            return Optional.empty();
         } else {
            try {
               return Optional.of(parser.apply(string));
            } catch (NumberFormatException var6) {
               throw exceptionSupplier.get().createWithContext(reader, string);
            }
         }
      }

      private static boolean isAllowedInputChar(StringReader reader) {
         char c = reader.peek();
         if ((c < '0' || c > '9') && c != '-') {
            return c != '.' ? false : !reader.canRead(2) || reader.peek(1) != '.';
         } else {
            return true;
         }
      }
   }

   public record Doubles(MinMaxBounds.Bounds<Double> bounds, MinMaxBounds.Bounds<Double> boundsSqr) implements MinMaxBounds<Double> {
      public static final MinMaxBounds.Doubles ANY = new MinMaxBounds.Doubles(MinMaxBounds.Bounds.any());
      public static final Codec<MinMaxBounds.Doubles> CODEC = MinMaxBounds.Bounds.createCodec(Codec.DOUBLE)
         .validate(MinMaxBounds.Bounds::validateSwappedBoundsInCodec)
         .xmap(MinMaxBounds.Doubles::new, MinMaxBounds.Doubles::bounds);
      public static final StreamCodec<ByteBuf, MinMaxBounds.Doubles> STREAM_CODEC = MinMaxBounds.Bounds.createStreamCodec(ByteBufCodecs.DOUBLE)
         .map(MinMaxBounds.Doubles::new, MinMaxBounds.Doubles::bounds);

      private Doubles(MinMaxBounds.Bounds<Double> bounds) {
         this(bounds, bounds.map(Mth::square));
      }

      public static MinMaxBounds.Doubles exactly(double value) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.exactly(value));
      }

      public static MinMaxBounds.Doubles between(double min, double max) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.between(min, max));
      }

      public static MinMaxBounds.Doubles atLeast(double min) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.atLeast(min));
      }

      public static MinMaxBounds.Doubles atMost(double max) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.atMost(max));
      }

      public boolean matches(double value) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > value ? false : this.bounds.max.isEmpty() || !(this.bounds.max.get() < value);
      }

      public boolean matchesSqr(double value) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > value
            ? false
            : this.boundsSqr.max.isEmpty() || !(this.boundsSqr.max.get() < value);
      }

      public static MinMaxBounds.Doubles fromReader(StringReader reader) throws CommandSyntaxException {
         int i = reader.getCursor();
         MinMaxBounds.Bounds<Double> bounds = MinMaxBounds.Bounds.fromReader(
            reader, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble
         );
         if (bounds.areSwapped()) {
            reader.setCursor(i);
            throw ERROR_SWAPPED.createWithContext(reader);
         } else {
            return new MinMaxBounds.Doubles(bounds);
         }
      }
   }

   public record FloatDegrees(MinMaxBounds.Bounds<Float> bounds) implements MinMaxBounds<Float> {
      public static final MinMaxBounds.FloatDegrees ANY = new MinMaxBounds.FloatDegrees(MinMaxBounds.Bounds.any());
      public static final Codec<MinMaxBounds.FloatDegrees> CODEC = MinMaxBounds.Bounds.createCodec(Codec.FLOAT)
         .xmap(MinMaxBounds.FloatDegrees::new, MinMaxBounds.FloatDegrees::bounds);
      public static final StreamCodec<ByteBuf, MinMaxBounds.FloatDegrees> STREAM_CODEC = MinMaxBounds.Bounds.createStreamCodec(ByteBufCodecs.FLOAT)
         .map(MinMaxBounds.FloatDegrees::new, MinMaxBounds.FloatDegrees::bounds);

      public static MinMaxBounds.FloatDegrees fromReader(StringReader reader) throws CommandSyntaxException {
         MinMaxBounds.Bounds<Float> bounds = MinMaxBounds.Bounds.fromReader(
            reader, Float::parseFloat, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidFloat
         );
         return new MinMaxBounds.FloatDegrees(bounds);
      }
   }

   public record Ints(MinMaxBounds.Bounds<Integer> bounds, MinMaxBounds.Bounds<Long> boundsSqr) implements MinMaxBounds<Integer> {
      public static final MinMaxBounds.Ints ANY = new MinMaxBounds.Ints(MinMaxBounds.Bounds.any());
      public static final Codec<MinMaxBounds.Ints> CODEC = MinMaxBounds.Bounds.createCodec(Codec.INT)
         .validate(MinMaxBounds.Bounds::validateSwappedBoundsInCodec)
         .xmap(MinMaxBounds.Ints::new, MinMaxBounds.Ints::bounds);
      public static final StreamCodec<ByteBuf, MinMaxBounds.Ints> STREAM_CODEC = MinMaxBounds.Bounds.createStreamCodec(ByteBufCodecs.INT)
         .map(MinMaxBounds.Ints::new, MinMaxBounds.Ints::bounds);

      private Ints(MinMaxBounds.Bounds<Integer> bounds) {
         this(bounds, bounds.map(integer -> Mth.square(integer.longValue())));
      }

      public static MinMaxBounds.Ints exactly(int value) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.exactly(value));
      }

      public static MinMaxBounds.Ints between(int min, int max) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.between(min, max));
      }

      public static MinMaxBounds.Ints atLeast(int min) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.atLeast(min));
      }

      public static MinMaxBounds.Ints atMost(int max) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.atMost(max));
      }

      public boolean matches(int value) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > value ? false : this.bounds.max.isEmpty() || this.bounds.max.get() >= value;
      }

      public boolean matchesSqr(long value) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > value ? false : this.boundsSqr.max.isEmpty() || this.boundsSqr.max.get() >= value;
      }

      public static MinMaxBounds.Ints fromReader(StringReader reader) throws CommandSyntaxException {
         int i = reader.getCursor();
         MinMaxBounds.Bounds<Integer> bounds = MinMaxBounds.Bounds.fromReader(
            reader, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt
         );
         if (bounds.areSwapped()) {
            reader.setCursor(i);
            throw ERROR_SWAPPED.createWithContext(reader);
         } else {
            return new MinMaxBounds.Ints(bounds);
         }
      }
   }
}
