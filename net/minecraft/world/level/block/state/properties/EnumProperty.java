package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.util.StringRepresentable;

public final class EnumProperty<T extends Enum<T> & StringRepresentable> extends Property<T> {
   private final List<T> values;
   private final Map<String, T> names;
   private final int[] ordinalToIndex;

   private EnumProperty(String name, Class<T> clazz, List<T> values) {
      super(name, clazz);
      if (values.isEmpty()) {
         throw new IllegalArgumentException("Trying to make empty EnumProperty '" + name + "'");
      } else {
         this.values = List.copyOf(values);
         T[] enums = clazz.getEnumConstants();
         this.ordinalToIndex = new int[enums.length];

         for (T enum_ : enums) {
            this.ordinalToIndex[enum_.ordinal()] = values.indexOf(enum_);
         }

         Builder<String, T> builder = ImmutableMap.builder();

         for (T enum2 : values) {
            String string = enum2.getSerializedName();
            builder.put(string, enum2);
         }

         this.names = builder.buildOrThrow();
      }
   }

   @Override
   public List<T> getPossibleValues() {
      return this.values;
   }

   @Override
   public Optional<T> getValue(String value) {
      return Optional.ofNullable(this.names.get(value));
   }

   public String getName(T value) {
      return value.getSerializedName();
   }

   public int getInternalIndex(T enum_) {
      return this.ordinalToIndex[enum_.ordinal()];
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else {
         return object instanceof EnumProperty<?> enumProperty && super.equals(object) ? this.values.equals(enumProperty.values) : false;
      }
   }

   @Override
   public int generateHashCode() {
      int i = super.generateHashCode();
      return 31 * i + this.values.hashCode();
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz) {
      return create(name, clazz, enum_ -> true);
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, Predicate<T> filter) {
      return create(name, clazz, Arrays.<T>stream(clazz.getEnumConstants()).filter(filter).collect(Collectors.toList()));
   }

   @SafeVarargs
   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, T... values) {
      return create(name, clazz, List.of(values));
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, List<T> values) {
      return new EnumProperty<>(name, clazz, values);
   }
}
