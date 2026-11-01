package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface StringRepresentable {
   int PRE_BUILT_MAP_THRESHOLD = 16;

   String getSerializedName();

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> p_216440_) {
      return fromEnumWithMapping(p_216440_, p_304817_ -> p_304817_);
   }

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(
      Supplier<E[]> p_275615_, Function<String, String> p_275259_
   ) {
      E[] ae = (E[])p_275615_.get();
      Function<String, E> function = createNameLookup(ae, p_448911_ -> p_275259_.apply(p_448911_.getSerializedName()));
      return new StringRepresentable.EnumCodec<>(ae, function);
   }

   static <T extends StringRepresentable> Codec<T> fromValues(Supplier<T[]> p_304543_) {
      T[] at = (T[])p_304543_.get();
      Function<String, T> function = createNameLookup(at);
      ToIntFunction<T> tointfunction = Util.createIndexLookup(Arrays.asList(at));
      return new StringRepresentable.StringRepresentableCodec<>(at, function, tointfunction);
   }

   static <T extends StringRepresentable> Function<String, T> createNameLookup(T[] p_304419_) {
      return createNameLookup(p_304419_, StringRepresentable::getSerializedName);
   }

   static <T> Function<String, T> createNameLookup(T[] p_449457_, Function<T, String> p_449955_) {
      if (p_449457_.length > 16) {
         Map<String, T> map = Arrays.<T>stream(p_449457_).collect(Collectors.toMap(p_449955_, p_449196_ -> (T)p_449196_));
         return map::get;
      } else {
         return p_448916_ -> {
            for (T t : p_449457_) {
               if (p_449955_.apply(t).equals(p_448916_)) {
                  return t;
               }
            }

            return null;
         };
      }
   }

   static Keyable keys(final StringRepresentable[] p_14358_) {
      return new Keyable() {
         public <T> Stream<T> keys(DynamicOps<T> p_184758_) {
            return Arrays.stream(p_14358_).map(StringRepresentable::getSerializedName).map(p_184758_::createString);
         }
      };
   }

   public static class EnumCodec<E extends Enum<E> & StringRepresentable> extends StringRepresentable.StringRepresentableCodec<E> {
      private final Function<String, E> resolver;

      public EnumCodec(E[] p_216447_, Function<String, E> p_216448_) {
         super(p_216447_, p_216448_, p_216454_ -> p_216454_.ordinal());
         this.resolver = p_216448_;
      }

      @Nullable
      public E byName(String p_216456_) {
         return this.resolver.apply(p_216456_);
      }

      public E byName(String p_263077_, E p_263115_) {
         return Objects.requireNonNullElse(this.byName(p_263077_), p_263115_);
      }

      public E byName(String p_380162_, Supplier<? extends E> p_380294_) {
         return Objects.requireNonNullElseGet(this.byName(p_380162_), p_380294_);
      }
   }

   public static class StringRepresentableCodec<S extends StringRepresentable> implements Codec<S> {
      private final Codec<S> codec;

      public StringRepresentableCodec(S[] p_304774_, Function<String, S> p_304834_, ToIntFunction<S> p_304925_) {
         this.codec = ExtraCodecs.orCompressed(
            Codec.stringResolver(StringRepresentable::getSerializedName, p_304834_),
            ExtraCodecs.idResolverCodec(p_304925_, p_304986_ -> p_304986_ >= 0 && p_304986_ < p_304774_.length ? p_304774_[p_304986_] : null, -1)
         );
      }

      public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> p_304586_, T p_304692_) {
         return this.codec.decode(p_304586_, p_304692_);
      }

      public <T> DataResult<T> encode(S p_304936_, DynamicOps<T> p_304952_, T p_304437_) {
         return this.codec.encode(p_304936_, p_304952_, p_304437_);
      }
   }
}
