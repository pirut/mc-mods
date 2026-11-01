package net.neoforged.neoforge.client.event;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterRangeSelectItemModelPropertyEvent extends Event implements IModBusEvent {
   private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends RangeSelectItemModelProperty>> idMapper;

   @Internal
   public RegisterRangeSelectItemModelPropertyEvent(ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends RangeSelectItemModelProperty>> idMapper) {
      this.idMapper = idMapper;
   }

   public void register(Identifier location, MapCodec<? extends RangeSelectItemModelProperty> source) {
      this.idMapper.put(location, source);
   }
}
