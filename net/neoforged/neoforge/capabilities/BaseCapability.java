package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.ResourceLocation;

public abstract class BaseCapability<T, C> {
   private final ResourceLocation name;
   private final Class<T> typeClass;
   private final Class<C> contextClass;

   protected BaseCapability(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
      this.name = name;
      this.typeClass = typeClass;
      this.contextClass = contextClass;
   }

   public final ResourceLocation name() {
      return this.name;
   }

   public final Class<T> typeClass() {
      return this.typeClass;
   }

   public final Class<C> contextClass() {
      return this.contextClass;
   }
}
