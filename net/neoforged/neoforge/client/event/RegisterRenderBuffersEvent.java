package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.SequencedMap;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterRenderBuffersEvent extends Event implements IModBusEvent {
   private final SequencedMap<RenderType, ByteBufferBuilder> renderBuffers;

   @Internal
   public RegisterRenderBuffersEvent(SequencedMap<RenderType, ByteBufferBuilder> renderBuffers) {
      this.renderBuffers = renderBuffers;
   }

   public void registerRenderBuffer(RenderType renderType) {
      this.registerRenderBuffer(renderType, new ByteBufferBuilder(renderType.bufferSize()));
   }

   public void registerRenderBuffer(RenderType renderType, ByteBufferBuilder renderBuffer) {
      if (this.renderBuffers.containsKey(renderType)) {
         throw new IllegalStateException("Duplicate attempt to register render buffer: " + renderType);
      } else {
         this.renderBuffers.put(renderType, renderBuffer);
      }
   }
}
