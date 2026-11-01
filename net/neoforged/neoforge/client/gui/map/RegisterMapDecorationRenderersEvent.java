package net.neoforged.neoforge.client.gui.map;

import java.util.Locale;
import java.util.Map;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class RegisterMapDecorationRenderersEvent extends Event implements IModBusEvent {
   private final Map<MapDecorationType, IMapDecorationRenderer> renderers;

   @Internal
   public RegisterMapDecorationRenderersEvent(Map<MapDecorationType, IMapDecorationRenderer> renderers) {
      this.renderers = renderers;
   }

   public void register(MapDecorationType type, IMapDecorationRenderer renderer) {
      IMapDecorationRenderer oldRenderer = this.renderers.put(type, renderer);
      if (oldRenderer != null) {
         throw new IllegalStateException(String.format(Locale.ROOT, "Duplicate renderer registration for %s (old: %s, new: %s)", type, oldRenderer, renderer));
      }
   }
}
