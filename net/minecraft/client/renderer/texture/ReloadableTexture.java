package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ReloadableTexture extends AbstractTexture {
   private final Identifier resourceId;

   public ReloadableTexture(Identifier p_467297_) {
      this.resourceId = p_467297_;
   }

   public Identifier resourceId() {
      return this.resourceId;
   }

   public void apply(TextureContents p_389491_) {
      boolean flag = p_389491_.clamp();
      boolean flag1 = p_389491_.blur();
      AddressMode addressmode = flag ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT;
      FilterMode filtermode = flag1 ? FilterMode.LINEAR : FilterMode.NEAREST;
      this.sampler = RenderSystem.getSamplerCache().getSampler(addressmode, addressmode, filtermode, filtermode, false);

      try (NativeImage nativeimage = p_389491_.image()) {
         this.doLoad(nativeimage);
      }
   }

   protected void doLoad(NativeImage p_389473_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.close();
      this.texture = gpudevice.createTexture(this.resourceId::toString, 5, TextureFormat.RGBA8, p_389473_.getWidth(), p_389473_.getHeight(), 1, 1);
      this.textureView = gpudevice.createTextureView(this.texture);
      gpudevice.createCommandEncoder().writeToTexture(this.texture, p_389473_);
   }

   public abstract TextureContents loadContents(ResourceManager var1) throws IOException;
}
