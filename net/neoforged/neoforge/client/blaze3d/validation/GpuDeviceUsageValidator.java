package net.neoforged.neoforge.client.blaze3d.validation;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.LogicOp;
import net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures;
import net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties;

public class GpuDeviceUsageValidator {
   private final GpuDeviceProperties properties;
   private final GpuDeviceFeatures enabledFeatures;
   private final boolean checkReservedUsageBits;

   GpuDeviceUsageValidator(ValidationGpuDevice device, boolean checkReservedUsageBits) {
      this.properties = device.deviceProperties();
      this.enabledFeatures = device.enabledFeatures();
      this.checkReservedUsageBits = checkReservedUsageBits;
   }

   void validateBufferUsage(int usage) {
      if (this.checkReservedUsageBits && (usage & 0xFF000000) != 0) {
         throw new IllegalArgumentException("Use of reserved GpuBuffer usage bits with a backend that does not utilize them.");
      } else {
         usage &= 16777215;
         int knownGpuBufferBits = 511;
         if ((usage & ~knownGpuBufferBits) != 0) {
            throw new IllegalArgumentException("Use of undefined GpuBuffer usage bits");
         }
      }
   }

   void validateTextureUsage(int usage) {
      if (this.checkReservedUsageBits && (usage & 0xFF000000) != 0) {
         throw new IllegalArgumentException("Use of reserved GpuTexture usage bits with a backend that does not utilize them.");
      } else {
         usage &= 16777215;
         int knownGpuTextureBits = 31;
         if ((usage & ~knownGpuTextureBits) != 0) {
            throw new IllegalArgumentException("Use of undefined GpuTexture usage bits");
         }
      }
   }

   void validatePipeline(RenderPipeline pipeline) {
      if (pipeline.getColorLogic() != LogicOp.NONE && !this.enabledFeatures.logicOp()) {
         throw new IllegalArgumentException(
            String.format("Cannot use LogicOp other than NONE without enabling 'logicOp' device feature, %s", pipeline.getLocation())
         );
      }
   }
}
