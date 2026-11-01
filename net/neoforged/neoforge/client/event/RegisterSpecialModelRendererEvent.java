package net.neoforged.neoforge.client.event;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterSpecialModelRendererEvent extends Event implements IModBusEvent {
   private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked>> idMapper;

   @Internal
   public RegisterSpecialModelRendererEvent(ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked>> idMapper) {
      this.idMapper = idMapper;
   }

   public void register(Identifier location, MapCodec<? extends SpecialModelRenderer.Unbaked> source) {
      this.idMapper.put(location, source);
   }
}
