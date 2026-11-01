package net.neoforged.neoforge.common.world.chunk;

import java.util.function.Consumer;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class RegisterTicketControllersEvent extends Event implements IModBusEvent {
   private final Consumer<TicketController> registrar;

   RegisterTicketControllersEvent(Consumer<TicketController> registrar) {
      this.registrar = registrar;
   }

   public void register(TicketController controller) {
      this.registrar.accept(controller);
   }
}
