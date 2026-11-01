package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelState;

public interface ModelStateExtension {
   private ModelState self() {
      return (ModelState)this;
   }

   default boolean mayApplyArbitraryRotation() {
      ModelState self = this.self();
      return !(self instanceof BlockModelRotation) && !(self instanceof BlockModelRotation.WithUvLock);
   }
}
