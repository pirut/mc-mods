package net.neoforged.neoforge.client.event;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterEntitySpectatorShadersEvent extends Event implements IModBusEvent {
   private final Map<EntityType<?>, Identifier> shaders;

   @Internal
   public RegisterEntitySpectatorShadersEvent(Map<EntityType<?>, Identifier> shaders) {
      this.shaders = shaders;
   }

   public void register(EntityType<?> entityType, Identifier shader) {
      this.shaders.put(entityType, shader);
   }
}
