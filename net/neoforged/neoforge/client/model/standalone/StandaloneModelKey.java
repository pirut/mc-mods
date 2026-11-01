package net.neoforged.neoforge.client.model.standalone;

import net.minecraft.client.resources.model.ModelDebugName;

public final class StandaloneModelKey<T> {
   private final ModelDebugName name;

   public StandaloneModelKey(ModelDebugName name) {
      this.name = name;
   }

   public String getName() {
      return this.name.debugName();
   }

   @Override
   public String toString() {
      return "StandaloneModelKey[name=" + this.name.debugName() + "]";
   }
}
