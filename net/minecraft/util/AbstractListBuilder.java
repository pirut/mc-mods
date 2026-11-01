package net.minecraft.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import java.util.function.UnaryOperator;

abstract class AbstractListBuilder<T, B> implements ListBuilder<T> {
   private final DynamicOps<T> ops;
   protected DataResult<B> builder = DataResult.success(this.initBuilder(), Lifecycle.stable());

   protected AbstractListBuilder(DynamicOps<T> ops) {
      this.ops = ops;
   }

   public DynamicOps<T> ops() {
      return this.ops;
   }

   protected abstract B initBuilder();

   protected abstract B append(B builder, T value);

   protected abstract DataResult<T> build(B builder, T value);

   public ListBuilder<T> add(T object) {
      this.builder = this.builder.map(object2 -> this.append((B)object2, object));
      return this;
   }

   public ListBuilder<T> add(DataResult<T> dataResult) {
      this.builder = this.builder.apply2stable(this::append, dataResult);
      return this;
   }

   public ListBuilder<T> withErrorsFrom(DataResult<?> dataResult) {
      this.builder = this.builder.flatMap(object -> dataResult.map(object2 -> object));
      return this;
   }

   public ListBuilder<T> mapError(UnaryOperator<String> unaryOperator) {
      this.builder = this.builder.mapError(unaryOperator);
      return this;
   }

   public DataResult<T> build(T object) {
      DataResult<T> dataResult = this.builder.flatMap(object2 -> this.build((B)object2, object));
      this.builder = DataResult.success(this.initBuilder(), Lifecycle.stable());
      return dataResult;
   }
}
