package net.neoforged.neoforge.client.blaze3d.opengl;

import net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ImmutableGlDeviceProperties(String backendName, String apiName) implements GpuDeviceProperties {
   public ImmutableGlDeviceProperties(GpuDeviceProperties properties) {
      this(properties.backendName(), properties.apiName());
   }
}
