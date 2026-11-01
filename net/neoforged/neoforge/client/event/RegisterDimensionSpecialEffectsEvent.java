package net.neoforged.neoforge.client.event;

import java.util.Map;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterDimensionSpecialEffectsEvent extends Event implements IModBusEvent {
   private final Map<ResourceLocation, DimensionSpecialEffects> effects;

   @Internal
   public RegisterDimensionSpecialEffectsEvent(Map<ResourceLocation, DimensionSpecialEffects> effects) {
      this.effects = effects;
   }

   public void register(ResourceLocation dimensionType, DimensionSpecialEffects effects) {
      this.effects.put(dimensionType, effects);
   }
}
