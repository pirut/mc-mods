package net.minecraft.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public class ExtraCodecs {
   public static final Codec<JsonElement> JSON = converter(JsonOps.INSTANCE);
   public static final Codec<Object> JAVA = converter(JavaOps.INSTANCE);
   public static final Codec<Tag> NBT = converter(NbtOps.INSTANCE);
   public static final Codec<Vector2fc> VECTOR2F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         list -> Util.fixedSize(list, 2).map(listx -> new Vector2f((Float)listx.get(0), (Float)listx.get(1))),
         vector2fc -> List.of(vector2fc.x(), vector2fc.y())
      );
   public static final Codec<Vector3fc> VECTOR3F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         list -> Util.fixedSize(list, 3).map(listx -> new Vector3f((Float)listx.get(0), (Float)listx.get(1), (Float)listx.get(2))),
         vector3fc -> List.of(vector3fc.x(), vector3fc.y(), vector3fc.z())
      );
   public static final Codec<Vector3ic> VECTOR3I = Codec.INT
      .listOf()
      .comapFlatMap(
         list -> Util.fixedSize(list, 3).map(listx -> new Vector3i((Integer)listx.get(0), (Integer)listx.get(1), (Integer)listx.get(2))),
         vector3ic -> List.of(vector3ic.x(), vector3ic.y(), vector3ic.z())
      );
   public static final Codec<Vector4fc> VECTOR4F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         list -> Util.fixedSize(list, 4).map(listx -> new Vector4f((Float)listx.get(0), (Float)listx.get(1), (Float)listx.get(2), (Float)listx.get(3))),
         vector4fc -> List.of(vector4fc.x(), vector4fc.y(), vector4fc.z(), vector4fc.w())
      );
   public static final Codec<Quaternionfc> QUATERNIONF_COMPONENTS = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         list -> Util.fixedSize(list, 4)
            .map(listx -> new Quaternionf((Float)listx.get(0), (Float)listx.get(1), (Float)listx.get(2), (Float)listx.get(3)).normalize()),
         quaternionfc -> List.of(quaternionfc.x(), quaternionfc.y(), quaternionfc.z(), quaternionfc.w())
      );
   public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.FLOAT.fieldOf("angle").forGetter(axisAngle4f -> axisAngle4f.angle),
            VECTOR3F.fieldOf("axis").forGetter(axisAngle4f -> new Vector3f(axisAngle4f.x, axisAngle4f.y, axisAngle4f.z))
         )
         .apply(instance, AxisAngle4f::new)
   );
   public static final Codec<Quaternionfc> QUATERNIONF = Codec.withAlternative(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
   public static final Codec<Matrix4fc> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap(list -> Util.fixedSize(list, 16).map(listx -> {
      Matrix4f matrix4f = new Matrix4f();

      for (int i = 0; i < listx.size(); i++) {
         matrix4f.setRowColumn(i >> 2, i & 3, (Float)listx.get(i));
      }

      return matrix4f.determineProperties();
   }), matrix4fc -> {
      FloatList floatList = new FloatArrayList(16);

      for (int i = 0; i < 16; i++) {
         floatList.add(matrix4fc.getRowColumn(i >> 2, i & 3));
      }

      return floatList;
   });
   private static final String HEX_COLOR_PREFIX = "#";
   public static final Codec<Integer> RGB_COLOR_CODEC = Codec.withAlternative(
      Codec.INT, VECTOR3F, vector3fc -> ARGB.colorFromFloat(1.0F, vector3fc.x(), vector3fc.y(), vector3fc.z())
   );
   public static final Codec<Integer> ARGB_COLOR_CODEC = Codec.withAlternative(
      Codec.INT, VECTOR4F, vector4fc -> ARGB.colorFromFloat(vector4fc.w(), vector4fc.x(), vector4fc.y(), vector4fc.z())
   );
   public static final Codec<Integer> STRING_RGB_COLOR = Codec.withAlternative(hexColor(6).xmap(ARGB::opaque, ARGB::transparent), RGB_COLOR_CODEC);
   public static final Codec<Integer> STRING_ARGB_COLOR = Codec.withAlternative(hexColor(8), ARGB_COLOR_CODEC);
   public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE
      .flatComapMap(
         UnsignedBytes::toInt,
         integer -> integer > 255 ? DataResult.error(() -> "Unsigned byte was too large: " + integer + " > 255") : DataResult.success(integer.byteValue())
      );
   public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, integer -> "Value must be non-negative: " + integer);
   public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, integer -> "Value must be positive: " + integer);
   public static final Codec<Long> NON_NEGATIVE_LONG = longRangeWithMessage(0L, Long.MAX_VALUE, long_ -> "Value must be non-negative: " + long_);
   public static final Codec<Long> POSITIVE_LONG = longRangeWithMessage(1L, Long.MAX_VALUE, long_ -> "Value must be positive: " + long_);
   public static final Codec<Float> NON_NEGATIVE_FLOAT = floatRangeMinInclusiveWithMessage(
      0.0F, Float.MAX_VALUE, float_ -> "Value must be non-negative: " + float_
   );
   public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, float_ -> "Value must be positive: " + float_);
   public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(string -> {
      try {
         return DataResult.success(Pattern.compile(string));
      } catch (PatternSyntaxException var2) {
         return DataResult.error(() -> "Invalid regex pattern '" + string + "': " + var2.getMessage());
      }
   }, Pattern::pattern);
   public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
   public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(string -> {
      try {
         return DataResult.success(Base64.getDecoder().decode(string));
      } catch (IllegalArgumentException var2) {
         return DataResult.error(() -> "Malformed base64 string");
      }
   }, bs -> Base64.getEncoder().encodeToString(bs));
   public static final Codec<String> ESCAPED_STRING = Codec.STRING
      .comapFlatMap(string -> DataResult.success(StringEscapeUtils.unescapeJava(string)), StringEscapeUtils::escapeJava);
   public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING
      .comapFlatMap(
         string -> string.startsWith("#")
            ? Identifier.read(string.substring(1)).map(identifier -> new ExtraCodecs.TagOrElementLocation(identifier, true))
            : Identifier.read(string).map(identifier -> new ExtraCodecs.TagOrElementLocation(identifier, false)),
         ExtraCodecs.TagOrElementLocation::decoratedId
      );
   public static final Function<Optional<Long>, OptionalLong> toOptionalLong = optional -> optional.map(OptionalLong::of).orElseGet(OptionalLong::empty);
   public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = optionalLong -> optionalLong.isPresent()
      ? Optional.of(optionalLong.getAsLong())
      : Optional.empty();
   public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
      .xmap(longStream -> BitSet.valueOf(longStream.toArray()), bitSet -> Arrays.stream(bitSet.toLongArray()));
   public static final int MAX_PROPERTY_NAME_LENGTH = 64;
   public static final int MAX_PROPERTY_VALUE_LENGTH = 32767;
   public static final int MAX_PROPERTY_SIGNATURE_LENGTH = 1024;
   public static final int MAX_PROPERTIES = 16;
   private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.sizeLimitedString(64).fieldOf("name").forGetter(Property::name),
            Codec.sizeLimitedString(32767).fieldOf("value").forGetter(Property::value),
            Codec.sizeLimitedString(1024).optionalFieldOf("signature").forGetter(property -> Optional.ofNullable(property.signature()))
         )
         .apply(instance, (string, string2, optional) -> new Property(string, string2, (String)optional.orElse(null)))
   );
   public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(
         Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
            .validate(map -> map.size() > 16 ? DataResult.error(() -> "Cannot have more than 16 properties, but was " + map.size()) : DataResult.success(map)),
         PROPERTY.sizeLimitedListOf(16)
      )
      .xmap(either -> {
         Builder<String, Property> builder = ImmutableMultimap.builder();
         either.ifLeft(map -> map.forEach((string, list) -> {
            for (String string2 : list) {
               builder.put(string, new Property(string, string2));
            }
         })).ifRight(list -> {
            for (Property property : list) {
               builder.put(property.name(), property);
            }
         });
         return new PropertyMap(builder.build());
      }, propertyMap -> Either.right(propertyMap.values().stream().toList()));
   public static final Codec<String> PLAYER_NAME = Codec.string(0, 16)
      .validate(
         string -> StringUtil.isValidPlayerName(string)
            ? DataResult.success(string)
            : DataResult.error(() -> "Player name contained disallowed characters: '" + string + "'")
      );
   public static final Codec<GameProfile> AUTHLIB_GAME_PROFILE = gameProfileCodec(UUIDUtil.AUTHLIB_CODEC).codec();
   public static final MapCodec<GameProfile> STORED_GAME_PROFILE = gameProfileCodec(UUIDUtil.CODEC);
   public static final Codec<String> NON_EMPTY_STRING = Codec.STRING
      .validate(string -> string.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(string));
   public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(string -> {
      int[] is = string.codePoints().toArray();
      return is.length != 1 ? DataResult.error(() -> "Expected one codepoint, got: " + string) : DataResult.success(is[0]);
   }, Character::toString);
   public static final Codec<String> RESOURCE_PATH_CODEC = Codec.STRING
      .validate(
         string -> !Identifier.isValidPath(string)
            ? DataResult.error(() -> "Invalid string to use as a resource path element: " + string)
            : DataResult.success(string)
      );
   public static final Codec<URI> UNTRUSTED_URI = Codec.STRING.comapFlatMap(string -> {
      try {
         return DataResult.success(Util.parseAndValidateUntrustedUri(string));
      } catch (URISyntaxException var2) {
         return DataResult.error(var2::getMessage);
      }
   }, URI::toString);
   public static final Codec<String> CHAT_STRING = Codec.STRING.validate(string -> {
      for (int i = 0; i < string.length(); i++) {
         char c = string.charAt(i);
         if (!StringUtil.isAllowedChatCharacter(c)) {
            return DataResult.error(() -> "Disallowed chat character: '" + c + "'");
         }
      }

      return DataResult.success(string);
   });

   public static <T> Codec<T> converter(DynamicOps<T> ops) {
      return Codec.PASSTHROUGH.xmap(dynamic -> dynamic.convert(ops).getValue(), object -> new Dynamic(ops, object));
   }

   private static Codec<Integer> hexColor(int length) {
      long l = (1L << length * 4) - 1L;
      return Codec.STRING.comapFlatMap(string -> {
         if (!string.startsWith("#")) {
            return DataResult.error(() -> "Hex color must begin with #");
         } else {
            int j = string.length() - "#".length();
            if (j != length) {
               return DataResult.error(() -> "Hex color is wrong size, expected " + length + " digits but got " + j);
            } else {
               try {
                  long m = HexFormat.fromHexDigitsToLong(string, "#".length(), string.length());
                  return m >= 0L && m <= l ? DataResult.success((int)m) : DataResult.error(() -> "Color value out of range: " + string);
               } catch (NumberFormatException var7) {
                  return DataResult.error(() -> "Invalid color value: " + string);
               }
            }
         }
      }, integer -> "#" + HexFormat.of().toHexDigits(integer.intValue(), length));
   }

   public static <P, I> Codec<I> intervalCodec(
      Codec<P> codec, String minFieldName, String maxFieldName, BiFunction<P, P, DataResult<I>> factory, Function<I, P> minGetter, Function<I, P> maxGetter
   ) {
      Codec<I> codec2 = Codec.list(codec).comapFlatMap(list -> Util.fixedSize(list, 2).flatMap(listx -> {
         P object = (P)listx.get(0);
         P object2 = (P)listx.get(1);
         return factory.apply(object, object2);
      }), object -> ImmutableList.of(minGetter.apply((I)object), maxGetter.apply((I)object)));
      Codec<I> codec3 = RecordCodecBuilder.create(
            instance -> instance.group(codec.fieldOf(minFieldName).forGetter(Pair::getFirst), codec.fieldOf(maxFieldName).forGetter(Pair::getSecond))
               .apply(instance, Pair::of)
         )
         .comapFlatMap(
            pair -> factory.apply((P)pair.getFirst(), (P)pair.getSecond()), object -> Pair.of(minGetter.apply((I)object), maxGetter.apply((I)object))
         );
      Codec<I> codec4 = Codec.withAlternative(codec2, codec3);
      return Codec.either(codec, codec4)
         .comapFlatMap(either -> (DataResult)either.map(object -> factory.apply((P)object, (P)object), DataResult::success), object -> {
            P object2 = minGetter.apply((I)object);
            P object3 = maxGetter.apply((I)object);
            return Objects.equals(object2, object3) ? Either.left(object2) : Either.right(object);
         });
   }

   public static <A> ResultFunction<A> orElsePartial(A value) {
      return new ResultFunction<A>() {
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> dynamicOps, T object, DataResult<Pair<A, T>> dataResult) {
            MutableObject<String> mutableObject = new MutableObject();
            Optional<Pair<A, T>> optional = dataResult.resultOrPartial(mutableObject::setValue);
            return optional.isPresent() ? dataResult : DataResult.error(() -> "(" + (String)mutableObject.get() + " -> using default)", Pair.of(value, object));
         }

         public <T> DataResult<T> coApply(DynamicOps<T> dynamicOps, A object, DataResult<T> dataResult) {
            return dataResult;
         }

         @Override
         public String toString() {
            return "OrElsePartial[" + value + "]";
         }
      };
   }

   public static <E> Codec<E> idResolverCodec(ToIntFunction<E> encoder, IntFunction<E> decoder, int notFoundValue) {
      return Codec.INT
         .flatXmap(
            integer -> Optional.ofNullable(decoder.apply(integer))
               .<DataResult>map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown element id: " + integer)),
            object -> {
               int j = encoder.applyAsInt((E)object);
               return j == notFoundValue ? DataResult.error(() -> "Element with unknown id: " + object) : DataResult.success(j);
            }
         );
   }

   public static <I, E> Codec<E> idResolverCodec(Codec<I> idCodec, Function<I, E> idToValue, Function<E, I> valueToId) {
      return idCodec.flatXmap(object -> {
         E object2 = idToValue.apply((I)object);
         return object2 == null ? DataResult.error(() -> "Unknown element id: " + object) : DataResult.success(object2);
      }, object -> {
         I object2 = valueToId.apply((E)object);
         return object2 == null ? DataResult.error(() -> "Element with unknown id: " + object) : DataResult.success(object2);
      });
   }

   public static <E> Codec<E> orCompressed(Codec<E> first, Codec<E> second) {
      return new Codec<E>() {
         public <T> DataResult<T> encode(E object, DynamicOps<T> dynamicOps, T object2) {
            return dynamicOps.compressMaps() ? second.encode(object, dynamicOps, object2) : first.encode(object, dynamicOps, object2);
         }

         public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> dynamicOps, T object) {
            return dynamicOps.compressMaps() ? second.decode(dynamicOps, object) : first.decode(dynamicOps, object);
         }

         @Override
         public String toString() {
            return first + " orCompressed " + second;
         }
      };
   }

   public static <E> MapCodec<E> orCompressed(MapCodec<E> first, MapCodec<E> second) {
      return new MapCodec<E>() {
         public <T> RecordBuilder<T> encode(E object, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
            return dynamicOps.compressMaps() ? second.encode(object, dynamicOps, recordBuilder) : first.encode(object, dynamicOps, recordBuilder);
         }

         public <T> DataResult<E> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
            return dynamicOps.compressMaps() ? second.decode(dynamicOps, mapLike) : first.decode(dynamicOps, mapLike);
         }

         public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
            return second.keys(dynamicOps);
         }

         public String toString() {
            return first + " orCompressed " + second;
         }
      };
   }

   public static <E> Codec<E> overrideLifecycle(Codec<E> codec, Function<E, Lifecycle> applyLifecycle, Function<E, Lifecycle> coApplyLifecycle) {
      return codec.mapResult(new ResultFunction<E>() {
         public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> dynamicOps, T object, DataResult<Pair<E, T>> dataResult) {
            return dataResult.result().map(pair -> dataResult.setLifecycle(applyLifecycle.apply((E)pair.getFirst()))).orElse(dataResult);
         }

         public <T> DataResult<T> coApply(DynamicOps<T> dynamicOps, E object, DataResult<T> dataResult) {
            return dataResult.setLifecycle(coApplyLifecycle.apply(object));
         }

         @Override
         public String toString() {
            return "WithLifecycle[" + applyLifecycle + " " + coApplyLifecycle + "]";
         }
      });
   }

   public static <E> Codec<E> overrideLifecycle(Codec<E> codec, Function<E, Lifecycle> lifecycleGetter) {
      return overrideLifecycle(codec, lifecycleGetter, lifecycleGetter);
   }

   public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> key, Codec<V> value) {
      return new ExtraCodecs.StrictUnboundedMapCodec<>(key, value);
   }

   public static <E> Codec<List<E>> compactListCodec(Codec<E> elementCodec) {
      return compactListCodec(elementCodec, elementCodec.listOf());
   }

   public static <E> Codec<List<E>> compactListCodec(Codec<E> elementCodec, Codec<List<E>> listCodec) {
      return Codec.either(listCodec, elementCodec)
         .xmap(either -> (List)either.map(list -> list, List::of), list -> list.size() == 1 ? Either.right(list.getFirst()) : Either.left(list));
   }

   private static Codec<Integer> intRangeWithMessage(int min, int max, Function<Integer, String> errorMessage) {
      return Codec.INT
         .validate(
            integer -> integer.compareTo(min) >= 0 && integer.compareTo(max) <= 0
               ? DataResult.success(integer)
               : DataResult.error(() -> errorMessage.apply(integer))
         );
   }

   public static Codec<Integer> intRange(int min, int max) {
      return intRangeWithMessage(min, max, integer -> "Value must be within range [" + min + ";" + max + "]: " + integer);
   }

   private static Codec<Long> longRangeWithMessage(long min, long max, Function<Long, String> messageGetter) {
      return Codec.LONG
         .validate(
            long_ -> long_.compareTo(min) >= 0L && long_.compareTo(max) <= 0L ? DataResult.success(long_) : DataResult.error(() -> messageGetter.apply(long_))
         );
   }

   public static Codec<Long> longRange(int min, int max) {
      return longRangeWithMessage(min, max, long_ -> "Value must be within range [" + min + ";" + max + "]: " + long_);
   }

   private static Codec<Float> floatRangeMinInclusiveWithMessage(float min, float max, Function<Float, String> errorMessage) {
      return Codec.FLOAT
         .validate(
            float_ -> float_.compareTo(min) >= 0 && float_.compareTo(max) <= 0
               ? DataResult.success(float_)
               : DataResult.error(() -> errorMessage.apply(float_))
         );
   }

   private static Codec<Float> floatRangeMinExclusiveWithMessage(float min, float max, Function<Float, String> errorMessage) {
      return Codec.FLOAT
         .validate(
            float_ -> float_.compareTo(min) > 0 && float_.compareTo(max) <= 0 ? DataResult.success(float_) : DataResult.error(() -> errorMessage.apply(float_))
         );
   }

   public static Codec<Float> floatRange(float min, float max) {
      return floatRangeMinInclusiveWithMessage(min, max, float_ -> "Value must be within range [" + min + ";" + max + "]: " + float_);
   }

   public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> codec) {
      return codec.validate(list -> list.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(list));
   }

   public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> codec) {
      return codec.validate(
         holderSet -> holderSet.unwrap().right().filter(List::isEmpty).isPresent()
            ? DataResult.error(() -> "List must have contents")
            : DataResult.success(holderSet)
      );
   }

   public static <M extends Map<?, ?>> Codec<M> nonEmptyMap(Codec<M> mapCodec) {
      return mapCodec.validate(map -> map.isEmpty() ? DataResult.error(() -> "Map must have contents") : DataResult.success(map));
   }

   public static <E> MapCodec<E> retrieveContext(Function<DynamicOps<?>, DataResult<E>> retriever) {
      class ContextRetrievalCodec extends MapCodec<E> {
         public <T> RecordBuilder<T> encode(E object, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
            return recordBuilder;
         }

         public <T> DataResult<E> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
            return retriever.apply(dynamicOps);
         }

         public String toString() {
            return "ContextRetrievalCodec[" + retriever + "]";
         }

         public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
            return Stream.empty();
         }
      }

      return new ContextRetrievalCodec();
   }

   public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> typeGetter) {
      return collection -> {
         Iterator<E> iterator = collection.iterator();
         if (iterator.hasNext()) {
            T object = typeGetter.apply(iterator.next());

            while (iterator.hasNext()) {
               E object2 = iterator.next();
               T object3 = typeGetter.apply(object2);
               if (object3 != object) {
                  return DataResult.error(() -> "Mixed type list: element " + object2 + " had type " + object3 + ", but list is of type " + object);
               }
            }
         }

         return DataResult.success(collection, Lifecycle.stable());
      };
   }

   public static <A> Codec<A> catchDecoderException(Codec<A> codec) {
      return Codec.of(codec, new Decoder<A>() {
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> dynamicOps, T object) {
            try {
               return codec.decode(dynamicOps, object);
            } catch (Exception var4) {
               return DataResult.error(() -> "Caught exception decoding " + object + ": " + var4.getMessage());
            }
         }
      });
   }

   public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter dateTimeFormatter) {
      return Codec.STRING.comapFlatMap(string -> {
         try {
            return DataResult.success(dateTimeFormatter.parse(string));
         } catch (Exception var3) {
            return DataResult.error(var3::getMessage);
         }
      }, dateTimeFormatter::format);
   }

   public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> codec) {
      return codec.xmap(toOptionalLong, fromOptionalLong);
   }

   private static MapCodec<GameProfile> gameProfileCodec(Codec<UUID> uuidCodec) {
      return RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               uuidCodec.fieldOf("id").forGetter(GameProfile::id),
               PLAYER_NAME.fieldOf("name").forGetter(GameProfile::name),
               PROPERTY_MAP.optionalFieldOf("properties", PropertyMap.EMPTY).forGetter(GameProfile::properties)
            )
            .apply(instance, GameProfile::new)
      );
   }

   public static <K, V> Codec<Map<K, V>> sizeLimitedMap(Codec<Map<K, V>> mapCodec, int maxSize) {
      return mapCodec.validate(
         map -> map.size() > maxSize
            ? DataResult.error(() -> "Map is too long: " + map.size() + ", expected range [0-" + maxSize + "]")
            : DataResult.success(map)
      );
   }

   public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> codec) {
      return Codec.unboundedMap(codec, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
   }

   @Deprecated
   public static <K, V> MapCodec<V> dispatchOptionalValue(
      String key1, String key2, Codec<K> codec, Function<? super V, ? extends K> keyGetter, Function<? super K, ? extends Codec<? extends V>> codecGetter
   ) {
      return new MapCodec<V>() {
         public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
            return Stream.of((T[])(new Object[]{dynamicOps.createString(key1), dynamicOps.createString(key2)}));
         }

         public <T> DataResult<V> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
            T object = (T)mapLike.get(key1);
            return object == null ? DataResult.error(() -> "Missing \"" + key1 + "\" in: " + mapLike) : codec.decode(dynamicOps, object).flatMap(pair -> {
               T objectx = Objects.requireNonNullElseGet((T)mapLike.get(key2), dynamicOps::emptyMap);
               return codecGetter.apply((K)pair.getFirst()).decode(dynamicOps, objectx).map(Pair::getFirst);
            });
         }

         public <T> RecordBuilder<T> encode(V object, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
            K object2 = (K)keyGetter.apply(object);
            recordBuilder.add(key1, codec.encodeStart(dynamicOps, object2));
            DataResult<T> dataResult = this.encode(codecGetter.apply(object2), object, dynamicOps);
            if (dataResult.result().isEmpty() || !Objects.equals(dataResult.result().get(), dynamicOps.emptyMap())) {
               recordBuilder.add(key2, dataResult);
            }

            return recordBuilder;
         }

         private <T, V2 extends V> DataResult<T> encode(Codec<V2> valueCodec, V value, DynamicOps<T> ops) {
            return valueCodec.encodeStart(ops, value);
         }
      };
   }

   public static <A> Codec<Optional<A>> optionalEmptyMap(Codec<A> codec) {
      return new Codec<Optional<A>>() {
         public <T> DataResult<Pair<Optional<A>, T>> decode(DynamicOps<T> dynamicOps, T object) {
            return isEmptyMap(dynamicOps, object)
               ? DataResult.success(Pair.of(Optional.empty(), object))
               : codec.decode(dynamicOps, object).map(pair -> pair.mapFirst(Optional::of));
         }

         private static <T> boolean isEmptyMap(DynamicOps<T> ops, T value) {
            Optional<MapLike<T>> optional = ops.getMap(value).result();
            return optional.isPresent() && optional.get().entries().findAny().isEmpty();
         }

         public <T> DataResult<T> encode(Optional<A> input, DynamicOps<T> ops, T value) {
            return input.isEmpty() ? DataResult.success(ops.emptyMap()) : codec.encode(input.get(), ops, value);
         }
      };
   }

   @Deprecated
   public static <E extends Enum<E>> Codec<E> legacyEnum(Function<String, E> fromString) {
      return Codec.STRING.comapFlatMap(string -> {
         try {
            return DataResult.success(fromString.apply(string));
         } catch (IllegalArgumentException var3) {
            return DataResult.error(() -> "No value with id: " + string);
         }
      }, Enum::toString);
   }

   public static class LateBoundIdMapper<I, V> {
      private final BiMap<I, V> idToValue = HashBiMap.create();

      public Codec<V> codec(Codec<I> idCodec) {
         BiMap<V, I> biMap = this.idToValue.inverse();
         return ExtraCodecs.idResolverCodec(idCodec, this.idToValue::get, biMap::get);
      }

      public ExtraCodecs.LateBoundIdMapper<I, V> put(I id, V value) {
         Objects.requireNonNull(value, () -> "Value for " + id + " is null");
         this.idToValue.put(id, value);
         return this;
      }

      public Set<V> values() {
         return Collections.unmodifiableSet(this.idToValue.values());
      }
   }

   public record StrictUnboundedMapCodec<K, V>(Codec<K> a, Codec<V> b) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
      public <T> DataResult<Map<K, V>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
         com.google.common.collect.ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

         for (Pair<T, T> pair : mapLike.entries().toList()) {
            DataResult<K> dataResult = this.keyCodec().parse(dynamicOps, pair.getFirst());
            DataResult<V> dataResult2 = this.elementCodec().parse(dynamicOps, pair.getSecond());
            DataResult<Pair<K, V>> dataResult3 = dataResult.apply2stable(Pair::of, dataResult2);
            Optional<Error<Pair<K, V>>> optional = dataResult3.error();
            if (optional.isPresent()) {
               String string = optional.get().message();
               return DataResult.error(() -> dataResult.result().isPresent() ? "Map entry '" + dataResult.result().get() + "' : " + string : string);
            }

            if (!dataResult3.result().isPresent()) {
               return DataResult.error(() -> "Empty or invalid map contents are not allowed");
            }

            Pair<K, V> pair2 = (Pair<K, V>)dataResult3.result().get();
            builder.put(pair2.getFirst(), pair2.getSecond());
         }

         Map<K, V> map = builder.build();
         return DataResult.success(map);
      }

      public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> dynamicOps, T object) {
         return dynamicOps.getMap(object)
            .setLifecycle(Lifecycle.stable())
            .flatMap(mapLike -> this.decode(dynamicOps, mapLike))
            .map(map -> Pair.of(map, object));
      }

      public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T value) {
         return this.encode(input, ops, ops.mapBuilder()).build(value);
      }

      @Override
      public String toString() {
         return "StrictUnboundedMapCodec[" + this.a + " -> " + this.b + "]";
      }

      public Codec<K> keyCodec() {
         return this.a;
      }

      public Codec<V> elementCodec() {
         return this.b;
      }
   }

   public record TagOrElementLocation(Identifier id, boolean tag) {
      @Override
      public String toString() {
         return this.decoratedId();
      }

      private String decoratedId() {
         return this.tag ? "#" + this.id : this.id.toString();
      }
   }
}
