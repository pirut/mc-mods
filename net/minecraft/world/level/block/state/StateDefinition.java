package net.minecraft.world.level.block.state;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

public class StateDefinition<O, S extends StateHolder<O, S>> {
   static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
   private final O owner;
   private final ImmutableSortedMap<String, Property<?>> propertiesByName;
   private final ImmutableList<S> states;

   protected StateDefinition(Function<O, S> p_61052_, O p_61053_, StateDefinition.Factory<O, S> p_61054_, Map<String, Property<?>> p_61055_) {
      this.owner = p_61053_;
      this.propertiesByName = ImmutableSortedMap.copyOf(p_61055_);
      Supplier<S> $$4 = () -> p_61052_.apply(p_61053_);
      MapCodec<S> $$5 = MapCodec.of(Encoder.empty(), Decoder.unit($$4));
      UnmodifiableIterator $$7 = this.propertiesByName.entrySet().iterator();

      while ($$7.hasNext()) {
         Entry<String, Property<?>> $$6 = (Entry<String, Property<?>>)$$7.next();
         $$5 = appendPropertyCodec($$5, $$4, $$6.getKey(), $$6.getValue());
      }

      MapCodec<S> $$7x = $$5;
      Map<Map<Property<?>, Comparable<?>>, S> $$8 = Maps.newLinkedHashMap();
      List<S> $$9 = Lists.newArrayList();
      Stream<List<Pair<Property<?>, Comparable<?>>>> $$10 = Stream.of(Collections.emptyList());
      UnmodifiableIterator var11 = this.propertiesByName.values().iterator();

      while (var11.hasNext()) {
         Property<?> $$11 = (Property<?>)var11.next();
         $$10 = $$10.flatMap(p_61072_ -> $$11.getPossibleValues().stream().map(p_155961_ -> {
            List<Pair<Property<?>, Comparable<?>>> $$3 = Lists.newArrayList(p_61072_);
            $$3.add(Pair.of($$11, p_155961_));
            return $$3;
         }));
      }

      $$10.forEach(p_325887_ -> {
         Reference2ObjectArrayMap<Property<?>, Comparable<?>> $$6 = new Reference2ObjectArrayMap(p_325887_.size());

         for (Pair<Property<?>, Comparable<?>> $$7xx : p_325887_) {
            $$6.put((Property)$$7xx.getFirst(), (Comparable)$$7xx.getSecond());
         }

         S $$8x = p_61054_.create(p_61053_, $$6, $$7);
         $$8.put($$6, $$8x);
         $$9.add($$8x);
      });

      for (S $$12 : $$9) {
         $$12.populateNeighbours($$8);
      }

      this.states = ImmutableList.copyOf($$9);
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> MapCodec<S> appendPropertyCodec(
      MapCodec<S> p_61077_, Supplier<S> p_61078_, String p_61079_, Property<T> p_61080_
   ) {
      return Codec.mapPair(p_61077_, p_61080_.valueCodec().fieldOf(p_61079_).orElseGet(p_187541_ -> {}, () -> p_61080_.value(p_61078_.get())))
         .xmap(
            p_187536_ -> (StateHolder)((StateHolder)p_187536_.getFirst()).setValue(p_61080_, ((Property.Value)p_187536_.getSecond()).value()),
            p_187533_ -> Pair.of(p_187533_, p_61080_.value(p_187533_))
         );
   }

   public ImmutableList<S> getPossibleStates() {
      return this.states;
   }

   public S any() {
      return (S)this.states.get(0);
   }

   public O getOwner() {
      return this.owner;
   }

   public Collection<Property<?>> getProperties() {
      return this.propertiesByName.values();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
         .add("block", this.owner)
         .add("properties", this.propertiesByName.values().stream().map(Property::getName).collect(Collectors.toList()))
         .toString();
   }

   @Nullable
   public Property<?> getProperty(String p_61082_) {
      return (Property<?>)this.propertiesByName.get(p_61082_);
   }

   public static class Builder<O, S extends StateHolder<O, S>> {
      private final O owner;
      private final Map<String, Property<?>> properties = Maps.newHashMap();

      public Builder(O p_61098_) {
         this.owner = p_61098_;
      }

      public StateDefinition.Builder<O, S> add(Property<?>... p_61105_) {
         for (Property<?> $$1 : p_61105_) {
            this.validateProperty($$1);
            this.properties.put($$1.getName(), $$1);
         }

         return this;
      }

      private <T extends Comparable<T>> void validateProperty(Property<T> p_61100_) {
         String $$1 = p_61100_.getName();
         if (!StateDefinition.NAME_PATTERN.matcher($$1).matches()) {
            throw new IllegalArgumentException(this.owner + " has invalidly named property: " + $$1);
         } else {
            Collection<T> $$2 = p_61100_.getPossibleValues();
            if ($$2.size() <= 1) {
               throw new IllegalArgumentException(this.owner + " attempted use property " + $$1 + " with <= 1 possible values");
            } else {
               for (T $$3 : $$2) {
                  String $$4 = p_61100_.getName($$3);
                  if (!StateDefinition.NAME_PATTERN.matcher($$4).matches()) {
                     throw new IllegalArgumentException(this.owner + " has property: " + $$1 + " with invalidly named value: " + $$4);
                  }
               }

               if (this.properties.containsKey($$1)) {
                  throw new IllegalArgumentException(this.owner + " has duplicate property: " + $$1);
               }
            }
         }
      }

      public StateDefinition<O, S> create(Function<O, S> p_61102_, StateDefinition.Factory<O, S> p_61103_) {
         return new StateDefinition<>(p_61102_, this.owner, p_61103_, this.properties);
      }
   }

   public interface Factory<O, S> {
      S create(O var1, Reference2ObjectArrayMap<Property<?>, Comparable<?>> var2, MapCodec<S> var3);
   }
}
