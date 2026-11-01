package net.neoforged.neoforge.client.event;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterSpriteSourcesEvent extends Event implements IModBusEvent {
   private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpriteSource>> idMapper;

   @Internal
   public RegisterSpriteSourcesEvent(ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpriteSource>> idMapper) {
      this.idMapper = idMapper;
   }

   public void register(Identifier id, MapCodec<? extends SpriteSource> codec) {
      this.idMapper.put(id, codec);
   }
}
