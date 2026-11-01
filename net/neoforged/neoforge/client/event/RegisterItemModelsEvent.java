package net.neoforged.neoforge.client.event;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterItemModelsEvent extends Event implements IModBusEvent {
   private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemModel.Unbaked>> idMapper;

   @Internal
   public RegisterItemModelsEvent(ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemModel.Unbaked>> idMapper) {
      this.idMapper = idMapper;
   }

   public void register(Identifier location, MapCodec<? extends ItemModel.Unbaked> source) {
      this.idMapper.put(location, source);
   }
}
