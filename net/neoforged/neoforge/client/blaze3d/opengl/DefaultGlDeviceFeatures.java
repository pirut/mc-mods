package net.neoforged.neoforge.client.blaze3d.opengl;

import net.minecraft.util.Util;
import net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures;

public class DefaultGlDeviceFeatures implements GpuDeviceFeatures {
   @Override
   public boolean logicOp() {
      return Util.getPlatform() != Util.OS.WINDOWS || !Util.isAarch64();
   }
}
