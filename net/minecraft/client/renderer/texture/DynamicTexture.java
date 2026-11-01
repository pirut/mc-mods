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
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DynamicTexture extends AbstractTexture implements Dumpable {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   private NativeImage pixels;

   public DynamicTexture(Supplier<String> p_404638_, NativeImage p_404902_) {
      this.pixels = p_404902_;
      this.createTexture(p_404638_);
      this.upload();
   }

   public DynamicTexture(String p_405039_, int p_405248_, int p_405417_, boolean p_405707_) {
      this.pixels = new NativeImage(p_405248_, p_405417_, p_405707_);
      this.createTexture(p_405039_);
   }

   public DynamicTexture(Supplier<String> p_405362_, int p_404897_, int p_404881_, boolean p_405310_) {
      this.pixels = new NativeImage(p_404897_, p_404881_, p_405310_);
      this.createTexture(p_405362_);
   }

   private void createTexture(Supplier<String> p_418333_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.texture = gpudevice.createTexture(p_418333_, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = gpudevice.createTextureView(this.texture);
   }

   private void createTexture(String p_418300_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.texture = gpudevice.createTexture(p_418300_, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = gpudevice.createTextureView(this.texture);
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

   public void setPixels(NativeImage p_117989_) {
      if (this.pixels != null) {
         this.pixels.close();
      }

      this.pixels = p_117989_;
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
   public void dumpContents(Identifier p_468271_, Path p_276105_) throws IOException {
      if (this.pixels != null) {
         String s = p_468271_.toDebugFileName() + ".png";
         Path path = p_276105_.resolve(s);
         this.pixels.writeToFile(path);
      }
   }
}
