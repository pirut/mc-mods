package net.minecraft.util.context;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.util.Set;

public class ContextKeySet {
   public static final ContextKeySet EMPTY = new ContextKeySet.Builder().build();
   private final Set<ContextKey<?>> required;
   private final Set<ContextKey<?>> allowed;

   ContextKeySet(Set<ContextKey<?>> p_381057_, Set<ContextKey<?>> p_381141_) {
      this.required = Set.copyOf(p_381057_);
      this.allowed = Set.copyOf(Sets.union(p_381057_, p_381141_));
   }

   public Set<ContextKey<?>> required() {
      return this.required;
   }

   public Set<ContextKey<?>> allowed() {
      return this.allowed;
   }

   @Override
   public String toString() {
      return "["
         + Joiner.on(", ").join(this.allowed.stream().map(p_466444_ -> (this.required.contains(p_466444_) ? "!" : "") + p_466444_.name()).iterator())
         + "]";
   }

   public static class Builder {
      private final Set<ContextKey<?>> required = Sets.newIdentityHashSet();
      private final Set<ContextKey<?>> optional = Sets.newIdentityHashSet();

      public ContextKeySet.Builder required(ContextKey<?> p_380985_) {
         if (this.optional.contains(p_380985_)) {
            throw new IllegalArgumentException("Parameter " + p_380985_.name() + " is already optional");
         } else {
            this.required.add(p_380985_);
            return this;
         }
      }

      public ContextKeySet.Builder optional(ContextKey<?> p_381112_) {
         if (this.required.contains(p_381112_)) {
            throw new IllegalArgumentException("Parameter " + p_381112_.name() + " is already required");
         } else {
            this.optional.add(p_381112_);
            return this;
         }
      }

      public ContextKeySet build() {
         return new ContextKeySet(this.required, this.optional);
      }
   }
}
