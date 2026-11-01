package net.neoforged.neoforge.client.blaze3d.validation;

import com.mojang.blaze3d.textures.GpuTexture;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ValidationGpuTexture extends GpuTexture {
   private final GpuTexture realTexture;
   private final GpuDeviceUsageValidator validator;

   public ValidationGpuTexture(GpuTexture realTexture, GpuDeviceUsageValidator validator) {
      super(
         realTexture.usage(),
         realTexture.getLabel(),
         realTexture.getFormat(),
         realTexture.getWidth(0),
         realTexture.getHeight(0),
         realTexture.getDepthOrLayers(),
         realTexture.getMipLevels()
      );
      this.realTexture = realTexture;
      this.validator = validator;
   }

   @Internal
   public GpuTexture getRealTexture() {
      return this.realTexture;
   }

   @Override
   public void close() {
      this.realTexture.close();
   }

   @Override
   public boolean isClosed() {
      return this.realTexture.isClosed();
   }
}
