package net.minecraft.world.level.block.state;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class StateHolder<O, S> {
   public static final String NAME_TAG = "Name";
   public static final String PROPERTIES_TAG = "Properties";
   private static final Function<Entry<Property<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<Entry<Property<?>, Comparable<?>>, String>() {
      public String apply(@Nullable Entry<Property<?>, Comparable<?>> p_61155_) {
         if (p_61155_ == null) {
            return "<NULL>";
         } else {
            Property<?> $$1 = p_61155_.getKey();
            return $$1.getName() + "=" + this.getName($$1, p_61155_.getValue());
         }
      }

      private <T extends Comparable<T>> String getName(Property<T> p_61152_, Comparable<?> p_61153_) {
         return p_61152_.getName((T)p_61153_);
      }
   };
   protected final O owner;
   private final Reference2ObjectArrayMap<Property<?>, Comparable<?>> values;
   private Table<Property<?>, Comparable<?>, S> neighbours;
   protected final MapCodec<S> propertiesCodec;

   protected StateHolder(O p_61117_, Reference2ObjectArrayMap<Property<?>, Comparable<?>> p_326342_, MapCodec<S> p_61119_) {
      this.owner = p_61117_;
      this.values = p_326342_;
      this.propertiesCodec = p_61119_;
   }

   public <T extends Comparable<T>> S cycle(Property<T> p_61123_) {
      return this.setValue(p_61123_, findNextInCollection(p_61123_.getPossibleValues(), this.getValue(p_61123_)));
   }

   protected static <T> T findNextInCollection(Collection<T> p_61131_, T p_61132_) {
      Iterator<T> $$2 = p_61131_.iterator();

      while ($$2.hasNext()) {
         if ($$2.next().equals(p_61132_)) {
            if ($$2.hasNext()) {
               return $$2.next();
            }

            return p_61131_.iterator().next();
         }
      }

      return $$2.next();
   }

   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder();
      $$0.append(this.owner);
      if (!this.getValues().isEmpty()) {
         $$0.append('[');
         $$0.append(this.getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
         $$0.append(']');
      }

      return $$0.toString();
   }

   public Collection<Property<?>> getProperties() {
      return Collections.unmodifiableCollection(this.values.keySet());
   }

   public <T extends Comparable<T>> boolean hasProperty(Property<T> p_61139_) {
      return this.values.containsKey(p_61139_);
   }

   public <T extends Comparable<T>> T getValue(Property<T> p_61144_) {
      Comparable<?> $$1 = (Comparable<?>)this.values.get(p_61144_);
      if ($$1 == null) {
         throw new IllegalArgumentException("Cannot get property " + p_61144_ + " as it does not exist in " + this.owner);
      } else {
         return p_61144_.getValueClass().cast($$1);
      }
   }

   public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> p_61146_) {
      Comparable<?> $$1 = (Comparable<?>)this.values.get(p_61146_);
      return $$1 == null ? Optional.empty() : Optional.of(p_61146_.getValueClass().cast($$1));
   }

   public <T extends Comparable<T>, V extends T> S setValue(Property<T> p_61125_, V p_61126_) {
      Comparable<?> $$2 = (Comparable<?>)this.values.get(p_61125_);
      if ($$2 == null) {
         throw new IllegalArgumentException("Cannot set property " + p_61125_ + " as it does not exist in " + this.owner);
      } else if ($$2.equals(p_61126_)) {
         return (S)this;
      } else {
         S $$3 = (S)this.neighbours.get(p_61125_, p_61126_);
         if ($$3 == null) {
            throw new IllegalArgumentException("Cannot set property " + p_61125_ + " to " + p_61126_ + " on " + this.owner + ", it is not an allowed value");
         } else {
            return $$3;
         }
      }
   }

   public <T extends Comparable<T>, V extends T> S trySetValue(Property<T> p_263324_, V p_263334_) {
      Comparable<?> $$2 = (Comparable<?>)this.values.get(p_263324_);
      if ($$2 != null && !$$2.equals(p_263334_)) {
         S $$3 = (S)this.neighbours.get(p_263324_, p_263334_);
         if ($$3 == null) {
            throw new IllegalArgumentException("Cannot set property " + p_263324_ + " to " + p_263334_ + " on " + this.owner + ", it is not an allowed value");
         } else {
            return $$3;
         }
      } else {
         return (S)this;
      }
   }

   public void populateNeighbours(Map<Map<Property<?>, Comparable<?>>, S> p_61134_) {
      if (this.neighbours != null) {
         throw new IllegalStateException();
      } else {
         Table<Property<?>, Comparable<?>, S> $$1 = HashBasedTable.create();
         ObjectIterator var3 = this.values.entrySet().iterator();

         while (var3.hasNext()) {
            Entry<Property<?>, Comparable<?>> $$2 = (Entry<Property<?>, Comparable<?>>)var3.next();
            Property<?> $$3 = $$2.getKey();

            for (Comparable<?> $$4 : $$3.getPossibleValues()) {
               if (!$$4.equals($$2.getValue())) {
                  $$1.put($$3, $$4, p_61134_.get(this.makeNeighbourValues($$3, $$4)));
               }
            }
         }

         this.neighbours = (Table<Property<?>, Comparable<?>, S>)($$1.isEmpty() ? $$1 : ArrayTable.create($$1));
      }
   }

   private Map<Property<?>, Comparable<?>> makeNeighbourValues(Property<?> p_61141_, Comparable<?> p_61142_) {
      Map<Property<?>, Comparable<?>> $$2 = new Reference2ObjectArrayMap(this.values);
      $$2.put(p_61141_, p_61142_);
      return $$2;
   }

   public Map<Property<?>, Comparable<?>> getValues() {
      return this.values;
   }

   protected static <O, S extends StateHolder<O, S>> Codec<S> codec(Codec<O> p_61128_, Function<O, S> p_61129_) {
      return p_61128_.dispatch(
         "Name",
         p_61121_ -> p_61121_.owner,
         p_338076_ -> {
            S $$2 = p_61129_.apply((O)p_338076_);
            return $$2.getValues().isEmpty()
               ? MapCodec.unit($$2)
               : $$2.propertiesCodec.codec().lenientOptionalFieldOf("Properties").xmap(p_187544_ -> p_187544_.orElse($$2), Optional::of);
         }
      );
   }
}
