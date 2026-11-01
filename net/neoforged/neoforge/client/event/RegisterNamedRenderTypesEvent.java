package net.neoforged.neoforge.client.event;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.RenderTypeGroup;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterNamedRenderTypesEvent extends Event implements IModBusEvent {
   private final Map<Identifier, RenderTypeGroup> renderTypes;

   @Internal
   public RegisterNamedRenderTypesEvent(Map<Identifier, RenderTypeGroup> renderTypes) {
      this.renderTypes = renderTypes;
   }

   public void register(Identifier key, ChunkSectionLayer chunkLayer, Function<Identifier, RenderType> entityRenderType) {
      Preconditions.checkArgument(!this.renderTypes.containsKey(key), "Render type already registered: " + key);
      this.renderTypes.put(key, new RenderTypeGroup(chunkLayer, entityRenderType));
   }
}
