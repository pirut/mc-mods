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
         Optional<T> optional1 = this.max();
         return optional.equals(optional1) ? optional : Optional.empty();
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> any() {
         return new MinMaxBounds.Bounds<>(Optional.empty(), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> exactly(T p_469224_) {
         Optional<T> optional = Optional.of(p_469224_);
         return new MinMaxBounds.Bounds<>(optional, optional);
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> between(T p_469471_, T p_467452_) {
         return new MinMaxBounds.Bounds<>(Optional.of(p_469471_), Optional.of(p_467452_));
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> atLeast(T p_467833_) {
         return new MinMaxBounds.Bounds<>(Optional.of(p_467833_), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> atMost(T p_467474_) {
         return new MinMaxBounds.Bounds<>(Optional.empty(), Optional.of(p_467474_));
      }

      public <U extends Number & Comparable<U>> MinMaxBounds.Bounds<U> map(Function<T, U> p_467974_) {
         return new MinMaxBounds.Bounds<>(this.min.map(p_467974_), this.max.map(p_467974_));
      }

      static <T extends Number & Comparable<T>> Codec<MinMaxBounds.Bounds<T>> createCodec(Codec<T> p_467760_) {
         Codec<MinMaxBounds.Bounds<T>> codec = RecordCodecBuilder.create(
            p_469318_ -> p_469318_.group(
                  p_467760_.optionalFieldOf("min").forGetter(MinMaxBounds.Bounds::min), p_467760_.optionalFieldOf("max").forGetter(MinMaxBounds.Bounds::max)
               )
               .apply(p_469318_, MinMaxBounds.Bounds::new)
         );
         return Codec.either(codec, p_467760_)
            .xmap(p_467854_ -> (MinMaxBounds.Bounds)p_467854_.map(p_469720_ -> p_469720_, p_467164_ -> exactly((T)p_467164_)), p_467787_ -> {
               Optional<T> optional = p_467787_.asPoint();
               return optional.isPresent() ? Either.right(optional.get()) : Either.left(p_467787_);
            });
      }

      static <B extends ByteBuf, T extends Number & Comparable<T>> StreamCodec<B, MinMaxBounds.Bounds<T>> createStreamCodec(final StreamCodec<B, T> p_467836_) {
         return new StreamCodec<B, MinMaxBounds.Bounds<T>>() {
            private static final int MIN_FLAG = 1;
            private static final int MAX_FLAG = 2;

            public MinMaxBounds.Bounds<T> decode(B p_467201_) {
               byte b0 = p_467201_.readByte();
               Optional<T> optional = (b0 & 1) != 0 ? Optional.of(p_467836_.decode(p_467201_)) : Optional.empty();
               Optional<T> optional1 = (b0 & 2) != 0 ? Optional.of(p_467836_.decode(p_467201_)) : Optional.empty();
               return new MinMaxBounds.Bounds<>(optional, optional1);
            }

            public void encode(B p_467330_, MinMaxBounds.Bounds<T> p_468206_) {
               Optional<T> optional = p_468206_.min();
               Optional<T> optional1 = p_468206_.max();
               p_467330_.writeByte((optional.isPresent() ? 1 : 0) | (optional1.isPresent() ? 2 : 0));
               optional.ifPresent(p_468905_ -> p_467836_.encode(p_467330_, (T)p_468905_));
               optional1.ifPresent(p_467650_ -> p_467836_.encode(p_467330_, (T)p_467650_));
            }
         };
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> fromReader(
         StringReader p_467295_, Function<String, T> p_469550_, Supplier<DynamicCommandExceptionType> p_468747_
      ) throws CommandSyntaxException {
         if (!p_467295_.canRead()) {
            throw MinMaxBounds.ERROR_EMPTY.createWithContext(p_467295_);
         } else {
            int i = p_467295_.getCursor();

            try {
               Optional<T> optional = readNumber(p_467295_, p_469550_, p_468747_);
               Optional<T> optional1;
               if (p_467295_.canRead(2) && p_467295_.peek() == '.' && p_467295_.peek(1) == '.') {
                  p_467295_.skip();
                  p_467295_.skip();
                  optional1 = readNumber(p_467295_, p_469550_, p_468747_);
               } else {
                  optional1 = optional;
               }

               if (optional.isEmpty() && optional1.isEmpty()) {
                  throw MinMaxBounds.ERROR_EMPTY.createWithContext(p_467295_);
               } else {
                  return new MinMaxBounds.Bounds<>(optional, optional1);
               }
            } catch (CommandSyntaxException var6) {
               p_467295_.setCursor(i);
               throw new CommandSyntaxException(var6.getType(), var6.getRawMessage(), var6.getInput(), i);
            }
         }
      }

      private static <T extends Number> Optional<T> readNumber(
         StringReader p_468303_, Function<String, T> p_467179_, Supplier<DynamicCommandExceptionType> p_469078_
      ) throws CommandSyntaxException {
         int i = p_468303_.getCursor();

         while (p_468303_.canRead() && isAllowedInputChar(p_468303_)) {
            p_468303_.skip();
         }

         String s = p_468303_.getString().substring(i, p_468303_.getCursor());
         if (s.isEmpty()) {
            return Optional.empty();
         } else {
            try {
               return Optional.of(p_467179_.apply(s));
            } catch (NumberFormatException var6) {
               throw p_469078_.get().createWithContext(p_468303_, s);
            }
         }
      }

      private static boolean isAllowedInputChar(StringReader p_467863_) {
         char c0 = p_467863_.peek();
         if ((c0 < '0' || c0 > '9') && c0 != '-') {
            return c0 != '.' ? false : !p_467863_.canRead(2) || p_467863_.peek(1) != '.';
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

      private Doubles(MinMaxBounds.Bounds<Double> p_468277_) {
         this(p_468277_, p_468277_.map(Mth::square));
      }

      public static MinMaxBounds.Doubles exactly(double p_467560_) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.exactly(p_467560_));
      }

      public static MinMaxBounds.Doubles between(double p_468339_, double p_468949_) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.between(p_468339_, p_468949_));
      }

      public static MinMaxBounds.Doubles atLeast(double p_467012_) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.atLeast(p_467012_));
      }

      public static MinMaxBounds.Doubles atMost(double p_468397_) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.atMost(p_468397_));
      }

      public boolean matches(double p_469211_) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > p_469211_ ? false : this.bounds.max.isEmpty() || !(this.bounds.max.get() < p_469211_);
      }

      public boolean matchesSqr(double p_468802_) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > p_468802_
            ? false
            : this.boundsSqr.max.isEmpty() || !(this.boundsSqr.max.get() < p_468802_);
      }

      public static MinMaxBounds.Doubles fromReader(StringReader p_467548_) throws CommandSyntaxException {
         int i = p_467548_.getCursor();
         MinMaxBounds.Bounds<Double> bounds = MinMaxBounds.Bounds.fromReader(
            p_467548_, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble
         );
         if (bounds.areSwapped()) {
            p_467548_.setCursor(i);
            throw ERROR_SWAPPED.createWithContext(p_467548_);
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

      public static MinMaxBounds.FloatDegrees fromReader(StringReader p_468441_) throws CommandSyntaxException {
         MinMaxBounds.Bounds<Float> bounds = MinMaxBounds.Bounds.fromReader(
            p_468441_, Float::parseFloat, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidFloat
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

      private Ints(MinMaxBounds.Bounds<Integer> p_466835_) {
         this(p_466835_, p_466835_.map(p_467825_ -> Mth.square(p_467825_.longValue())));
      }

      public static MinMaxBounds.Ints exactly(int p_467679_) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.exactly(p_467679_));
      }

      public static MinMaxBounds.Ints between(int p_469238_, int p_468518_) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.between(p_469238_, p_468518_));
      }

      public static MinMaxBounds.Ints atLeast(int p_467761_) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.atLeast(p_467761_));
      }

      public static MinMaxBounds.Ints atMost(int p_469194_) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.atMost(p_469194_));
      }

      public boolean matches(int p_469313_) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > p_469313_ ? false : this.bounds.max.isEmpty() || this.bounds.max.get() >= p_469313_;
      }

      public boolean matchesSqr(long p_468469_) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > p_468469_
            ? false
            : this.boundsSqr.max.isEmpty() || this.boundsSqr.max.get() >= p_468469_;
      }

      public static MinMaxBounds.Ints fromReader(StringReader p_469940_) throws CommandSyntaxException {
         int i = p_469940_.getCursor();
         MinMaxBounds.Bounds<Integer> bounds = MinMaxBounds.Bounds.fromReader(
            p_469940_, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt
         );
         if (bounds.areSwapped()) {
            p_469940_.setCursor(i);
            throw ERROR_SWAPPED.createWithContext(p_469940_);
         } else {
            return new MinMaxBounds.Ints(bounds);
         }
      }
   }
}
