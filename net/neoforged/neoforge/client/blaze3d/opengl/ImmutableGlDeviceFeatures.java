package net.neoforged.neoforge.client.blaze3d.opengl;

import net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ImmutableGlDeviceFeatures(boolean logicOp) implements GpuDeviceFeatures {
   public ImmutableGlDeviceFeatures(GpuDeviceFeatures features) {
      this(features.logicOp());
   }
}
