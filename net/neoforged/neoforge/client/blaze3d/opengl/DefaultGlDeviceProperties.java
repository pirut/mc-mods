package net.neoforged.neoforge.client.blaze3d.opengl;

import net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties;

public class DefaultGlDeviceProperties implements GpuDeviceProperties {
   @Override
   public String backendName() {
      return "Default";
   }

   @Override
   public String apiName() {
      return "OpenGL";
   }
}
