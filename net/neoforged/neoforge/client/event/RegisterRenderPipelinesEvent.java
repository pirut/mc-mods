package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.function.Consumer;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterRenderPipelinesEvent extends Event implements IModBusEvent {
   private final Consumer<RenderPipeline> registrar;

   @Internal
   public RegisterRenderPipelinesEvent(Consumer<RenderPipeline> registrar) {
      this.registrar = registrar;
   }

   public void registerPipeline(RenderPipeline pipeline) {
      this.registrar.accept(pipeline);
   }
}
