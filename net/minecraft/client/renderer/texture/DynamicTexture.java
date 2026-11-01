package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class DynamicTexture extends AbstractTexture implements Dumpable {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   private NativeImage pixels;

   public DynamicTexture(Supplier<String> label, NativeImage pixels) {
      this.pixels = pixels;
      this.createTexture(label);
      this.upload();
   }

   public DynamicTexture(String label, int width, int height, boolean useCalloc) {
      this.pixels = new NativeImage(width, height, useCalloc);
      this.createTexture(label);
   }

   public DynamicTexture(Supplier<String> label, int width, int height, boolean useCalloc) {
      this.pixels = new NativeImage(width, height, useCalloc);
      this.createTexture(label);
   }

   private void createTexture(Supplier<String> label) {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.texture = gpuDevice.createTexture(label, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = gpuDevice.createTextureView(this.texture);
   }

   private void createTexture(String label) {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.texture = gpuDevice.createTexture(label, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = gpuDevice.createTextureView(this.texture);
   }

   public void upload() {
      if (this.pixels != null && this.texture != null) {
         RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels);
      } else {
         LOGGER.warn("Trying to upload disposed texture {}", this.getTexture().getLabel());
      }
   }

   @Nullable
   public NativeImage getPixels() {
      return this.pixels;
   }

   public void setPixels(NativeImage pixels) {
      if (this.pixels != null) {
         this.pixels.close();
      }

      this.pixels = pixels;
   }

   @Override
   public void close() {
      if (this.pixels != null) {
         this.pixels.close();
         this.pixels = null;
      }

      super.close();
   }

   @Override
   public void dumpContents(Identifier resourceLocation, Path path) throws IOException {
      if (this.pixels != null) {
         String string = resourceLocation.toDebugFileName() + ".png";
         Path path2 = path.resolve(string);
         this.pixels.writeToFile(path2);
      }
   }
}
