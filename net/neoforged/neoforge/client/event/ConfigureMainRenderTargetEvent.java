package net.neoforged.neoforge.client.event;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class ConfigureMainRenderTargetEvent extends Event implements IModBusEvent {
   private boolean enableStencil;

   public boolean isStencilEnabled() {
      return this.enableStencil;
   }

   public void enableStencil() {
      this.enableStencil = true;
   }
}
