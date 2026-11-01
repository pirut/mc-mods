package net.minecraft.core.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class DataComponentExactPredicate implements Predicate<DataComponentGetter> {
   public static final Codec<DataComponentExactPredicate> CODEC = DataComponentType.VALUE_MAP_CODEC
      .xmap(
         p_399796_ -> new DataComponentExactPredicate(p_399796_.entrySet().stream().map(TypedDataComponent::fromEntryUnchecked).collect(Collectors.toList())),
         p_399915_ -> p_399915_.expectedComponents
            .stream()
            .filter(p_399578_ -> !p_399578_.type().isTransient())
            .collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value))
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentExactPredicate> STREAM_CODEC = TypedDataComponent.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(DataComponentExactPredicate::new, p_399906_ -> p_399906_.expectedComponents);
   public static final DataComponentExactPredicate EMPTY = new DataComponentExactPredicate(List.of());
   private final List<TypedDataComponent<?>> expectedComponents;

   DataComponentExactPredicate(List<TypedDataComponent<?>> p_399476_) {
      this.expectedComponents = p_399476_;
   }

   public static DataComponentExactPredicate.Builder builder() {
      return new DataComponentExactPredicate.Builder();
   }

   public static <T> DataComponentExactPredicate expect(DataComponentType<T> p_399793_, T p_399502_) {
      return new DataComponentExactPredicate(List.of(new TypedDataComponent<>(p_399793_, p_399502_)));
   }

   public static DataComponentExactPredicate allOf(DataComponentMap p_400188_) {
      return new DataComponentExactPredicate(ImmutableList.copyOf(p_400188_));
   }

   public static DataComponentExactPredicate someOf(DataComponentMap p_400247_, DataComponentType<?>... p_399678_) {
      DataComponentExactPredicate.Builder datacomponentexactpredicate$builder = new DataComponentExactPredicate.Builder();

      for (DataComponentType<?> datacomponenttype : p_399678_) {
         TypedDataComponent<?> typeddatacomponent = p_400247_.getTyped(datacomponenttype);
         if (typeddatacomponent != null) {
            datacomponentexactpredicate$builder.expect(typeddatacomponent);
         }
      }

      return datacomponentexactpredicate$builder.build();
   }

   public boolean isEmpty() {
      return this.expectedComponents.isEmpty();
   }

   @Override
   public boolean equals(Object p_399699_) {
      return p_399699_ instanceof DataComponentExactPredicate datacomponentexactpredicate
         && this.expectedComponents.equals(datacomponentexactpredicate.expectedComponents);
   }

   @Override
   public int hashCode() {
      return this.expectedComponents.hashCode();
   }

   @Override
   public String toString() {
      return this.expectedComponents.toString();
   }

   public boolean test(DataComponentGetter p_400037_) {
      for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
         Object object = p_400037_.get(typeddatacomponent.type());
         if (!Objects.equals(typeddatacomponent.value(), object)) {
            return false;
         }
      }

      return true;
   }

   public boolean alwaysMatches() {
      return this.expectedComponents.isEmpty();
   }

   public DataComponentPatch asPatch() {
      DataComponentPatch.Builder datacomponentpatch$builder = DataComponentPatch.builder();

      for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
         datacomponentpatch$builder.set(typeddatacomponent);
      }

      return datacomponentpatch$builder.build();
   }

   public static class Builder {
      private final List<TypedDataComponent<?>> expectedComponents = new ArrayList<>();

      Builder() {
      }

      public <T> DataComponentExactPredicate.Builder expect(TypedDataComponent<T> p_400286_) {
         return this.expect(p_400286_.type(), p_400286_.value());
      }

      public <T> DataComponentExactPredicate.Builder expect(DataComponentType<? super T> p_400166_, T p_400039_) {
         for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
            if (typeddatacomponent.type() == p_400166_) {
               throw new IllegalArgumentException("Predicate already has component of type: '" + p_400166_ + "'");
            }
         }

         this.expectedComponents.add(new TypedDataComponent<>(p_400166_, p_400039_));
         return this;
      }

      public DataComponentExactPredicate build() {
         return new DataComponentExactPredicate(List.copyOf(this.expectedComponents));
      }
   }
}
