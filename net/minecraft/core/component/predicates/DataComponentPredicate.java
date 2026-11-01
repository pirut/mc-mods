package net.minecraft.core.component.predicates;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public interface DataComponentPredicate {
   Codec<Map<DataComponentPredicate.Type<?>, DataComponentPredicate>> CODEC = Codec.dispatchedMap(
      DataComponentPredicate.Type.CODEC, DataComponentPredicate.Type::codec
   );
   StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<?>> SINGLE_STREAM_CODEC = DataComponentPredicate.Type.STREAM_CODEC
      .dispatch(DataComponentPredicate.Single::type, DataComponentPredicate.Type::singleStreamCodec);
   StreamCodec<RegistryFriendlyByteBuf, Map<DataComponentPredicate.Type<?>, DataComponentPredicate>> STREAM_CODEC = SINGLE_STREAM_CODEC.apply(
         ByteBufCodecs.list(64)
      )
      .map(
         p_411079_ -> p_411079_.stream().collect(Collectors.toMap(DataComponentPredicate.Single::type, DataComponentPredicate.Single::predicate)),
         p_411095_ -> p_411095_.entrySet().stream().map(DataComponentPredicate.Single::fromEntry).toList()
      );

   static MapCodec<DataComponentPredicate.Single<?>> singleCodec(String p_399515_) {
      return DataComponentPredicate.Type.CODEC.dispatchMap(p_399515_, DataComponentPredicate.Single::type, DataComponentPredicate.Type::wrappedCodec);
   }

   boolean matches(DataComponentGetter var1);

   public static final class AnyValueType extends DataComponentPredicate.TypeBase<AnyValue> {
      private final AnyValue predicate;

      public AnyValueType(AnyValue p_456098_) {
         super(MapCodec.unitCodec(p_456098_));
         this.predicate = p_456098_;
      }

      public AnyValue predicate() {
         return this.predicate;
      }

      public DataComponentType<?> componentType() {
         return this.predicate.type();
      }

      public static DataComponentPredicate.AnyValueType create(DataComponentType<?> p_455765_) {
         return new DataComponentPredicate.AnyValueType(new AnyValue(p_455765_));
      }
   }

   public static final class ConcreteType<T extends DataComponentPredicate> extends DataComponentPredicate.TypeBase<T> {
      public ConcreteType(Codec<T> p_455920_) {
         super(p_455920_);
      }
   }

   public record Single<T extends DataComponentPredicate>(DataComponentPredicate.Type<T> type, T predicate) {
      static <T extends DataComponentPredicate> MapCodec<DataComponentPredicate.Single<T>> wrapCodec(
         DataComponentPredicate.Type<T> p_455881_, Codec<T> p_455076_
      ) {
         return RecordCodecBuilder.mapCodec(
            p_455907_ -> p_455907_.group(p_455076_.fieldOf("value").forGetter(DataComponentPredicate.Single::predicate))
               .apply(p_455907_, p_455247_ -> new DataComponentPredicate.Single<>(p_455881_, (T)p_455247_))
         );
      }

      private static <T extends DataComponentPredicate> DataComponentPredicate.Single<T> fromEntry(Entry<DataComponentPredicate.Type<?>, T> p_411082_) {
         return new DataComponentPredicate.Single<>((DataComponentPredicate.Type<T>)p_411082_.getKey(), p_411082_.getValue());
      }
   }

   public interface Type<T extends DataComponentPredicate> {
      Codec<DataComponentPredicate.Type<?>> CODEC = Codec.either(
            BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE.byNameCodec(), BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec()
         )
         .xmap(DataComponentPredicate.Type::copyOrCreateType, DataComponentPredicate.Type::unpackType);
      StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Type<?>> STREAM_CODEC = ByteBufCodecs.either(
            ByteBufCodecs.registry(Registries.DATA_COMPONENT_PREDICATE_TYPE), ByteBufCodecs.registry(Registries.DATA_COMPONENT_TYPE)
         )
         .map(DataComponentPredicate.Type::copyOrCreateType, DataComponentPredicate.Type::unpackType);

      private static <T extends DataComponentPredicate.Type<?>> Either<T, DataComponentType<?>> unpackType(T p_456290_) {
         return p_456290_ instanceof DataComponentPredicate.AnyValueType datacomponentpredicate$anyvaluetype
            ? Either.right(datacomponentpredicate$anyvaluetype.componentType())
            : Either.left(p_456290_);
      }

      private static DataComponentPredicate.Type<?> copyOrCreateType(Either<DataComponentPredicate.Type<?>, DataComponentType<?>> p_456228_) {
         return (DataComponentPredicate.Type<?>)p_456228_.map(p_454387_ -> p_454387_, DataComponentPredicate.AnyValueType::create);
      }

      Codec<T> codec();

      MapCodec<DataComponentPredicate.Single<T>> wrappedCodec();

      StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec();
   }

   public abstract static class TypeBase<T extends DataComponentPredicate> implements DataComponentPredicate.Type<T> {
      private final Codec<T> codec;
      private final MapCodec<DataComponentPredicate.Single<T>> wrappedCodec;
      private final StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec;

      public TypeBase(Codec<T> p_455169_) {
         this.codec = p_455169_;
         this.wrappedCodec = DataComponentPredicate.Single.wrapCodec(this, p_455169_);
         this.singleStreamCodec = ByteBufCodecs.<DataComponentPredicate>fromCodecWithRegistries(p_455169_)
            .map(p_455703_ -> new DataComponentPredicate.Single<>(this, (T)p_455703_), DataComponentPredicate.Single::predicate);
      }

      @Override
      public Codec<T> codec() {
         return this.codec;
      }

      @Override
      public MapCodec<DataComponentPredicate.Single<T>> wrappedCodec() {
         return this.wrappedCodec;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec() {
         return this.singleStreamCodec;
      }
   }
}
