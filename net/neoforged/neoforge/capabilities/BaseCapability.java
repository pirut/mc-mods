package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.Identifier;

public abstract class BaseCapability<T, C> {
   private final Identifier name;
   private final Class<T> typeClass;
   private final Class<C> contextClass;

   protected BaseCapability(Identifier name, Class<T> typeClass, Class<C> contextClass) {
      this.name = name;
      this.typeClass = typeClass;
      this.contextClass = contextClass;
   }

   public final Identifier name() {
      return this.name;
   }

   public final Class<T> typeClass() {
      return this.typeClass;
   }

   public final Class<C> contextClass() {
      return this.contextClass;
   }
}
