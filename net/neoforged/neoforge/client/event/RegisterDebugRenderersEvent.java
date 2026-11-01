package net.neoforged.neoforge.client.event;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class RegisterDebugRenderersEvent extends Event implements IModBusEvent {
   private final Consumer<Function<Minecraft, DebugRenderer.SimpleDebugRenderer>> registrar;

   @Internal
   public RegisterDebugRenderersEvent(Consumer<Function<Minecraft, DebugRenderer.SimpleDebugRenderer>> registrar) {
      this.registrar = registrar;
   }

   public void register(Function<Minecraft, DebugRenderer.SimpleDebugRenderer> factory) {
      this.registrar.accept(factory);
   }

   public void register(DebugRenderer.SimpleDebugRenderer renderer) {
      this.register(client -> renderer);
   }
}
