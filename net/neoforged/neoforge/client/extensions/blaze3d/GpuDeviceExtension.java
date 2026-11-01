package net.neoforged.neoforge.client.extensions.blaze3d;

import net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures;
import net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties;

public interface GpuDeviceExtension {
   GpuDeviceProperties deviceProperties();

   GpuDeviceFeatures enabledFeatures();
}
