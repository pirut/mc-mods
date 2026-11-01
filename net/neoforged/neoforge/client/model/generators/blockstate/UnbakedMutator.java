package net.neoforged.neoforge.client.model.generators.blockstate;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import org.jspecify.annotations.Nullable;

public final class UnbakedMutator {
   private final Map<Class<?>, UnbakedMutator.Handler<?>> handlers;

   private UnbakedMutator(Map<Class<?>, UnbakedMutator.Handler<?>> handlers) {
      this.handlers = Collections.unmodifiableMap(handlers);
   }

   public <T extends BlockStateModel.Unbaked> T apply(T unbaked) {
      UnbakedMutator.Handler<T> handler = this.getHandler(unbaked);
      if (handler == null) {
         throw new UnsupportedOperationException(
            "This unbaked transform cannot be applied to unbaked model " + unbaked.getClass() + ", it only supports: " + this.handlers.keySet()
         );
      } else {
         return handler.apply(unbaked);
      }
   }

   @Nullable
   private <T extends BlockStateModel.Unbaked> UnbakedMutator.Handler<T> getHandler(T unbaked) {
      return (UnbakedMutator.Handler<T>)this.handlers.get(unbaked.getClass());
   }

   public static UnbakedMutator.Builder builder() {
      return new UnbakedMutator.Builder();
   }

   public static final class Builder {
      private final Map<Class<?>, UnbakedMutator.Handler<?>> handlers = new IdentityHashMap<>();

      private Builder() {
      }

      public <T extends BlockStateModel.Unbaked> UnbakedMutator.Builder add(Class<T> supportedClass, UnaryOperator<T> operator) {
         if (this.handlers.containsKey(supportedClass)) {
            throw new IllegalStateException("There is already a mutator registered for " + supportedClass);
         } else {
            this.handlers.put(supportedClass, new UnbakedMutator.Handler<>(supportedClass, operator));
            return this;
         }
      }

      public UnbakedMutator build() {
         return new UnbakedMutator(this.handlers);
      }
   }

   private record Handler<T>(Class<T> supportedClass, UnaryOperator<T> operator) {
      public T apply(BlockStateModel.Unbaked unbaked) {
         return this.operator.apply(this.supportedClass.cast(unbaked));
      }
   }
}
