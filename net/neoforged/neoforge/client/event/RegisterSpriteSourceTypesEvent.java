package net.neoforged.neoforge.client.event;

import com.google.common.collect.BiMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterSpriteSourceTypesEvent extends Event implements IModBusEvent {
   private final BiMap<ResourceLocation, SpriteSourceType> types;

   @Internal
   public RegisterSpriteSourceTypesEvent(BiMap<ResourceLocation, SpriteSourceType> types) {
      this.types = types;
   }

   @Deprecated(
      forRemoval = true,
      since = "1.21.1"
   )
   public SpriteSourceType register(ResourceLocation id, MapCodec<? extends SpriteSource> codec) {
      if (this.types.containsKey(id)) {
         throw new IllegalStateException("Duplicate sprite source type registration " + id);
      } else {
         SpriteSourceType sourceType = new SpriteSourceType(codec);
         this.register(id, sourceType);
         return sourceType;
      }
   }

   public void register(ResourceLocation id, SpriteSourceType sourceType) {
      if (this.types.containsKey(id)) {
         throw new IllegalStateException("Duplicate sprite source type registration " + id);
      } else {
         this.types.put(id, sourceType);
      }
   }
}
