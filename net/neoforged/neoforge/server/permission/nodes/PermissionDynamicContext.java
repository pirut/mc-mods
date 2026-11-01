package net.neoforged.neoforge.server.permission.nodes;

import java.util.Objects;

public final class PermissionDynamicContext<T> {
   private PermissionDynamicContextKey<T> dynamic;
   private T value;

   PermissionDynamicContext(PermissionDynamicContextKey<T> dynamic, T value) {
      this.dynamic = dynamic;
      this.value = value;
   }

   public PermissionDynamicContextKey<T> getDynamic() {
      return this.dynamic;
   }

   public T getValue() {
      return this.value;
   }

   public String getSerializedValue() {
      return this.dynamic.serializer().apply(this.value);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof PermissionDynamicContext otherContext)
            ? false
            : this.dynamic.equals(otherContext.dynamic) && this.value.equals(otherContext.value);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.dynamic, this.value);
   }
}
