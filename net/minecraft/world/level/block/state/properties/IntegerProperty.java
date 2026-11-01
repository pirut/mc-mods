package net.minecraft.world.level.block.state.properties;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public final class IntegerProperty extends Property<Integer> {
   private final IntImmutableList values;
   private final int min;
   private final int max;

   private IntegerProperty(String name, int min, int max) {
      super(name, Integer.class);
      if (min < 0) {
         throw new IllegalArgumentException("Min value of " + name + " must be 0 or greater");
      } else if (max <= min) {
         throw new IllegalArgumentException("Max value of " + name + " must be greater than min (" + min + ")");
      } else {
         this.min = min;
         this.max = max;
         this.values = IntImmutableList.toList(IntStream.range(min, max + 1));
      }
   }

   @Override
   public List<Integer> getPossibleValues() {
      return this.values;
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else {
         return object instanceof IntegerProperty integerProperty && super.equals(object) ? this.values.equals(integerProperty.values) : false;
      }
   }

   @Override
   public int generateHashCode() {
      return 31 * super.generateHashCode() + this.values.hashCode();
   }

   public static IntegerProperty create(String name, int min, int max) {
      return new IntegerProperty(name, min, max);
   }

   @Override
   public Optional<Integer> getValue(String value) {
      try {
         int i = Integer.parseInt(value);
         return i >= this.min && i <= this.max ? Optional.of(i) : Optional.empty();
      } catch (NumberFormatException var3) {
         return Optional.empty();
      }
   }

   public String getName(Integer value) {
      return value.toString();
   }

   public int getInternalIndex(Integer integer) {
      return integer <= this.max ? integer - this.min : -1;
   }
}
