package net.minecraft.advancements.criterion;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

public record StatePropertiesPredicate(List<StatePropertiesPredicate.PropertyMatcher> properties) {
   private static final Codec<List<StatePropertiesPredicate.PropertyMatcher>> PROPERTIES_CODEC = Codec.unboundedMap(
         Codec.STRING, StatePropertiesPredicate.ValueMatcher.CODEC
      )
      .xmap(
         p_467209_ -> p_467209_.entrySet()
            .stream()
            .map(
               p_469014_ -> new StatePropertiesPredicate.PropertyMatcher(
                  (String)p_469014_.getKey(), (StatePropertiesPredicate.ValueMatcher)p_469014_.getValue()
               )
            )
            .toList(),
         p_467388_ -> p_467388_.stream()
            .collect(Collectors.toMap(StatePropertiesPredicate.PropertyMatcher::name, StatePropertiesPredicate.PropertyMatcher::valueMatcher))
      );
   public static final Codec<StatePropertiesPredicate> CODEC = PROPERTIES_CODEC.xmap(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);
   public static final StreamCodec<ByteBuf, StatePropertiesPredicate> STREAM_CODEC = StatePropertiesPredicate.PropertyMatcher.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);

   public <S extends StateHolder<?, S>> boolean matches(StateDefinition<?, S> p_468532_, S p_467613_) {
      for (StatePropertiesPredicate.PropertyMatcher statepropertiespredicate$propertymatcher : this.properties) {
         if (!statepropertiespredicate$propertymatcher.match(p_468532_, p_467613_)) {
            return false;
         }
      }

      return true;
   }

   public boolean matches(BlockState p_467210_) {
      return this.matches(p_467210_.getBlock().getStateDefinition(), p_467210_);
   }

   public boolean matches(FluidState p_469682_) {
      return this.matches(p_469682_.getType().getStateDefinition(), p_469682_);
   }

   public Optional<String> checkState(StateDefinition<?, ?> p_467427_) {
      for (StatePropertiesPredicate.PropertyMatcher statepropertiespredicate$propertymatcher : this.properties) {
         Optional<String> optional = statepropertiespredicate$propertymatcher.checkState(p_467427_);
         if (optional.isPresent()) {
            return optional;
         }
      }

      return Optional.empty();
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<StatePropertiesPredicate.PropertyMatcher> matchers = ImmutableList.builder();

      private Builder() {
      }

      public static StatePropertiesPredicate.Builder properties() {
         return new StatePropertiesPredicate.Builder();
      }

      public StatePropertiesPredicate.Builder hasProperty(Property<?> p_468687_, String p_467604_) {
         this.matchers.add(new StatePropertiesPredicate.PropertyMatcher(p_468687_.getName(), new StatePropertiesPredicate.ExactMatcher(p_467604_)));
         return this;
      }

      public StatePropertiesPredicate.Builder hasProperty(Property<Integer> p_468233_, int p_468811_) {
         return this.hasProperty(p_468233_, Integer.toString(p_468811_));
      }

      public StatePropertiesPredicate.Builder hasProperty(Property<Boolean> p_467673_, boolean p_467120_) {
         return this.hasProperty(p_467673_, Boolean.toString(p_467120_));
      }

      public <T extends Comparable<T> & StringRepresentable> StatePropertiesPredicate.Builder hasProperty(Property<T> p_467734_, T p_469750_) {
         return this.hasProperty(p_467734_, p_469750_.getSerializedName());
      }

      public Optional<StatePropertiesPredicate> build() {
         return Optional.of(new StatePropertiesPredicate(this.matchers.build()));
      }
   }

   record ExactMatcher(String value) implements StatePropertiesPredicate.ValueMatcher {
      public static final Codec<StatePropertiesPredicate.ExactMatcher> CODEC = Codec.STRING
         .xmap(StatePropertiesPredicate.ExactMatcher::new, StatePropertiesPredicate.ExactMatcher::value);
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.ExactMatcher> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
         .map(StatePropertiesPredicate.ExactMatcher::new, StatePropertiesPredicate.ExactMatcher::value);

      @Override
      public <T extends Comparable<T>> boolean match(StateHolder<?, ?> p_467843_, Property<T> p_469792_) {
         T t = p_467843_.getValue(p_469792_);
         Optional<T> optional = p_469792_.getValue(this.value);
         return optional.isPresent() && t.compareTo(optional.get()) == 0;
      }
   }

   record PropertyMatcher(String name, StatePropertiesPredicate.ValueMatcher valueMatcher) {
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.PropertyMatcher> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         StatePropertiesPredicate.PropertyMatcher::name,
         StatePropertiesPredicate.ValueMatcher.STREAM_CODEC,
         StatePropertiesPredicate.PropertyMatcher::valueMatcher,
         StatePropertiesPredicate.PropertyMatcher::new
      );

      public <S extends StateHolder<?, S>> boolean match(StateDefinition<?, S> p_467975_, S p_469325_) {
         Property<?> property = p_467975_.getProperty(this.name);
         return property != null && this.valueMatcher.match(p_469325_, property);
      }

      public Optional<String> checkState(StateDefinition<?, ?> p_467489_) {
         Property<?> property = p_467489_.getProperty(this.name);
         return property != null ? Optional.empty() : Optional.of(this.name);
      }
   }

   record RangedMatcher(Optional<String> minValue, Optional<String> maxValue) implements StatePropertiesPredicate.ValueMatcher {
      public static final Codec<StatePropertiesPredicate.RangedMatcher> CODEC = RecordCodecBuilder.create(
         p_469497_ -> p_469497_.group(
               Codec.STRING.optionalFieldOf("min").forGetter(StatePropertiesPredicate.RangedMatcher::minValue),
               Codec.STRING.optionalFieldOf("max").forGetter(StatePropertiesPredicate.RangedMatcher::maxValue)
            )
            .apply(p_469497_, StatePropertiesPredicate.RangedMatcher::new)
      );
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.RangedMatcher> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
         StatePropertiesPredicate.RangedMatcher::minValue,
         ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
         StatePropertiesPredicate.RangedMatcher::maxValue,
         StatePropertiesPredicate.RangedMatcher::new
      );

      @Override
      public <T extends Comparable<T>> boolean match(StateHolder<?, ?> p_468581_, Property<T> p_468220_) {
         T t = p_468581_.getValue(p_468220_);
         if (this.minValue.isPresent()) {
            Optional<T> optional = p_468220_.getValue(this.minValue.get());
            if (optional.isEmpty() || t.compareTo(optional.get()) < 0) {
               return false;
            }
         }

         if (this.maxValue.isPresent()) {
            Optional<T> optional1 = p_468220_.getValue(this.maxValue.get());
            if (optional1.isEmpty() || t.compareTo(optional1.get()) > 0) {
               return false;
            }
         }

         return true;
      }
   }

   interface ValueMatcher {
      Codec<StatePropertiesPredicate.ValueMatcher> CODEC = Codec.either(
            StatePropertiesPredicate.ExactMatcher.CODEC, StatePropertiesPredicate.RangedMatcher.CODEC
         )
         .xmap(Either::unwrap, p_468677_ -> {
            if (p_468677_ instanceof StatePropertiesPredicate.ExactMatcher statepropertiespredicate$exactmatcher) {
               return Either.left(statepropertiespredicate$exactmatcher);
            } else if (p_468677_ instanceof StatePropertiesPredicate.RangedMatcher statepropertiespredicate$rangedmatcher) {
               return Either.right(statepropertiespredicate$rangedmatcher);
            } else {
               throw new UnsupportedOperationException();
            }
         });
      StreamCodec<ByteBuf, StatePropertiesPredicate.ValueMatcher> STREAM_CODEC = ByteBufCodecs.either(
            StatePropertiesPredicate.ExactMatcher.STREAM_CODEC, StatePropertiesPredicate.RangedMatcher.STREAM_CODEC
         )
         .map(Either::unwrap, p_467845_ -> {
            if (p_467845_ instanceof StatePropertiesPredicate.ExactMatcher statepropertiespredicate$exactmatcher) {
               return Either.left(statepropertiespredicate$exactmatcher);
            } else if (p_467845_ instanceof StatePropertiesPredicate.RangedMatcher statepropertiespredicate$rangedmatcher) {
               return Either.right(statepropertiespredicate$rangedmatcher);
            } else {
               throw new UnsupportedOperationException();
            }
         });

      <T extends Comparable<T>> boolean match(StateHolder<?, ?> var1, Property<T> var2);
   }
}
