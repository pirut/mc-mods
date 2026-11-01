package net.minecraft.client.data.models.blockstates;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.state.properties.Property;

@Environment(EnvType.CLIENT)
public abstract class PropertyDispatch<V> {
   private final Map<PropertyValueList, V> values = new HashMap<>();

   protected void putValue(PropertyValueList properties, V value) {
      V object = this.values.put(properties, value);
      if (object != null) {
         throw new IllegalStateException("Value " + properties + " is already defined");
      }
   }

   Map<PropertyValueList, V> getEntries() {
      this.verifyComplete();
      return Map.copyOf(this.values);
   }

   private void verifyComplete() {
      List<Property<?>> list = this.getDefinedProperties();
      Stream<PropertyValueList> stream = Stream.of(PropertyValueList.EMPTY);

      for (Property<?> property : list) {
         stream = stream.flatMap(propertyValueList -> property.getAllValues().map(propertyValueList::extend));
      }

      List<PropertyValueList> list2 = stream.filter(propertyValueList -> !this.values.containsKey(propertyValueList)).toList();
      if (!list2.isEmpty()) {
         throw new IllegalStateException("Missing definition for properties: " + list2);
      }
   }

   abstract List<Property<?>> getDefinedProperties();

   public static <T1 extends Comparable<T1>> PropertyDispatch.C1<MultiVariant, T1> initial(Property<T1> property) {
      return new PropertyDispatch.C1<>(property);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<MultiVariant, T1, T2> initial(
      Property<T1> property1, Property<T2> property2
   ) {
      return new PropertyDispatch.C2<>(property1, property2);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<MultiVariant, T1, T2, T3> initial(
      Property<T1> property1, Property<T2> property2, Property<T3> property3
   ) {
      return new PropertyDispatch.C3<>(property1, property2, property3);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<MultiVariant, T1, T2, T3, T4> initial(
      Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4
   ) {
      return new PropertyDispatch.C4<>(property1, property2, property3, property4);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<MultiVariant, T1, T2, T3, T4, T5> initial(
      Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4, Property<T5> property5
   ) {
      return new PropertyDispatch.C5<>(property1, property2, property3, property4, property5);
   }

   public static <T1 extends Comparable<T1>> PropertyDispatch.C1<VariantMutator, T1> modify(Property<T1> property) {
      return new PropertyDispatch.C1<>(property);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<VariantMutator, T1, T2> modify(
      Property<T1> property1, Property<T2> property2
   ) {
      return new PropertyDispatch.C2<>(property1, property2);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<VariantMutator, T1, T2, T3> modify(
      Property<T1> property1, Property<T2> property2, Property<T3> property3
   ) {
      return new PropertyDispatch.C3<>(property1, property2, property3);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<VariantMutator, T1, T2, T3, T4> modify(
      Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4
   ) {
      return new PropertyDispatch.C4<>(property1, property2, property3, property4);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<VariantMutator, T1, T2, T3, T4, T5> modify(
      Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4, Property<T5> property5
   ) {
      return new PropertyDispatch.C5<>(property1, property2, property3, property4, property5);
   }

   @Environment(EnvType.CLIENT)
   public static class C1<V, T1 extends Comparable<T1>> extends PropertyDispatch<V> {
      private final Property<T1> property1;

      C1(Property<T1> property1) {
         this.property1 = property1;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1);
      }

      public PropertyDispatch.C1<V, T1> select(T1 property, V value) {
         PropertyValueList propertyValueList = PropertyValueList.of(this.property1.value(property));
         this.putValue(propertyValueList, value);
         return this;
      }

      public PropertyDispatch<V> generate(Function<T1, V> generator) {
         this.property1.getPossibleValues().forEach(comparable -> this.select((T1)comparable, generator.apply((T1)comparable)));
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class C2<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;

      C2(Property<T1> property1, Property<T2> property2) {
         this.property1 = property1;
         this.property2 = property2;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2);
      }

      public PropertyDispatch.C2<V, T1, T2> select(T1 property1, T2 property2, V value) {
         PropertyValueList propertyValueList = PropertyValueList.of(this.property1.value(property1), this.property2.value(property2));
         this.putValue(propertyValueList, value);
         return this;
      }

      public PropertyDispatch<V> generate(BiFunction<T1, T2, V> generator) {
         this.property1
            .getPossibleValues()
            .forEach(
               comparable -> this.property2
                  .getPossibleValues()
                  .forEach(comparable2 -> this.select((T1)comparable, (T2)comparable2, generator.apply((T1)comparable, (T2)comparable2)))
            );
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class C3<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;

      C3(Property<T1> property1, Property<T2> property2, Property<T3> property3) {
         this.property1 = property1;
         this.property2 = property2;
         this.property3 = property3;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3);
      }

      public PropertyDispatch.C3<V, T1, T2, T3> select(T1 property1, T2 property2, T3 property3, V value) {
         PropertyValueList propertyValueList = PropertyValueList.of(
            this.property1.value(property1), this.property2.value(property2), this.property3.value(property3)
         );
         this.putValue(propertyValueList, value);
         return this;
      }

      public PropertyDispatch<V> generate(Function3<T1, T2, T3, V> generator) {
         this.property1
            .getPossibleValues()
            .forEach(
               comparable -> this.property2
                  .getPossibleValues()
                  .forEach(
                     comparable2 -> this.property3
                        .getPossibleValues()
                        .forEach(
                           comparable3 -> this.select(
                              (T1)comparable, (T2)comparable2, (T3)comparable3, (V)generator.apply(comparable, comparable2, comparable3)
                           )
                        )
                  )
            );
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class C4<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>>
      extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;
      private final Property<T4> property4;

      C4(Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4) {
         this.property1 = property1;
         this.property2 = property2;
         this.property3 = property3;
         this.property4 = property4;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3, this.property4);
      }

      public PropertyDispatch.C4<V, T1, T2, T3, T4> select(T1 property1, T2 property2, T3 property3, T4 property4, V value) {
         PropertyValueList propertyValueList = PropertyValueList.of(
            this.property1.value(property1), this.property2.value(property2), this.property3.value(property3), this.property4.value(property4)
         );
         this.putValue(propertyValueList, value);
         return this;
      }

      public PropertyDispatch<V> generate(Function4<T1, T2, T3, T4, V> generator) {
         this.property1
            .getPossibleValues()
            .forEach(
               comparable -> this.property2
                  .getPossibleValues()
                  .forEach(
                     comparable2 -> this.property3
                        .getPossibleValues()
                        .forEach(
                           comparable3 -> this.property4
                              .getPossibleValues()
                              .forEach(
                                 comparable4 -> this.select(
                                    (T1)comparable,
                                    (T2)comparable2,
                                    (T3)comparable3,
                                    (T4)comparable4,
                                    (V)generator.apply(comparable, comparable2, comparable3, comparable4)
                                 )
                              )
                        )
                  )
            );
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class C5<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>>
      extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;
      private final Property<T4> property4;
      private final Property<T5> property5;

      C5(Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4, Property<T5> property5) {
         this.property1 = property1;
         this.property2 = property2;
         this.property3 = property3;
         this.property4 = property4;
         this.property5 = property5;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3, this.property4, this.property5);
      }

      public PropertyDispatch.C5<V, T1, T2, T3, T4, T5> select(T1 property1, T2 property2, T3 property3, T4 property4, T5 property5, V value) {
         PropertyValueList propertyValueList = PropertyValueList.of(
            this.property1.value(property1),
            this.property2.value(property2),
            this.property3.value(property3),
            this.property4.value(property4),
            this.property5.value(property5)
         );
         this.putValue(propertyValueList, value);
         return this;
      }

      public PropertyDispatch<V> generate(Function5<T1, T2, T3, T4, T5, V> generator) {
         this.property1
            .getPossibleValues()
            .forEach(
               comparable -> this.property2
                  .getPossibleValues()
                  .forEach(
                     comparable2 -> this.property3
                        .getPossibleValues()
                        .forEach(
                           comparable3 -> this.property4
                              .getPossibleValues()
                              .forEach(
                                 comparable4 -> this.property5
                                    .getPossibleValues()
                                    .forEach(
                                       comparable5 -> this.select(
                                          (T1)comparable,
                                          (T2)comparable2,
                                          (T3)comparable3,
                                          (T4)comparable4,
                                          (T5)comparable5,
                                          (V)generator.apply(comparable, comparable2, comparable3, comparable4, comparable5)
                                       )
                                    )
                              )
                        )
                  )
            );
         return this;
      }
   }
}
