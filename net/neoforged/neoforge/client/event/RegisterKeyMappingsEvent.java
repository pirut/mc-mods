package net.neoforged.neoforge.client.event;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterKeyMappingsEvent extends Event implements IModBusEvent {
   private final Options options;

   @Internal
   public RegisterKeyMappingsEvent(Options options) {
      this.options = options;
   }

   public void register(KeyMapping key) {
      this.options.keyMappings = (KeyMapping[])ArrayUtils.add(this.options.keyMappings, key);
   }
}
