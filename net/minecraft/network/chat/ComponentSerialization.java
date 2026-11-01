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
import net.neoforged.neoforge.common.util.InsertingContents;

public class ComponentSerialization {
   public static final Codec<Component> CODEC = Codec.recursive("Component", ComponentSerialization::createCodec);
   public static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);
   public static final StreamCodec<RegistryFriendlyByteBuf, Component> TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> TRUSTED_OPTIONAL_STREAM_CODEC = TRUSTED_STREAM_CODEC.apply(
      ByteBufCodecs::optional
   );
   public static final StreamCodec<ByteBuf, Component> TRUSTED_CONTEXT_FREE_STREAM_CODEC = ByteBufCodecs.fromCodecTrusted(CODEC);

   public static Codec<Component> flatRestrictedCodec(final int p_394111_) {
      return new Codec<Component>() {
         public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> p_330684_, T p_331653_) {
            return ComponentSerialization.CODEC
               .decode(p_330684_, p_331653_)
               .flatMap(
                  p_392612_ -> this.isTooLarge(p_330684_, (Component)p_392612_.getFirst())
                     ? DataResult.error(() -> "Component was too large: greater than max size " + p_394111_)
                     : DataResult.success(p_392612_)
               );
         }

         public <T> DataResult<T> encode(Component p_330452_, DynamicOps<T> p_330865_, T p_331460_) {
            return ComponentSerialization.CODEC.encodeStart(p_330865_, p_330452_);
         }

         private <T> boolean isTooLarge(DynamicOps<T> p_393757_, Component p_393963_) {
            DataResult<JsonElement> dataresult = ComponentSerialization.CODEC.encodeStart(asJsonOps(p_393757_), p_393963_);
            return dataresult.isSuccess() && GsonHelper.encodesLongerThan((JsonElement)dataresult.getOrThrow(), p_394111_);
         }

         private static <T> DynamicOps<JsonElement> asJsonOps(DynamicOps<T> p_330749_) {
            return (DynamicOps<JsonElement>)(p_330749_ instanceof RegistryOps<T> registryops ? registryops.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE);
         }
      };
   }

   private static MutableComponent createFromList(List<Component> p_304405_) {
      MutableComponent mutablecomponent = p_304405_.get(0).copy();

      for (int i = 1; i < p_304405_.size(); i++) {
         mutablecomponent.append(p_304405_.get(i));
      }

      return mutablecomponent;
   }

   public static <T> MapCodec<T> createLegacyComponentMatcher(
      ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends T>> p_443304_, Function<T, MapCodec<? extends T>> p_304852_, String p_307551_
   ) {
      MapCodec<T> mapcodec = new ComponentSerialization.FuzzyCodec<>(p_443304_.values(), p_304852_);
      MapCodec<T> mapcodec1 = p_443304_.codec(Codec.STRING).dispatchMap(p_307551_, p_304852_, p_443511_ -> p_443511_);
      MapCodec<T> mapcodec2 = new ComponentSerialization.StrictEither<>(p_307551_, mapcodec1, mapcodec);
      return ExtraCodecs.orCompressed(mapcodec2, mapcodec1);
   }

   private static Codec<Component> createCodec(Codec<Component> p_304554_) {
      ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ComponentContents>> lateboundidmapper = new ExtraCodecs.LateBoundIdMapper<>();
      bootstrap(lateboundidmapper);
      MapCodec<ComponentContents> mapcodec = createLegacyComponentMatcher(lateboundidmapper, ComponentContents::codec, "type");
      Codec<Component> codec = RecordCodecBuilder.create(
         p_337494_ -> p_337494_.group(
               mapcodec.forGetter(Component::getContents),
               ExtraCodecs.nonEmptyList(p_304554_.listOf()).optionalFieldOf("extra", List.of()).forGetter(Component::getSiblings),
               Style.Serializer.MAP_CODEC.forGetter(Component::getStyle)
            )
            .apply(p_337494_, MutableComponent::new)
      );
      return Codec.either(Codec.either(Codec.STRING, ExtraCodecs.nonEmptyList(p_304554_.listOf())), codec)
         .xmap(
            p_304547_ -> (Component)p_304547_.map(
               p_304568_ -> (Component)p_304568_.map(Component::literal, ComponentSerialization::createFromList), p_304887_ -> p_304887_
            ),
            p_304501_ -> {
               String s = p_304501_.tryCollapseToString();
               return s != null ? Either.left(Either.left(s)) : Either.right(p_304501_);
            }
         );
   }

   private static void bootstrap(ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ComponentContents>> p_442635_) {
      p_442635_.put("text", PlainTextContents.MAP_CODEC);
      p_442635_.put("translatable", TranslatableContents.MAP_CODEC);
      p_442635_.put("keybind", KeybindContents.MAP_CODEC);
      p_442635_.put("score", ScoreContents.MAP_CODEC);
      p_442635_.put("selector", SelectorContents.MAP_CODEC);
      p_442635_.put("nbt", NbtContents.MAP_CODEC);
      p_442635_.put("object", ObjectContents.MAP_CODEC);
      p_442635_.put("neoforge:inserting", InsertingContents.MAP_CODEC);
   }

   static class FuzzyCodec<T> extends MapCodec<T> {
      private final Collection<MapCodec<? extends T>> codecs;
      private final Function<T, ? extends MapEncoder<? extends T>> encoderGetter;

      public FuzzyCodec(Collection<MapCodec<? extends T>> p_443376_, Function<T, ? extends MapEncoder<? extends T>> p_304594_) {
         this.codecs = p_443376_;
         this.encoderGetter = p_304594_;
      }

      public <S> DataResult<T> decode(DynamicOps<S> p_304922_, MapLike<S> p_304447_) {
         for (MapDecoder<? extends T> mapdecoder : this.codecs) {
            DataResult<? extends T> dataresult = mapdecoder.decode(p_304922_, p_304447_);
            if (dataresult.result().isPresent()) {
               return (DataResult<T>)dataresult;
            }
         }

         return DataResult.error(() -> "No matching codec found");
      }

      public <S> RecordBuilder<S> encode(T p_304409_, DynamicOps<S> p_304945_, RecordBuilder<S> p_304823_) {
         MapEncoder<T> mapencoder = (MapEncoder<T>)this.encoderGetter.apply(p_304409_);
         return mapencoder.encode(p_304409_, p_304945_, p_304823_);
      }

      public <S> Stream<S> keys(DynamicOps<S> p_304770_) {
         return this.codecs.stream().flatMap(p_304401_ -> p_304401_.keys(p_304770_)).distinct();
      }

      public String toString() {
         return "FuzzyCodec[" + this.codecs + "]";
      }
   }

   static class StrictEither<T> extends MapCodec<T> {
      private final String typeFieldName;
      private final MapCodec<T> typed;
      private final MapCodec<T> fuzzy;

      public StrictEither(String p_307439_, MapCodec<T> p_307644_, MapCodec<T> p_307415_) {
         this.typeFieldName = p_307439_;
         this.typed = p_307644_;
         this.fuzzy = p_307415_;
      }

      public <O> DataResult<T> decode(DynamicOps<O> p_307279_, MapLike<O> p_307655_) {
         return p_307655_.get(this.typeFieldName) != null ? this.typed.decode(p_307279_, p_307655_) : this.fuzzy.decode(p_307279_, p_307655_);
      }

      public <O> RecordBuilder<O> encode(T p_307352_, DynamicOps<O> p_307510_, RecordBuilder<O> p_307297_) {
         return this.fuzzy.encode(p_307352_, p_307510_, p_307297_);
      }

      public <T1> Stream<T1> keys(DynamicOps<T1> p_307616_) {
         return Stream.concat(this.typed.keys(p_307616_), this.fuzzy.keys(p_307616_)).distinct();
      }
   }
}
