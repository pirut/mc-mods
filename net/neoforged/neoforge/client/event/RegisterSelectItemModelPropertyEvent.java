package net.neoforged.neoforge.client.event;

import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterSelectItemModelPropertyEvent extends Event implements IModBusEvent {
   private final ExtraCodecs.LateBoundIdMapper<Identifier, SelectItemModelProperty.Type<?, ?>> idMapper;

   @Internal
   public RegisterSelectItemModelPropertyEvent(ExtraCodecs.LateBoundIdMapper<Identifier, SelectItemModelProperty.Type<?, ?>> idMapper) {
      this.idMapper = idMapper;
   }

   public void register(Identifier location, SelectItemModelProperty.Type<?, ?> source) {
      this.idMapper.put(location, source);
   }
}
