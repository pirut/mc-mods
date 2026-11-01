package net.neoforged.neoforge.client.event;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterConditionalItemModelPropertyEvent extends Event implements IModBusEvent {
   private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> idMapper;

   @Internal
   public RegisterConditionalItemModelPropertyEvent(ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> idMapper) {
      this.idMapper = idMapper;
   }

   public void register(Identifier location, MapCodec<? extends ConditionalItemModelProperty> source) {
      this.idMapper.put(location, source);
   }
}
