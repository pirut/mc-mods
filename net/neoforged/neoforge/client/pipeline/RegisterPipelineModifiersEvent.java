package net.neoforged.neoforge.client.pipeline;

import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterPipelineModifiersEvent extends Event implements IModBusEvent {
   RegisterPipelineModifiersEvent() {
   }

   public void register(ResourceKey<PipelineModifier> key, PipelineModifier modifier) {
      if (PipelineModifiers.MODIFIERS.putIfAbsent(key, modifier) != null) {
         throw new IllegalStateException("Duplicate modifier registration for key " + key);
      }
   }
}
