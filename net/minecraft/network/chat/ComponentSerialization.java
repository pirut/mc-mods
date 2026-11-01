package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;

public class ComponentSerialization {
   public static final Codec<Component> CODEC = Codec.recursive("Component", ComponentSerialization::createCodec);
   public static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);
   public static final StreamCodec<RegistryFriendlyByteBuf, Component> TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> TRUSTED_OPTIONAL_STREAM_CODEC = TRUSTED_STREAM_CODEC.apply(
      ByteBufCodecs::optional
   );
   public static final StreamCodec<ByteBuf, Component> TRUSTED_CONTEXT_FREE_STREAM_CODEC = ByteBufCodecs.fromCodecTrusted(CODEC);

   public static Codec<Component> flatRestrictedCodec(int maxSize) {
      return new Codec<Component>() {
         public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> dynamicOps, T object) {
            return ComponentSerialization.CODEC
               .decode(dynamicOps, object)
               .flatMap(
                  pair -> this.isTooLarge(dynamicOps, (Component)pair.getFirst())
                     ? DataResult.error(() -> "Component was too large: greater than max size " + maxSize)
                     : DataResult.success(pair)
               );
         }

         public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T value) {
            return ComponentSerialization.CODEC.encodeStart(ops, input);
         }

         private <T> boolean isTooLarge(DynamicOps<T> ops, Component component) {
            DataResult<JsonElement> dataResult = ComponentSerialization.CODEC.encodeStart(asJsonOps(ops), component);
            return dataResult.isSuccess() && GsonHelper.encodesLongerThan((JsonElement)dataResult.getOrThrow(), maxSize);
         }

         private static <T> DynamicOps<JsonElement> asJsonOps(DynamicOps<T> ops) {
            return (DynamicOps<JsonElement>)(ops instanceof RegistryOps<T> registryOps ? registryOps.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE);
         }
      };
   }

   private static MutableComponent createFromList(List<Component> components) {
      MutableComponent mutableComponent = components.get(0).copy();

      for (int i = 1; i < components.size(); i++) {
         mutableComponent.append(components.get(i));
      }

      return mutableComponent;
   }

   public static <T> MapCodec<T> createLegacyComponentMatcher(
      ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends T>> mapper, Function<T, MapCodec<? extends T>> encoderGetter, String fieldName
   ) {
      MapCodec<T> mapCodec = new ComponentSerialization.FuzzyCodec<>(mapper.values(), encoderGetter);
      MapCodec<T> mapCodec2 = mapper.codec(Codec.STRING).dispatchMap(fieldName, encoderGetter, mapCodecx -> mapCodecx);
      MapCodec<T> mapCodec3 = new ComponentSerialization.StrictEither<>(fieldName, mapCodec2, mapCodec);
      return ExtraCodecs.orCompressed(mapCodec3, mapCodec2);
   }

   private static Codec<Component> createCodec(Codec<Component> codec) {
      ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ComponentContents>> lateBoundIdMapper = new ExtraCodecs.LateBoundIdMapper<>();
      bootstrap(lateBoundIdMapper);
      MapCodec<ComponentContents> mapCodec = createLegacyComponentMatcher(lateBoundIdMapper, ComponentContents::codec, "type");
      Codec<Component> codec2 = RecordCodecBuilder.create(
         instance -> instance.group(
               mapCodec.forGetter(Component::getContents),
               ExtraCodecs.nonEmptyList(codec.listOf()).optionalFieldOf("extra", List.of()).forGetter(Component::getSiblings),
               Style.Serializer.MAP_CODEC.forGetter(Component::getStyle)
            )
            .apply(instance, MutableComponent::new)
      );
      return Codec.either(Codec.either(Codec.STRING, ExtraCodecs.nonEmptyList(codec.listOf())), codec2)
         .xmap(
            either -> (Component)either.map(
               eitherx -> (Component)eitherx.map(Component::literal, ComponentSerialization::createFromList), component -> component
            ),
            component -> {
               String string = component.tryCollapseToString();
               return string != null ? Either.left(Either.left(string)) : Either.right(component);
            }
         );
   }

   private static void bootstrap(ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ComponentContents>> mapper) {
      mapper.put("text", PlainTextContents.MAP_CODEC);
      mapper.put("translatable", TranslatableContents.MAP_CODEC);
      mapper.put("keybind", KeybindContents.MAP_CODEC);
      mapper.put("score", ScoreContents.MAP_CODEC);
      mapper.put("selector", SelectorContents.MAP_CODEC);
      mapper.put("nbt", NbtContents.MAP_CODEC);
      mapper.put("object", ObjectContents.MAP_CODEC);
   }

   static class FuzzyCodec<T> extends MapCodec<T> {
      private final Collection<MapCodec<? extends T>> codecs;
      private final Function<T, ? extends MapEncoder<? extends T>> encoderGetter;

      public FuzzyCodec(Collection<MapCodec<? extends T>> codecs, Function<T, ? extends MapEncoder<? extends T>> encoderGetter) {
         this.codecs = codecs;
         this.encoderGetter = encoderGetter;
      }

      public <S> DataResult<T> decode(DynamicOps<S> dynamicOps, MapLike<S> mapLike) {
         for (MapDecoder<? extends T> mapDecoder : this.codecs) {
            DataResult<? extends T> dataResult = mapDecoder.decode(dynamicOps, mapLike);
            if (dataResult.result().isPresent()) {
               return (DataResult<T>)dataResult;
            }
         }

         return DataResult.error(() -> "No matching codec found");
      }

      public <S> RecordBuilder<S> encode(T object, DynamicOps<S> dynamicOps, RecordBuilder<S> recordBuilder) {
         MapEncoder<T> mapEncoder = (MapEncoder<T>)this.encoderGetter.apply(object);
         return mapEncoder.encode(object, dynamicOps, recordBuilder);
      }

      public <S> Stream<S> keys(DynamicOps<S> dynamicOps) {
         return this.codecs.stream().flatMap(mapCodec -> mapCodec.keys(dynamicOps)).distinct();
      }

      public String toString() {
         return "FuzzyCodec[" + this.codecs + "]";
      }
   }

   static class StrictEither<T> extends MapCodec<T> {
      private final String typeFieldName;
      private final MapCodec<T> typed;
      private final MapCodec<T> fuzzy;

      public StrictEither(String typeFieldName, MapCodec<T> typed, MapCodec<T> fuzzy) {
         this.typeFieldName = typeFieldName;
         this.typed = typed;
         this.fuzzy = fuzzy;
      }

      public <O> DataResult<T> decode(DynamicOps<O> dynamicOps, MapLike<O> mapLike) {
         return mapLike.get(this.typeFieldName) != null ? this.typed.decode(dynamicOps, mapLike) : this.fuzzy.decode(dynamicOps, mapLike);
      }

      public <O> RecordBuilder<O> encode(T object, DynamicOps<O> dynamicOps, RecordBuilder<O> recordBuilder) {
         return this.fuzzy.encode(object, dynamicOps, recordBuilder);
      }

      public <T1> Stream<T1> keys(DynamicOps<T1> dynamicOps) {
         return Stream.concat(this.typed.keys(dynamicOps), this.fuzzy.keys(dynamicOps)).distinct();
      }
   }
}
