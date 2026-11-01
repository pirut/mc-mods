package net.neoforged.neoforge.client.event;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class TextureAtlasStitchedEvent extends Event implements IModBusEvent {
   private final TextureAtlas atlas;

   @Internal
   public TextureAtlasStitchedEvent(TextureAtlas atlas) {
      this.atlas = atlas;
   }

   public TextureAtlas getAtlas() {
      return this.atlas;
   }
}
