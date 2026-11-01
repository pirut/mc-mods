package net.minecraft.util.context;

import net.minecraft.resources.Identifier;

public class ContextKey<T> {
   private final Identifier name;

   public ContextKey(Identifier p_467667_) {
      this.name = p_467667_;
   }

   public static <T> ContextKey<T> vanilla(String p_381171_) {
      return new ContextKey<>(Identifier.withDefaultNamespace(p_381171_));
   }

   public Identifier name() {
      return this.name;
   }

   @Override
   public String toString() {
      return "<parameter " + this.name + ">";
   }
}
