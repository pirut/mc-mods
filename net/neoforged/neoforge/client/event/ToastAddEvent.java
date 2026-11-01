package net.neoforged.neoforge.client.event;

import net.minecraft.client.gui.components.toasts.Toast;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class ToastAddEvent extends Event implements ICancellableEvent {
   private final Toast toast;

   public ToastAddEvent(Toast toast) {
      this.toast = toast;
   }

   public Toast getToast() {
      return this.toast;
   }
}
