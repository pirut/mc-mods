package net.neoforged.neoforge.client.event;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterMaterialAtlasesEvent extends Event implements IModBusEvent {
   private final Map<ResourceLocation, ResourceLocation> atlases;

   @Internal
   public RegisterMaterialAtlasesEvent(Map<ResourceLocation, ResourceLocation> atlases) {
      this.atlases = atlases;
   }

   public void register(ResourceLocation atlasLocation, ResourceLocation atlasInfoLocation) {
      ResourceLocation oldAtlasInfoLoc = this.atlases.putIfAbsent(atlasLocation, atlasInfoLocation);
      if (oldAtlasInfoLoc != null) {
         throw new IllegalStateException(
            String.format("Duplicate registration of atlas: %s (old info: %s, new info: %s)", atlasLocation, oldAtlasInfoLoc, atlasInfoLocation)
         );
      }
   }
}
