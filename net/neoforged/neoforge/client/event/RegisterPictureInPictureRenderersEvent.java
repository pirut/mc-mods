package net.neoforged.neoforge.client.event;

import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class RegisterPictureInPictureRenderersEvent extends Event implements IModBusEvent {
   private final List<PictureInPictureRendererRegistration<?>> renderers;

   @Internal
   public RegisterPictureInPictureRenderersEvent(List<PictureInPictureRendererRegistration<?>> renderers) {
      this.renderers = renderers;
   }

   public <T extends PictureInPictureRenderState> void register(
      Class<T> stateClass, Function<MultiBufferSource.BufferSource, PictureInPictureRenderer<T>> factory
   ) {
      this.renderers.add(new PictureInPictureRendererRegistration<>(stateClass, factory));
   }
}
