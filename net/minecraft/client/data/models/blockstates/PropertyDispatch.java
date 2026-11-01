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
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;

@OnlyIn(Dist.CLIENT)
public abstract class PropertyDispatch<V> {
   private final Map<PropertyValueList, V> values = new HashMap<>();

   protected void putValue(PropertyValueList p_404906_, V p_405670_) {
      V v = this.values.put(p_404906_, p_405670_);
      if (v != null) {
         throw new IllegalStateException("Value " + p_404906_ + " is already defined");
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
         stream = stream.flatMap(p_405250_ -> property.getAllValues().map(p_405250_::extend));
      }

      List<PropertyValueList> list1 = stream.filter(p_405347_ -> !this.values.containsKey(p_405347_)).toList();
      if (!list1.isEmpty()) {
         throw new IllegalStateException("Missing definition for properties: " + list1);
      }
   }

   abstract List<Property<?>> getDefinedProperties();

   public static <T1 extends Comparable<T1>> PropertyDispatch.C1<MultiVariant, T1> initial(Property<T1> p_405379_) {
      return new PropertyDispatch.C1<>(p_405379_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<MultiVariant, T1, T2> initial(
      Property<T1> p_387480_, Property<T2> p_387328_
   ) {
      return new PropertyDispatch.C2<>(p_387480_, p_387328_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<MultiVariant, T1, T2, T3> initial(
      Property<T1> p_405312_, Property<T2> p_405357_, Property<T3> p_405599_
   ) {
      return new PropertyDispatch.C3<>(p_405312_, p_405357_, p_405599_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<MultiVariant, T1, T2, T3, T4> initial(
      Property<T1> p_387365_, Property<T2> p_388534_, Property<T3> p_388376_, Property<T4> p_388082_
   ) {
      return new PropertyDispatch.C4<>(p_387365_, p_388534_, p_388376_, p_388082_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<MultiVariant, T1, T2, T3, T4, T5> initial(
      Property<T1> p_387985_, Property<T2> p_386573_, Property<T3> p_388732_, Property<T4> p_388219_, Property<T5> p_387978_
   ) {
      return new PropertyDispatch.C5<>(p_387985_, p_386573_, p_388732_, p_388219_, p_387978_);
   }

   public static <T1 extends Comparable<T1>> PropertyDispatch.C1<VariantMutator, T1> modify(Property<T1> p_386819_) {
      return new PropertyDispatch.C1<>(p_386819_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<VariantMutator, T1, T2> modify(
      Property<T1> p_405839_, Property<T2> p_405127_
   ) {
      return new PropertyDispatch.C2<>(p_405839_, p_405127_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<VariantMutator, T1, T2, T3> modify(
      Property<T1> p_386876_, Property<T2> p_387034_, Property<T3> p_387165_
   ) {
      return new PropertyDispatch.C3<>(p_386876_, p_387034_, p_387165_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<VariantMutator, T1, T2, T3, T4> modify(
      Property<T1> p_405364_, Property<T2> p_405658_, Property<T3> p_405625_, Property<T4> p_405082_
   ) {
      return new PropertyDispatch.C4<>(p_405364_, p_405658_, p_405625_, p_405082_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<VariantMutator, T1, T2, T3, T4, T5> modify(
      Property<T1> p_404784_, Property<T2> p_405605_, Property<T3> p_405623_, Property<T4> p_404668_, Property<T5> p_404790_
   ) {
      return new PropertyDispatch.C5<>(p_404784_, p_405605_, p_405623_, p_404668_, p_404790_);
   }

   public static <T1 extends Comparable<T1>> PropertyDispatch.C1<UnbakedMutator, T1> modifyUnbaked(Property<T1> p1) {
      return new PropertyDispatch.C1<>(p1);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<UnbakedMutator, T1, T2> modifyUnbaked(
      Property<T1> p1, Property<T2> p2
   ) {
      return new PropertyDispatch.C2<>(p1, p2);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<UnbakedMutator, T1, T2, T3> modifyUnbaked(
      Property<T1> p1, Property<T2> p2, Property<T3> p3
   ) {
      return new PropertyDispatch.C3<>(p1, p2, p3);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<UnbakedMutator, T1, T2, T3, T4> modifyUnbaked(
      Property<T1> p1, Property<T2> p2, Property<T3> p3, Property<T4> p4
   ) {
      return new PropertyDispatch.C4<>(p1, p2, p3, p4);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<UnbakedMutator, T1, T2, T3, T4, T5> modifyUnbaked(
      Property<T1> p1, Property<T2> p2, Property<T3> p3, Property<T4> p4, Property<T5> p5
   ) {
      return new PropertyDispatch.C5<>(p1, p2, p3, p4, p5);
   }

   @OnlyIn(Dist.CLIENT)
   public static class C1<V, T1 extends Comparable<T1>> extends PropertyDispatch<V> {
      private final Property<T1> property1;

      C1(Property<T1> p_386736_) {
         this.property1 = p_386736_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1);
      }

      public PropertyDispatch.C1<V, T1> select(T1 p_388287_, V p_405673_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(this.property1.value(p_388287_));
         this.putValue(propertyvaluelist, p_405673_);
         return this;
      }

      public PropertyDispatch<V> generate(Function<T1, V> p_387942_) {
         this.property1.getPossibleValues().forEach(p_403957_ -> this.select((T1)p_403957_, p_387942_.apply((T1)p_403957_)));
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class C2<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;

      C2(Property<T1> p_387463_, Property<T2> p_388076_) {
         this.property1 = p_387463_;
         this.property2 = p_388076_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2);
      }

      public PropertyDispatch.C2<V, T1, T2> select(T1 p_386621_, T2 p_388046_, V p_404740_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(this.property1.value(p_386621_), this.property2.value(p_388046_));
         this.putValue(propertyvaluelist, p_404740_);
         return this;
      }

      public PropertyDispatch<V> generate(BiFunction<T1, T2, V> p_386812_) {
         this.property1
            .getPossibleValues()
            .forEach(
               p_388697_ -> this.property2
                  .getPossibleValues()
                  .forEach(p_403960_ -> this.select((T1)p_388697_, (T2)p_403960_, p_386812_.apply((T1)p_388697_, (T2)p_403960_)))
            );
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class C3<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;

      C3(Property<T1> p_387044_, Property<T2> p_388481_, Property<T3> p_386589_) {
         this.property1 = p_387044_;
         this.property2 = p_388481_;
         this.property3 = p_386589_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3);
      }

      public PropertyDispatch.C3<V, T1, T2, T3> select(T1 p_387229_, T2 p_387080_, T3 p_388837_, V p_405130_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(
            this.property1.value(p_387229_), this.property2.value(p_387080_), this.property3.value(p_388837_)
         );
         this.putValue(propertyvaluelist, p_405130_);
         return this;
      }

      public PropertyDispatch<V> generate(Function3<T1, T2, T3, V> p_405032_) {
         this.property1
            .getPossibleValues()
            .forEach(
               p_388004_ -> this.property2
                  .getPossibleValues()
                  .forEach(
                     p_387043_ -> this.property3
                        .getPossibleValues()
                        .forEach(p_403964_ -> this.select((T1)p_388004_, (T2)p_387043_, (T3)p_403964_, (V)p_405032_.apply(p_388004_, p_387043_, p_403964_)))
                  )
            );
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class C4<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>>
      extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;
      private final Property<T4> property4;

      C4(Property<T1> p_387540_, Property<T2> p_386832_, Property<T3> p_387679_, Property<T4> p_387920_) {
         this.property1 = p_387540_;
         this.property2 = p_386832_;
         this.property3 = p_387679_;
         this.property4 = p_387920_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3, this.property4);
      }

      public PropertyDispatch.C4<V, T1, T2, T3, T4> select(T1 p_387500_, T2 p_386632_, T3 p_386444_, T4 p_386485_, V p_405540_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(
            this.property1.value(p_387500_), this.property2.value(p_386632_), this.property3.value(p_386444_), this.property4.value(p_386485_)
         );
         this.putValue(propertyvaluelist, p_405540_);
         return this;
      }

      public PropertyDispatch<V> generate(Function4<T1, T2, T3, T4, V> p_405663_) {
         this.property1
            .getPossibleValues()
            .forEach(
               p_386818_ -> this.property2
                  .getPossibleValues()
                  .forEach(
                     p_388830_ -> this.property3
                        .getPossibleValues()
                        .forEach(
                           p_387951_ -> this.property4
                              .getPossibleValues()
                              .forEach(
                                 p_403969_ -> this.select(
                                    (T1)p_386818_, (T2)p_388830_, (T3)p_387951_, (T4)p_403969_, (V)p_405663_.apply(p_386818_, p_388830_, p_387951_, p_403969_)
                                 )
                              )
                        )
                  )
            );
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class C5<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>>
      extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;
      private final Property<T4> property4;
      private final Property<T5> property5;

      C5(Property<T1> p_386746_, Property<T2> p_388273_, Property<T3> p_386988_, Property<T4> p_386613_, Property<T5> p_386493_) {
         this.property1 = p_386746_;
         this.property2 = p_388273_;
         this.property3 = p_386988_;
         this.property4 = p_386613_;
         this.property5 = p_386493_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3, this.property4, this.property5);
      }

      public PropertyDispatch.C5<V, T1, T2, T3, T4, T5> select(T1 p_388392_, T2 p_386456_, T3 p_386640_, T4 p_386474_, T5 p_388406_, V p_405046_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(
            this.property1.value(p_388392_),
            this.property2.value(p_386456_),
            this.property3.value(p_386640_),
            this.property4.value(p_386474_),
            this.property5.value(p_388406_)
         );
         this.putValue(propertyvaluelist, p_405046_);
         return this;
      }

      public PropertyDispatch<V> generate(Function5<T1, T2, T3, T4, T5, V> p_405294_) {
         this.property1
            .getPossibleValues()
            .forEach(
               p_388135_ -> this.property2
                  .getPossibleValues()
                  .forEach(
                     p_388174_ -> this.property3
                        .getPossibleValues()
                        .forEach(
                           p_387752_ -> this.property4
                              .getPossibleValues()
                              .forEach(
                                 p_388362_ -> this.property5
                                    .getPossibleValues()
                                    .forEach(
                                       p_403975_ -> this.select(
                                          (T1)p_388135_,
                                          (T2)p_388174_,
                                          (T3)p_387752_,
                                          (T4)p_388362_,
                                          (T5)p_403975_,
                                          (V)p_405294_.apply(p_388135_, p_388174_, p_387752_, p_388362_, p_403975_)
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
